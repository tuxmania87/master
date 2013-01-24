import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZFromItem;
import org.gibello.zql.ZQuery;
import org.gibello.zql.ZStatement;
import org.gibello.zql.ZqlParser;


public class QueryUtils {
	public static String orderList[] = { "<=", ">=", "=", "IS NULL",
			"IS NOT NULL", "OR", "AND" };
	
	public static boolean isTableInFromClause(String table,
			Vector<ZFromItem> fromClause) {
		Iterator<ZFromItem> it = fromClause.iterator();
		while (it.hasNext()) {
			ZFromItem z = it.next();
			if (table.equals(z.getTable()))
				return true;
		}
		return false;
	}

	public static String getFlippedOperator(String op) {
		if (op.equals(">=")) {
			return "<=";
		}
		if (op.equals("<=")) {
			return ">=";
		}
		if (op.equals(">")) {
			return "<";
		}
		if (op.equals("<")) {
			return ">";
		}
		return op;
	}

	public static String getOppositeOperator(String op) {
		if (op.equals(">=")) {
			return "<";
		}
		if (op.equals("<=")) {
			return ">";
		}
		if (op.equals(">")) {
			return "<=";
		}
		if (op.equals("<")) {
			return ">=";
		}
		if (op.equals("=")) {
			return "<>";
		}
		if (op.equals("<>")) {
			return "=";
		}
		if (op.equals("AND")) {
			return "OR";
		}
		if (op.equals("OR")) {
			return "AND";
		}
		if (op.equals("IS NOT NULL")) {
			return "IS NULL";
		}
		if (op.equals("IS NULL")) {
			return "IS NOT NULL";
		}
		return op;
	}

	public static ZExp negate(ZExp exp) {
		if (exp instanceof ZConstant) {
			return ((ZConstant) exp);
		}

		ZExpression casted = (ZExpression) exp;

		ZExpression result = new ZExpression(
				getOppositeOperator(casted.getOperator()));
		Iterator it = casted.getOperands().iterator();
		while (it.hasNext()) {
			result.addOperand(negate((ZExp) it.next()));
		}
		return result;
	}

	public static int indexOf(String needle) {
		for (int i = 0; i < orderList.length; i++)
			if (orderList[i].equals(needle))
				return i;
		return -1;
	}

	public static boolean isLesserThan(ZExp a, ZExp b) {
		if (b instanceof ZConstant)
			return true;
		if (a instanceof ZConstant)
			return false;

		// look for index of operator
		ZExpression t1 = (ZExpression) a;
		ZExpression t2 = (ZExpression) b;

		if (indexOf(t1.getOperator()) == indexOf(t2.getOperator())) {
			return t1.getOperand(0).toString()
					.compareTo(t2.getOperand(0).toString()) < 0;
		}

		return indexOf(t1.getOperator()) < indexOf(t2.getOperator());

	}

	public static ZExp sortedTree(ZExp exp) {
		if (exp instanceof ZConstant) {
			return (ZConstant) exp;
		}

		ZExpression t1 = (ZExpression) exp;

		Iterator it = t1.getOperands().iterator();

		int ix = 0;
		ZExp feld[] = new ZExp[t1.getOperands().size()];

		while (it.hasNext()) {
			feld[ix++] = (ZExp) it.next();
		}

		for (int i = 0; i < feld.length; i++) {
			for (int j = 0; j < feld.length - 1; j++) {
				if (isLesserThan(feld[j + 1], feld[j])) {
					ZExp help = feld[j + 1];
					feld[j + 1] = feld[j];
					feld[j] = help;
				}
			}
		}

		ZExpression result = new ZExpression(t1.getOperator());

		// eliminate duplicate entrys on same layer
		ArrayList<Integer> indexe = new ArrayList<Integer>();

		int i = 0;
		int zaehler = 0;
		String probant = null;

		while (i < feld.length) {
			if (zaehler == 0) {
				probant = feld[i].toString();
				zaehler++;
				indexe.add(i);
			} else {
				if (feld[i].toString().equals(probant)) {
					zaehler++;
				} else {
					probant = feld[i].toString();
					zaehler = 0;
					indexe.add(i);
				}
			}
			i++;
		}

		/*
		 * for(i=0; i< feld.length;i++) { result.addOperand(feld[i]); }
		 */

		it = indexe.iterator();
		while (it.hasNext()) {
			int ind = Integer.valueOf(it.next().toString());
			result.addOperand(feld[ind]);
		}

		return result;
	}

	
	public static ZExp operatorCompression(ZExp exp, ZExp parent) {

		if (exp instanceof ZConstant) {
			return (ZConstant) exp;
		}
		
		if(exp instanceof ZQuery) {
			return exp;
		}

		ZExpression parentExp = (ZExpression) parent;
		ZExpression casted = (ZExpression) exp;

		if (parent == null) {
			ZExpression z = new ZExpression(casted.getOperator());
			Iterator it = casted.getOperands().iterator();
			while (it.hasNext()) {
				ZExp Next = (ZExp) it.next();
				if (Next instanceof ZConstant) {
					z.addOperand(Next);
				} else if(Next instanceof ZQuery) {
					z.addOperand(Next);
				} else {
					ZExpression t1 = (ZExpression) operatorCompression((ZExp) Next, exp);
					if (t1 == null) {
						z.addOperand(Next);
					} else {
						Iterator it2 = t1.getOperands().iterator();
						while (it2.hasNext()) {
							z.addOperand((ZExp) it2.next());
						}
					}
				}
			}
			return z;
		}

		if (parentExp.getOperator().equals(casted.getOperator())) {

			Vector newVec = new Vector();

			// first attach all childs except current
			Iterator it = casted.getOperands().iterator();
			while (it.hasNext()) {
				ZExp Curr = (ZExp) it.next();
				ZExp help = operatorCompression((ZExp) Curr, exp);
				if (help == null) {
					newVec.add(Curr);
				} else {
					newVec.add(help);
				}

			}

			parentExp.setOperands(newVec);
			return parentExp;
		}

		return null;
	}

	public static ZExp dfs_orderPairs(ZExp exp) {
		// filter all pairs one layer before leaf layer
		// and sort them so that a ? b or a ? 4
		if (exp instanceof ZExpression) {
			ZExpression casted = (ZExpression) exp;
			if (casted.getOperands().size() == 2
					&& casted.getOperand(0) instanceof ZConstant
					&& casted.getOperand(1) instanceof ZConstant) {
				ZConstant arg0 = (ZConstant) casted.getOperand(0);
				ZConstant arg1 = (ZConstant) casted.getOperand(1);
				if (arg0.getType() == ZConstant.NUMBER
						&& arg1.getType() == ZConstant.COLUMNNAME) {
					ZExpression ret = new ZExpression(
							getFlippedOperator(casted.getOperator()));
					ret.addOperand(arg1);
					ret.addOperand(arg0);
					return ret;
				}
				if (arg0.getType() == ZConstant.COLUMNNAME
						&& arg1.getType() == ZConstant.COLUMNNAME
						&& arg1.toString().compareTo(arg0.toString()) < 0) {
					ZExpression ret = new ZExpression(
							getFlippedOperator(casted.getOperator()));
					ret.addOperand(arg1);
					ret.addOperand(arg0);
					return ret;
				}
				// TODO: könnte man zusammenfassen, da bei beiden das gleiche
				// gemacht wird. Sieht dann nur unübersichtlich auf, weil alles
				// in einer if
				return casted;
			}
			// ELSE, we dont have 2 ZConstant Childs, but we are in a
			// ZExpression
			ZExpression ret = new ZExpression(casted.getOperator());
			Iterator it = casted.getOperands().iterator();
			while (it.hasNext()) {
				ZExp arg = (ZExp) it.next();
				ret.addOperand(dfs_orderPairs(arg));
			}
			return ret;
		}

		return exp;

	}

	public static ZExp dfs_work(ZExp exp) {

		/*
		 * System.out.println("Expression: "+exp); if(exp instanceof
		 * ZExpression) System.out.println("ZExpression");
		 */
		if(exp instanceof ZQuery) {
			return exp;
		}
		
		if (exp instanceof ZConstant) {
			return ((ZConstant) exp);
		}

		String operator = null;
		ZExpression casted = null;
		try {
			operator = ((ZExpression) exp).getOperator();
			casted = (ZExpression) exp;
		} catch (Exception e) {
			operator = null;
			casted = null;
		}

		if (casted.getOperands().size() == 2
				&& casted.getOperand(0) instanceof ZConstant
				&& casted.getOperand(1) instanceof ZConstant) {

			ZConstant arg0 = (ZConstant) casted.getOperand(0);
			ZConstant arg1 = (ZConstant) casted.getOperand(1);

			if (arg0.getType() == ZConstant.NUMBER
					&& arg1.getType() == ZConstant.NUMBER) {
				int a = Integer.parseInt(arg0.toString());
				int b = Integer.parseInt(arg1.toString());
				if (operator.equals("+")) {
					return new ZConstant(String.valueOf(a + b),
							ZConstant.NUMBER);
				}
				if (operator.equals("-")) {
					return new ZConstant(String.valueOf(a - b),
							ZConstant.NUMBER);
				}
				if (operator.equals("*")) {
					return new ZConstant(String.valueOf(a * b),
							ZConstant.NUMBER);
				}
				if (operator.equals("/") && a % b == 0) {
					return new ZConstant(String.valueOf(a / b),
							ZConstant.NUMBER);
				}

			} else {
				if (operator.equals(">")) {
					ZConstant c1 = (ZConstant) casted.getOperand(0);
					ZConstant c2 = (ZConstant) casted.getOperand(1);

					if (c1.getType() == ZConstant.NUMBER
							&& c2.getType() != ZConstant.NUMBER) {
						ZExpression t1 = new ZExpression(">=");
						t1.addOperand(new ZConstant(String.valueOf(Integer
								.parseInt(c1.toString()) - 1), ZConstant.NUMBER));
						t1.addOperand(c2);
						return t1;
					}
					if (c2.getType() == ZConstant.NUMBER
							&& c1.getType() != ZConstant.NUMBER) {
						ZExpression t1 = new ZExpression(">=");
						t1.addOperand(c1);
						t1.addOperand(new ZConstant(String.valueOf(Integer
								.parseInt(c2.toString()) - 1), ZConstant.NUMBER));
						return t1;
					}
					// TODO
					if (c2.getType() == ZConstant.NUMBER
							&& c1.getType() == ZConstant.NUMBER) {
						// either false or true
					}
					if (c1.getType() == ZConstant.COLUMNNAME
							&& c2.getType() == ZConstant.COLUMNNAME) {
						ZExpression left = new ZExpression("-");
						left.addOperand(c1);
						left.addOperand(new ZConstant("1", ZConstant.NUMBER));
						ZExpression t1 = new ZExpression(">=");
						t1.addOperand(left);
						t1.addOperand(c2);
						return t1;
					}
				}
				if (operator.equals("<")) {
					ZConstant c1 = (ZConstant) casted.getOperand(0);
					ZConstant c2 = (ZConstant) casted.getOperand(1);

					if (c1.getType() == ZConstant.NUMBER
							&& c2.getType() != ZConstant.NUMBER) {
						ZExpression t1 = new ZExpression("<=");
						t1.addOperand(new ZConstant(String.valueOf(Integer
								.parseInt(c1.toString()) + 1), ZConstant.NUMBER));
						t1.addOperand(c2);
						return t1;
					}
					if (c2.getType() == ZConstant.NUMBER
							&& c1.getType() != ZConstant.NUMBER) {
						ZExpression t1 = new ZExpression("<=");
						t1.addOperand(c1);
						t1.addOperand(new ZConstant(String.valueOf(Integer
								.parseInt(c2.toString()) + 1), ZConstant.NUMBER));
						return t1;
					}
					// TODO
					if (c2.getType() == ZConstant.NUMBER
							&& c1.getType() == ZConstant.NUMBER) {
						// either false or true
					}
					if (c1.getType() == ZConstant.COLUMNNAME
							&& c2.getType() == ZConstant.COLUMNNAME) {
						ZExpression right = new ZExpression("-");
						right.addOperand(c2);
						right.addOperand(new ZConstant("1", ZConstant.NUMBER));
						ZExpression t1 = new ZExpression("<=");
						t1.addOperand(c1);
						t1.addOperand(right);
						return t1;
					}
				}
			}
		}

		if (operator != null && operator.equals("NOT")) {
			// System.out.println("Operand "+casted.getOperand(0));
			ZExpression t1 = (ZExpression) casted.getOperand(0);
			return negate(t1);
		}

		if (operator != null && operator.equals("LIKE")) {
			if (!casted.getOperand(1).toString().contains("%")) {
				ZExpression t1 = new ZExpression("=");
				t1.addOperand(casted.getOperand(0));
				t1.addOperand(casted.getOperand(1));
				exp = t1;
				return exp;
			}
		}
		if (operator != null && operator.equals("BETWEEN")) {

			ZExpression t1 = new ZExpression(">=");
			ZExpression t2 = new ZExpression("<=");

			try {
				t1.addOperand(dfs_work(((ZExpression) exp).getOperand(0)));
			} catch (Exception e) {
			}

			try {
				t1.addOperand(dfs_work(((ZExpression) exp).getOperand(1)));
			} catch (Exception e) {
			}

			try {
				t2.addOperand(dfs_work(((ZExpression) exp).getOperand(0)));
			} catch (Exception e) {
			}

			try {
				t2.addOperand(dfs_work(((ZExpression) exp).getOperand(2)));
			} catch (Exception e) {
			}

			exp = new ZExpression("AND");
			((ZExpression) exp).addOperand(t1);
			((ZExpression) exp).addOperand(t2);

			return exp;
		}
		Vector teilbaueme = new Vector();
		Iterator operanten = ((ZExpression) exp).getOperands().iterator();
		while (operanten.hasNext()) {
			ZExp temp = (ZExp) operanten.next();
			
			teilbaueme.add(temp);
			
		}
		ZExpression x = new ZExpression(((ZExpression) exp).getOperator());
		operanten = teilbaueme.iterator();
		while (operanten.hasNext()) {
			x.addOperand((ZExp) operanten.next());
		}
		return x;
	}

	 public static int getLevenshteinDistance (String s, String t) {
		    if (s == null || t == null) {
		      throw new IllegalArgumentException("Strings must not be null");
		    }     
		    int n = s.length(); // length of s
		    int m = t.length(); // length of t
		      
		    if (n == 0) {
		      return m;
		    } else if (m == 0) {
		      return n;
		    }

		    int p[] = new int[n+1]; //'previous' cost array, horizontally
		    int d[] = new int[n+1]; // cost array, horizontally
		    int _d[]; //placeholder to assist in swapping p and d

		    // indexes into strings s and t
		    int i; // iterates through s
		    int j; // iterates through t

		    char t_j; // jth character of t

		    int cost; // cost

		    for (i = 0; i<=n; i++) {
		       p[i] = i;
		    }
		      
		    for (j = 1; j<=m; j++) {
		       t_j = t.charAt(j-1);
		       d[0] = j;
		      
		       for (i=1; i<=n; i++) {
		          cost = s.charAt(i-1)==t_j ? 0 : 1;
		          // minimum of cell to the left+1, to the top+1, diagonally left and up +cost				
		          d[i] = Math.min(Math.min(d[i-1]+1, p[i]+1),  p[i-1]+cost);  
		       }

		       // copy current distance counts to 'previous row' distance counts
		       _d = p;
		       p = d;
		       d = _d;
		    } 
		      
		    // our last action in the above loop was to switch d and p, so p now 
		    // actually has the most recent cost counts
		    return p[n];
		  }

}

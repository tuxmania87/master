package de.unihalle.sqlequalizer;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZFromItem;
import org.gibello.zql.ZQuery;
import org.gibello.zql.ZSelectItem;

public class QueryUtils {

	static {
		HashMap<String, String> mes = new HashMap<String, String>();
		mes.put("numberJoins", "Joins");
		mes.put("numberFormulasWHERE", "(where expression) atomic formulas");
		mes.put("numberFormulasHAVING", "(having expression) atomic formulas");
		mes.put("numberTables", "tables");
		mes.put("numberSELECT", "items in select part");
		mes.put("numberORDERBY", "orderby statements");
		mes.put("numberSubquery", "subqueries");
		mes.put("existsDISTINCT", "DISTINCT statement");
		mes.put("existsGROUPBY", "GROUP BY statement");
		mes.put("existsStatementsinSELECT", "STATE IN SEL");
		mes.put("existsHAVING", "HAVING statement");
		mes.put("existsORDERBY", "ORDER BY statements");
		mes.put("parserTreeDepth", "parser tree depth");
		mes.put("less", "less");
		mes.put("more", "more");
		mes.put("firstquery", "the sample solution");
		mes.put("aWITHSPACE", "a ");
		mes.put("noSPACE", "no ");
		mes.put("one", "one");
		mes.put("none", "none");

		messages = mes;
	}

	public static HashMap<String, String> messages;

	public static String orderList[] = { "OR", "<=", ">=", "<", ">", "=",
			"IS NULL", "IS NOT NULL" , "EXISTS", "ANY", "ALL"};

	public static String commutativeOperators[] = { "=", "OR", "AND" };
	public static String comparisonOperators[] = { "<", "<=", ">", ">=", "=" };
	public static String subQueryOperators[] = { "EXISTS", "ANY", "SOME", "ALL" };

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

	public static String[] getLeafes(ZExp root) {

		ArrayList<String> list = new ArrayList<String>();
		traverseDFSAndGatherLeafs(root, list);

		return list.toArray(new String[list.size()]);

	}

	private static void traverseDFSAndGatherLeafs(ZExp node,
			ArrayList<String> leafs) {
		if (node instanceof ZConstant) {
			leafs.add(node.toString());
			return;
		}

		if (node instanceof ZExpression) {
			ZExpression e = (ZExpression) node;
			for (int i = 0; i < e.getOperands().size(); i++)
				traverseDFSAndGatherLeafs(e.getOperand(i), leafs);
		}

	}

	public static int DFSFirstDifferentNodeValue(ZExp r1, ZExp r2) {

		if (r1 instanceof ZExpression && r2 instanceof ZExpression) {
			ZExpression z1 = (ZExpression) r1;
			ZExpression z2 = (ZExpression) r2;

			if (z1.getOperator().equals(z2.getOperator())) {
				// traverse
				int max = Math.max(z1.getOperands().size(), z2.getOperands()
						.size());
				for (int i = 0; i < max; i++) {

					int v = DFSFirstDifferentNodeValue(z1.getOperand(i),
							z2.getOperand(i));
					if (v != 0)
						return v;
				}

				return 0;

			} else {
				System.out.println(z1.getOperator() + " " + z2.getOperator()
						+ " " + new OperatorComparer().compare(z1, z2));
				return new OperatorComparer().compare(z1, z2);
			}

		}

		if (r1 instanceof ZExpression && r2 instanceof ZConstant) {
			return 1;
		}

		if (r2 instanceof ZExpression && r1 instanceof ZConstant) {
			return -1;
		}

		return 0;
	}

	public static ZExp negate(ZExp exp) {
		if (exp instanceof ZConstant) 
			return exp;

		if(exp instanceof ZQuery) {
			return exp;
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

	public static boolean isIdenticalResultSets(ResultSet r1, ResultSet r2) {
		try {
			int col = 1;
			while (r1.next() && r2.next()) {
				Object res1 = r1.getObject(col);
				Object res2 = r2.getObject(col);

				if (!res1.equals(res2)) {
					throw new RuntimeException(String.format(
							"%s and %s aren't equal at common position %d",
							res1, res2, col));
				}

				// rs1 and rs2 must reach last row in the same iteration
				if ((r1.isLast() != r2.isLast())) {
					throw new RuntimeException(
							"The two ResultSets contains different number of columns!");
				}

				col++;

			}
		} catch (SQLException e) {
			return false;
		}
		return true;

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

	public static int depth(ZExp exp) {
		if (!(exp instanceof ZExpression)) {
			return 1;
		}
		ZExpression z = (ZExpression) exp;
		int max = 0;
		Iterator it = z.getOperands().iterator();
		while (it.hasNext()) {
			int x = depth((ZExp) it.next());
			if (x > max)
				max = x;
		}
		return max + 1;
	}

	public static ZExp distribute(ZExp r) {
		if (r instanceof ZConstant)
			return r;
		if (r instanceof ZExpression) {
			ZExpression exp = (ZExpression) r;
			if (exp.getOperator().equals("OR")) {

				int i = 0;
				int i2 = 0;
				ZExpression mergeExpr = null;
				ZExp mergePartner = null;
				for (; i < exp.getOperands().size(); i++) {
					if (exp.getOperand(i) instanceof ZExpression
							&& ((ZExpression) exp.getOperand(i)).getOperator()
									.equals("AND")) {
						mergeExpr = (ZExpression) exp.getOperand(i);
						break;
					}
				}

				if (mergeExpr == null)
					return r;

				if (i - 1 >= 0) { // take last entry to apply distribute law
					mergePartner = exp.getOperand(i - 1);
				} else if (i + 1 < exp.getOperands().size()) {
					mergePartner = exp.getOperand(i + 1);
				}

				if (exp.getOperands().size() > 2) {
					ZExpression ret = new ZExpression("OR");
					for (int j = 0; j < exp.getOperands().size(); j++) {
						if (exp.getOperand(j) != mergeExpr
								&& exp.getOperand(j) != mergePartner) {
							ret.addOperand(exp.getOperand(j));
						}
					}

					ZExpression newAND = new ZExpression("AND");

					Iterator<ZExp> it = mergeExpr.getOperands().iterator();

					while (it.hasNext()) {
						newAND.addOperand(new ZExpression("OR", mergePartner,
								it.next()));
					}
					ret.addOperand(newAND);
					return ret;

				} else {
					ZExpression ret = new ZExpression("AND");

					Iterator<ZExp> it = mergeExpr.getOperands().iterator();

					while (it.hasNext()) {
						ret.addOperand(new ZExpression("OR", mergePartner, it
								.next()));
					}
					return ret;
				}

			}
			ZExpression ret2 = new ZExpression(exp.getOperator());
			Iterator it = exp.getOperands().iterator();
			while (it.hasNext()) {
				ret2.addOperand(distribute((ZExp) it.next()));
			}

			return ret2;

		}

		return r;

	}

	public static ZExp sortTree(ZExp exp) {
		if (exp instanceof ZConstant)
			return exp;

		if (exp instanceof ZExpression) {
			ZExpression e = (ZExpression) exp;

			// if current Operator is commutative, then we
			// can sort the childs in the order we want
			if (Arrays.asList(commutativeOperators).contains(e.getOperator())) {
				Collections.sort(e.getOperands(), new OperatorComparer());
			}

			ZExpression ret = new ZExpression(e.getOperator());
			// proceed with sorting children
			for (int i = 0; i < e.getOperands().size(); i++) {
				ret.addOperand(sortTree(e.getOperand(i)));
			}

			return ret;

		}
		return exp;

	}

	public static ZExp rebuilder(ZExp node) {

		if (node instanceof ZConstant)
			return node;

		if (node instanceof ZExpression) {
			ZExpression z = (ZExpression) node;
			ZExpression ret = new ZExpression(z.getOperator());

			for (int i = 0; i < z.getOperands().size(); i++) {
				ret.addOperand(rebuilder(z.getOperand(i)));
			}

			// untersuche kinder
			HashMap<String, ArrayList<ZExp>> masterlist = new HashMap<String, ArrayList<ZExp>>();
			ArrayList<ZExp> restList = new ArrayList<ZExp>();

			for (int i = 0; i < ret.getOperands().size(); i++) {
				if (ret.getOperand(i) instanceof ZExpression) {
					ZExpression z0 = (ZExpression) ret.getOperand(i);
					if (masterlist.containsKey(z0.getOperator())) {
						masterlist.get(z0.getOperator()).add(z0);
					} else {
						ArrayList<ZExp> tlist = new ArrayList<ZExp>();
						tlist.add(z0);
						masterlist.put(z0.getOperator(), tlist);
					}

				} else {
					restList.add(z.getOperand(i));
				}
			}

			// if hasmap empty or has only entrys with size 1 we do not have to
			// do anything
			if (masterlist.isEmpty())
				return ret;

			Set<String> keys = masterlist.keySet();
			Iterator<String> it = keys.iterator();

			boolean abbort = true;
			while (it.hasNext()) {
				if (masterlist.get(it.next()).size() > 1)
					abbort = false;
			}

			if (abbort)
				return ret;

			it = keys.iterator();
			while (it.hasNext()) {
				String current = it.next();
				ArrayList<ZExp> tl = masterlist.get(current);
				Collections.sort(tl, new LeafOrder());

				
			}
			ZExpression zret = new ZExpression(z.getOperator());
			
			it = keys.iterator();
			while (it.hasNext()) {
				String current = it.next();
				ArrayList<ZExp> tl = masterlist.get(current);

				for (int i = 0; i < tl.size(); i++)
					zret.addOperand(tl.get(i));

			}

			Iterator<ZExp> it2 = restList.iterator();
			while (it2.hasNext()) {
				zret.addOperand(it2.next());
			}

			return zret;
		}

		return node;
	}

	public static boolean sortTreeEqualOperators(ZExp node, ZExp parent) {

		if (node instanceof ZExpression) {
			ZExpression z = (ZExpression) node;

			HashMap<String, ArrayList<ZExp>> masterlist = new HashMap<String, ArrayList<ZExp>>();
			ArrayList<ZExp> restList = new ArrayList<ZExp>();

			for (int i = 0; i < z.getOperands().size(); i++) {
				if (z.getOperand(i) instanceof ZExpression) {
					ZExpression z0 = (ZExpression) z.getOperand(i);
					if (masterlist.containsKey(z0.getOperator())) {
						masterlist.get(z0.getOperator()).add(z0);
					} else {
						ArrayList<ZExp> tlist = new ArrayList<ZExp>();
						tlist.add(z0);
						masterlist.put(z0.getOperator(), tlist);
					}

				} else {
					restList.add(z.getOperand(i));
				}
			}

			// if hasmap empty or has only entrys with size 1 we do not have to
			// do anything
			if (masterlist.isEmpty())
				return true;

			Set<String> keys = masterlist.keySet();
			Iterator<String> it = keys.iterator();

			boolean abbort = true;
			while (it.hasNext()) {
				if (masterlist.get(it.next()).size() > 1)
					abbort = false;
			}

			if (abbort)
				return true;

			it = keys.iterator();
			while (it.hasNext()) {
				String current = it.next();
				boolean checker = true;
				ArrayList<ZExp> tl = masterlist.get(current);

				for (int i = 0; i < tl.size(); i++) {
					checker &= sortTreeEqualOperators(tl.get(i), z);
				}

				if (checker) {
					// all subtrees are in the last possible layer
					// so we have to sort with leafsets
					System.out.println("sortieren von " + current
							+ " und Vater: " + z);

					Collections.sort(tl, new LeafOrder());

				}
			}

			z = new ZExpression(z.getOperator());

			it = keys.iterator();
			while (it.hasNext()) {
				String current = it.next();
				ArrayList<ZExp> tl = masterlist.get(current);

				for (int i = 0; i < tl.size(); i++)
					z.addOperand(tl.get(i));

			}

			Iterator<ZExp> it2 = restList.iterator();
			while (it2.hasNext()) {
				z.addOperand(it2.next());
			}

			node = z;
		}
		return false;

	}

	public static ZExp sortedTree(ZExp exp) {
		String old = null;
		ZExp ret = exp;
		do {
			old = ret.toString();
		    ret = sortTree(rebuilder(ret));
		} while(! old.equals(ret.toString()));
		
		return ret;
	}

	public static String compareMetaInfos(MetaQueryInfo m1, MetaQueryInfo m2)
			throws IllegalArgumentException, IllegalAccessException {
		// we go through every attribute and compare number or false/true
		String res = "";
		for (Field f : m1.getClass().getDeclaredFields()) {
			if (f.getType().toString().equals("int")) {
				String word = null;
				if (f.getInt(m1) < f.getInt(m2)) {
					word = messages.get("less");
				} else if (f.getInt(m1) > f.getInt(m2)) {
					word = messages.get("more");
				}
				if (word != null) {
					String x = "There are " + word + " "
							+ messages.get(f.getName()) + " in the "
							+ messages.get("firstquery") + " (" + f.getInt(m1)
							+ ") than in yours (" + f.getInt(m2) + ").";
					res += x + "<br>";
				}
			} else if (f.getType().toString().equals("boolean")) {
				if (f.getBoolean(m1) != f.getBoolean(m2)) {
					String p1 = f.getBoolean(m1) ? "aWITHSPACE" : "noSPACE";
					String p2 = f.getBoolean(m2) ? "one" : "none";
					String x = "There is " + messages.get(p1)
							+ messages.get(f.getName())
							+ " in the sample solution but there is "
							+ messages.get(p2) + " in your solution!";
					res += x + "<br>";
				}
			}
		}

		return res;
	}

	public static ZExp operatorCompression(ZExp exp, ZExp parent) {

		if (exp instanceof ZConstant) {
			return (ZConstant) exp;
		}

		if (exp instanceof ZQuery) {
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
				} else if (Next instanceof ZQuery) {
					z.addOperand(Next);
				} else {
					ZExpression t1 = (ZExpression) operatorCompression(
							(ZExp) Next, exp);
					if (t1 == null) {
						z.addOperand(operatorCompression(Next, null));
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

	public static ZExp pushDownNegate(ZExp exp) {
		if (exp instanceof ZExpression) {
			ZExpression z = (ZExpression) exp;
			
			ZExpression toHandle = null;
			
			if(((ZExpression) exp).getOperator().equals("NOT") && 
					((ZExpression) exp).getOperand(0) instanceof ZExpression &&
					! Arrays.asList(QueryUtils.subQueryOperators).contains(((ZExpression) ((ZExpression) exp).getOperand(0)).getOperator())
					) {
				toHandle = (ZExpression) negate(z.getOperand(0));
			} else {
				toHandle = z;
			}
			
			Iterator<ZExp> it = toHandle.getOperands().iterator();
			
			ZExpression ret = new ZExpression(toHandle.getOperator());
			
			while(it.hasNext()) {
				ret.addOperand(pushDownNegate(it.next()));
			}
			
			return ret;
			
		}
		
		return exp;
		
		/*if (exp instanceof ZExpression) {
			ZExpression ret = new ZExpression(((ZExpression) exp).getOperator());
			Iterator<ZExp> it = ((ZExpression) exp).getOperands().iterator();

			while (it.hasNext()) {
				ZExp cur = it.next();
				if (cur instanceof ZExpression
						&& ((ZExpression) cur).getOperator().equals("NOT")) {
					ret.addOperand(negate(cur));
				} else {
					ret.addOperand(cur);
				}
			}
			return ret;
		}
		return exp;
		*/
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

	public static ZExp evaluateArithmetic(ZExp exp) {
		/*
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
		move to eval
		*/
		
		return exp;
	}
	
	public static ZExp replaceSubqueries(ZExp exp,ArrayList<Table> tables) {
		
		if(exp instanceof ZExpression) {
			ZExpression z = (ZExpression) exp;
			if(Arrays.asList(comparisonOperators).contains(z.getOperator())) {
				if(z.getOperand(1) instanceof ZExpression) {
					ZExpression z2 = (ZExpression) z.getOperand(1);
					
					if(z2.getOperator().equals("ALL")) {
						
						ZQuery tquery = (ZQuery) z2.getOperand(0);
						ZSelectItem selItem = (ZSelectItem) tquery.getSelect().firstElement();
						
						//we look up if t_2 may be null 
						for(int i = 0; i<tables.size(); i++) {
							Table tt = tables.get(i);
							for(int j = 0; j< tt.columns.size(); j++) {
								if(tt.columns.get(j).name.equals(selItem.getColumn()) 
										&& tt.columns.get(j).canBeNull) {
									//we dont replace it because it can be null
									ZExpression ret = new ZExpression(((ZExpression) exp).getOperator());
									ret.addOperand(z.getOperand(0));
									ret.addOperand(replaceSubqueries(z.getOperand(1), tables));
									return ret;
								}
							}
						}
						
						ZExpression notnode = new ZExpression("NOT");
						ZExpression anynode = new ZExpression("ANY");
						ZExpression comparisonnode = new ZExpression(QueryUtils.getOppositeOperator(z.getOperator()));
						notnode.addOperand(comparisonnode);
						comparisonnode.addOperand(z.getOperand(0));
						comparisonnode.addOperand(anynode);
						anynode.addOperand(z2.getOperand(0));
						return replaceSubqueries(notnode, tables);
						
					}
					
					if(z2.getOperator().equals("ANY") || z2.getOperator().equals("SOME")) {
						ZExpression existsnode = new ZExpression("EXISTS");
						
						ZQuery query = (ZQuery) z2.getOperand(0);
						
						ZSelectItem selItem = (ZSelectItem) query.getSelect().firstElement();
						for(int i = 0; i<tables.size(); i++) {
							Table tt = tables.get(i);
							for(int j = 0; j< tt.columns.size(); j++) {
								if(tt.columns.get(j).name.equals(selItem.getColumn()) 
										&& tt.columns.get(j).canBeNull) {
									//we dont replace it because it can be null
									ZExpression ret = new ZExpression(((ZExpression) exp).getOperator());
									ret.addOperand(z.getOperand(0));
									ret.addOperand(replaceSubqueries(z.getOperand(1), tables));
									return ret;
								}
							}
						}
						
						//query is normalized which means either AND is root node of where clause
						// or there is no and in where clause
						
						if(query.getWhere() instanceof ZExpression && ((ZExpression) query.getWhere()).getOperator().equals("AND")) {
							
								//just add t_1 = t_2 to and 
								
								ZExpression equalnode = new ZExpression("=");
								equalnode.addOperand(z.getOperand(0));
								equalnode.addOperand( new ZConstant( ((ZSelectItem)query.getSelect().firstElement()).toString(), ZConstant.COLUMNNAME)  );
								((ZExpression) query.getWhere()).addOperand(equalnode);
							} else {
								//we have to add AND to first layer
								ZExpression equalnode = new ZExpression(z.getOperator());
								equalnode.addOperand(z.getOperand(0));
								equalnode.addOperand( new ZConstant( ((ZSelectItem)query.getSelect().firstElement()).toString(), ZConstant.COLUMNNAME) );
								
								if(query.getWhere() instanceof ZExpression) {
									ZExpression andnode = new ZExpression("AND");
									andnode.addOperand(query.getWhere());
									andnode.addOperand(equalnode);
									query.addWhere(andnode);
								} else {
									query.addWhere(equalnode);
								}
							}
							
						
						
						existsnode.addOperand(query);
						return existsnode;
					}
				}
					
			}
			
			Iterator it = z.getOperands().iterator();
			ZExpression ret = new ZExpression(z.getOperator());
			while(it.hasNext()) {
				ret.addOperand(replaceSubqueries((ZExp) it.next(), tables));
			}
			return ret;
			
			
		}
		
		
		return exp;
	}
	
	public static ZExp replaceSyntacticVariantes(ZExp exp) {

		if (exp instanceof ZQuery) {
			return exp;
		}

		if (exp instanceof ZConstant) {
			return exp;
		}
		
		if(exp instanceof ZExpression) {
			String operator = ((ZExpression) exp).getOperator();
			ZExpression casted = (ZExpression) exp;
			
			/**
			 * replaces a BETWEEN b AND c with a >= b AND a <= c
			 */
			if (operator.equals("BETWEEN")) {

				ZExpression t1 = new ZExpression(">=");
				ZExpression t2 = new ZExpression("<=");

				t1.addOperand(casted.getOperand(0));
				t1.addOperand(casted.getOperand(1));
				t2.addOperand(casted.getOperand(0));
				t2.addOperand(casted.getOperand(2));
				
				exp = new ZExpression("AND");
				casted.addOperand(replaceSyntacticVariantes(t1));
				casted.addOperand(replaceSyntacticVariantes(t2));

				return exp;
			}
		
			/**
			 * replaces a IN (const1, const2, ..., constn) with
			 * a = const1 OR a = const 2 OR ... OR a = constn
			 * 
			 * subqueries with IN are not replaced here but in replaceSubqueries()
			 */
			if(operator.equals("IN")) {
				//check if only constant are children
				Iterator it = casted.getOperands().iterator();
				
				ZExpression orList = new ZExpression("OR");
				
				ZConstant firstOperand = (ZConstant) it.next();
				while(it.hasNext()) {
					ZExp elem = (ZExp) it.next();
					if( ! (elem instanceof ZConstant)) {
						return exp;
					} else {
						ZExpression tExpression = new ZExpression("=");
						tExpression.addOperand(firstOperand);
						tExpression.addOperand(elem);
						orList.addOperand(tExpression);
					}
						
				}
				
				return orList;
				
			}
			
			
			if(operator.equals("EXISTS")) {
				ZQuery zq = (ZQuery) casted.getOperand(0);
				Vector<ZSelectItem> selItems = new Vector<ZSelectItem>();
				ZSelectItem selItem = new ZSelectItem("1");
				selItems.add(selItem);
				zq.addSelect(selItems);
				
				ZExpression ret = new ZExpression("EXISTS");
				ret.addOperand(zq);
				return ret;
				
			}
			
			Iterator<ZExp> it = casted.getOperands().iterator();
			
			ZExpression ret = new ZExpression(casted.getOperator());
			while(it.hasNext()) {
				ret.addOperand(replaceSyntacticVariantes(it.next()));
			}
			return ret;
			
		}
		
		/*if (operator.equals("LIKE")) {
			if (!casted.getOperand(1).toString().contains("%")) {
				ZExpression t1 = new ZExpression("=");
				t1.addOperand(casted.getOperand(0));
				t1.addOperand(casted.getOperand(1));
				exp = t1;
				return exp;
			}
		}
		*/
		
		
		return exp;
	}

	public static int getLevenshteinDistance(String s, String t) {
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

		int p[] = new int[n + 1]; // 'previous' cost array, horizontally
		int d[] = new int[n + 1]; // cost array, horizontally
		int _d[]; // placeholder to assist in swapping p and d

		// indexes into strings s and t
		int i; // iterates through s
		int j; // iterates through t

		char t_j; // jth character of t

		int cost; // cost

		for (i = 0; i <= n; i++) {
			p[i] = i;
		}

		for (j = 1; j <= m; j++) {
			t_j = t.charAt(j - 1);
			d[0] = j;

			for (i = 1; i <= n; i++) {
				cost = s.charAt(i - 1) == t_j ? 0 : 1;
				// minimum of cell to the left+1, to the top+1, diagonally left
				// and up +cost
				d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1]
						+ cost);
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

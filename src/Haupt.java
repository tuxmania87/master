import java.beans.Expression;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.Vector;

import org.gibello.zql.ParseException;
import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZQuery;
import org.gibello.zql.ZStatement;
import org.gibello.zql.ZqlParser;


public class Haupt {

	public static String orderList[] = { "<=", ">=", "=", "IS NULL", "IS NOT NULL", "OR", "AND"};
	
	public static String getFlippedOperator(String op) {
		if(op.equals(">=")) {
			return "<=";
		}
		if(op.equals("<=")) {
			return ">=";
		}
		if(op.equals(">")) {
			return "<";
		}
		if(op.equals("<")) {
			return ">";
		}
		return op;
	}
	
	public static String getOppositeOperator(String op) {
		if(op.equals(">=")) {
			return "<";
		}
		if(op.equals("<=")) {
			return ">";
		}
		if(op.equals(">")) {
			return "<=";
		}
		if(op.equals("<")) {
			return ">=";
		}
		if(op.equals("=")) {
			return "<>";
		}
		if(op.equals("<>")) {
			return "=";
		}
		if(op.equals("AND")) {
			return "OR";
		}
		if(op.equals("OR")) {
			return "AND";
		}
		if(op.equals("IS NOT NULL")) {
			return "IS NULL";
		}
		if(op.equals("IS NULL")) {
			return "IS NOT NULL";
		}
		return op;
	}
	
	public static ZExp negate(ZExp exp) {
		if(exp instanceof ZConstant) {
			return ((ZConstant) exp);
		}
		
		ZExpression casted = (ZExpression) exp;
		
		ZExpression result = new ZExpression(getOppositeOperator(casted.getOperator()));
		Iterator it = casted.getOperands().iterator();
		while(it.hasNext()) {
			result.addOperand(negate( (ZExp) it.next()));
		}
		return result;
	}
	
	public static int indexOf(String needle) {
		for(int i=0; i<orderList.length; i++)
			if(orderList[i].equals(needle))
				return i;
		return -1;
	}
	
	public static boolean isLesserThan(ZExp a, ZExp b) {
		if(b instanceof ZConstant)
			return true;
		if(a instanceof ZConstant)
			return false;
		
		//look for index of operator
		ZExpression t1 = (ZExpression) a;
		ZExpression t2 = (ZExpression) b;
		
		if(indexOf(t1.getOperator()) == indexOf(t2.getOperator())) {
			return t1.getOperand(0).toString().compareTo(t2.getOperand(0).toString()) < 0;
		}
		
		return indexOf(t1.getOperator()) < indexOf(t2.getOperator());
		
	}
	
	public static ZExp sortedTree(ZExp exp) {
		if(exp instanceof ZConstant) {
			return (ZConstant)exp;
		}
		
		ZExpression t1 = (ZExpression) exp;
		
		Iterator it = t1.getOperands().iterator();
		
		int ix=0;
		ZExp feld[] = new ZExp[t1.getOperands().size()];
		
		while(it.hasNext()) {
			feld[ix++] = (ZExp) it.next();
		}
		
		for(int i=0; i< feld.length; i++) {
			for(int j=0; j< feld.length-1; j++) {
				if(isLesserThan(feld[j+1], feld[j])) {
					ZExp help = feld[j+1];
					feld[j+1] = feld[j];
					feld[j] = help;
				}
			}
		}
		
		ZExpression result = new ZExpression(t1.getOperator());
		
		for(int i=0; i< feld.length;i++) {
			result.addOperand(feld[i]);
		}
		
		
		return result;
	}
	
	
	public static ZExp operatorCompression(ZExp exp, ZExp parent) {
		
		if(exp instanceof ZConstant) {
			return (ZConstant) exp;
		}
		
		ZExpression parentExp = (ZExpression) parent;
		ZExpression casted = (ZExpression) exp;
		
		if(parent == null) {
			ZExpression z = new ZExpression(casted.getOperator());
			Iterator it = casted.getOperands().iterator();
			while(it.hasNext()) {
				ZExp Next = (ZExp) it.next();
				ZExpression t1 = (ZExpression) operatorCompression((ZExp) Next, exp);
				if(t1 == null) {
					z.addOperand(Next);
				} else {
					Iterator it2 = t1.getOperands().iterator();
					while(it2.hasNext()) {
						z.addOperand((ZExp) it2.next());
					}
				}
			}
			return z;
		}
		
		if(parentExp.getOperator().equals(casted.getOperator())) {
			
			Vector newVec = new Vector();
			
			//first attach all childs except current
			Iterator it = casted.getOperands().iterator();
			while(it.hasNext()) {
				ZExp Curr = (ZExp) it.next();
				ZExp help = operatorCompression((ZExp) Curr, exp);
				if(help == null) {
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
		//filter all pairs one layer before leaf layer
		// and sort them so that  a ? b   or a ? 4 
		if(exp instanceof ZExpression) {
			ZExpression casted = (ZExpression) exp;
			if(casted.getOperands().size() == 2 && casted.getOperand(0) instanceof ZConstant && casted.getOperand(1) instanceof ZConstant) {
				ZConstant arg0 = (ZConstant) casted.getOperand(0);
				ZConstant arg1 = (ZConstant) casted.getOperand(1);
				if(arg0.getType() == ZConstant.NUMBER && arg1.getType() == ZConstant.COLUMNNAME) {
					ZExpression ret = new ZExpression(getFlippedOperator(casted.getOperator()));
					ret.addOperand(arg1); ret.addOperand(arg0);
					return ret;
				}
				if(arg0.getType() == ZConstant.COLUMNNAME && arg1.getType() == ZConstant.COLUMNNAME && arg1.toString().compareTo(arg0.toString()) < 0 ) {
					ZExpression ret = new ZExpression(getFlippedOperator(casted.getOperator()));
					ret.addOperand(arg1); ret.addOperand(arg0);
					return ret;
				}
				//TODO: könnte man zusammenfassen, da bei beiden das gleiche gemacht wird. Sieht dann nur unübersichtlich auf, weil alles
				// in einer if 
				return casted;
			}
			//ELSE, we dont have 2 ZConstant Childs, but we are in a ZExpression
			ZExpression ret = new ZExpression(casted.getOperator());
			Iterator it = casted.getOperands().iterator();
			while(it.hasNext()) {
				ZExp arg = (ZExp) it.next();
				ret.addOperand(dfs_orderPairs(arg));
			}
			return ret;
		}
		
		return exp;
		
		
	}
	
	public static ZExp dfs_work(ZExp exp) {
		
		/*System.out.println("Expression: "+exp);
		if(exp instanceof ZExpression) 
			System.out.println("ZExpression");*/
		if(exp instanceof ZConstant) {
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
		
		if(casted.getOperands().size() == 2 && casted.getOperand(0) instanceof ZConstant && casted.getOperand(1) instanceof ZConstant) {
			try {
				int a = Integer.parseInt(casted.getOperand(0).toString());
				int b = Integer.parseInt(casted.getOperand(1).toString());
				
				if(operator.equals("+")) {
					return new ZConstant(String.valueOf(a+b), ZConstant.NUMBER);
				}
				if(operator.equals("-")) {
					return new ZConstant(String.valueOf(a-b), ZConstant.NUMBER);
				}
				if(operator.equals("*")) {
					return new ZConstant(String.valueOf(a*b), ZConstant.NUMBER);
				}
				if(operator.equals("/") && a%b == 0) {
					return new ZConstant(String.valueOf(a/b), ZConstant.NUMBER);
				}
				
				
				
			} catch(Exception e) {
				if(operator.equals(">")) {
					ZConstant c1 = (ZConstant) casted.getOperand(0);
					ZConstant c2 = (ZConstant) casted.getOperand(1);

					if(c1.getType() == ZConstant.NUMBER && c2.getType() != ZConstant.NUMBER) {
						ZExpression t1 = new ZExpression(">=");
						t1.addOperand(new ZConstant(String.valueOf(Integer.parseInt(c1.toString())-1), ZConstant.NUMBER));
						t1.addOperand(c2);
						return t1;
					}
					if(c2.getType() == ZConstant.NUMBER && c1.getType() != ZConstant.NUMBER) {
						ZExpression t1 = new ZExpression(">=");
						t1.addOperand(c1);
						t1.addOperand(new ZConstant(String.valueOf(Integer.parseInt(c2.toString())-1), ZConstant.NUMBER));
						return t1;
					}
					//TODO
					if(c2.getType() == ZConstant.NUMBER && c1.getType() == ZConstant.NUMBER) {
						//either false or true
					}
				}
				if(operator.equals("<")) {
					ZConstant c1 = (ZConstant) casted.getOperand(0);
					ZConstant c2 = (ZConstant) casted.getOperand(1);

					if(c1.getType() == ZConstant.NUMBER && c2.getType() != ZConstant.NUMBER) {
						ZExpression t1 = new ZExpression("<=");
						t1.addOperand(new ZConstant(String.valueOf(Integer.parseInt(c1.toString())+1), ZConstant.NUMBER));
						t1.addOperand(c2);
						return t1;
					}
					if(c2.getType() == ZConstant.NUMBER && c1.getType() != ZConstant.NUMBER) {
						ZExpression t1 = new ZExpression("<=");
						t1.addOperand(c1);
						t1.addOperand(new ZConstant(String.valueOf(Integer.parseInt(c2.toString())+1), ZConstant.NUMBER));
						return t1;
					}
					//TODO
					if(c2.getType() == ZConstant.NUMBER && c1.getType() == ZConstant.NUMBER) {
						//either false or true
					}
				}
			}
		}
		
		
		if(operator != null && operator.equals("NOT")) {
			//System.out.println("Operand "+casted.getOperand(0));
			ZExpression t1 = (ZExpression) casted.getOperand(0);
			return negate(t1);
		}
		
		if(operator != null && operator.equals("LIKE")) {
			if(!casted.getOperand(1).toString().contains("%")) {
				ZExpression t1 = new ZExpression("=");
				t1.addOperand(casted.getOperand(0));
				t1.addOperand(casted.getOperand(1));
				exp = t1;
				return exp;
			}
		}
		if(operator != null && operator.equals("BETWEEN")) {
			
			
			ZExpression t1 = new ZExpression(">=");
			ZExpression t2 = new ZExpression("<=");
			
			try {
				t1.addOperand(dfs_work(((ZExpression) exp).getOperand(0)));
			} catch(Exception e) {
			}
			
			try {
				t1.addOperand(dfs_work(((ZExpression) exp).getOperand(1)));
			} catch(Exception e) {
			}
			
			try {
				t2.addOperand(dfs_work(((ZExpression) exp).getOperand(0)));
			} catch(Exception e) {
			}
			
			try {
				t2.addOperand(dfs_work(((ZExpression) exp).getOperand(2)));
			} catch(Exception e) {
			}
			
			
			exp = new ZExpression("AND");
			((ZExpression) exp).addOperand(t1);
			((ZExpression) exp).addOperand(t2);
			
			return exp;
		} 
			Vector teilbaueme = new Vector();
			Iterator operanten = ((ZExpression) exp).getOperands().iterator();
			while(operanten.hasNext()) {
				ZExp temp = (ZExp) operanten.next();
				try {
					ZExpression texp = (ZExpression) temp;
					teilbaueme.add(dfs_work(texp));
				} catch(Exception e) {
					ZConstant tconst = (ZConstant) temp;
					teilbaueme.add(tconst);
				}
			}
			ZExpression x = new ZExpression(((ZExpression) exp).getOperator());
			operanten = teilbaueme.iterator();
			while(operanten.hasNext()) {
				x.addOperand((ZExp) operanten.next());
			}
			return x;
		}
	
	
	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException {
		ZqlParser zp = new ZqlParser();
		//zp.initParser(new ByteArrayInputStream("select * from test where b > z and a > z;".getBytes()));
		zp.initParser(new ByteArrayInputStream("select * from test where arg between 4/2 and 3+2 and bla like 'foo' and not (x>=a or b is not null) and ( g > 5 and s >2 );".getBytes()));
		//zp.initParser(new ByteArrayInputStream("CREATE TABLE customer (First_Name char(50),	Last_Name char(50),	Address char(50),City char(50),	Country char(25),Birth_Date date)".getBytes()));
		ZStatement zs = zp.readStatement();
		System.out.println(zs);
		
		
		
		ZQuery q = (ZQuery) zs;
		
		
		
		ZExpression zexp = (ZExpression) q.getWhere();
		//System.out.println(zexp.getOperand(2));
		
		
		ZExpression test = (ZExpression) sortedTree(operatorCompression(dfs_work(dfs_work(dfs_orderPairs(zexp))),null));
		//ZExpression test = (ZExpression) operatorCompression(zexp,null);
		
		
		ZQuery neu = new ZQuery();
		neu.addSelect(q.getSelect());
		neu.addFrom(q.getFrom());
		neu.addWhere(test);
		
		System.out.println(neu);
	
	}

}

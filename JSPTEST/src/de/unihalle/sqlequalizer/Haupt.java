package de.unihalle.sqlequalizer;

import java.beans.Expression;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gibello.zql.ParseException;
import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZFromItem;
import org.gibello.zql.ZQuery;
import org.gibello.zql.ZSelectItem;
import org.gibello.zql.ZStatement;
import org.gibello.zql.ZqlParser;

public class Haupt {

	/**
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		
	
		/*QueryHandler q = new QueryHandler();
		q.createTable("create table emp (empno int, ename varchar(500), job varchar(500), mgr int, hiredate datetime, sal int, comm int, deptno int)");
		q.createTable("create table dept (deptno int, dname varchar(500), location varchar(500))");
		q.createTable("create table test (a int, b int, c int, d int, x int, y int)");

		// ZQuery z =
		// q.equalize("select * from emp e where e.sal > (select sum(sal) from emp x where e.sal > x.sal);");

		// q.setOriginalStatement("SELECT * FROM emp e, dept d WHERE  NOT(dname = 'NY' OR d.deptno < 100);");
		q.setOriginalStatement("SELECT d, x FROM test WHERE  d < x order by x;");
		// q.setOriginalStatement("SELECT * FROM emp e LEFT OUTER JOIN dept d ON e.empno = d.deptno WHERE 1=1;");

		String s1 = q.original.toString();

		/*
		 * ZQuery z = q.equalize();
		 * 
		 * String s2 = z.toString();
		 * 
		 * System.out.println("alt "+s1);
		 * System.out.println("neu: "+z.toString());
		 * 
		 * //System.out.println(new Double(QueryUtils.getLevenshteinDistance(s1,
		 * s2)).doubleValue()/new Double(Math.max(s1.length(),
		 * s2.length())).doubleValue());
		 *
		 */
		QueryHandler q2 = new QueryHandler();
		q2.createTable("create table emp (empno int, ename varchar(500), job varchar(500), mgr int, hiredate datetime, sal int not null, comm int, deptno int)");
		q2.createTable("create table dept (deptno int, dname varchar(500), location varchar(500))");
		q2.createTable("create table test (a float(5,2) references emp(sal) not null, b int, c int, d int, x int, y int)");
		q2.createTable("create table test2 (a int)");
		
		
		

		
		//q2.setOriginalStatement("select * from test t where (x or (g and h) or z or not (a > 2 and (b or (c and d))));");
		//q2.setOriginalStatement("select * from emp  where deptno > 2 AND sal > ALL (select sal from emp  where deptno=20);");
		//q2.setOriginalStatement("SELECT e1.empno, e1.sal FROM   emp e1 WHERE  e1.sal > ALL (SELECT e2.sal                      FROM   emp e2                      WHERE  e2.deptno = 20); ;");
		q2.setOriginalStatement("select * from emp e where 10 > 5 and ( 2 > 3 or 3 > 2);");
		System.out.println(q2.original);
		ZQuery res = q2.equalize();
		System.out.println(res);
		
		System.out.println();
		
		
		System.out.println("STELLE: "+QueryUtils.places("a1.sal", q2));
		
		q2 = new QueryHandler();
		q2.createTable("create table emp (empno int, ename varchar(500), job varchar(500), mgr int, hiredate datetime, sal int not null, comm int, deptno int)");
		q2.createTable("create table dept (deptno int, dname varchar(500), location varchar(500))");
		q2.createTable("create table test (a int references emp(sal) not null, b int, c int, d int, x int, y int)");
		
		
		q2.setOriginalStatement("select * from emp e where sal > deptno ;");
		System.out.println(q2.original);
		res = q2.equalize();
		System.out.println(res);
		
		//q2.setOriginalStatement("select * from test t where ( ( a<4 ) or (b <3 ) ) and ( (b<3) or(a<4));");
		
	
		//ZExp knf = q2.tranformToKNF(q2.original.getWhere());
		//System.out.println(knf);
		
		//ZExp result = q2.tranformToKNF(q2.original.getWhere());
		
		//QueryUtils.sortTreeEqualOperators(result, null);
		/*
		result = QueryUtils.replaceSyntacticVariantes(result);
		result = QueryUtils.replaceSubqueries(result);
		result = QueryUtils.replaceSyntacticVariantes(result);
		result = QueryUtils.sortedTree(result);
		
		System.out.println(result);*/
	
		/*q2.setOriginalStatement("select * from test t where ( (b<3) or(a<4)) and ( ( a<4 ) or (b <3 ) );");
		result = QueryUtils.sortTree(q2.tranformToKNF(q2.original.getWhere()));
		result = QueryUtils.sortedTree(result);
		System.out.println(result);*/
		
	
		System.out.println("b".compareTo("a"));
		
	
		
	
	}

	public static boolean examine(ZExp e1, ZExp e2) {
		
		
		if (e1 instanceof ZConstant && e2 instanceof ZConstant)
			return ((ZConstant) e1).getValue().equals(((ZConstant) e2).getValue());

		ZExpression ze1 = (ZExpression) e1;
		ZExpression ze2 = (ZExpression) e2;
		
		if (!ze1.getOperator().equals(ze2
				.getOperator()) && !flippable(ze1.getOperator()) && !oppositeable(ze1.getOperator())) {
			return false;
		}

		if (ze1.getOperator().equals(QueryUtils.getFlippedOperator(ze2
				.getOperator())) && flippable(ze1.getOperator())) {
			ZExpression temp = new ZExpression(
					QueryUtils.getFlippedOperator(ze1.getOperator()));
			Vector<ZExp> t2 = (Vector<ZExp>) ze1.getOperands().clone();
			Collections.reverse(t2);
			temp.setOperands(t2);
			return examine(temp, ze2);
		}
		
		if( oppositeable(ze1.getOperator()) && ze1.getOperator().equals(getOppositeArithmetic(ze2.getOperator()))) {
			if(ze1.getOperands().size() != ze2.getOperands().size()) {//teilbaueme muessen gleich viele argumente haben
				return false;
			}
			for(int i=1; i<ze1.getOperands().size(); i++) {
				if(!examine((ZExp)ze1.getOperands().get(i), (ZExp) ze2.getOperands().get(i))) {
					return false;
				}
			}
			
			return true;
		}

		if (kommutativ(ze1.getOperator())) {

			for (int j = 0; j < ze1.getOperands().size(); j++) {

				ZExp current = (ZExp) ze1.getOperands().get(j);
				int[] pos = searchForIn(current, ze2.getOperands());

				for (int i = 0; i < pos.length; i++) {
					if (ze2.getOperands().get(pos[i]) == null) {
					} else {
						boolean result = examine(current, (ZExp) ze2
								.getOperands().get(pos[i]));
						if (result) {
							ze1.getOperands().set(j, null);
							ze2.getOperands().set(pos[i], null);
							break;
						}
					}
				}
				boolean flipped = false;
				if(flippable(current) && pos.length == 0) {
					int fpos[] = searchForIn( new ZExpression(QueryUtils.getFlippedOperator( ((ZExpression) current).getOperator())), ze2.getOperands());
					if(fpos.length > 0)
						flipped = true;
					for(int i=0; i<fpos.length; i++) {
						boolean result = examine(current,(ZExp) ze2.getOperands().get(fpos[i]));
						if(result) {
							ze1.getOperands().set(j, null);
							ze2.getOperands().set(fpos[i], null);
							break;
						}	
					}
				}
				
				if(current instanceof ZExpression && oppositeable(((ZExpression) current).getOperator()) && pos.length == 0 && !flipped) {
					//we like mostly only have two arguments
					int apos[] = searchForIn( new ZExpression(getOppositeArithmetic( ((ZExpression) current).getOperator())), ze2.getOperands());
					for(int i=0; i<apos.length; i++) {
						boolean result = examine(current,(ZExp) ze2.getOperands().get(apos[i]));
						if(result) {
							ze1.getOperands().set(j, null);
							ze2.getOperands().set(apos[i], null);
							break;
						}	
					}
				}

			}

			// IF OPD1 == OPD2 == EMPTY
			if (isAllNull(ze1.getOperands()) && isAllNull(ze2.getOperands())) {
				return true;
			} else {
				return false;
			}

		} else {
			for(int i=0; i< ze1.getOperands().size(); i++) {
				if(!examine((ZExp) ze1.getOperands().get(i), (ZExp) ze2.getOperands().get(i))) {
					return false;
				}
			}
			return true;
		}

		
	}

	public static boolean isAllNull(Vector<ZExp> t) {
		Iterator<ZExp> it = t.iterator();
		while (it.hasNext()) {
			if (it.next() != null)
				return false;
		}
		return true;
	}

	public static boolean flippable(ZExp opx) {
		String op = "";
		if (opx instanceof ZExpression) {
			op = ((ZExpression) opx).getOperator();

			String[] ok = { ">", ">=", "<=", "<" };
			if (Arrays.asList(ok).contains(op))
				return true;
			return false;
		} else {
			return false;
		}
	}

	public static boolean flippable(String op) {
		String[] ok = { ">", ">=", "<=", "<" };
		if (Arrays.asList(ok).contains(op))
			return true;
		return false;
	}
	
	public static String getOppositeArithmetic(String op) {
		if(op.equals("+")) 
			return "-";
		if(op.equals("-")) 
			return "+";
		if(op.equals("*")) 
			return "/";
		if(op.equals("/")) 
			return "*";
		return op;
	}
	
	public static boolean oppositeable(String op) {
		String[] ok = { "+", "-", "*", "/" };
		if (Arrays.asList(ok).contains(op))
			return true;
		return false;
	}

	public static boolean kommutativ(String op) {
		return !flippable(op);
	}

	public static String zexp2string(ZExp z) {
		if (z instanceof ZConstant) {
			return ((ZConstant) z).getValue();
		} else if (z instanceof ZExpression) {
			return ((ZExpression) z).getOperator();
		}
		return "nichts";
	}

	public static int[] searchForIn(ZExp needle, Vector<ZExp> vhaystack) {
		String sneedle = zexp2string(needle);

		String[] haystack = new String[vhaystack.size()];
		for (int i = 0; i < haystack.length; i++) {
			haystack[i] = zexp2string(vhaystack.get(i));
		}

		ArrayList<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < haystack.length; i++) {
			if (sneedle.equals(haystack[i])) {
				result.add(new Integer(i));
			}
		}

		int[] ires = new int[result.size()];
		for (int i = 0; i < ires.length; i++) {
			ires[i] = result.get(i).intValue();
		}

		return ires;
	}

}

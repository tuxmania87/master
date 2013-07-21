package de.unihalle.sqlequalizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZQuery;

import com.mysql.jdbc.StringUtils;

public class Haupt {

	/**
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		QueryHandler q2 = new QueryHandler();
		q2.createTable("create table emp (empno int, ename varchar(500), job varchar(500), mgr int, hiredate datetime, sal int not null, comm int, deptno int)");
		q2.createTable("create table dept (deptno int, dname varchar(500), location varchar(500))");
		q2.createTable("create table test (a float(5,2) references emp(sal) not null, b int, c int, d int, x int, y int)");
		q2.createTable("create table test2 (a int)");

		q2.setOriginalStatement("select sal, ename from emp e where 10 > 5 and ( 2 > 3 or 3 > 2);");
		System.out.println(q2.original);
		ZQuery res = q2.equalize(false);
		System.out.println(res);

		System.out.println();

		System.out.println("STELLE: " + QueryUtils.places("a1.sal", q2));

		q2 = new QueryHandler();
		q2.createTable("create table emp (empno int, ename varchar(500), job varchar(500), mgr int, hiredate datetime, sal int not null, comm int, deptno int)");
		q2.createTable("create table dept (deptno int, dname varchar(500), location varchar(500))");
		q2.createTable("create table test (a int references emp(sal) not null, b int, c int, d int, x int, y int)");

		q2.setOriginalStatement("select sal from emp e where sal > deptno ;");
		System.out.println(q2.original);
		res = q2.equalize(true);
		System.out.println(res);

		String[] feld = new String[] { "a", "b1", "b2", "c", "d1", "d2", "e", "f", "g1", "g2", "g3" };
		int[][] pos =  { new int[] { 0,3,4,5,6,7,8,9,10  }, new int[] {0,1,2,3,6,7,8,9,10}, new int[] {0,1,2,3,4,5,6,7} };

		String[][] test = getPermutationOfWithFixedPosGroupes(feld, pos);
		
		
		String[] feldx = new String[] { "a", "e1", "e2", "c", "d1", "d2" };
		
		
		String[][] anotherTest = new String[3][];
		anotherTest[0] = new String[] { "b1" , "b2" };
		anotherTest[1] = new String[] { "d1" , "d2" };
		anotherTest[2] = new String[] { "e1" , "e2" };
		
		testf1(new String[0][],anotherTest, 0);
		
	}

	/*
	 * def permute(charSet, soFar): if charSet is empty: print soFar //base case
	 * for each element 'e' of charSet permute(charSet without e, soFar + e)
	 * //recurse
	 */
	
	
	public static void testf1(String[][] right,String[][] data, int i) {
		if( i == data.length) {
			doubleArrayOutput(right);
			return;
		}
		
		ArrayList<String[]> perms = new ArrayList<String[]>();
		permute(data[i], new String[0], perms);
		
		for(int j = 0; j< perms.size(); j++) {
			String[][] t_feld = new String[right.length+1][];
			int ty = 0;
			for(; ty < right.length; ty++) {
				t_feld[ty] = right[ty].clone();
			}
			t_feld[ty] = perms.get(j).clone();
			
			testf1(t_feld,data,i+1);
		}
		
	}
	
	
	private static void doubleArrayOutput(String[][] test) {
		for(int i=0; i< test.length; i++) {
			for(int j=0; j< test[i].length; j++) {
				System.out.print(test[i][j]+" ");
			}
			System.out.print(" | ");
		}
		System.out.println();
	}

	private static boolean isNumberinArray(int needle, int[] haystack) {
		for (int i = 0; i < haystack.length; i++)
			if (needle == haystack[i])
				return true;

		return false;
	}

	private static int[][] removeElemFromPos(int pos, int[][] haystack) {
		int[][] ret = new int[haystack.length-1][];
		int c = 0;
		int i = 0;
		
		while(c < ret.length) {
			if(i == pos)
				i++;
			else {
				ret[c++] = haystack[i++];
			}
		}
		
		return ret;
	}
	
	public static String[][] getPermutationOfWithFixedPosGroupes(String[] arr,
			int[][] fixedPos) {
		
		ArrayList<String[]> collect = new ArrayList<String[]>();
		
		for(int i=0; i< fixedPos.length; i++) {
			int[] curGroup = fixedPos[i];
			
			int[][] restGroup = removeElemFromPos(i, fixedPos);
			
			String[][] t_res = getPermutationOfWithFixedPos(arr, curGroup);
			
			for(int j = 0; j<t_res.length; j++) {
				String[] cur = t_res[j];
				for(int x = 0; x< restGroup.length; x++) {
					String[][] t_res2 = getPermutationOfWithFixedPos(cur, restGroup[x]);
					
					for(int i1=0; i1< t_res2.length; i1++) {
						collect.add(t_res2[i1]);
					}
				}
			}
			
			//doubleArrayOutput(t_res);
		}
		
		//sort
		
		class t_comprarer implements Comparator<String[]> {

			@Override
			public int compare(String[] o1, String[] o2) {
				String s1="";
				String s2="";
				for(int i=0; i< o1.length; i++) {
					s1 += o1[i]+",";
					s2 += o2[i]+",";
				}
				return s1.compareTo(s2);
			}
		}
		
		Collections.sort(collect, new t_comprarer());
		
		int i = 0;
		String checker = "";
		
		ArrayList<String[]> SetList = new ArrayList<String[]>();
		
		while(i < collect.size()) {
			if (checker.equals(implode(",",collect.get(i)))) {
				
			} else {
				checker = implode(",",collect.get(i));
				SetList.add(collect.get(i));
			}
			i++;
		}
		
		System.out.println(collect.size()+" "+SetList.size());
		
		for(int iy = 0; iy< collect.size(); iy++)
			System.out.println(implode(",",collect.get(iy)));
		
		return null;
	}
	
	private static String implode(String glue, String[] arr) {
		String res = "";
		
		for(int i = 0; i< arr.length; i++) {
			res += arr[i]+glue;
		}
		
		return res.substring(0,res.length()-glue.length());
	}
	
	public static String[][] getPermutationOfWithFixedPos(String[] arr,
			int[] fixedPos) {

		// step 1 filter fixed values

		String[] filtered = new String[arr.length - fixedPos.length];
		int count = 0;
		int ncount = 0;

		while (count < arr.length) {
			if (isNumberinArray(count, fixedPos)) {
				count++;
			} else {
				filtered[ncount++] = arr[count++];
			}

		}

		ArrayList<String[]> res = new ArrayList<String[]>();

		Haupt.permute(filtered, new String[0], res);

		String[][] newRes = new String[res.size()][];

		for (int i = 0; i < res.size(); i++) {
			String[] cur = res.get(i);
			String[] newCur = new String[cur.length + fixedPos.length];

			int inputIndex = 0;
			int oldIndex = 0;

			while (inputIndex < newCur.length) {

				if (isNumberinArray(inputIndex, fixedPos)) {
					newCur[inputIndex] = arr[inputIndex];
					inputIndex++;
				} else {
					newCur[inputIndex++] = cur[oldIndex++];
				}
			}
			newRes[i] = newCur;

		}

		return newRes;
	}

	public static void permute(String[] in, String[] res,
			ArrayList<String[]> realResult) {
		if (in.length == 0) {
			// output
			realResult.add(res);
		}

		for (int i = 0; i < in.length; i++) {

			String[] newin = new String[in.length - 1];
			int newcount = 0;
			int oldcount = 0;

			while (newcount < newin.length) {
				if (oldcount == i)
					oldcount++;

				newin[newcount++] = in[oldcount++];
			}

			String[] newres = new String[res.length + 1];
			int j = 0;
			for (; j < res.length; j++) {
				newres[j] = res[j];
			}
			newres[j] = in[i];

			permute(newin, newres, realResult);

		}

	}

	public static boolean examine(ZExp e1, ZExp e2) {

		if (e1 instanceof ZConstant && e2 instanceof ZConstant)
			return ((ZConstant) e1).getValue().equals(
					((ZConstant) e2).getValue());

		ZExpression ze1 = (ZExpression) e1;
		ZExpression ze2 = (ZExpression) e2;

		if (!ze1.getOperator().equals(ze2.getOperator())
				&& !flippable(ze1.getOperator())
				&& !oppositeable(ze1.getOperator())) {
			return false;
		}

		if (ze1.getOperator().equals(
				QueryUtils.getFlippedOperator(ze2.getOperator()))
				&& flippable(ze1.getOperator())) {
			ZExpression temp = new ZExpression(
					QueryUtils.getFlippedOperator(ze1.getOperator()));
			Vector<ZExp> t2 = (Vector<ZExp>) ze1.getOperands().clone();
			Collections.reverse(t2);
			temp.setOperands(t2);
			return examine(temp, ze2);
		}

		if (oppositeable(ze1.getOperator())
				&& ze1.getOperator().equals(
						getOppositeArithmetic(ze2.getOperator()))) {
			if (ze1.getOperands().size() != ze2.getOperands().size()) {// teilbaueme
																		// muessen
																		// gleich
																		// viele
																		// argumente
																		// haben
				return false;
			}
			for (int i = 1; i < ze1.getOperands().size(); i++) {
				if (!examine((ZExp) ze1.getOperands().get(i), (ZExp) ze2
						.getOperands().get(i))) {
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
				if (flippable(current) && pos.length == 0) {
					int fpos[] = searchForIn(
							new ZExpression(
									QueryUtils
											.getFlippedOperator(((ZExpression) current)
													.getOperator())),
							ze2.getOperands());
					if (fpos.length > 0)
						flipped = true;
					for (int i = 0; i < fpos.length; i++) {
						boolean result = examine(current, (ZExp) ze2
								.getOperands().get(fpos[i]));
						if (result) {
							ze1.getOperands().set(j, null);
							ze2.getOperands().set(fpos[i], null);
							break;
						}
					}
				}

				if (current instanceof ZExpression
						&& oppositeable(((ZExpression) current).getOperator())
						&& pos.length == 0 && !flipped) {
					// we like mostly only have two arguments
					int apos[] = searchForIn(
							new ZExpression(
									getOppositeArithmetic(((ZExpression) current)
											.getOperator())), ze2.getOperands());
					for (int i = 0; i < apos.length; i++) {
						boolean result = examine(current, (ZExp) ze2
								.getOperands().get(apos[i]));
						if (result) {
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
			for (int i = 0; i < ze1.getOperands().size(); i++) {
				if (!examine((ZExp) ze1.getOperands().get(i), (ZExp) ze2
						.getOperands().get(i))) {
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
		if (op.equals("+"))
			return "-";
		if (op.equals("-"))
			return "+";
		if (op.equals("*"))
			return "/";
		if (op.equals("/"))
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

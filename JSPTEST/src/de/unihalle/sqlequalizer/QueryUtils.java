package de.unihalle.sqlequalizer;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import org.gibello.zql.ParseException;
import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZFromItem;
import org.gibello.zql.ZQuery;
import org.gibello.zql.ZSelectItem;
import org.gibello.zql.ZStatement;
import org.gibello.zql.ZqlParser;

/**
 * Class is a useful collection of static functions helping to handle queries
 * 
 * @author Robert Hartmann
 *
 */
public class QueryUtils {

	//mes is a Hasmap containg output strings when Metainfo of queries are compared
	// it maps class attributes to output strings
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
		mes.put("samplesolution", "the sample solution");
		mes.put("aWITHSPACE", "a ");
		mes.put("noSPACE", "no ");
		mes.put("one", "one");
		mes.put("none", "none");

		messages = mes;
	}

	public static HashMap<String, String> messages;

	//An operator with lesser index has a higher priority
	// there is no AND because in KNF, AND only occures as Root Node
	public static String orderList[] = { "OR", "<=", ">=", "<", ">", "=", "+", "-",
			"IS NULL", "IS NOT NULL", "EXISTS", "ANY", "ALL" };

	//supported database types for external database
	public static String validDBtypes[] = { "mysql", "postgresql", "oracle" };
	
	public static String commutativeOperators[] = { "=", "OR", "AND", "+" };
	public static String comparisonOperators[] = { "<", "<=", ">", ">=", "=" };
	public static String subQueryOperators[] = { "EXISTS", "ANY", "SOME", "ALL" };

	/**
	 * Checks if table exist in FROM Clause
	 * @param table Table to be checked
	 * @param fromClause FromClause 
	 * @return true if table is in fromClause, false otherwise
	 */
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
	
	public static ZQuery cloneQuery(ZQuery i) {
		ZqlParser zp = new ZqlParser();
		
		String qstring = i.toString()+";";
		zp.initParser(new ByteArrayInputStream(qstring.getBytes()));

		ZStatement zs = null;
		try {
			zs = zp.readStatement();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ZQuery q = (ZQuery) zs;
	
		return q;
	}

	public static String formatDBSchema(String schema) {

		String response = "";
		boolean firstParen = false;
		int space = 0;
		for (int i = 0; i < schema.length(); i++) {
			if (schema.charAt(i) == '(' && !firstParen) {
				firstParen = true;
				response += "(<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				continue;
			} else if (schema.charAt(i) == ' ') {
				space++;
				if (space == 2)
					response += "<span style=\"font-weight:bold;\">";
				if (space == 3)
					response += "</span>";
			} else if (firstParen && schema.charAt(i) == ',') {
				response += ",<br>&nbsp;&nbsp;&nbsp;&nbsp;";
				continue;
			}

			if (i == schema.length() - 1)
				response += "<br>";
			response += schema.charAt(i);
		}

		return response;
	}

	/**
	 * Returns flipped operator as in 
	 * a OPERATOR b equals b FLIPPED_OPERATOR a
	 * 
	 * @param op Operator to be flipped
	 * @return Flipped Operator
	 */
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

	/**
	 * Returns opposite Operator as in
	 * Not( a OPERATOR B) equals a OPPOSITE_OPERATOR b
	 * 
	 * @param op Operator 
	 * @return Opposite operator of op
	 */
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

	
	/**
	 * Executes a DFS stepwise on two trees represented by rootnode r1 and rootnode r2.
	 * If the two current nodes differ we check their Values with OperatorComparer() and 
	 * return the result
	 * 
	 * @param r1 rootnode of tree 1
	 * @param r2 rootnode of tree 2
	 * @return 1 if r2 is less than r1, -1 the other way around, 0 if both trees are equal
	 */
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
		
		if (r1 instanceof ZQuery && !(r2 instanceof ZQuery))
			return -1;

		if (r2 instanceof ZQuery && !(r1 instanceof ZQuery))
			return 1;

		if (r1 instanceof ZQuery && r2 instanceof ZQuery) {
			// depth has to decide
			ZQuery z1 = (ZQuery) r1;
			ZQuery z2 = (ZQuery) r2;

			return DFSFirstDifferentNodeValue(z1.getWhere(), z2.getWhere());
		}
		
		/*if (r1 instanceof ZExpression && r2 instanceof ZConstant) {
			return 1;
		}

		if (r2 instanceof ZExpression && r1 instanceof ZConstant) {
			return -1;
		}
		
		if(r2 instanceof ZConstant && r1 instanceof ZConstant) {
			ZConstant zc1 = (ZConstant) r1;
			ZConstant zc2 = (ZConstant) r2;
			
			return zc1.getValue().compareTo(zc2.getValue());
		}*/
		
		return new OperatorComparer().compare(r1, r2);
	
	}

	public static ZExp negate(ZExp exp) {
		if (exp instanceof ZConstant)
			return exp;

		if (exp instanceof ZQuery) {
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

	/**
	 * Checks if the ResultSets of two sql queries are identical
	 * 
	 * @param r1 ResultSet of query 1
	 * @param r2 ResultSet of query 2
	 * @return true if r1 equals r2, false otherwise
	 * @throws SQLException
	 */
	public static boolean isIdenticalResultSets(ResultSet r1, ResultSet r2)
			throws SQLException {

		ResultSetMetaData m1 = r1.getMetaData();
		ResultSetMetaData m2 = r2.getMetaData();

		if (m1.getColumnCount() != m2.getColumnCount())
			return false;

		int k = 0;
		try {

			while (r1.next() && r2.next()) {
				for (int i = 1; i <= m1.getColumnCount(); i++) {

					Object res1 = r1.getObject(i);
					Object res2 = r2.getObject(i);

					if (res1 == null && res2 == null)
						continue;

					if (!res1.equals(res2)) {
						throw new RuntimeException(String.format(
								"%s and %s aren't equal at common position %d",
								res1, res2, i));
					}

					// rs1 and rs2 must reach last row in the same iteration
					if ((r1.isLast() != r2.isLast())) {
						throw new RuntimeException(
								"The two ResultSets contains different number of columns!");
					}

				}
				k++;
			}
		} catch (SQLException e) {
			return false;
		}

		if(k == 0) {
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

	/**
	 * gets Depth of a tree represented by exp
	 * 
	 * @param exp RootNode of tree
	 * @return Depth of Tree
	 */
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

	/**
	 * Applies the distribution law so that we can form a CNF
	 * 
	 * @param r rootnode of formula
	 * @return CNF of formula represented by r
	 */
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

	/**
	 * Adjust Funtion as it is presented in master thesis
	 * 
	 * @param attribute 
	 * @param q
	 * @return adjusted value
	 */
	public static double adjust(String attribute, QueryHandler q) {
		int places1 = places(attribute, q);

		return 1 / Math.pow(10, places1);
	}

	/**
	 * If attribute is a numeric value, returns the places after the colon by using tables saved in QueryHandler q Object
	 * e.g.  2.34 has two places (34) after the colon
	 * 
	 * @param attribute
	 * @param q
	 * @return Number of places after point/colon
	 */
	public static int places(String attribute, QueryHandler q) {
		// attribte has the form tablealias.varname
		String table = attribute.split("\\.")[0];
		String att = attribute.split("\\.")[1];
		Set<String> keys = q.Zuordnung.keySet();
		Iterator<String> it = keys.iterator();

		while (it.hasNext()) {
			String key = it.next();
			if (q.Zuordnung.get(key).equals(table)) {
				// check if key is an old alias or an actual table
				for (int i = 0; i < q.tables.size(); i++) {
					if (q.tables.get(i).name.equals(key)) {
						// we found the actual table, return places
						for (int j = 0; j < q.tables.get(i).columns.size(); j++) {
							if (q.tables.get(i).columns.get(j).name.equals(att)) {
								return q.tables.get(i).columns.get(j).digitsAfterColon;
							}
						}
					}
				}

			}
		}
		return 0;
	}

	/**
	 * Sorts the tree represented by root node exp
	 * 
	 * @param exp rootnode representing a tree
	 * @return sorted trees rootnode
	 */
	public static ZExp sortTree(ZExp exp) {
		if (exp instanceof ZConstant)
			return exp;

		if (exp instanceof ZExpression) {
			ZExpression e = (ZExpression) exp;

			ZExpression ret = new ZExpression(e.getOperator());
			// proceed with sorting children
			for (int i = 0; i < e.getOperands().size(); i++) {
				ret.addOperand(sortTree(e.getOperand(i)));
			}

			// if current Operator is commutative, then we
			// can sort the childs in the order we want
			if (Arrays.asList(commutativeOperators).contains(ret.getOperator())) {
				Collections.sort(ret.getOperands(), new OperatorComparer());
			}

			return ret;

		}
		return exp;

	}


	
	/**
	 * Meta function controlling the sort process that is sorting and elimation of duplicates
	 * 
	 * @param exp rootnode representing a tree
	 * @return sorted trees rootnode
	 */
	public static ZExp sortedTree(ZExp exp) {

		if (exp == null)
			return null;

		String old = null;
		ZExp ret = exp;
		do {
			old = ret.toString();
			ret = sortTree(ret);
			ret = evaluateArithmetic(ret);
			
		} while (!old.equals(ret.toString()));

		// duplicate are beeing removed now
		ret = removeDuplicates(ret);

		return ret;
	}

	/**
	 * After parser Tree is sorted we can find Duplicates of AND and OR nodes easily by investigate the neighbour nodes
	 * 
	 * @param exp rootnode representing a tree
	 * @return duplicate free tree (considering AND and OR)
	 */
	public static ZExp removeDuplicates(ZExp exp) {

		if (exp instanceof ZExpression) {
			ZExpression casted = (ZExpression) exp;
			if (!casted.getOperator().toLowerCase().equals("and")
					&& !casted.getOperator().toLowerCase().equals("or"))
				return exp;
			else {
				ZExpression newExp = new ZExpression(casted.getOperator());
				String checker = "";

				Iterator<ZExp> childs = casted.getOperands().iterator();
				while (childs.hasNext()) {
					ZExp current = childs.next();
					if (checker.equals(current.toString())) {
					} else {
						checker = current.toString();
						newExp.addOperand(removeDuplicates(current));
					}
				}

				return newExp;
			}
		}

		return exp;

	}

	
	public static String compareMetaInfos2(QueryHandler[] ss, MetaQueryInfo m2) {
		
		return "";
	}
	
	/**
	 * Compares two MetaInfoQuery Objects coming from two queries.
	 * Formulates a String containing every difference from the two queries.
	 * 
	 * @param m1 Metainfo of Query 1
	 * @param m2 Metainfo of Query 2
	 * @return All diferences in String
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
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
							+ messages.get("samplesolution") + " ("
							+ f.getInt(m1) + ") than in yours (" + f.getInt(m2)
							+ ").";
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

	private static <T> boolean equalsVectors(Vector<T> v1, Vector<T> v2) {
		if (v1.size() != v2.size())
			return false;

		for (int i = 0; i < v1.size(); i++) {
			T z1 = v1.get(i);
			T z2 = v2.get(i);

			if (!z1.toString().equals(z2.toString()))
				return false;
		}

		return true;
	}

	public static Vector<ZFromItem> cloneFromItems(Vector<ZFromItem> data) {

		Vector<ZFromItem> res = new Vector<ZFromItem>();
		Iterator<ZFromItem> i = data.iterator();

		while (i.hasNext()) {
			ZFromItem z = i.next();

			ZFromItem clone = new ZFromItem(z.getTable());
			clone.setAlias(z.getAlias());
			res.add(clone);
		}

		return res;

	}

	
	public static ArrayList<String[]> createPermutationsSetClause(String[][] right,
			String[][] data, int i) {
		if (i == data.length) {
			
			String[] t_res = new String[right.length];
			
			for(int ix=0; ix<right.length; ix++) {
				t_res[ix] = right[ix][0];
			}
			
			ArrayList<String[]> t_res2 = new ArrayList<String[]>();
			t_res2.add(t_res);
			return t_res2;
		}

		ArrayList<String[]> perms = new ArrayList<String[]>();
		permute(data[i], new String[0], perms);

		ArrayList<String[]> erg = new ArrayList<String[]>();

		for (int j = 0; j < perms.size(); j++) {
			String[][] t_feld = new String[right.length + 1][];
			int ty = 0;
			for (; ty < right.length; ty++) {
				t_feld[ty] = right[ty].clone();
			}
			t_feld[ty] = perms.get(j).clone();

			erg.addAll(createPermutationsSetClause(t_feld, data, i + 1));
			System.out.println("BREAK");

		}
		return erg;
	}
	
	public static ArrayList<String[]> createPermutations(String[][] right,
			String[][] data, int i) {
		if (i == data.length) {
			String res = "";
			for (int x = 0; x < right.length; x++) {
				res += implode(",", right[x]) + ",";
			}
			ArrayList<String[]> t_res = new ArrayList<String[]>();
			t_res.add(res.substring(0, res.length() - 1).split(","));
			return t_res;
		}

		ArrayList<String[]> perms = new ArrayList<String[]>();
		permute(data[i], new String[0], perms);

		ArrayList<String[]> erg = new ArrayList<String[]>();

		for (int j = 0; j < perms.size(); j++) {
			String[][] t_feld = new String[right.length + 1][];
			int ty = 0;
			for (; ty < right.length; ty++) {
				t_feld[ty] = right[ty].clone();
			}
			t_feld[ty] = perms.get(j).clone();

			erg.addAll(createPermutations(t_feld, data, i + 1));

		}
		return erg;
	}

	private static String implode(String glue, String[] arr) {
		String res = "";

		for (int i = 0; i < arr.length; i++) {
			res += arr[i] + glue;
		}

		return res.substring(0, res.length() - glue.length());
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

	/**
	 * Compares two standardized queries by comparing each part of the queries.
	 * Formulates a String containg all none matching parts of the two queries
	 * 
	 * @param q1 
	 * @param q2
	 * @return All Diferences in a String
	 */
	public static String compareStandardizedQueries(ZQuery q1, ZQuery q2) {
		// additionally we want to compare single parts of the queries
		// (select,from,where,group by, having, order by)
		String res = "";

		if ((q1.getSelect() != null && q2.getSelect() != null && !equalsVectors(
				q1.getSelect(), q2.getSelect()))
				|| (q1.getSelect() == null && q2.getSelect() != null)
				|| ((q1.getSelect() != null && q2.getSelect() == null))) {
			res += "<span style=\"font-weight:bold;\">SELECT:</span>   <span style=\"color:red;\">not identical!</span><br>";
		}

		if ((q1.getFrom() != null && q2.getFrom() != null && !equalsVectors(
				q1.getFrom(), q2.getFrom()))
				|| (q1.getFrom() == null && q2.getFrom() != null)
				|| ((q1.getFrom() != null && q2.getFrom() == null))) {
			res += "<span style=\"font-weight:bold;\">FROM:</span>   <span style=\"color:red;\">not identical!</span><br>";
		}

		if ((q1.getWhere() != null && q2.getWhere() != null && !q1.getWhere()
				.toString().toLowerCase()
				.equals(q2.getWhere().toString().toLowerCase()))
				|| (q1.getWhere() == null && q2.getWhere() != null)
				|| ((q1.getWhere() != null && q2.getWhere() == null))) {
			res += "<span style=\"font-weight:bold;\">WHERE:</span>   <span style=\"color:red;\">not identical!</span><br>";
		}

		if ((q1.getGroupBy() != null && q2.getGroupBy() != null && !q1
				.getGroupBy().toString().toLowerCase()
				.equals(q2.getGroupBy().toString().toLowerCase()))
				|| (q1.getGroupBy() == null && q2.getGroupBy() != null)
				|| ((q1.getGroupBy() != null && q2.getGroupBy() == null))) {
			res += "<span style=\"font-weight:bold;\">GROUP BY:</span>   <span style=\"color:red;\">not identical!</span><br>";
		}

		if ((q1.getOrderBy() != null && q2.getOrderBy() != null && !equalsVectors(
				q1.getOrderBy(), q2.getOrderBy()))
				|| (q1.getOrderBy() == null && q2.getOrderBy() != null)
				|| ((q1.getOrderBy() != null && q2.getOrderBy() == null))) {
			res += "<span style=\"font-weight:bold;\">ORDER BY:</span>   <span style=\"color:red;\">not identical!</span><br>";
		}

		return res;
	}

	/**
	 * Operator Compression as explained in @see master thesis
	 * 
	 * @param exp Current Node
	 * @param parent Parent Node
	 * @return Tree without unnecessary parenthesis
	 */
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

			if (((ZExpression) exp).getOperator().equals("NOT")
					&& ((ZExpression) exp).getOperand(0) instanceof ZExpression
					&& !Arrays.asList(QueryUtils.subQueryOperators).contains(
							((ZExpression) ((ZExpression) exp).getOperand(0))
									.getOperator())) {
				toHandle = (ZExpression) negate(z.getOperand(0));
			} else {
				toHandle = z;
			}

			Iterator<ZExp> it = toHandle.getOperands().iterator();

			ZExpression ret = new ZExpression(toHandle.getOperator());

			while (it.hasNext()) {
				ret.addOperand(pushDownNegate(it.next()));
			}

			return ret;

		}

		return exp;

	}

	
	private static boolean areChildsNumeric(Vector<ZExp> childs) {
		Iterator<ZExp> it = childs.iterator();

		while (it.hasNext()) {
			ZExp cur = it.next();
			if (!(cur instanceof ZConstant)
					|| ((ZConstant) cur).getType() != ZConstant.NUMBER)
				return false;
		}

		return true;
	}

	private static boolean areChildsNumericORString(Vector<ZExp> childs) {
		Iterator<ZExp> it = childs.iterator();

		while (it.hasNext()) {
			ZExp cur = it.next();
			if (!(cur instanceof ZConstant)
					|| (((ZConstant) cur).getType() != ZConstant.NUMBER && ((ZConstant) cur)
							.getType() != ZConstant.STRING))
				return false;
		}

		return true;
	}

	private static ZExp formatAllNumerics(ZExp root, int places) {
		if (root instanceof ZConstant) {
			ZConstant c = (ZConstant) root;
			if (c.getType() == ZConstant.NUMBER) {

				String pattern = "0";
				if (places > 0) {
					pattern = "0.";
					for (int i = 0; i < places; i++)
						pattern += "0";
				}

				NumberFormat nf = NumberFormat
						.getNumberInstance(Locale.ENGLISH);
				DecimalFormat df = (DecimalFormat) nf;
				df.applyPattern(pattern);

				return new ZConstant(
						df.format(Double.parseDouble(c.getValue())),
						ZConstant.NUMBER);
			}
		}

		if (root instanceof ZExpression) {
			ZExpression z = (ZExpression) root;
			Iterator<ZExp> it = z.getOperands().iterator();

			ZExpression newExp = new ZExpression(z.getOperator());
			while (it.hasNext()) {
				newExp.addOperand(formatAllNumerics(it.next(), places));
			}
			return newExp;
		}

		return root;
	}

	/**
	 * Rounds the numeric constants to their correct places after colon
	 * 
	 * @param exp Arithmetic Expression
	 * @param fromList Tables under FROM
	 * @param qh QueryHandler with Information about data types 
	 * @return rootnode of tree with correct decimal places
	 */
	public static ZExp correctDecimalPlaces(ZExp exp,
			Vector<ZFromItem> fromList, QueryHandler qh) {
		if (exp instanceof ZExpression) {
			ZExpression z = (ZExpression) exp;
			if (z.getOperator().equals("=") || z.getOperator().equals(">=")
					|| z.getOperator().equals("<=")
					|| z.getOperator().equals(">")
					|| z.getOperator().equals("<")) {

				ZConstant dominantConstant = null;
				if (z.getOperand(0) instanceof ZConstant
						&& ((ZConstant) z.getOperand(0)).getType() == ZConstant.COLUMNNAME) {
					dominantConstant = (ZConstant) z.getOperand(0);
				}

				if (z.getOperand(1) instanceof ZConstant
						&& ((ZConstant) z.getOperand(1)).getType() == ZConstant.COLUMNNAME) {
					dominantConstant = (ZConstant) z.getOperand(1);
				}

				if (dominantConstant == null)
					return exp;
				else {
					// look for datatype in definition
					String tablealias = dominantConstant.getValue()
							.split("\\.")[0];
					String attributename = dominantConstant.getValue().split(
							"\\.")[1];
					String tablename = null;

					for (int i = 0; i < fromList.size(); i++) {
						ZFromItem zfi = fromList.get(i);
						if (zfi.getAlias().equals(tablealias))
							tablename = zfi.getTable();
					}

					Integer places = null;

					for (int i = 0; i < qh.tables.size(); i++) {
						Table t = qh.tables.get(i);
						if (t.name.equals(tablename)) {
							for (int j = 0; j < t.columns.size(); j++) {
								if (t.columns.get(j).name.equals(attributename)
										&& t.columns.get(j).type == Column.NUMBER) {
									places = new Integer(
											t.columns.get(j).digitsAfterColon);
								}
							}
						}
					}

					if (places == null)
						return exp;
					else {
						return formatAllNumerics(exp, places.intValue());
					}

				}

			} else {
				ZExpression newExp = new ZExpression(z.getOperator());
				Iterator<ZExp> it = z.getOperands().iterator();
				while (it.hasNext()) {
					newExp.addOperand(correctDecimalPlaces(it.next(), fromList,
							qh));

				}
				return newExp;
			}
		}
		return exp;
	}

	/**
	 * Evaluate all Arithmetic and logic Expressions if they only contain constants
	 * 
	 * @param exp rootnode of arithmetic expression
	 * @return rootnode of evaluated arithmetic expression
	 */
	public static ZExp evaluateArithmetic(ZExp exp) {

		if (exp instanceof ZConstant)
			return exp;

		if (exp instanceof ZExpression) {
			ZExpression z = (ZExpression) exp;

			ZExpression ret = new ZExpression(z.getOperator());

			Iterator<ZExp> it = z.getOperands().iterator();
			while (it.hasNext()) {
				ZExp evalueted = evaluateArithmetic(it.next());
				if (evalueted != null)
					ret.addOperand(evalueted);
			}

			if (ret.getOperands() == null) {
				return null;
			}

			// check childs
			if (ret.getOperator().equals("+")
					&& areChildsNumeric(ret.getOperands())) {
				double val = Double.parseDouble(((ZConstant) ret.getOperand(0))
						.getValue())
						+ Double.parseDouble(((ZConstant) ret.getOperand(1))
								.getValue());

				return new ZConstant(String.valueOf(val), ZConstant.NUMBER);
			}

			if (ret.getOperator().equals("-")
					&& areChildsNumeric(ret.getOperands())) {
				return new ZConstant(
						String.valueOf(Double.parseDouble(((ZConstant) ret
								.getOperand(0)).getValue())
								- Double.parseDouble(((ZConstant) ret
										.getOperand(1)).getValue())),
						ZConstant.NUMBER);
			}

			if (ret.getOperator().equals("*")
					&& areChildsNumeric(ret.getOperands())) {
				return new ZConstant(
						String.valueOf(Double.parseDouble(((ZConstant) ret
								.getOperand(0)).getValue())
								* Double.parseDouble(((ZConstant) ret
										.getOperand(1)).getValue())),
						ZConstant.NUMBER);
			}

			if (ret.getOperator().equals("/")
					&& areChildsNumeric(ret.getOperands())) {
				return new ZConstant(
						String.valueOf(Double.parseDouble(((ZConstant) ret
								.getOperand(0)).getValue())
								/ Double.parseDouble(((ZConstant) ret
										.getOperand(1)).getValue())),
						ZConstant.NUMBER);
			}

			if (ret.getOperator().equals(">")
					&& areChildsNumericORString(ret.getOperands())) {
				if (areChildsNumeric(ret.getOperands())) {
					boolean val = Double.parseDouble(((ZConstant) ret
							.getOperand(0)).getValue()) > Double
							.parseDouble(((ZConstant) ret.getOperand(1))
									.getValue());
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				} else {
					boolean val = ((ZConstant) ret.getOperand(0)).getValue()
							.compareTo(
									((ZConstant) ret.getOperand(1)).getValue()) == 1;
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				}
			}

			if (ret.getOperator().equals("<")
					&& areChildsNumericORString(ret.getOperands())) {
				if (areChildsNumeric(ret.getOperands())) {
					boolean val = Double.parseDouble(((ZConstant) ret
							.getOperand(0)).getValue()) < Double
							.parseDouble(((ZConstant) ret.getOperand(1))
									.getValue());
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				} else {
					boolean val = ((ZConstant) ret.getOperand(0)).getValue()
							.compareTo(
									((ZConstant) ret.getOperand(1)).getValue()) == -1;
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				}
			}

			if (ret.getOperator().equals(">=")
					&& areChildsNumericORString(ret.getOperands())) {
				if (areChildsNumeric(ret.getOperands())) {
					boolean val = Double.parseDouble(((ZConstant) ret
							.getOperand(0)).getValue()) >= Double
							.parseDouble(((ZConstant) ret.getOperand(1))
									.getValue());
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				} else {
					boolean val = ((ZConstant) ret.getOperand(0)).getValue()
							.compareTo(
									((ZConstant) ret.getOperand(1)).getValue()) >= 0;
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				}
			}

			if (ret.getOperator().equals("<=")
					&& areChildsNumericORString(ret.getOperands())) {
				if (areChildsNumeric(ret.getOperands())) {
					boolean val = Double.parseDouble(((ZConstant) ret
							.getOperand(0)).getValue()) <= Double
							.parseDouble(((ZConstant) ret.getOperand(1))
									.getValue());
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				} else {
					boolean val = ((ZConstant) ret.getOperand(0)).getValue()
							.compareTo(
									((ZConstant) ret.getOperand(1)).getValue()) <= 0;
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				}
			}

			if (ret.getOperator().equals("=")
					&& areChildsNumericORString(ret.getOperands())) {
				if (areChildsNumeric(ret.getOperands())) {
					boolean val = Double.parseDouble(((ZConstant) ret
							.getOperand(0)).getValue()) == Double
							.parseDouble(((ZConstant) ret.getOperand(1))
									.getValue());
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				} else {
					boolean val = ((ZConstant) ret.getOperand(0)).getValue()
							.compareTo(
									((ZConstant) ret.getOperand(1)).getValue()) == 0;
					if (val) {
						return null;
					} else {
						return new ZConstant("false", ZConstant.NULL);
					}
				}
			}

			if (ret.getOperator().equals("AND")) {
				Iterator<ZExp> it2 = ret.getOperands().iterator();

				while (it2.hasNext()) {
					ZExp cur = it2.next();
					if (cur instanceof ZConstant
							&& ((ZConstant) cur).getType() == ZConstant.NULL
							&& ((ZConstant) cur).getValue().equals("false")) {
						return new ZConstant("false", ZConstant.NULL);
					}
				}
			}

			if (ret.getOperator().equals("OR")) {
				Iterator<ZExp> it2 = ret.getOperands().iterator();

				while (it2.hasNext()) {
					ZExp cur = it2.next();
					if (cur instanceof ZConstant
							&& ((ZConstant) cur).getType() == ZConstant.NULL
							&& ((ZConstant) cur).getValue().equals("false")) {
						return null;
					}
				}
			}

			if (ret.getOperands().size() == 0)
				return null;

			return ret;
		}

		return exp;
	}

	/**
	 * Adds implicit formulas warning! after this function one have to recall the
	 * KNF function because its structure will be destroyed in this function.
	 * 
	 * @return Rootnode of tree with added implicit formulas as stated in master thesis
	 */
	public static ZExp addImplicitFormulas(ZExp exp, QueryHandler q) {

		if (exp instanceof ZExpression) {

			ZExpression z = (ZExpression) exp;

			if (z.getOperator().equals("=")
					&& ((z.getOperand(0) instanceof ZExpression && z
							.getOperand(1) instanceof ZConstant) || (z
							.getOperand(1) instanceof ZExpression && z
							.getOperand(0) instanceof ZConstant))) {
				ZExpression zexp = (ZExpression) ((z.getOperand(0) instanceof ZExpression) ? z
						.getOperand(0) : z.getOperand(1));

				if ((zexp.getOperator().equals("+")
						|| zexp.getOperator().equals("-")
						|| zexp.getOperator().equals("*") || zexp.getOperator()
						.equals("/"))
						&& zexp.getOperands().size() == 2
						&& zexp.getOperand(0) instanceof ZConstant
						&& zexp.getOperand(1) instanceof ZConstant) {
					ZConstant zconst = (ZConstant) ((z.getOperand(0) instanceof ZConstant) ? z
							.getOperand(0) : z.getOperand(1));

					// replace occurence of all equivalent formulas ( in paper
					// its set M_1 )
					ZConstant a = (ZConstant) zexp.getOperand(0);
					ZConstant b = (ZConstant) zexp.getOperand(1);

					ZExpression andnode = new ZExpression("AND");

					if (zexp.getOperator().equals("+")) {
						ZExpression eq1 = new ZExpression("=");
						eq1.addOperand(a);
						eq1.addOperand(new ZExpression("-", zconst, b));

						ZExpression eq2 = new ZExpression("=");
						eq2.addOperand(b);
						eq2.addOperand(new ZExpression("-", zconst, a));

						andnode.addOperand(z);
						andnode.addOperand(eq1);
						andnode.addOperand(eq2);

						return andnode;
					} else if (zexp.getOperator().equals("-")) {
						// operator is "-"
						ZExpression eq1 = new ZExpression("=");
						eq1.addOperand(a);
						eq1.addOperand(new ZExpression("+", zconst, b));

						ZExpression eq2 = new ZExpression("=");
						eq2.addOperand(b);
						eq2.addOperand(new ZExpression("-", a, zconst));

						andnode.addOperand(z);
						andnode.addOperand(eq1);
						andnode.addOperand(eq2);

						return andnode;
					} else if (zexp.getOperator().equals("*")) {
						ZExpression eq1 = new ZExpression("=");
						eq1.addOperand(b);
						eq1.addOperand(new ZExpression("/", zconst, a));

						ZExpression eq2 = new ZExpression("=");
						eq2.addOperand(a);
						eq2.addOperand(new ZExpression("/", zconst, b));

						andnode.addOperand(z);
						andnode.addOperand(eq1);
						andnode.addOperand(eq2);

						return andnode;

					} else if (zexp.getOperator().equals("/")) {
						ZExpression eq1 = new ZExpression("=");
						eq1.addOperand(b);
						eq1.addOperand(new ZExpression("/", a, zconst));

						ZExpression eq2 = new ZExpression("=");
						eq2.addOperand(a);
						eq2.addOperand(new ZExpression("*", zconst, b));

						andnode.addOperand(z);
						andnode.addOperand(eq1);
						andnode.addOperand(eq2);

						return andnode;
					}

				}

			}

			if (z.getOperator().equals(">") || z.getOperator().equals("<")
					|| z.getOperator().equals(">=")
					|| z.getOperator().equals("<=")) {
				ZExpression andnode = new ZExpression("AND");

				ZExp child1 = addImplicitFormulas(z.getOperand(0), q);
				ZExp child2 = addImplicitFormulas(z.getOperand(1), q);

				ZExpression orig = new ZExpression(z.getOperator(), child1,
						child2);
				ZExpression mirror = new ZExpression(
						getFlippedOperator(z.getOperator()), child2, child1);

				andnode.addOperand(orig);
				andnode.addOperand(mirror);

				if ((!(child1 instanceof ZConstant) || !(child2 instanceof ZConstant))
						|| ((((ZConstant) child1).getType() != ZConstant.NUMBER) && (((ZConstant) child2)
								.getType() != ZConstant.NUMBER)))
					return andnode;

				ZConstant zc1 = (ZConstant) child1;
				ZConstant zc2 = (ZConstant) child2;

				if ((z.getOperator().equals(">")
						&& zc1.getType() != ZConstant.NUMBER && zc2.getType() == ZConstant.NUMBER)
						|| (z.getOperator().equals("<")
								&& zc1.getType() == ZConstant.NUMBER && zc2
								.getType() != ZConstant.NUMBER)) {

					ZConstant number = zc1.getType() == ZConstant.NUMBER ? zc1
							: zc2;
					ZConstant variable = zc1.getType() != ZConstant.NUMBER ? zc1
							: zc2;

					double sum = Double.parseDouble(number.getValue())
							+ adjust(variable.getValue(), q);
					ZExpression node1 = new ZExpression(
							">=",
							variable,
							new ZConstant(String.valueOf(sum), ZConstant.NUMBER));
					ZExpression node2 = new ZExpression("<=", new ZConstant(
							String.valueOf(sum), ZConstant.NUMBER), variable);
					andnode.addOperand(node1);
					andnode.addOperand(node2);
					return andnode;
				}

				if ((z.getOperator().equals(">=")
						&& zc1.getType() != ZConstant.NUMBER && zc2.getType() == ZConstant.NUMBER)
						|| (z.getOperator().equals("<=")
								&& zc1.getType() == ZConstant.NUMBER && zc2
								.getType() != ZConstant.NUMBER)) {

					ZConstant number = zc1.getType() == ZConstant.NUMBER ? zc1
							: zc2;
					ZConstant variable = zc1.getType() != ZConstant.NUMBER ? zc1
							: zc2;

					double adj = adjust(variable.getValue(), q);
					double x = Double.parseDouble(number.getValue()) - adj;

					ZExpression node1 = new ZExpression(">", variable,
							new ZConstant(String.valueOf(x), ZConstant.NUMBER));
					ZExpression node2 = new ZExpression("<", new ZConstant(
							String.valueOf(x), ZConstant.NUMBER), variable);
					andnode.addOperand(node1);
					andnode.addOperand(node2);
					return andnode;
				}

			}

			ZExpression ret = new ZExpression(z.getOperator());
			Iterator<ZExp> it = z.getOperands().iterator();
			while (it.hasNext()) {
				ret.addOperand(addImplicitFormulas(it.next(), q));
			}
			return ret;

		}

		return exp;
	}

	/**
	 * Replaces Subqueries as Stated in Master Thesis
	 * 
	 * @param exp rootnode representing tree where the subqueries should be replaced
	 * @param tables All used Tables under FROM for evaluating if a column can be null
	 * @return Rootnode of tree with replaced subqueries
	 */
	public static ZExp replaceSubqueries(ZExp exp, ArrayList<Table> tables) {

		if (exp instanceof ZExpression) {
			ZExpression z = (ZExpression) exp;
			
			if( (z.getOperator().equals("IN") || z.getOperator().equals("NOT IN")) && z.getOperand(1) instanceof ZQuery) {

				ZQuery subq = (ZQuery) z.getOperand(1);
				ZSelectItem selItem = (ZSelectItem) subq.getSelect()
						.firstElement();
				
				// we look up if t_2 may be null
				for (int i = 0; i < tables.size(); i++) {
					Table tt = tables.get(i);
					for (int j = 0; j < tt.columns.size(); j++) {
						if (tt.columns.get(j).name.equals(selItem
								.getColumn())
								&& tt.columns.get(j).canBeNull) {
							// we dont replace it because it can be null
							ZExpression ret = new ZExpression(
									((ZExpression) exp).getOperator());
							ret.addOperand(z.getOperand(0));
							ret.addOperand(replaceSubqueries(
									z.getOperand(1), tables));
							return ret;
						}
					}
				}
				
				ZExpression equal_op = new ZExpression("=");
				ZExpression any_op = new ZExpression("ANY");
				
				equal_op.addOperand(z.getOperand(0));
				equal_op.addOperand(any_op);
				
				any_op.addOperand(z.getOperand(1));
				
				ZExp result = replaceSubqueries(equal_op, tables);
				
				if(z.getOperator().equals("NOT IN")) {
					ZExpression ret = new ZExpression("NOT");
					ZExpression ret2 = new ZExpression("EXISTS");
					ret.addOperand(ret2);
					ret2.addOperand( ((ZExpression) result).getOperand(0));
					return ret;
				}
				
				return result;

			}
			if (Arrays.asList(comparisonOperators).contains(z.getOperator())) {
				if (z.getOperand(1) instanceof ZExpression) {
					ZExpression z2 = (ZExpression) z.getOperand(1);

					if (z2.getOperator().equals("ALL")) {

						ZQuery tquery = (ZQuery) z2.getOperand(0);
						ZSelectItem selItem = (ZSelectItem) tquery.getSelect()
								.firstElement();

						// we look up if t_2 may be null
						for (int i = 0; i < tables.size(); i++) {
							Table tt = tables.get(i);
							for (int j = 0; j < tt.columns.size(); j++) {
								if (tt.columns.get(j).name.equals(selItem
										.getColumn())
										&& tt.columns.get(j).canBeNull) {
									// we dont replace it because it can be null
									ZExpression ret = new ZExpression(
											((ZExpression) exp).getOperator());
									ret.addOperand(z.getOperand(0));
									ret.addOperand(replaceSubqueries(
											z.getOperand(1), tables));
									return ret;
								}
							}
						}

						ZExpression notnode = new ZExpression("NOT");
						ZExpression anynode = new ZExpression("ANY");
						ZExpression comparisonnode = new ZExpression(
								QueryUtils.getOppositeOperator(z.getOperator()));
						notnode.addOperand(comparisonnode);
						comparisonnode.addOperand(z.getOperand(0));
						comparisonnode.addOperand(anynode);
						anynode.addOperand(z2.getOperand(0));
						return replaceSubqueries(notnode, tables);

					}

					if (z2.getOperator().equals("ANY")
							|| z2.getOperator().equals("SOME")) {
						ZExpression existsnode = new ZExpression("EXISTS");

						ZQuery query = (ZQuery) z2.getOperand(0);

						ZSelectItem selItem = (ZSelectItem) query.getSelect()
								.firstElement();
						for (int i = 0; i < tables.size(); i++) {
							Table tt = tables.get(i);
							for (int j = 0; j < tt.columns.size(); j++) {
								if (tt.columns.get(j).name.equals(selItem
										.getColumn())
										&& tt.columns.get(j).canBeNull) {
									// we dont replace it because it can be null
									ZExpression ret = new ZExpression(
											((ZExpression) exp).getOperator());
									ret.addOperand(z.getOperand(0));
									ret.addOperand(replaceSubqueries(
											z.getOperand(1), tables));
									return ret;
								}
							}
						}

						// query is normalized which means either AND is root
						// node of where clause
						// or there is no and in where clause
						
						if (query.getWhere() instanceof ZExpression
								&& ((ZExpression) query.getWhere())
										.getOperator().equals("AND")) {

							// just add t_1 = t_2 to and

							ZExpression equalnode = new ZExpression("=");
							equalnode.addOperand(z.getOperand(0));
							equalnode.addOperand(new ZConstant(
									((ZSelectItem) query.getSelect()
											.firstElement()).toString(),
									ZConstant.COLUMNNAME));
							((ZExpression) query.getWhere())
									.addOperand(equalnode);
						} else {
							// we have to add AND to first layer
							ZExpression equalnode = new ZExpression(
									z.getOperator());
							equalnode.addOperand(z.getOperand(0));
							equalnode.addOperand(new ZConstant(
									((ZSelectItem) query.getSelect()
											.firstElement()).toString(),
									ZConstant.COLUMNNAME));

							if (query.getWhere() instanceof ZExpression) {
								ZExpression andnode = new ZExpression("AND");
								andnode.addOperand(query.getWhere());
								andnode.addOperand(equalnode);
								query.addWhere(andnode);
							} else {
								query.addWhere(equalnode);
							}
						}

						existsnode.addOperand(query);
						query.getSelect().clear();
						query.getSelect().add(new ZSelectItem("1"));
						
						return existsnode;
					}
				}

			}

			Iterator it = z.getOperands().iterator();
			ZExpression ret = new ZExpression(z.getOperator());
			while (it.hasNext()) {
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

		if (exp instanceof ZExpression) {
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
			 * replaces a IN (const1, const2, ..., constn) with a = const1 OR a
			 * = const 2 OR ... OR a = constn
			 * 
			 * subqueries with IN are not replaced here but in
			 * replaceSubqueries()
			 */
			if (operator.equals("IN")) {
				// check if only constant are children
				Iterator it = casted.getOperands().iterator();

				ZExpression orList = new ZExpression("OR");

				ZConstant firstOperand = (ZConstant) it.next();
				while (it.hasNext()) {
					ZExp elem = (ZExp) it.next();
					if (!(elem instanceof ZConstant)) {
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

			if (operator.equals("EXISTS")) {
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
			while (it.hasNext()) {
				ret.addOperand(replaceSyntacticVariantes(it.next()));
			}
			return ret;

		}

		/*
		 * if (operator.equals("LIKE")) { if
		 * (!casted.getOperand(1).toString().contains("%")) { ZExpression t1 =
		 * new ZExpression("="); t1.addOperand(casted.getOperand(0));
		 * t1.addOperand(casted.getOperand(1)); exp = t1; return exp; } }
		 */

		return exp;
	}

	private static int getLevenshteinDistance(String s, String t) {
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

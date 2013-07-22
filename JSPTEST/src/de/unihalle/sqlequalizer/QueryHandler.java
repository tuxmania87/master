package de.unihalle.sqlequalizer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.gibello.zql.ParseException;
import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZFromItem;
import org.gibello.zql.ZGroupBy;
import org.gibello.zql.ZQuery;
import org.gibello.zql.ZSelectItem;
import org.gibello.zql.ZStatement;
import org.gibello.zql.ZqlParser;

public class QueryHandler {

	public HashMap<String, String> Zuordnung;
	public ArrayList<Table> tables;
	public int aliascount = 1;

	public MetaQueryInfo before = null;
	public MetaQueryInfo after = null;
	public ZQuery original = null;
	public ZQuery workingCopy = null;
	public QueryHandler parent = null;

	private boolean respectColumnOrder = false;

	public String getCorrectAlias(String data, Vector<ZFromItem> fromItems, HashMap<String, String> sub) throws Exception{
		
		String[] split = data.split("\\.");

		// do we already have alias? then just change it!
		if (split.length > 1) {
			// if Zuordnung.get(split[0]) == null then
			// we are maybe in a subquery and have to checck parent
			// alias table (for all parents)

			if (Zuordnung.get(split[0]) == null) {
				QueryHandler iterator = this.parent;

				while (iterator != null) {
					if (iterator.Zuordnung.get(split[0]) != null) {
						return sub.get(iterator.Zuordnung.get(split[0])) + "."+ split[1];
					}
					iterator = iterator.parent;
				}

			} else {
				return sub.get(Zuordnung.get(split[0])) + "."+ split[1];
			}

			throw new Exception("couldnt find matching table!");

		} else if (split.length == 1) {
			String targetTable = null;
			// lookup tables
			Iterator<Table> it = tables.iterator();
			while (it.hasNext()) {
				Table t = it.next();
				Iterator<Column> it2 = t.columns.iterator();
				while (it2.hasNext()) {
					if (split[0].equals(it2.next().name)) {
						if (QueryUtils.isTableInFromClause(t.name,
								fromItems)) {
							if (targetTable == null) {
								// check ob tabelle mehrfach in HashTable
								// vorkommt
								int i = 1;
								while (Zuordnung.containsKey(t.name + i)) {
									i++;
								}
								if (i > 1) {
									throw new Exception(
											"Tabelle '"
													+ t.name
													+ "' kommt mehrfach in der FROM Klausel vor. Nicht eindeutig auf welche sich '"
													+ split[0]
													+ "' bezieht!");
								}
								targetTable = t.name;
							} else {
								throw new Exception(
										"Tupelvariable in mehreren Tabellen vorhanden! Ambigious!");
							}
						}
					}
				}
			}
			// we found alias, located in targetTable
			if (targetTable == null) {
				throw new Exception(
						"Das Tupel "
								+ split[0]
								+ " konnte in keiner der Tabellen in der FROM Klausel gefunden werden!");
			}
			return sub.get(Zuordnung.get(targetTable)) + "."+ split[0];
		}
		return data;
	}
	
	public ZExp makeAliases(ZExp exp, Vector<ZFromItem> fromItems, HashMap<String, String> sub)
			throws Exception {

		if (exp instanceof ZConstant
				&& ((ZConstant) exp).getType() == ZConstant.COLUMNNAME) {
			return new ZConstant(getCorrectAlias(((ZConstant) exp).getValue(), fromItems, sub), ((ZConstant) exp).getType());
			
		}
		if (exp instanceof ZConstant)
			return exp;

		if (exp instanceof ZQuery) {
			// new
			QueryHandler qh = new QueryHandler();
			qh.parent = this;
			qh.tables = (ArrayList<Table>) this.tables.clone();
			qh.setOriginalStatement(exp.toString());
			return qh.equalize(true)[0];

			// old
			// return handleQUERY((ZQuery)exp);
		}

		if (exp instanceof ZExpression) {
			ZExpression zexp = (ZExpression) exp;
			Iterator<ZExp> it = zexp.getOperands().iterator();
			ZExpression ret = new ZExpression(zexp.getOperator());
			while (it.hasNext()) {
				ret.addOperand(makeAliases(it.next(), fromItems, sub));
			}
			return ret;
		}

		return null;
	}

	public ZGroupBy handleGROUPBYStatement(ZGroupBy z) {
		if (z == null)
			return null;
		Vector<ZExp> v = z.getGroupBy();
		// ZExp[] groupby = new ZExp[v.size()];
		ZExp[] groupby = (ZExp[]) v.toArray(new ZExp[v.size()]);
		for (int i = 0; i < groupby.length; i++) {
			for (int j = 0; j < groupby.length; j++) {
				if (groupby[i].toString().compareTo(groupby[j].toString()) < 0) {
					ZExp helper = groupby[i];
					groupby[i] = groupby[j];
					groupby[j] = helper;
				}
			}
		}
		Vector<ZExp> ret1 = new Vector<ZExp>(Arrays.asList(groupby));
		ZGroupBy ret = new ZGroupBy(ret1);
		ret.setHaving(z.getHaving());
		return ret;
	}

	public ZQuery[] handleQUERY(ZQuery q) throws Exception {

		class tempSorter implements Comparator<ZSelectItem> {

			@Override
			public int compare(ZSelectItem o1, ZSelectItem o2) {

				return o1.getColumn().compareTo(o2.getColumn());

			}

		}

		if (!this.respectColumnOrder) {
			Collections.sort(q.getSelect(), new tempSorter());
			Collections.sort(original.getSelect(), new tempSorter());
		}

		before = new MetaQueryInfo(original);
		handleFROMClause(q.getFrom());
		Vector<ZSelectItem> newSELECTClause = handleSELECTClause(q.getSelect());

		// if we got the same table several times in FROM
		// we have to produce permutations and do query handling
		// for each of them

		Vector<ZFromItem> v = q.getFrom();

		Iterator<ZFromItem> it = v.iterator();

		ArrayList<ArrayList<String>> fromList = new ArrayList<ArrayList<String>>();
		ArrayList<String> listGroups = new ArrayList<String>();
		ZFromItem z = it.next();
		String table_checker = z.getTable();
		it = v.iterator();
		while (it.hasNext()) {
			z = it.next();
			if (!z.getTable().equals(table_checker)) {
				fromList.add(listGroups);
				listGroups = new ArrayList<String>();
				table_checker = z.getTable();
				listGroups.add(z.getAlias());
			} else {
				listGroups.add(z.getAlias());
			}
		}

		fromList.add(listGroups);

		// transfert into String[][]
		String[][] fromListArray = new String[fromList.size()][];
		for (int i = 0; i < fromListArray.length; i++) {
			fromListArray[i] = fromList.get(i).toArray(
					new String[fromList.get(i).size()]);
		}

		ArrayList<String[]> permutations = QueryUtils.createPermutations(
				new String[0][], fromListArray, 0);

		ZQuery[] newQueries = new ZQuery[permutations.size()];

		// foreach permutation of FROM list, create Query and process

		for (int i = 0; i < permutations.size(); i++) {

			ZQuery newQ = new ZQuery();
			
			//rename alias, use permutations
			String[] permutation = permutations.get(i);
			Vector<ZFromItem> v1 = q.getFrom();
			
			HashMap<String, String> substis = new HashMap<String, String>();
			
			for(int y = 0; y < v1.size(); y++) {
				substis.put(v1.get(y).getAlias(), permutation[y]);
			}
			
			
			
			
			ZExp newWHEREClause = handleWHEREClause(q.getWhere(), q.getFrom(),
					this,  substis);
			ZGroupBy newGROUPBYClause = handleGROUPBYStatement(q.getGroupBy());

			
			newQ.addFrom(q.getFrom());
			newQ.addWhere(newWHEREClause);

			if (newWHEREClause instanceof ZConstant
					&& ((ZConstant) newWHEREClause).getValue().equals("false"))
				throw new Exception(
						"Your Query always return the empty set, hence the where condition is not satisfiable.");

			newQ.addSelect(newSELECTClause);
			newQ.addGroupBy(newGROUPBYClause);
			newQ.addOrderBy(q.getOrderBy());

			after = new MetaQueryInfo(newQ);
			newQueries[i] = newQ;
		}
		return newQueries;

	}

	public void handleFROMClause(Vector<ZFromItem> from) {
		String aliasname = "a";
		Iterator<ZFromItem> zfrom = from.iterator();

		while (zfrom.hasNext()) {
			ZFromItem z = zfrom.next();
			if (z.getAlias() != null) {
				Zuordnung.put(z.getAlias(), aliasname + aliascount);
			}
			if (Zuordnung.get(z.getTable()) == null) {
				Zuordnung.put(z.getTable(), aliasname + aliascount);
			} else {
				int newpos = 1;
				while (Zuordnung.get(z.getTable().toString() + newpos) != null) {
					newpos++;
				}
				Zuordnung.put(z.getTable().toString() + newpos, aliasname
						+ aliascount);
			}
			z.setAlias(aliasname + aliascount);
			aliascount++;
		}
	}

	public Vector<ZSelectItem> handleSELECTClause(Vector<ZSelectItem> sel)
			throws Exception {
		Iterator<ZSelectItem> it = sel.iterator();

		Vector<ZSelectItem> ret = new Vector<ZSelectItem>();

		while (it.hasNext()) {
			ZSelectItem z = it.next();

			// Exceptions like *
			if (z.isWildcard()) {
				ret.add(z);
				continue;
			}

			String[] split = z.getColumn().split("\\.");
			// if there is already an alias, replace it
			// else look table up and set correct alias
			if (split.length == 2) {
				String addString = Zuordnung.get(split[0]) + "." + split[1];
				if (z.getAggregate() != null) {
					addString = z.getAggregate() + "(" + addString + ")";
				}
				ret.add(new ZSelectItem(addString));
			} else if (split.length == 1) {
				String targetTable = null;
				Iterator<Table> i = tables.iterator();
				while (i.hasNext()) {
					Table tab = i.next();
					Iterator<Column> j = tab.columns.iterator();
					while (j.hasNext()) {
						Column c = j.next();
						if (c.name.equals(z.getColumn())) {
							if (targetTable == null) {
								targetTable = tab.name;
							} else {
								throw new Exception(
										"Ambiguous column name: "
												+ c.name
												+ " is contained by more than one tables you mentioned in FROM.");
							}
						}
					}
				}
				// targetTable lokup

				String foundTable = Zuordnung.get(targetTable);

				if (foundTable == null) {
					throw new Exception("Column name unknown: " + split[0]
							+ " is not contained by any table in FROM!");
				}

				String addString = foundTable + "." + split[0];
				if (z.getAggregate() != null) {
					addString = z.getAggregate() + "(" + addString + ")";
				}
				ret.add(new ZSelectItem(addString));
			}

		}

		return ret;
	}

	public ZExp tranformToKNF(ZExp root) {

		if (root == null)
			return null;

		ZExp step = root;
		String old = "";

		do {
			old = step.toString();
			step = QueryUtils.operatorCompression(step, null);
			step = QueryUtils.pushDownNegate(step);
			step = QueryUtils.distribute(step);
		} while (!step.toString().equals(old));

		return step;
	}

	public ZExp handleWHEREClause(ZExp zexp, Vector<ZFromItem> from,
			QueryHandler q, HashMap<String, String> substis) throws Exception {

		if (zexp == null)
			return null;

		ArrayList<Table> tables = q.tables;

		// ZExp whereCondition = null;
		ZExp whereCondition = zexp;

		whereCondition = (ZExpression) makeAliases(zexp, from, substis);

		// recursvie for subqueries TODO

		whereCondition = tranformToKNF(whereCondition);

		String check = null;

		do {
			check = whereCondition.toString();
			whereCondition = QueryUtils.replaceSubqueries(whereCondition,
					tables);
			whereCondition = QueryUtils
					.replaceSyntacticVariantes(whereCondition);
		} while (!check.equals(whereCondition.toString()));

		whereCondition = QueryUtils.addImplicitFormulas(whereCondition, q);
		whereCondition = QueryUtils.evaluateArithmetic(whereCondition);
		
		whereCondition = QueryUtils.testf1(whereCondition, from, q);

		
		// KNF must be restored because we added possibly and nodes in between
		// the tree
		whereCondition = tranformToKNF(whereCondition);

		/*
		 * whereCondition = (ZExpression) QueryUtils
		 * .dfs_orderPairs(whereCondition); whereCondition = (ZExpression)
		 * QueryUtils.dfs_work(whereCondition); whereCondition = (ZExpression)
		 * QueryUtils.dfs_work(whereCondition); whereCondition = (ZExpression)
		 * QueryUtils.operatorCompression( whereCondition, null); whereCondition
		 * = (ZExpression) QueryUtils.dfs_work(whereCondition);
		 */
		whereCondition = QueryUtils.sortedTree(whereCondition);

		return whereCondition;
	}

	public void setOriginalStatement(String s) throws ParseException {

		if (s.trim().charAt(s.trim().length() - 1) != ';')
			s += ';';

		ZqlParser zp = new ZqlParser();
		zp.initParser(new ByteArrayInputStream(s.getBytes()));

		ZStatement zs = zp.readStatement();
		ZQuery q = (ZQuery) zs;
		this.original = q;

		zp = new ZqlParser();
		zp.initParser(new ByteArrayInputStream(s.getBytes()));

		zs = zp.readStatement();
		this.workingCopy = (ZQuery) zs;

	}

	public QueryHandler(String s) {
		Zuordnung = new HashMap<String, String>();
		tables = new ArrayList<Table>();

		ZqlParser zp = new ZqlParser();
		zp.initParser(new ByteArrayInputStream(s.getBytes()));

		ZStatement zs;
		try {
			zs = zp.readStatement();
		} catch (ParseException e) {
			return;
		}
		ZQuery q = (ZQuery) zs;
		this.original = q;

	}

	public QueryHandler(ZQuery z) {
		Zuordnung = new HashMap<String, String>();
		tables = new ArrayList<Table>();

		this.original = z;
	}

	public QueryHandler() {
		Zuordnung = new HashMap<String, String>();
		tables = new ArrayList<Table>();
	}

	// TODO: returns false if not parseable
	public boolean createTable(String createTableStatement) {
		tables.add(new Table(createTableStatement));
		return true;
	}

	public ZQuery[] equalize(boolean respect) throws Exception {
		this.respectColumnOrder = respect;
		if (parent != null)
			aliascount = parent.aliascount;
		return handleQUERY(workingCopy);
	}

	/*
	 * public ZQuery equalize(String sqlStatement) throws Exception { ZqlParser
	 * zp = new ZqlParser(); zp.initParser(new
	 * ByteArrayInputStream(sqlStatement.getBytes()));
	 * 
	 * ZStatement zs = zp.readStatement(); ZQuery q = (ZQuery) zs;
	 * 
	 * System.out.println("original: "+ q.toString());
	 * 
	 * return (ZQuery) handleQUERY(q); }
	 */

}

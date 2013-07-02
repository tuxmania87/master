package de.unihalle.sqlequalizer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

	public ZExp makeAliases(ZExp exp, Vector<ZFromItem> fromItems)
			throws Exception {

		if (exp instanceof ZConstant
				&& ((ZConstant) exp).getType() == ZConstant.COLUMNNAME) {

			// TODO:
			String a = ((ZConstant) exp).toString();

			String[] split = a.split("\\.");

			// do we already have alias? then just change it!
			if (split.length > 1) {
				// if Zuordnung.get(split[0]) == null then
				// we are maybe in a subquery and have to checck parent
				// alias table (for all parents)

				if (Zuordnung.get(split[0]) == null) {
					QueryHandler iterator = this.parent;

					while (iterator != null) {
						if (iterator.Zuordnung.get(split[0]) != null) {
							return new ZConstant(
									iterator.Zuordnung.get(split[0]) + "."
											+ split[1],
									((ZConstant) exp).getType());
						}
						iterator = iterator.parent;
					}

				} else {
					return new ZConstant(Zuordnung.get(split[0]) + "."
							+ split[1], ((ZConstant) exp).getType());
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
				return new ZConstant(Zuordnung.get(targetTable) + "."
						+ split[0], ZConstant.COLUMNNAME);

			}
		}
		if (exp instanceof ZConstant)
			return exp;

		if (exp instanceof ZQuery) {
			// new
			QueryHandler qh = new QueryHandler();
			qh.parent = this;
			qh.tables = (ArrayList<Table>) this.tables.clone();
			qh.setOriginalStatement(exp.toString());
			return qh.equalize();

			// old
			// return handleQUERY((ZQuery)exp);
		}

		if (exp instanceof ZExpression) {
			ZExpression zexp = (ZExpression) exp;
			Iterator<ZExp> it = zexp.getOperands().iterator();
			ZExpression ret = new ZExpression(zexp.getOperator());
			while (it.hasNext()) {
				ret.addOperand(makeAliases(it.next(), fromItems));
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

	public ZExp handleQUERY(ZQuery q) throws Exception {
		before = new MetaQueryInfo(original);
		handleFROMClause(q.getFrom());
		Vector<ZSelectItem> newSELECTClause = handleSELECTClause(q.getSelect());

		// reassemle query temporarily for preprocessing
		/*
		 * ZQuery tempQuery = new ZQuery(); tempQuery.addFrom(q.getFrom());
		 * tempQuery.addSelect(newSELECTClause);
		 * tempQuery.addWhere(q.getWhere());
		 * tempQuery.addGroupBy(q.getGroupBy());
		 * tempQuery.addOrderBy(q.getOrderBy());
		 */

		ZExp newWHEREClause = handleWHEREClause(
				q.getWhere(), q.getFrom(), this);
		ZGroupBy newGROUPBYClause = handleGROUPBYStatement(q.getGroupBy());

		ZQuery newQ = new ZQuery();
		newQ.addFrom(q.getFrom());
		newQ.addWhere(newWHEREClause);
		
		newQ.addSelect(newSELECTClause);
		newQ.addGroupBy(newGROUPBYClause);
		newQ.addOrderBy(q.getOrderBy());

		after = new MetaQueryInfo(newQ);

		return newQ;
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
										"Nicht eindeutig: Spalte "
												+ c.name
												+ " kommt in mehreren Tabellen vor! Bitte Spaltenalias verwenden!");
							}
						}
					}
				}
				// targetTable lokup
				String addString = Zuordnung.get(targetTable) + "." + split[0];
				if (z.getAggregate() != null) {
					addString = z.getAggregate() + "(" + addString + ")";
				}
				ret.add(new ZSelectItem(addString));
			}

		}
		return ret;
	}

	public ZExp tranformToKNF(ZExp root) {

		if(root == null)
			return null;
		
		ZExp step = root;
		String old = "";

		do {
			old =  step.toString();
			step = QueryUtils.operatorCompression(step, null);
			step = QueryUtils.pushDownNegate(step);
			step = QueryUtils.distribute(step);
		} while (!step.toString().equals(old));

		return step;
	}

	public ZExp handleWHEREClause(ZExp zexp, Vector<ZFromItem> from, QueryHandler q)
			throws Exception {

		if (zexp == null)
			return null;

		 ArrayList<Table> tables = q.tables;
		
		// ZExp whereCondition = null;
		ZExp whereCondition = zexp;

		whereCondition = (ZExpression) makeAliases(zexp, from);
		
		//recursvie for subqueries TODO
		
		whereCondition = tranformToKNF(whereCondition); 
		
		String check = null;
		
		do {
			check = whereCondition.toString();
			whereCondition = QueryUtils.replaceSubqueries(whereCondition,tables);
			whereCondition = QueryUtils.replaceSyntacticVariantes(whereCondition);
		} while(!check.equals(whereCondition.toString()));
		
		
		whereCondition = QueryUtils.addImplicitFormulas(whereCondition, q);
		whereCondition = QueryUtils.evaluateArithmetic(whereCondition);
		
		//elimnate duplicate trees somewhere , maybe after sorting for easier detection?
		
		//KNF must be restored because we added possibly and nodes in between the tree
		whereCondition = tranformToKNF(whereCondition);
		
		/*whereCondition = (ZExpression) QueryUtils
				.dfs_orderPairs(whereCondition);
		whereCondition = (ZExpression) QueryUtils.dfs_work(whereCondition);
		whereCondition = (ZExpression) QueryUtils.dfs_work(whereCondition);
		whereCondition = (ZExpression) QueryUtils.operatorCompression(
				whereCondition, null);
		whereCondition = (ZExpression) QueryUtils.dfs_work(whereCondition);*/
		whereCondition =  QueryUtils.sortedTree(whereCondition);

		return whereCondition;
	}

	public void setOriginalStatement(String s) throws ParseException {
		if (s.charAt(s.length() - 1) != ';')
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

	public ZQuery equalize() throws Exception {
		if (parent != null)
			aliascount = parent.aliascount;
		return (ZQuery) handleQUERY(workingCopy);
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

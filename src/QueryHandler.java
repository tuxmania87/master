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
	
	public ZQuery original = null;

	public ZExp makeAliases(ZExp exp, Vector<ZFromItem> fromItems)
			throws Exception {
		if (exp instanceof ZConstant
				&& ((ZConstant) exp).getType() == ZConstant.COLUMNNAME) {

			// TODO:
			String a = ((ZConstant) exp).toString();

			String[] split = a.split("\\.");

			// do we already have alias? then just change it!
			if (split.length > 1) {
				return new ZConstant(Zuordnung.get(split[0]) + "." + split[1],
						((ZConstant) exp).getType());
			} else if (split.length == 1) {
				String targetTable = null;
				// lookup tables
				Iterator<Table> it = tables.iterator();
				while (it.hasNext()) {
					Table t = it.next();
					Iterator<Column> it2 = t.columns.iterator();
					while (it2.hasNext()) {
						if (split[0].equals(it2.next().name)) {
							if (QueryUtils.isTableInFromClause(t.name, fromItems)) {
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
			return handleQUERY((ZQuery)exp);
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
		Vector<ZExp> v = z.getGroupBy();
		//ZExp[] groupby = new ZExp[v.size()];
		ZExp[] groupby = (ZExp[]) v.toArray(new ZExp[v.size()]);
		for(int i = 0; i< groupby.length;i++) {
			for(int j = 0; j < groupby.length; j++) {
				if(groupby[i].toString().compareTo(groupby[j].toString()) < 0) {
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
		handleFROMClause(q.getFrom());
		Vector<ZSelectItem> newSELECTClause = handleSELECTClause(q.getSelect());
		
		ZExpression newWHEREClause = (ZExpression) handleWHEREClause(q.getWhere(), q.getFrom());
		ZGroupBy newGROUPBYClause = handleGROUPBYStatement(q.getGroupBy());
		
		ZQuery newQ = new ZQuery();
		newQ.addFrom(q.getFrom());
		newQ.addWhere(newWHEREClause);
		newQ.addSelect(newSELECTClause);
		newQ.addGroupBy(newGROUPBYClause);
		return newQ;
	}
	
	public void handleFROMClause(Vector<ZFromItem> from) {
		String aliasname = "a";
		Iterator<ZFromItem> zfrom = from.iterator();
		
		while (zfrom.hasNext()) {
			ZFromItem z = zfrom.next();
			if (z.getAlias() != null) {
				Zuordnung.put(z.getAlias(), aliasname+aliascount);
			}
			if (Zuordnung.get(z.getTable()) == null) {
				Zuordnung.put(z.getTable(), aliasname+aliascount);
			} else {
				int newpos = 1;
				while (Zuordnung.get(z.getTable().toString() + newpos) != null) {
					newpos++;
				}
				Zuordnung.put(z.getTable().toString() + newpos,
						aliasname+aliascount);
			}
			z.setAlias(aliasname+aliascount);
			aliascount++;
		}
	}
	
	public Vector<ZSelectItem> handleSELECTClause(Vector<ZSelectItem> sel) throws Exception {
		Iterator<ZSelectItem> it = sel.iterator();
		
		Vector<ZSelectItem> ret = new Vector<ZSelectItem>();
				
		while(it.hasNext()) {
			ZSelectItem z = it.next();
			
			//Exceptions like *
			if(z.isWildcard()) {
				ret.add(z);
				continue;
			}
			
			String[] split = z.getColumn().split("\\.");
			//if	there is already an alias, replace it
			//else	look table up and set correct alias 
			if(split.length == 2) {
				String addString = Zuordnung.get(split[0])+"."+split[1];
				if(z.getAggregate() != null) {
					addString = z.getAggregate()+"("+addString+")";
				}
				ret.add(new ZSelectItem(addString));
			} else if(split.length==1) {
				String targetTable = null;
				Iterator<Table> i = tables.iterator();
				while(i.hasNext()) {
					Table tab = i.next();
					Iterator<Column> j = tab.columns.iterator();
					while(j.hasNext()) {
						Column c = j.next();
						if(c.name.equals(z.getColumn())) {
							if(targetTable == null) {
								targetTable = tab.name;
							} else {
								throw new Exception("Nicht eindeutig: Spalte "+c.name+" kommt in mehreren Tabellen vor! Bitte Spaltenalias verwenden!");
							}
						}
					}
				}
				//targetTable lokup
				String addString = Zuordnung.get(targetTable)+"."+split[0];
				if(z.getAggregate() != null) {
					addString = z.getAggregate()+"("+addString+")";
				}
				ret.add(new ZSelectItem(addString));
			}
			
		}
		return ret;
	}

	public ZExp handleWHEREClause(ZExp zexp, Vector<ZFromItem> from)throws Exception {

		if (zexp == null)
			return null;

		ZExp whereCondition = null;
		whereCondition = (ZExpression) makeAliases(zexp, from);
		whereCondition = (ZExpression) QueryUtils.dfs_orderPairs(whereCondition);
		whereCondition = (ZExpression) QueryUtils.dfs_work(whereCondition);
		whereCondition = (ZExpression) QueryUtils.dfs_work(whereCondition);
		whereCondition = (ZExpression) QueryUtils.operatorCompression(whereCondition, null);
		whereCondition = (ZExpression) QueryUtils.dfs_work(whereCondition);
		whereCondition = (ZExpression) QueryUtils.sortedTree(whereCondition);

		return whereCondition;
	}

	public void setOriginalStatement(String s) throws ParseException {
		ZqlParser zp = new ZqlParser();
		zp.initParser(new ByteArrayInputStream(s.getBytes()));
		
		ZStatement zs = zp.readStatement();
		ZQuery q = (ZQuery) zs;
		this.original = q;
	}
	
	public QueryHandler() {
		Zuordnung = new HashMap<String, String>(); 
		tables = new ArrayList<Table>();
	}
	
	//TODO: returns false if not parseable
	public boolean createTable(String createTableStatement) {
		tables.add(new Table(createTableStatement));
		return true;
	}
	
	public ZQuery equalize() throws Exception {
		return (ZQuery) handleQUERY(original);
	}
	
	public ZQuery equalize(String sqlStatement) throws Exception {
		ZqlParser zp = new ZqlParser();
		zp.initParser(new ByteArrayInputStream(sqlStatement.getBytes()));
		
		ZStatement zs = zp.readStatement();
		ZQuery q = (ZQuery) zs;
		
		System.out.println("original: "+ q.toString());
		
		return (ZQuery) handleQUERY(q);
	}

}

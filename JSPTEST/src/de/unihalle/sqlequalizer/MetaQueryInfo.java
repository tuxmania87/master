package de.unihalle.sqlequalizer;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Vector;

import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZQuery;
import org.gibello.zql.ZSelectItem;

/**
 * Class collects and maintains meta information
 * about onee single query, hold in attribute currentQuery
 * 
 * @author Robert Hartmann
 *
 */
public class MetaQueryInfo {

	int numberJoins = 0;
	int numberFormulasWHERE = 0;
	int numberFormulasHAVING = 0;
	int numberTables = 0;
	int numberSELECT = 0;
	int numberORDERBY = 0;
	boolean existsDISTINCT = false;
	boolean existsGROUPBY = false;
	boolean existsStatementsinSELECT = true;
	boolean existsHAVING = false;
	boolean existsORDERBY = false;
	int parserTreeDepth = 0;
	int numberSubquery = 0;
	
	public ZQuery currentQuery;
	
	/**
	 * Common constructor that awaits the query from 
	 * which the information should be get.
	 * @param q SQL-Query
	 */
	public MetaQueryInfo(ZQuery q) {
		currentQuery = q;
		handleQuery();
	}
	

	/**
	 * Concat all Metainformation name and values with colon.
	 */
	@Override
	public String toString() {
	  StringBuilder sb = new StringBuilder();
	  sb.append(getClass().getName());
	  sb.append(": ");
	  for (Field f : getClass().getDeclaredFields()) {
	    sb.append(f.getName());
	    sb.append("=");
	    try {
			sb.append(f.get(this));
		} catch (Exception e) {
			sb.append("illegal");
		}
	    sb.append(", ");
	  }
	  return sb.toString();
	}
	
	/**
	 * Check query for every attribute saved in this class 
	 * using helper functions
	 *
	 */
	public void handleQuery() {
		existsDISTINCT = currentQuery.isDistinct();
		existsGROUPBY = currentQuery.getGroupBy() != null;
		existsORDERBY = currentQuery.getOrderBy() != null;
		if(existsGROUPBY) {
			existsHAVING = currentQuery.getGroupBy().getHaving() != null;

			//look for things in group by already exists in select
			 Vector<ZExp> gVec = currentQuery.getGroupBy().getGroupBy();
			 Vector<ZSelectItem> sVec = currentQuery.getSelect();
			 Iterator it = gVec.iterator();
			 boolean failed = false;
			 while(it.hasNext()) {
				 if(! isInVector(sVec, it.next().toString())) 
					 failed = true;
			 }
			 existsStatementsinSELECT = !failed;
			 
		}
		numberSELECT = currentQuery.getSelect().size();
		if(existsORDERBY)
			numberORDERBY = currentQuery.getOrderBy().size();
		numberTables = currentQuery.getFrom().size();
		
		/*
		 * Number of joins
		 * 
		 *  To do this, we have to rename all the variables already. 
		 *  thats why the meta info has to applied AFTER handling the FROM-Part
		 */
		numberJoins = countJOINS(currentQuery.getWhere());
		
		/*
		 * Number of atomar formulas
		 * 
		 * A formula that doesn't contain any arithmetic operation as operator 
		 * is an atomic formula
		 */
		numberFormulasWHERE = countAtomicFormulas(currentQuery.getWhere());
		if(existsHAVING)
			numberFormulasHAVING = countAtomicFormulas(currentQuery.getGroupBy().getHaving());
		
		parserTreeDepth = QueryUtils.depth(currentQuery.getWhere());
		
		/*
		 * Numer of Subqueries
		 * 
		 * simple dfs
		 */
		numberSubquery = countSubqueries(currentQuery.getWhere());
	}
	
	/**
	 * counts the amounts of subqueries in a query
	 * @param root root node for dfs. Usually begins with surrounding query root node
	 * @return number of subqueries under root
	 */
	public int countSubqueries(ZExp root) {
		if(root instanceof ZConstant)
			return 0;
		if(root instanceof ZExpression) {
			ZExpression z = (ZExpression) root;
			Iterator<ZExp> it = z.getOperands().iterator();
			
			int v = 0;
			while(it.hasNext()) {
				v += countSubqueries(it.next());
			}
			return v;
		}
		
		if(root instanceof ZQuery) {
			ZQuery zq = (ZQuery) root;
			return 1 + countSubqueries(zq.getWhere());
		}
		return 0;
	}
	
	/**
	 * Counts the amount of atomic formulas under root
	 * @param root Root node of applied dfs.
	 * @return Number of atomic formuas under root node.
	 */
	public int countAtomicFormulas(ZExp root) {
		if(root instanceof ZConstant)
			return 0;
		
		if(root instanceof ZExpression) {
			ZExpression z = (ZExpression) root;
			
			if(z.getOperator().equals("+") ||
					z.getOperator().equals("-") ||
					z.getOperator().equals("*") ||
					z.getOperator().equals("/")) {
				return 0;
			}
			
			if(z.getOperator().equals("AND") ||
					z.getOperator().equals("OR")) {
				Iterator<ZExp> it = z.getOperands().iterator();
				int v = 0;
				while(it.hasNext()) {
					v += countAtomicFormulas(it.next());
				}
				return v;
			}
			
			return 1;
				
		}
		return 0;
	}
	
	/**
	 * Counts Joins under root node
	 * @param root Root node for applied dfs.
	 * @return Number of Joins
	 */
	public int countJOINS(ZExp root) {
		if(root instanceof ZConstant)
			return 0;
		
		if(root instanceof ZExpression) {
			ZExpression z = (ZExpression) root;
			
			//a join is always between two tupelvariables
			//here named table1 and table2
			String table1 = null;
			String table2 = null;
			
			Iterator<ZExp> it = z.getOperands().iterator();
			while(it.hasNext()) {
				ZExp item = it.next();
				if(item instanceof ZConstant && ((ZConstant) item).getType() == ZConstant.COLUMNNAME) {
					if(table1 == null) {
						table1 = ((ZConstant) item).getValue().split("\\.")[0];
					} else if(table2 == null) {
						table2 = ((ZConstant) item).getValue().split("\\.")[0];
					}
				}
			}
			
			if(table2 == null) {
				//we didnt find tablenames so we have to recusively look deeper
				it = z.getOperands().iterator();
				int v = 0;
				while(it.hasNext()) {
					ZExp item = it.next();
					if(item instanceof ZExpression) {
						v += countJOINS(item);
					}
				}
				return v;
				
			} else {
				//we have to compare the occured tables
				if( !table1.equals(table2))
					return 1;
				else 
					return 0;
			}
			
			
			
		}
		return 0;
	}
	
	
	private static boolean isInVector(Vector<ZSelectItem> v, String s) {
		Iterator<ZSelectItem> i = v.iterator();
		while(i.hasNext()) {
			if(i.next().toString().equals(s))
				return true;
		}
		return false;
	}
	
}

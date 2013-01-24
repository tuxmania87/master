import java.beans.Expression;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.gibello.zql.ParseException;
import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZFromItem;
import org.gibello.zql.ZQuery;
import org.gibello.zql.ZStatement;
import org.gibello.zql.ZqlParser;

public class Haupt {

	/**
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		
		
		QueryHandler q = new QueryHandler();
		q.createTable("create table emp (empno int, ename varchar(500), job varchar(500), mgr int, hiredate datetime, sal int, comm int, deptno int)");
		q.createTable("create table dept (deptno int, dname varchar(500), location varchar(500))");
		
		//ZQuery z = q.equalize("select * from emp e where e.sal > (select sum(sal) from emp x where e.sal > x.sal);");
		
		
		q.setOriginalStatement("select max(sal) from emp group by job,ename having 1=1;");
		
		String s1 = q.original.toString();
		
		ZQuery z = q.equalize();
		
		String s2 = z.toString();
		
		System.out.println("alt "+s1);
		System.out.println("neu: "+z.toString());
		
		System.out.println(new Double(QueryUtils.getLevenshteinDistance(s1, s2)).doubleValue()/new Double(Math.max(s1.length(), s2.length())).doubleValue());
		
		
	}

}

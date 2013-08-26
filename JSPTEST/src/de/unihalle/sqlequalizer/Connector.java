package de.unihalle.sqlequalizer;


import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;


/**
 * class creates and maintaines the main database 
 * connection to the internal database.
 * 
 * MySQL, PostgreSQL and Oracle DB are supported
 * 
 * @author Robert Hartmann
 *
 */
public class Connector {

	private static Connection current = null;
	
	
	/**
	 * Handles all the connections. Class holds exaclty one connection. 
	 * Currently working on mysql, use other stubs to connect to the database
	 * of your choice
	 * @return
	 */
	public static Connection getConnection() {
		try {
				
			
			String dburi = null;
			String dbname = null;
			String dbuser = null;
			String dbpw = null;
			
		
				Properties prop = new Properties();
				prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("dbsettings.txt"));
			

				 dburi = prop.getProperty("dburi");
				 dbname = prop.getProperty("dbname");
				 dbuser = prop.getProperty("dbuser");
				 dbpw =prop.getProperty("dbpw");
		
			
			
		/*
		 *      == POSTGRE SQL ==
				Class.forName("org.postgresql.Driver");
				if(current == null || current.isClosed()) 
				   current = DriverManager.getConnection("jdbc:postgresql://"+dburi+"/"+dbname+"?user="+dbuser+"&password="+dbpw);
			    
			   // == ORACLE DB ==
				Class.forName("oracle.jdbc.driver.OracleDriver");
				if(current == null || current.isClosed())
				   current = DriverManager.getConnection("jdbc:oracle:thin:@"+dburi+":"+dbname, dbuser, dbpw);	
		 */
			
				// == MYSQL ==
				Class.forName("com.mysql.jdbc.Driver");
				if(current == null || current.isClosed()) 
					current = DriverManager.getConnection("jdbc:mysql://"+dburi+"/"+dbname+"?user="+dbuser+"&password="+dbpw);
		
		
		
		
		return current;
		
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}

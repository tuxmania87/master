package de.unihalle.sqlequalizer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLWarning;

public class Login {

	public static int login(String u, String p) {
		try {
		
		Connection conn = Connector.getConnection();
		
		
		// Print all warnings
	      for( SQLWarning warn = conn.getWarnings(); warn != null; warn = warn.getNextWarning() )
	      {
	          System.out.println( "SQL Warning:" ) ;
	          System.out.println( "State  : " + warn.getSQLState()  ) ;
	          System.out.println( "Message: " + warn.getMessage()   ) ;
	          System.out.println( "Error  : " + warn.getErrorCode() ) ;
	      }
		
		String q = "select id from users where name = ? and password = ?";
		
		PreparedStatement psmt = conn.prepareStatement(q);
		
		psmt.setString(1, u);
		psmt.setString(2, p);
		
		ResultSet res = psmt.executeQuery();
		
		res.next();
		return res.getInt("id");
		
			
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public static boolean isDozent(String u) {
		try {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:e:\\users\\robert\\workspace\\jsptest\\test.db");
		
		String q = "select 1 from users where name = ? and isDozent = 1";
		
		PreparedStatement psmt = conn.prepareStatement(q);
		psmt.setString(1, u);
		
		ResultSet rs =psmt.executeQuery();
		
		if(!rs.next())
			return false;
		
		return true;
		}
		catch(Exception e) {
			return false;
		}
	}
	
}

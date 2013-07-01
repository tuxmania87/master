package de.unihalle.sqlequalizer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLWarning;

public class Login {

	public static boolean login(String u, String p) {
		try {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:e:\\users\\robert\\workspace\\jsptest\\test.db");
		
		
		// Print all warnings
	      for( SQLWarning warn = conn.getWarnings(); warn != null; warn = warn.getNextWarning() )
	      {
	          System.out.println( "SQL Warning:" ) ;
	          System.out.println( "State  : " + warn.getSQLState()  ) ;
	          System.out.println( "Message: " + warn.getMessage()   ) ;
	          System.out.println( "Error  : " + warn.getErrorCode() ) ;
	      }
		
		String q = "select * from users where name = ? and password = ?";
		
		PreparedStatement psmt = conn.prepareStatement(q);
		
		psmt.setString(1, u);
		psmt.setString(2, p);
		ResultSet rs = psmt.executeQuery();
		conn.close();
		return rs.next();
		
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
}

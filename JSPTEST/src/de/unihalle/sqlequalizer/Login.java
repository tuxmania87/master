package de.unihalle.sqlequalizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLWarning;

/**
 * Class handles login for sql-equalizer. Login data are 
 * saved in internal database.
 * 
 * default is: 
 * login: admin
 * password: secure1234
 * 
 * @author Robert Hartmann
 *
 */
public class Login {

	/**
	 * Attempt to login with username u and password p
	 * 
	 * @param u username
	 * @param p password 
	 * @return id if user could be loged in, 0 otherwise
	 */
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
	
	/**
	 * Check if user is admin 
	 * 
	 * @param u username
	 * @return true if user is admin, false otherwise
	 */
	public static boolean isDozent(String u) {
		try {
		
		Connection conn = Connector.getConnection();
		
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

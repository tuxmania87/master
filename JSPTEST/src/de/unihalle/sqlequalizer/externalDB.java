package de.unihalle.sqlequalizer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class externalDB {
	
	public static ResultSet executeQueryOn(String q, int dbid) throws Exception{
	
			
			Connection conn =  Connector.getConnection();
			PreparedStatement ps = conn.prepareStatement("select * from external_database where id = ?");
			ps.setInt(1, dbid);
			
			String dbname = null;
			String dbuser = null;
			String dbpw = null;
			String dburi = null;
			String typ = null;
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				dbname = rs.getString("dbname");
				dbuser = rs.getString("username");
				dbpw = rs.getString("password");
				dburi = rs.getString("uri");
				typ  = rs.getString("typ");
			}
			
			if(dbname == null)
				return null;
			conn.close();
			
			if(typ.toLowerCase().trim().equals("mysql")) {
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://"+dburi+"/"+dbname+"?user="+dbuser+"&password="+dbpw);
			} else if(typ.toLowerCase().trim().equals("postgresql")) {
				Class.forName("org.postgresql.Driver");
				conn = DriverManager.getConnection("jdbc:postgresql://"+dburi+"/"+dbname+"?user="+dbuser+"&password="+dbpw);
			} else if(typ.toLowerCase().trim().equals("oracledb")) {
				Class.forName("oracle.jdbc.driver.OracleDriver");
				conn = DriverManager.getConnection("jdbc:oracle:thin:@"+dburi+":"+dbname, dbuser, dbpw);
			} else {
				throw new Exception("database type: "+typ+" not supported.");
			}
			
			Statement s = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE);
			s.execute(q);
			rs = s.getResultSet();
			
			return rs;
			
			
		
		
	}
	
}

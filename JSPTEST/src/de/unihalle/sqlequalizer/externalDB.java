package de.unihalle.sqlequalizer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class externalDB {
	
	public static ResultSet executeQueryOn(String q, int dbid) throws Exception{
	
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			Connection conn =  Connector.getConnection();
			PreparedStatement ps = conn.prepareStatement("select * from external_database where id = ?");
			ps.setInt(1, dbid);
			
			String dbname = null;
			String dbuser = null;
			String dbpw = null;
			String dburi = null;
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				dbname = rs.getString("dbname");
				dbuser = rs.getString("username");
				dbpw = rs.getString("password");
				dburi = rs.getString("uri");
			}
			
			if(dbname == null)
				return null;
			conn.close();
			conn = DriverManager.getConnection("jdbc:mysql://"+dburi+"/"+dbname+"?user="+dbuser+"&password="+dbpw);
			
			Statement s = conn.createStatement();
			s.execute(q);
			rs = s.getResultSet();
			
			return rs;
			
			
		
		
	}
	
}

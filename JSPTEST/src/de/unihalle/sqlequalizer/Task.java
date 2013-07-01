package de.unihalle.sqlequalizer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

public class Task {

	private Connection conn = null;

	public int id = 0;
	public String text = null;
	public String[] samplesolution = null;
	public String[] dbschema = null;
	public int[] externalDbs = null;
	
	public Task(Connection c, int id) {
		try {
			PreparedStatement ps = c
					.prepareStatement("select * from tasks t, dbschema d where id = ? AND id=taskid");
			ps.setInt(1, id);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				this.id = rs.getInt("id");
				text = rs.getString("description");
				dbschema = rs.getString("schema").split(";");
				
				
				//load Sample Solutions
				ArrayList<String> ssl = new ArrayList<String>();
				PreparedStatement ps2 = c.prepareStatement("select sqlstatement from samplesolutions where taskid = ?");
				ps2.setInt(1, id);
				
				ResultSet rs2 = ps2.executeQuery();
				while(rs2.next()) {
					ssl.add(rs2.getString("sqlstatement"));
				}
				samplesolution = ssl.toArray(new String[ssl.size()]);
				
			}
			
			ps = c.prepareStatement("select dbid from tasks_db where taskid = ?");
			ps.setInt(1, id);
			
			rs = ps.executeQuery();
			
			ArrayList<Integer> dbids = new ArrayList<Integer>();
			
			while(rs.next()) {
				dbids.add(new Integer(rs.getInt("dbid")));
			}
			
			externalDbs = new int[dbids.size()];
			for(int i=0; i< dbids.size(); i++) {
				externalDbs[i] = dbids.get(i).intValue();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static Connection connect() throws Exception {
		Class.forName("org.sqlite.JDBC");
		return DriverManager.getConnection("jdbc:sqlite:e:\\users\\robert\\workspace\\jsptest\\test.db");

	}

	public static Task[] getAllTasks() throws Exception {
		Connection c = connect();
		Statement s = c.createStatement();
		s.execute("select id from tasks");
		ResultSet r = s.getResultSet();

		ArrayList<Task> alist = new ArrayList<Task>();

		while (r.next()) {
			alist.add(new Task(c, r.getInt(1)));
		}
		c.close();
		return alist.toArray(new Task[alist.size()]);
	}

	public static void createTables(Connection conn) throws Exception {
		Statement stat = conn.createStatement();
		stat.execute("drop table if exists tasks;");
		stat.execute("drop table if exists samplesolutions;");
		stat.execute("drop table if exists users;");
		stat.execute("drop table if exists attempts;");
		stat.execute("create table tasks (" + "id int, "
				+ "description varchar(500), " + "createdAt datetime);");
		stat.execute("create table samplesolutions (" + "id int, "
				+ "taskid int, " + "sqlstatement varchar(500));");
		stat.execute("create table users (" + "id int, " + "name varchar(50), "
				+ "password varchar(50), " + "isDozent tinyint(1) " + ");");
		stat.execute("create table attempts (" + "id int, " + "userid int,"
				+ "taskid int," + "timeat datetime,"
				+ "sqlstatement varchar(200)," + "correct tinyint(1));");

	}

}

package de.unihalle.sqlequalizer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Storing all information about a task
 * @author Robert Hartmann
 *
 */
public class Task {

	private Connection conn = null;

	public int id = 0;
	public String text = null;
	public String[] samplesolution = null;
	public String[] dbschema = null;
	public int[] externalDbs = null;
	public boolean respectRow = false;
	public boolean respectColumn = false;
	public int schemaid = 0;
	
	/**
	 * Constructor stores all important information such as
	 * samplesolutions, external dbs, dbschema, text description of task
	 * 
	 * @param c Open Connection to internal database
	 * @param id Taskid
	 */
	public Task(Connection c, int id) {
		try {
			PreparedStatement ps = c
					.prepareStatement("select  t.id,schemaid,description,schem,respectColumnorder from tasks t left join dbschema d on schemaid=d.id where t.id = ?");
			
			ps.setInt(1, id);

			//System.out.println("select  t.id,schemaid,description,schem,respectColumnorder from tasks t left join dbschema d on schemaid=d.id where t.id = "+id);
			
			ResultSet rs = ps.executeQuery();

			
			
			while (rs.next()) {
				
				this.id = rs.getInt("id");
				text = rs.getString("description");
				
				if(rs.getString("schem") != null) {
					dbschema = rs.getString("schem").split(";");
				}
				schemaid = rs.getInt("schemaid");
				respectColumn = rs.getBoolean("respectColumnorder");
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
		return Connector.getConnection();

	}

	/**
	 * 
	 * @return Array of all Tasks as Objects
	 * @throws Exception
	 */
	public static Task[] getAllTasks() throws Exception {
		Connection c = connect();
		Statement s = c.createStatement();
		s.execute("select id from tasks");
		ResultSet r = s.getResultSet();

		ArrayList<Task> alist = new ArrayList<Task>();

		while (r.next()) {
			alist.add(new Task(c, r.getInt("id")));
			System.out.println("loaded "+r.getInt("id"));
		}
		c.close();
		return alist.toArray(new Task[alist.size()]);
	}



}

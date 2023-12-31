<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="de.unihalle.sqlequalizer.Connector"%>
<%@page import="de.unihalle.sqlequalizer.QueryUtils"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.Connection"%>
<%@page import="de.unihalle.sqlequalizer.Task"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<%@ include file="header.jsp" %>
</head>
<body>
<%@ include file="nav.jsp" %>

<%

if(request.getParameter("pschemaid") != null && request.getParameter("pschema") != null && request.getParameter("pschemaname") != null) {
	Connection c = Connector.getConnection();
	PreparedStatement prep = null;
	String q = "update dbschema set name = ?, schem = ? where id = ?";
	
	prep = c.prepareStatement(q);
	prep.setString(1, request.getParameter("pschemaname"));
	prep.setString(2, request.getParameter("pschema"));
	prep.setInt(3, Integer.parseInt(request.getParameter("pschemaid")));
	
	prep.executeUpdate();

}

if(request.getParameter("pdbid") != null) {
	Connection c = Connector.getConnection();
	PreparedStatement prep = null;
	String q = "update external_database set uri = ?,dbname = ?, username = ?, password = ?, typ = ? where id=?";
	
	prep = c.prepareStatement(q);
	prep.setString(1, request.getParameter("pdburi"));
	prep.setString(2, request.getParameter("pdbname"));
	prep.setString(3, request.getParameter("pdbusername"));
	prep.setString(4, request.getParameter("pdbpassword"));
	prep.setString(5, request.getParameter("pdbtyp"));
	
	prep.setInt(6, Integer.parseInt(request.getParameter("pdbid")));
	
	prep.executeUpdate();

}


if(request.getParameter("a") != null &&  request.getParameter("a").equals("addtask")) {
	Connection c = Connector.getConnection();
	Statement s = c.createStatement();
	ResultSet rs = s.executeQuery("select max(id) from tasks");
	rs.next(); 
	int newid = rs.getInt("max(id)") +1;
	
	String q = "insert into tasks (id,description,createdAt) values (?,?,?)";
	PreparedStatement prep = c.prepareStatement(q);
	prep.setInt(1, newid);
	prep.setString(2, "EMPTY");
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	prep.setString(3, df.format(new Date())); 
	
	prep.execute();

}

if(request.getParameter("a") != null &&  request.getParameter("a").equals("addschema")) {
	Connection c = Connector.getConnection();
	Statement s = c.createStatement();
	ResultSet rs = s.executeQuery("select max(id) from dbschema");
	rs.next(); 
	int newid = rs.getInt("max(id)") +1;
	
	String q = "insert into dbschema (id,name,schem) values (?,?,?)";
	PreparedStatement prep = c.prepareStatement(q);
	prep.setInt(1, newid);
	prep.setString(2, "EMPTY");
	prep.setString(3, "EMPTY");
	
	prep.execute();

}

%>
<div id="main_center">
<%

if(request.getParameter("a") != null &&  request.getParameter("a").equals("deltask")) {
	try {
		int taskid = Integer.parseInt(request.getParameter("i"));
		Connection c = Connector.getConnection();
		PreparedStatement psmt = c.prepareStatement("delete from tasks where id = ?");
		psmt.setInt(1, taskid);
		psmt.execute();
		psmt = c.prepareStatement("delete from samplesolutions where taskid = ?");
		psmt.setInt(1, taskid);
		psmt.execute();psmt = c.prepareStatement("delete from tasks_db where taskid = ?");
		psmt.setInt(1, taskid);
		psmt.execute();psmt = c.prepareStatement("delete from attempts where taskid = ?");
		psmt.setInt(1, taskid);
		psmt.execute();
	}
	catch (Exception e) {
		out.println("<span style=\"color:red\">error while deleting task:<br>"+e.getMessage()+"</span>");
	}
}


if(request.getParameter("a") != null &&  request.getParameter("a").equals("delschema")) {
	try {
		int schemaid = Integer.parseInt(request.getParameter("i"));
		Connection c = Connector.getConnection();
		//check if schema is still in use
		PreparedStatement psmt = c.prepareStatement("select 1 from tasks where schemaid = ?");
		psmt.setInt(1, schemaid);
		ResultSet rs = psmt.executeQuery();
		if(rs.next()) {
			out.println("<span style=\"color:red\">There are still tasks that use this schema. Make sure to unassign this schema from any task before you delete it.</span>");
		} else {
			psmt = c.prepareStatement("delete from dbschema where id = ?");
			psmt.setInt(1, schemaid);
			psmt.execute();
		}
		
	}
	catch (Exception e) {
		out.println("<span style=\"color:red\">error while deleting schema:<br>"+e.getMessage()+"</span>");
	}
}

if(request.getParameter("a") != null &&  request.getParameter("a").equals("delextdb")) {
	try {
		int extdbid = Integer.parseInt(request.getParameter("i"));
		Connection c = Connector.getConnection();
		//check if schema is still in use
		PreparedStatement psmt = c.prepareStatement("delete from external_database where id = ?");
		psmt.setInt(1, extdbid);
		psmt.execute();
		psmt = c.prepareStatement("delete from tasks_db where dbid = ?");
		psmt.setInt(1, extdbid);
		psmt.execute();
		
		
	}
	catch (Exception e) {
		out.println("<span style=\"color:red\">error while deleting schema:<br>"+e.getMessage()+"</span>");
	}
}

if(request.getParameter("a") != null &&  request.getParameter("a").equals("addexterndb")) {
	Connection c = Task.connect();
	Statement s = c.createStatement();
	ResultSet rs = s.executeQuery("select max(id) from external_database");
	rs.next(); 
	int newid = rs.getInt("max(id)") +1;
	
	String q = "insert into external_database (id,uri,dbname,username,password,typ) values (?,?,?,?,?,?)";
	PreparedStatement prep = c.prepareStatement(q);
	prep.setInt(1, newid);
	prep.setString(2, "EMPTY");
	prep.setString(3, "EMPTY");
	prep.setString(4, "EMPTY");
	prep.setString(5, "EMPTY");
	prep.setString(6, "mysql");
	
	prep.execute();
	c.close();
}

%>




<h2>Admin Control Panel</h2>

<h3>tasks</h3>

<table>
<tr><th>ID</th><th>description</th><th>edit</th><th>delete</th></tr>
<%
Task[] t = Task.getAllTasks();
for(int i=0; i<t.length; i++) {
	out.println("<tr><td>"+t[i].id+"</td><td>"+t[i].text+"</td><td><a href=\"acp_task.jsp?i="+t[i].id+"\">edit</a></td><td><a href=\"acp.jsp?i="+t[i].id+"&a=deltask\" onclick=\"return confirmbox('Are you sure you want to delete task number "+t[i].id+"?')\">delete</a></td></tr>");
}
%>
</table>
<span style="font-size:x-large;font-weight:bold;">+</span> <a href="acp.jsp?a=addtask">add task</a>

<h3>database schema</h3>

<table>
<tr><th>id</th><th>name</th><th>schema</th><th>edit</th><th>delete</th></tr>

<%
Connection c = Task.connect();
Statement s = c.createStatement();
s.execute("select * from dbschema");
ResultSet r = s.getResultSet();

while(r.next()) {
	String id = r.getString("id");
	if(request.getParameter("a") != null && request.getParameter("a").equals("editschema") && request.getParameter("i") != null && request.getParameter("i").equals(id)) {
		out.println("<form action=\"acp.jsp\" method=\"post\"><tr><td>"+r.getString("id")+"</td><td><input type=\"text\" name=\"pschemaname\" value=\""+r.getString("name")+"\" /></td><td><textarea cols=\"80\" name=\"pschema\" rows=\"5\">"+r.getString("schem")+"</textarea><td><input type=\"hidden\" name=\"pschemaid\" value=\""+id+"\"><input type=\"submit\" value=\"save\"></td><td></td></tr></form>");
	} else
		out.println("<tr><td>"+r.getString("id")+"</td><td>"+r.getString("name")+"</td><td>"+r.getString("schem").replaceAll("\\n", "<br>")+"</td><td><a href=\"acp.jsp?a=editschema&i="+r.getString("id")+"\">edit</a></td><td><a href=\"acp.jsp?a=delschema&i="+r.getString("id")+"\" onclick=\"return confirmbox('Are you sure you want to delete schema number "+r.getString("id")+"')\" >delete</a></td></tr>");
}


%>

</table>
<span style="font-size:x-large;font-weight:bold;">+</span> <a href="acp.jsp?a=addschema">add database schema</a>


<h3>external databases</h3>

<table><tr><th>id</th><th>URI</th><th>DB name</th><th>db type</th></tr>
<%
s = c.createStatement();
r = s.executeQuery("select * from external_database");

String maxdbid = "0";

while(r.next()) {
	String id = r.getString("id");
	if(request.getParameter("a") != null && request.getParameter("a").equals("editexterndb") && request.getParameter("i") != null && request.getParameter("i").equals(id)) { 
		out.print("<form action=\"acp.jsp\" method=\"post\"><tr><td>"+id+"<td><input type=\"text\" name=\"pdburi\" value=\""+r.getString("uri")+"\"></td><td><input type=\"text\" name=\"pdbname\" value=\""+r.getString("dbname")+"\"></td>");
		
		
		out.println("<td><select name=\"pdbtyp\">");
		for(int i=0; i< QueryUtils.validDBtypes.length; i++) {
			boolean selected = QueryUtils.validDBtypes[i].equals(r.getString("typ"));
			out.println("<option value=\""+QueryUtils.validDBtypes[i]+"\" "+(selected?"selected":"")+">"+QueryUtils.validDBtypes[i]+"</option>");
		}
		out.println("</select></td>");
		out.println("<td><input type=\"hidden\" name=\"pdbid\" value=\""+r.getString("id")+"\"><input type=\"text\" name=\"pdbusername\" value=\""+r.getString("username")+"\"></td><td><input type=\"text\" name=\"pdbpassword\" value=\""+r.getString("password")+"\"></td><td><input type=\"submit\" value=\"save\"></td><td></td></tr>");
	} else
		out.print("<tr><td>"+id+"<td>"+r.getString("uri")+"</td><td>"+r.getString("dbname")+"</td><td>"+r.getString("typ")+"</td><td><a href=\"acp.jsp?a=editexterndb&i="+r.getString("id")+"\">edit</a></td><td><a href=\"acp.jsp?a=delextdb&i="+r.getString("id")+"\" onclick=\"return confirmbox('Are you sure you want to delete externa database "+r.getString("uri")+"')\">delete</a></td></tr>");
}
%>
</table>
<span style="font-size:x-large;font-weight:bold;">+</span> <a href="acp.jsp?a=addexterndb">add external database</a>

<%
c.close();
%>

</div>

</body>
</html>
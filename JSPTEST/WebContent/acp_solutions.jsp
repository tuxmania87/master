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
<div id="main_center">
<h2>Solutions marked as 'unknown'</h2>

<%

Connection c = Connector.getConnection();
Statement s = c.createStatement();

ResultSet rs = s.executeQuery("select a.*,u.name from attempts a, users u where correct = 2 and a.userid = u.id order by taskid desc,id desc");

int oldtaskid = -1;
Task t = null;

while(rs.next()) {
	
	if(rs.getInt("taskid") != oldtaskid) {
		
		if(oldtaskid > -1) {
			out.println("</table></div><br>");
		}
		
		t = new Task(c, rs.getInt("taskid"));
		oldtaskid = t.id;
		out.println("<div class=\"description\">");
		out.println("<h3>task "+t.id + "</h3>");
		out.println("<h4>description</h4>"+t.text);
		out.println("<br><h4>sample solutions</h4><ul>");
		for(int i=0; i<t.samplesolution.length; i++) {
			out.println("<li>"+t.samplesolution[i]+"</li>");
		}
		out.println("</ul><h4>unknown solutions</h4>");
		out.println("<table><tr><td>user</td><td>time</td><td>attempt</td></tr>");
	}
	
	out.println("<tr><td>"+rs.getString("u.name")+"</td><td>"+rs.getString("timeat")+"</td><td>"+rs.getString("sqlstatement")+"</td></tr>");
	
}
out.println("</table></div>");
%>
</div>
</body>
</html>
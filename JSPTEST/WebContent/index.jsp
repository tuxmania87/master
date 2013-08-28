

<%@page import="de.unihalle.sqlequalizer.Task"%>
<%@page import="java.sql.Timestamp"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="de.unihalle.sqlequalizer.Connector"%>
<%@page import="java.sql.Connection"%>
<%@page import="java.util.Date"%>
<%@page import="java.io.File"%>
<%@page import="de.unihalle.sqlequalizer.QueryUtils"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
<%
	if(request.getParameter("a") != null && request.getParameter("a").equals("logout")) {
		request.getSession().removeAttribute("userid");
		request.getSession().removeAttribute("dozent");
		response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
		response.setHeader("Location", "index.jsp"); 
	}




%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<%@ include file="header.jsp" %>
</head>
<body>
<%@ include file="nav.jsp" %> 
<div id="main_center">
<% if(request.getSession().getAttribute("userid") != null) { %>
<span style="text-align:center;"><h2>welcome to the sql-equalizer</h2></span>
<h3>last 5 solutions</h3>
<table>
<tr><td>time</td><td>task</td><td>sql statement</td><td>correct?</td></tr>


<%

Connection c = Connector.getConnection();
PreparedStatement prep = c.prepareStatement("select * from attempts where userid = ? order by id desc");
prep.setString(1, request.getSession().getAttribute("userid").toString());
ResultSet r = prep.executeQuery();

int i = 0;
while(r.next() && i < 5) {
	
	out.println("<tr><td>"+r.getString("timeat")+"</td><td>"+r.getString("taskid")+"</td><td>"+r.getString("sqlstatement").replaceFirst("where", "where<br>")+"</td><td>"+(r.getInt("correct") == 0 ? "no" : (r.getInt("correct") == 1 ? "yes" : "unknown" ))+"</td></tr>");
	i++;
}
%>

</table>

<h3>statistics</h3>
<table>
<tr><td>task</td><td>#solutions</td><td>correct</td><td>ratio</td><td>last time sent in</td></tr>

<%

Task[] all = Task.getAllTasks();

c = Connector.getConnection();

String incomplete = "";

for(i=0; i< all.length; i++) {
	int taskid = all[i].id;
	prep = c.prepareStatement("select count(*) from attempts where userid = ? and taskid = ?");
	prep.setString(1, request.getSession().getAttribute("userid").toString());
	prep.setInt(2, taskid);
	r = prep.executeQuery();
	
	int total = 0;
	if(r.next())
		total = r.getInt("count(*)");
	
	prep = c.prepareStatement("select count(*) from attempts where userid = ? and taskid = ? and correct = 1");
	prep.setString(1, request.getSession().getAttribute("userid").toString());
	prep.setInt(2, taskid);
	r = prep.executeQuery();
	
	int correct = 0;
	if(r.next())
		correct = r.getInt("count(*)");
	
	prep = c.prepareStatement("select timeat from attempts where userid = ? and taskid = ? order by id desc");
	prep.setString(1, request.getSession().getAttribute("userid").toString());
	prep.setInt(2, taskid);
	r = prep.executeQuery();
	
	String lasttime = "";
	if(r.next())
		lasttime = r.getString("timeat");
	
	double ratio = total == 0? 0 : ( (double) correct / (double) total);
	
	ratio =    ((int) (ratio * 100)) / 100.0;
	
	if(correct == 0) {
		incomplete += "<tr><td>" + taskid +"</td><td>" + (total > 0 ? (total + " tries") : "not tried yet") +"</td><td><a href='task.jsp?t="+taskid+"'>solve me!</a></td></tr>";
	}
	
	
	out.println("<tr><td>"+taskid+"</td><td>"+total+"</td><td>"+correct+"</td><td>"+ ratio +"</td><td>"+lasttime+"</td></tr>");
	
}

%>

</table>


<% if(!incomplete.isEmpty()) { %>
<h3>unsolved tasks</h3>
<table>
<tr><td>task</td><td>attempts</td><td>link</td></tr>
<% out.println(incomplete); %>
</table>
<% } 
}
%>
<br>
</div>
</body>
</html>
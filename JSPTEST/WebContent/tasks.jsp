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
<div id="main_center">
<%@ include file="nav.jsp" %>
<h2>list of available tasks</h2>
<ul>
<%
Task[] t = Task.getAllTasks();
for(int i=0; i<t.length; i++) {
	out.println("<li><a href=\"task.jsp?t="+t[i].id+"\">"+t[i].text+"</a></li>");	 
} 
%>
</ul>
</div>
</body>
</html>
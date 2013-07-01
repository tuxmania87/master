

<%@page import="java.io.File"%>
<%@page import="de.unihalle.sqlequalizer.QueryUtils"%>
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
<span style="text-align:center;"><h2>welcome to the sql-equalizer</h2></span>
TODO Statistik Übersichten<br>
<a href="tasks.jsp">Aufgaben</a>
<br><% out.println(System.getProperty("java.version")); %>
</div>
</body>
</html>
<%@page import="java.util.Enumeration"%>
<%@page import="de.unihalle.sqlequalizer.Login"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
 <%

 int id = 0;
 if(request.getParameter("submit") != null) {
	 
	 id = Login.login(request.getParameter("pname"), request.getParameter("ppassword"));
	 if(id > 0) {
		 request.getSession().setAttribute("userid", id);
		 
		 if(Login.isDozent(request.getParameter("pname"))) {
			 request.getSession().setAttribute("dozent", 1);
		 }
		 
		 response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
		 response.setHeader("Location", request.getParameter("refer")!=null?request.getParameter("refer"):"/JSPTEST/" ); 
	 }
 } 
 
 %>
    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<%@ include file="header.jsp" %>
</head>
<body>
<div id="main_center"> 
<form action="login.jsp?refer=<% out.print(request.getParameter("refer")); %>" method="post">
<h2>Login</h2>
<table>
<tr><td>Name: </td><td><input type="text" name="pname" /></td></tr>
<tr><td>Password: </td><td><input type="password" name="ppassword" /></td></tr>
</table>
<input type="submit" value="submit" name="submit" />
</form>
</div>
</body>
</html>
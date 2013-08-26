<div id="nav">

<ul>
<li><a href="index.jsp">main</a></li>
<li><a href="tasks.jsp">task overview</a></li>
<% 

if(session.getAttribute("dozent") != null) {
	out.println("<li><a href=\"acp.jsp\">admin control panel</a></li>");
}

%>
<li><a href="index.jsp?a=logout">logout</a></li>
</ul>


</div>
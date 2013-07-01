<%
if( !request.getRequestURI().split("/")[request.getRequestURI().split("/").length-1].equals("login.jsp") &&
 request.getSession().getAttribute("userid") == null) {
	response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
	response.setHeader("Location", "login.jsp?refer="+request.getRequestURI()); 
}

%>

<title>SQL-Equalizer</title>
<link rel="stylesheet" type="text/css" href="style.css" />
<script type="text/javascript">

function toggle(elem) {
	alert(elem.parentNode.innerHTML);
	alert(elem.parentNode.nextSibiling.nodeType);
	
	if(elem.style.visibility == "none") {
		elem.style.visibility = "visible";
	} else {
		elem.style.visibility = "none";
	}
}

</script>
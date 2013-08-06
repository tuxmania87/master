<%@page import="java.util.Iterator"%>
<%@page import="java.util.Enumeration"%>
<%
if( !request.getRequestURI().split("/")[request.getRequestURI().split("/").length-1].equals("login.jsp") &&
 request.getSession().getAttribute("userid") == null) {
	
	String params = "";
	
	Enumeration en = request.getParameterNames();
	if(en.hasMoreElements()) params = "?";
	while(en.hasMoreElements()) {
		String next= en.nextElement().toString();
		String val = request.getParameter(next).equals("logout") ? "" : request.getParameter(next);
		params +=next + "=" + val + "&";
	}
	if(params.length() > 0)
		params = params.substring(0, params.length()-1);
	
	response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
	response.setHeader("Location", "login.jsp?refer="+request.getRequestURI()+params); 
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

function schemaNameChanged(elem) {
	var id = elem.parentNode.parentNode.firstChild.innerHTML;
	
}

</script>
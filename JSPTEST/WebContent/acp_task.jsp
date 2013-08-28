<%@page import="de.unihalle.sqlequalizer.Connector"%>
<%@page import="de.unihalle.sqlequalizer.QueryHandler"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Vector"%>
<%@page import="java.util.EnumSet"%>
<%@page import="java.util.Set"%>
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
Exception parseerror = null;

/*System.out.println("To out-put All the request-attributes received from request - ");

Enumeration enAttr = request.getAttributeNames(); 
while(enAttr.hasMoreElements()){
 String attributeName = (String)enAttr.nextElement();
 System.out.println("Attribute Name - "+attributeName+", Value - "+(request.getAttribute(attributeName)).toString());
}

System.out.println("To out-put All the request parameters received from request - ");

Enumeration enParams = request.getParameterNames(); 
while(enParams.hasMoreElements()){
 String paramName = (String)enParams.nextElement();
 System.out.println("Attribute Name - "+paramName+", Value - "+request.getParameter(paramName));
}*/


if(request.getParameter("pid") != null) {
	System.out.println(request.getParameter("pid"));
	
	
	int id = Integer.valueOf(request.getParameter("pid"));
	String desc = request.getParameter("pdesc");
	
	int count = 0;
	
	ArrayList<String> samplesolutions = new ArrayList<String>();
	
	
	
	while(request.getParameter("pss"+count) != null) {
		
		String statement = request.getParameter("pss"+count);
		//check if parseable
		QueryHandler tester = new QueryHandler();
		try {
			tester.setOriginalStatement(statement);
		} catch(Exception e) {
			parseerror = e;
		}
		
		samplesolutions.add(statement);
		count++;
	}
	
	String[] sampleSolutionArr = samplesolutions.toArray(new String[samplesolutions.size()]);
	
	int schema = Integer.parseInt(request.getParameter("pschema"));
	
	count = 1;
	
	ArrayList<Integer> extDbs = new ArrayList<Integer>();
	
	for(int i=1; i<= Integer.parseInt(request.getParameter("pmaxdbid")); i++) {
		if(request.getParameter("pedbid"+i) != null) {
			extDbs.add(i);

		}
	}
	boolean respectColumn = request.getParameter("prespectcolumn") != null;
	
	out.println(respectColumn);
	
	
	if(parseerror == null) {
	
	Connection c = Connector.getConnection();
	
	PreparedStatement pr = c.prepareStatement("update tasks set description=?,respectColumnorder=?,schemaid=? where id = ?");
	pr.setString(1, desc);
	pr.setInt(2, respectColumn?1:0);
	pr.setInt(3, schema);
	pr.setInt(4, id);
	
	pr.executeUpdate();
	
	//delete first then fill up
	pr = c.prepareStatement("delete from samplesolutions where taskid = ?");
	pr.setInt(1, id);
	pr.execute();
	
	Statement s = c.createStatement();
	ResultSet rs = s.executeQuery("select max(id) from samplesolutions");
	rs.next();
	int maxssid = rs.getInt("max(id)");
	
	for(int i=0; i<sampleSolutionArr.length; i++) {
		pr = c.prepareStatement("insert into samplesolutions (id,taskid,sqlstatement) values (?,?,?)");
		pr.setInt(1, ++maxssid);
		pr.setInt(2, id);
		pr.setString(3, sampleSolutionArr[i]);
		
		pr.execute();
	}
	
	pr = c.prepareStatement("delete from tasks_db where taskid = ?");
	pr.setInt(1, id);
	pr.execute();
	
	for(int i =0; i< extDbs.size(); i++) {
		pr = c.prepareStatement("insert into tasks_db (taskid,dbid) values (?,?)");
		pr.setInt(1, id);
		pr.setInt(2, extDbs.get(i));
		
		pr.execute();
	}
	
	c.close();
	
	response.sendRedirect("/JSPTEST/acp.jsp");
	} 
}


if(request.getParameter("a") != null && request.getParameter("a").equals("addSampleSolution")) {
	Connection c = Connector.getConnection();
	Statement st = c.createStatement();
	ResultSet rs = st.executeQuery("select max(id) from samplesolutions");
	if(rs.next()) {
		int maxid = rs.getInt("max(id)");
		st = c.createStatement();
		PreparedStatement psmt = c.prepareStatement("insert into samplesolutions (id,taskid,sqlstatement) values (?,?,?)");
		psmt.setInt(1, maxid + 1);
		psmt.setInt(2, Integer.valueOf(request.getParameter("i")));
		psmt.setString(3, "fill in samplesolution");
		psmt.execute();
	}
}

Task t = null;
if(request.getParameter("i") != null) {
	t = new Task(Task.connect(),Integer.parseInt(request.getParameter("i")));
} else {
	response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
	response.setHeader("Location", request.getParameter("refer")!=null?request.getParameter("refer"):"/JSPTEST/acp.jsp" ); 
}



if(t != null) {
	
	
	
	
%>


<div id="main_center">

<%
if(parseerror != null) {
	out.println("<span style=\"color:red;font-weight:bold;\">There was an error parsing your samplesolutions:</span><br>"+parseerror.toString());
}
%>

<form action="acp_task.jsp?i=<%out.print(t.id); %>" method="post">
<h2>edit task #<%out.print(t.id); %></h2>

<h3>description</h3>
<textarea cols="80" rows="6" name="pdesc"><% out.print(t.text); %></textarea>

<input type="hidden" name="pid" value="<% out.print(t.id); %>">

<h3>sample solutions</h3>
<%

if(t.samplesolution == null || t.samplesolution.length == 0) {
	out.println("<textarea cols=\"80\" rows=\"2\" name=\"pss0\"></textarea>");
} else {

	for(int i=0; i<t.samplesolution.length; i++) {
		out.println("<textarea cols=\"80\" rows=\"2\" name=\"pss"+i+"\">"+t.samplesolution[i]+"</textarea>");	
	}
}
%>
<br><a href="acp_task.jsp?i=<% out.print(t.id);%>&a=addSampleSolution"><span style="font-size:large; font-weight:bold;">+</span> Add another sample solution</a>

<h3>database schema</h3>
<select name="pschema">
<%
Connection c = Task.connect();
Statement s = c.createStatement();
ResultSet r = s.executeQuery("select * from dbschema");

while(r.next()) {
	String add = "";
	if(r.getInt("id") == t.schemaid) {
		add = " default";
	} 
	out.println("<option value=\""+r.getInt("id")+"\" "+add+">"+r.getString("name")+"</option>");
}

%>
</select>

<h3>respect column order</h3>
if disabled, columns may appear in arbitrary order<br>
respect column order ? <input type="checkbox" name="prespectcolumn" <% if(t.respectColumn) out.print("checked"); %> /> 

<h3>external database</h3>
<table><tr><th>use</th><th>uri</th><th>dbname</th><th>db type</th></tr>

<%

PreparedStatement prep = c.prepareStatement("select dbid from tasks_db where taskid = ?");
prep.setInt(1, t.id);

r = prep.executeQuery();
Vector<String> id_collection = new Vector<String>();

while(r.next()) {
	id_collection.add(r.getString("dbid"));
}

s = c.createStatement();
r = s.executeQuery("select * from external_database");

String maxdbid = "0";

while(r.next()) {
	String id = r.getString("id");
	maxdbid = id;
	out.print("<tr><td><input type=\"checkbox\" name=\"pedbid"+id+"\" ");
	if(id_collection.contains(id)) {
		out.print(" checked");
	}
	out.print("></td><td>"+r.getString("uri")+"</td><td>"+r.getString("dbname")+"</td><td>"+r.getString("typ")+"</td></tr>");
}


c.close();
%>
</table><br><br>
<input type="hidden" name="pmaxdbid" value="<% out.print(maxdbid); %>">
<input type="submit" value="save">

</form>
</div>
<% } %>
</body>
</html>
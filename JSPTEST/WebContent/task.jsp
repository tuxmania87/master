<%@page import="java.sql.ResultSet"%>
<%@page import="de.unihalle.sqlequalizer.externalDB"%>
<%@page import="de.unihalle.sqlequalizer.MetaQueryInfo"%>
<%@page import="de.unihalle.sqlequalizer.QueryUtils"%>
<%@page import="org.gibello.zql.ZQuery"%>
<%@page import="de.unihalle.sqlequalizer.QueryHandler"%>
<%@page import="java.sql.Connection"%>
<%@page import="de.unihalle.sqlequalizer.Task"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<%@ include file="header.jsp"%>
</head>
<%
	Connection c = Task.connect();
	Task t = new Task(c, Integer.parseInt(request.getParameter("t")));
%>
<body>
	<div id="main_center">

		<h2>task description:</h2>

		<div class="description">
			<%
				out.println(t.text);
			%>
		</div>

		<h2>database schema</h2>
		<div class="description">
			<%
				for (int i = 0; i < t.dbschema.length; i++)
					out.println(t.dbschema[i] + "<br>");
			%>
		</div>

		<h2>your solution</h2>
		<form action="task.jsp?t=<%out.print(request.getParameter("t"));%>"
			method="post">
			<textarea rows="10" cols="80" name="user_solution"><%
					if (request.getParameter("user_solution") != null)
						out.print(request.getParameter("user_solution").trim());%></textarea>

			<br> <input type="submit" value="submit" name="submit" />
		</form>

		<%
			if (request.getParameter("user_solution") != null) {
		%>
		<%
			QueryHandler qh = new QueryHandler();
				QueryHandler qh_ss = new QueryHandler();

				ZQuery q = null;
				ZQuery q2 = null;

				String compare = null;
				String error = null;

				for (int i = 0; i < t.dbschema.length; i++) {
					qh.createTable(t.dbschema[i] + ";");
					qh_ss.createTable(t.dbschema[i] + ";");
				}
				try {
					qh.setOriginalStatement(request
							.getParameter("user_solution"));
					qh_ss.setOriginalStatement(t.samplesolution[0]);
					q = qh.equalize();
					q2 = qh_ss.equalize();
					compare = QueryUtils.compareMetaInfos(qh_ss.before,
									qh.before);

				} catch (Exception e) {
					error = e.toString();
					e.printStackTrace();
				}
		%>
		<h2>
			your parsed statement &nbsp;&nbsp;<a href="#" onclick="toggle(this)"><span
				style="font-size: 18px;">+</span></a>
		</h2>
		<div class="description">
			<%
				if (qh.original != null) {
						out.println(qh.original);
					} else {
						out.println("Error while parsing your statement. Check warnings below.");
					}
			%>
		</div>


		<h2>your equalized statement</h2>
		<div class="description">
			<%
				if (q != null) {
						out.println(q);
					} else {
						out.println("Error while parsing your statement. Check warnings below.");
					}
			%>
		</div>

		<h2>feedback from sql-equalizer</h2>
		<div class="description">
			<%
				if (error == null) {

						//check with equalizer 
						if (q.toString().equals(q2.toString())) {
							out.println("Your solution could be matched with a sample solution hence it is correct.<br>");
							if (compare != null && compare.trim() != "") {
								out.println("however the sql-equalizer encountered the following:<br>"
										+ compare);
							}
						} else {
							out.println("Your solution could not be matched with a sample solution.<br>");
							if (t.externalDbs.length > 0)
								out.println("Your sql query will be checked with real data now. proceeding.....<br>");
							//check with databases

							for (int i = 0; i < t.externalDbs.length; i++) {
								//foreach database
								ResultSet r1 = null;
								ResultSet r2 = null;
								try {
									r1 = externalDB.executeQueryOn(
											qh.before.currentQuery.toString(),
											t.externalDbs[i]);
									r2 = externalDB.executeQueryOn(
											qh_ss.before.currentQuery.toString(),
											t.externalDbs[i]);
								} catch (Exception e) {
									out.println("Error while executing your query on databse with real data:<br>"
											+ e.toString());
								}
								if (r1 != null && r2 != null) {
									try {
										if (QueryUtils.isIdenticalResultSets(r1, r2)) {
											out.println("sample-solution and your query returned the same data but your solution could not be matched against any valid sample solution. please discuss your solution with your lecturer.<br>");
											out.println("LevenshteinDistance: "+QueryUtils.getLevenshteinDistance(q.toString(), q2.toString()));
										}
									} catch (RuntimeException e) {
										out.println("Your query returned invalid data:<br>"+e.toString());
									}
								}

							}
							out.println("<br> <br>Additional notifications from the sql-equalizer:<br>"+compare);
						}

						if (request.getSession().getAttribute("dozent") != null) {
							out.println(q);
							out.println("<br>");
							out.println(q2);
						}
					} else {
						out.println("sql-equalizer could not parse your statement:<br>"
								+ error);
					}
			%>
		</div>
		<%
			}
		%>
	</div>
</body>
</html>
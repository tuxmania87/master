
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="de.unihalle.sqlequalizer.Connector"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.sql.ResultSetMetaData"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="de.unihalle.sqlequalizer.externalDB"%>
<%@page import="de.unihalle.sqlequalizer.MetaQueryInfo"%>
<%@page import="de.unihalle.sqlequalizer.QueryUtils"%>
<%@page import="org.gibello.zql.ZQuery"%>
<%@page import="org.gibello.zql.ZOrderBy"%>
<%@page import="de.unihalle.sqlequalizer.QueryHandler"%>
<%@page import="java.sql.Connection"%>
<%@page import="java.util.Vector"%>
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
	boolean firststep_matching = false;
%>
<body>
	<%@ include file="nav.jsp"%>
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
					out.println(QueryUtils.formatDBSchema(t.dbschema[i]) + "<br><br>");
			%>
		</div>

		<h2>your solution</h2>
		<form action="task.jsp?t=<%out.print(request.getParameter("t"));%>"
			method="post">
			<textarea rows="10" cols="80" name="user_solution"><%
					if (request.getParameter("user_solution") != null) {
						out.print(request.getParameter("user_solution").trim());
						System.out.println(request.getParameter("user_solution").trim());
					}
				%></textarea>

			<br> <input type="submit" value="submit" name="submit" />
		</form>

		<%
			if (request.getParameter("user_solution") != null) {
		%>
		<%
			QueryHandler qh = new QueryHandler();
				QueryHandler qh_ss = new QueryHandler();

				ZQuery q = null;
				ZQuery[] q2 = null;

				String compare = null;
				String compareAfter = null;
				String error = null;

				for (int i = 0; i < t.dbschema.length; i++) {
					qh.createTable(t.dbschema[i] + ";");
					qh_ss.createTable(t.dbschema[i] + ";");
				}
				try {
					qh.setOriginalStatement(request
							.getParameter("user_solution"));
					qh_ss.setOriginalStatement(t.samplesolution[0]);
					q = qh.equalize(t.respectColumn)[0];
					q2 = qh_ss.equalize(t.respectColumn);
					compare = QueryUtils.compareMetaInfos(qh_ss.before,
							qh.before);

				} catch (Exception e) {
					error = e.toString();
					e.printStackTrace();
				}
		%>
		<h2>
			your parsed statement
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
		<div class="border_only">
		<h3>SQL-Equalizer part 1 (Matching after standardization)</h3>
			<%
				if (error == null) {
						
						
						
						compareAfter = QueryUtils.compareStandardizedQueries(q, q2[0]);
						
						for(int i = 0; i<q2.length; i++) {
							String temp = QueryUtils.compareStandardizedQueries(q, q2[i]);
							if(temp.length() < compareAfter.length())
								compareAfter = temp;
						}
						
						//check with equalizer 
						
						firststep_matching = false;
						for(int i = 0; i<q2.length; i++) {
							System.out.println(i+" "+q2[i].toString());
							if(q.toString().toLowerCase().equals(q2[i].toString().toLowerCase()))
								firststep_matching = true;
						}
						
						System.out.println("Student: "+q.toString());
						
						if (firststep_matching) {
							out.println("<span style=\"color:green;font-weight:bold;font-size:large;\">Your solution could be matched with a sample solution hence it is correct.</span><br>");
							
						} else {
							out.println("<span style=\"color:red;font-weight:bold;font-size:large;\">Your solution could not be matched with a sample solution.</span><br>");

						}

						/*if (request.getSession().getAttribute("dozent") != null) {
							out.println("<br><br>");
							out.println(q);
							out.println("<br>");
							out.println(q2);
						}*/
					} else {
						out.println("<span style=\"color:red;font-weight:bold;font-size:large;\">sql-equalizer could not parse your statement:</span><br>"
								+ "<span style=\"font-weight:bold;font-size:large;\">" + error + "</span>");
					}
			%>
		<br>
		<h3>SQL-Equalizer part 2 (execution of queries on real data)</h3>
		<%		
				if(error != null) {
					
				} else
				if (firststep_matching) {
					out.println("<span style=\"color:green;font-weight:bold;font-size:large;\">SQL-Equalizer could already match your solution with the sample solutin. No need for test real data.</span><br>");
				} else if (t.externalDbs.length > 0){
					out.println("Your sql query will be checked with real data now. proceeding.....<br>");
					
					//check on real data
					for (int i = 0; i < t.externalDbs.length; i++) {
							//foreach database
							ResultSet r1 = null;
							ResultSet r2 = null;
							
							
							String sampleSolutionQuery = "";
							String studentSolutionQuery = "";
							
							//preprocess queries
							//if student solution contains order by but sample doesnt, we add order by temorary
							if(qh.original.getOrderBy() != null && qh_ss.original.getOrderBy() == null) {
								sampleSolutionQuery = qh_ss.before.currentQuery.toString();
								
								Vector<ZOrderBy> vec = qh.before.currentQuery.getOrderBy();	
								Iterator<ZOrderBy> it = vec.iterator();
								
								sampleSolutionQuery +=  " ORDER BY ";
								
								while(it.hasNext()) {
									ZOrderBy obj = it.next();
									sampleSolutionQuery += obj.getExpression().toString();
									if(!obj.getAscOrder()) 
										sampleSolutionQuery += " DESC";
									sampleSolutionQuery += ",";
									
								}
								
								sampleSolutionQuery = sampleSolutionQuery.substring(0, sampleSolutionQuery.length()-1);
								
								System.out.println(sampleSolutionQuery);
								
								studentSolutionQuery = qh.before.currentQuery.toString();
							} else {
								sampleSolutionQuery = qh_ss.before.currentQuery.toString();
								studentSolutionQuery = qh.before.currentQuery.toString();
							}
							
							try {
								r1 = externalDB.executeQueryOn(
										studentSolutionQuery,
										t.externalDbs[i]);
								r2 = externalDB.executeQueryOn(
										sampleSolutionQuery,
										t.externalDbs[i]);
							} catch (Exception e) {
								out.println("Error while executing your query on databse with real data:<br>"
										+ "<span style='color:red;font-weight:bold;>"+ e.toString() + "</span>");
							}
							if (r1 != null && r2 != null) {
								try {
									
									if (QueryUtils.isIdenticalResultSets(r1, r2)) {
										out.println("<br><span style=\"color:red;font-weight:bold;font-size:large;\">sample-solution and your query returned the same data but your solution could not be matched against any valid sample solution.</span><br>");
										
									} else { throw new RuntimeException(); }
								} catch (RuntimeException e) {
									out.println("Your query returned invalid data. Showing both resultsets below:<br><br>");

									out.println("<table class=\"outer\" cellspan=\"10\">");
									out.println("<tr><th>Your Solution</th><th>Sample Solution</th></tr>");
									out.println("<tr><td>");

									// first table is resultset1
									out.println("<table class=\"inner\">");

									ResultSetMetaData meta = r1.getMetaData();

									out.println("<tr>");
									for (int j = 1; j <= meta.getColumnCount(); j++) {
										out.println("<th>" + meta.getColumnName(j)
												+ "</th>");
									}
									out.println("</tr>");

									//r1.beforeFirst();
									//r2.beforeFirst();
									
									while (r1.next()) {
										out.println("<tr>");
										for (int j = 1; j <= meta.getColumnCount(); j++) {
											if (r1.getObject(j) != null)
												out.println("<td>"
														+ r1.getObject(j)
																.toString()
														+ "</td>");
											else
												out.println("<td>NULL</td>");
										}
										out.println("</tr>");
									}

									out.println("</table>");

									out.println("</td><td>");

									// 2nd table is resultset2
									out.println("<table class=\"inner\">");

									meta = r2.getMetaData();

									out.println("<tr>");
									for (int j = 1; j <= meta.getColumnCount(); j++) {
										out.println("<th>" + meta.getColumnName(j)
												+ "</th>");
									}
									out.println("</tr>");

									while (r2.next()) {
										out.println("<tr>");
										for (int j = 1; j <= meta.getColumnCount(); j++) {
											if (r2.getObject(j) != null)
												out.println("<td>"
														+ r2.getObject(j)
																.toString()
														+ "</td>");
											else
												out.println("<td>NULL</td>");
										}
										out.println("</tr>");
									}

									out.println("</table>");

									out.println("</td></tr></table>");

								}
							}

						}
					//endof check
					
				} else {
					out.println("<span style=\"color:red;font-weight:bold;font-size:large;\">There is no real database to check your solution on real data.</span>");
				}
			%>
		
		<% if(error == null) { %>
			
		<h3>Important Notifications</h3>
		SQL-Equalizer compared each single part of the queries and detected some problems:<br>
		<% out.println(compareAfter+"<br>"+compare); %>
		
		<% } %>
				
				<%
				
				if(request.getSession().getAttribute("dozent") != null)
					out.println(qh_ss.after.currentQuery);
				
				%>
					
		</div>
		
	
		
		<%
		
		
		c = Connector.getConnection();
		
		PreparedStatement prep = c.prepareStatement("select max(id) from attempts");
		ResultSet res = prep.executeQuery();
		res.next();
		
		int maxid = res.getInt("max(id)");
		
		prep = c.prepareStatement("insert into attempts (id,userid,taskid,timeat,sqlstatement,correct) values (?,?,?,?,?,?)");
		prep.setInt(1, maxid+1);
		prep.setString(2, session.getAttribute("userid").toString());
		prep.setInt(3, t.id);
		prep.setString(4, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		
		if(qh.original == null) {
			prep.setString(5,request.getParameter("user_solution"));	
		} else {
			prep.setString(5, qh.original.toString());	
		}
		
		
		prep.setInt(6, firststep_matching?1:0);
		
		prep.execute();
		
			}
		%>
	</div>
</body>
</html>
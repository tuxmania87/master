package de.unihalle.sqlequalizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores Information of a certain Table by storing the including columns
 * @author Robert Hartmann
 *
 */
public class Table {

	
	public boolean existColumn(String c) {
		Iterator<Column> it = columns.iterator();
		while(it.hasNext()) {
			if(it.next().name.equals(c))
				return true;
		}
		return false;
	}
	
	ArrayList<Column> columns;
	String name;
	private int getIndexOfLastParenthesis(String s) {
		for (int i = s.length() - 1; i >= 0; i--) {
			if (s.charAt(i) == ')')
				return i;
		}
		return -1;
	}

	/**
	 * Standard constructor parsed Table Informations from sql statement
	 * 
	 * @param sqlStatement SQL-Statement
	 */
	Table(String sqlStatement) {
		//TODO: Parse if valid statement
		
		name = sqlStatement.trim().replaceAll("\\s+", " ").split(" ")[2];
		columns = new ArrayList<Column>();
		String content = sqlStatement.substring(sqlStatement.indexOf('(') + 1,
				getIndexOfLastParenthesis(sqlStatement));

		boolean inParens = false;
		String newcontent = "";
		for(int i = 0; i<content.length(); i++) {
			if(content.charAt(i) == '(')
				inParens = true;
			else if(content.charAt(i) == ')')
				inParens = false;
			
			if(content.charAt(i) == ',' && inParens)
				newcontent += "%";
			else
				newcontent += content.charAt(i);
		}

		content = newcontent;
		
		String[] rows = content.split(",");
		for (int i = 0; i < rows.length; i++) {
			
			String[] temp = rows[i].trim().split(" ");
			
			int digits = 0;
			int type = Column.OTHER;
			if (temp[1].toLowerCase().indexOf("int") >= 0
					|| temp[1].toLowerCase().indexOf("number") >= 0) {
				type = Column.NUMBER;
			} else if (temp[1].toLowerCase().indexOf("varchar") >= 0
					|| temp[1].toLowerCase().indexOf("text") >= 0) {
				type = Column.VARCHAR;
			}
			
			
			
			Pattern p1 = Pattern.compile("[a-z0-9]+\\(\\s*[0-9]+\\s*\\%\\s*([0-9]+)\\s*\\)");
			Matcher m1 = p1.matcher(temp[1].toLowerCase());
			
			if(m1.matches()) {
				type = Column.NUMBER;
				digits = Integer.parseInt(m1.group(1));
			}
			
			
			//check for references
			String ref = null;
			
			for(int j = 0; j< temp.length - 1; j++) {
				if(temp[j].toLowerCase().equals("references")) {
					Pattern p = Pattern.compile("([a-z0-9]+)\\(([a-z0-9]+)\\)");
					Matcher m = p.matcher(temp[j+1].toLowerCase());
					if(m.matches()) {
						ref = m.group(1)+"."+m.group(2);
					}
				}
			}
			
			//check for notnull
			boolean nul = true;
			for(int j = 1; j< temp.length; j++) {
				if((temp[j].toLowerCase().equals("null") && temp[j-1].toLowerCase().equals("not")) ||
						((temp[j].toLowerCase().equals("key") && temp[j-1].toLowerCase().equals("primary")))) {
					nul = false;
				}
			}
			
			//Column c = new Column(temp[0], type);
			//System.out.println(temp[0]+" "+type+" "+ref+" "+nul+" "+digits);
			Column c = new Column(temp[0], type, false, ref, nul, digits);
			columns.add(c);
		}
	}
	
	@Override
	public String toString() {
		String ret = "";
		ret += "Table: "+ name+ "\n";
		
		for(int i=0; i< columns.size(); i++) {
			ret += columns.get(i).name+" ("+columns.get(i).type+")"+"\n";
		}
		
		return ret;
	}

}

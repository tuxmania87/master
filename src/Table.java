import java.util.ArrayList;
import java.util.Iterator;

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

	Table(String n,String sqlStatement) {
		name = n;
		columns = new ArrayList<Column>();
		String content = sqlStatement.substring(sqlStatement.indexOf('(') + 1,
				getIndexOfLastParenthesis(sqlStatement));

		System.out.println(content);

		String[] rows = content.split(",");
		for (int i = 0; i < rows.length; i++) {
			String[] temp = rows[i].trim().split(" ");
			int type = Column.OTHER;
			if (temp[1].toLowerCase().indexOf("int") >= 0
					|| temp[1].toLowerCase().indexOf("number") >= 0) {
				type = Column.NUMBER;
			} else if (temp[1].toLowerCase().indexOf("varchar") >= 0
					|| temp[1].toLowerCase().indexOf("text") >= 0) {
				type = Column.VARCHAR;
			}
			Column c = new Column(temp[0], type);
			columns.add(c);
		}
	}
	
	@Override
	public String toString() {
		String ret = "";
		ret += "Table: "+ name+ System.lineSeparator();
		
		for(int i=0; i< columns.size(); i++) {
			ret += columns.get(i).name+" ("+columns.get(i).type+")"+System.lineSeparator();
		}
		
		return ret;
	}

}

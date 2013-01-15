
public class Column {

	static int NUMBER = 0;
	static int VARCHAR = 1;
	static int DATETIME = 2;
	static int OTHER = 3;
	
	String name;
	int type;
	boolean unique;
	String references_to;
	
	Column(String n, int t) {
		name = n;
		type = t;
		unique = false;
		references_to = null;
	}
	
	Column(String n, int t, boolean u) {
		name = n;
		type = t;
		unique = u;
		references_to = null;
	}
	
	Column(String n, int t, boolean u, String r) {
		name = n;
		type = t;
		unique = u;
		references_to = r;
	}
	
}

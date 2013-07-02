package de.unihalle.sqlequalizer;


public class Column {

	static int NUMBER = 0;
	static int VARCHAR = 1;
	static int DATETIME = 2;
	static int OTHER = 3;
	
	
	
	String name;
	int type;
	boolean unique;
	String references_to;
	boolean canBeNull;
	int digitsAfterColon = 0;
	
	Column(String n, int t) {
		name = n;
		type = t;
		unique = false;
		references_to = null;
		canBeNull = true;
	}
	
	Column(String n, int t, boolean u) {
		name = n;
		type = t;
		unique = u;
		references_to = null;
		canBeNull = true;
	}
	
	Column(String n, int t, boolean u, String r) {
		name = n;
		type = t;
		unique = u;
		references_to = r;
		canBeNull = true;
	}
	
	Column(String n, int t, boolean u, String r, boolean nul, int digits) {
		name = n;
		type = t;
		unique = u;
		references_to = r;
		canBeNull = nul;
		digitsAfterColon = digits;
	}
	
	
}

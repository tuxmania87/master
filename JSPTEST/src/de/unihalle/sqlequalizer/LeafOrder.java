package de.unihalle.sqlequalizer;

import java.util.Arrays;
import java.util.Comparator;

import org.gibello.zql.ZExp;

public class LeafOrder implements Comparator<ZExp>{

	@Override
	public int compare(ZExp o1, ZExp o2) {

		String[] leftLeafs = QueryUtils.getLeafes(o1);
		String[] rightLeafs = QueryUtils.getLeafes(o2);
		
		Arrays.sort(leftLeafs);
		Arrays.sort(rightLeafs);
		
		for(int i = 0; i< leftLeafs.length; i++) {
			if(i >= rightLeafs.length)
				return 1;
			
			if(! leftLeafs[i].equals(rightLeafs[i])) {
				return leftLeafs[i].compareTo(rightLeafs[i]);
			}
		}
		
		return 0;
	}
	

}

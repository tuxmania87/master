package de.unihalle.sqlequalizer;

import java.util.Arrays;
import java.util.Comparator;

import org.gibello.zql.ZConstant;
import org.gibello.zql.ZExp;
import org.gibello.zql.ZExpression;
import org.gibello.zql.ZQuery;

public class OperatorComparer implements Comparator<ZExp> {

	@Override
	public int compare(ZExp o1, ZExp o2) {
		
		ZExpression z1 = null;
		ZExpression z2 = null;
		
		ZConstant c1 = null;
		ZConstant c2 = null;
		
		
		if(o1 instanceof ZExpression)
			z1 = (ZExpression) o1;

		if(o2 instanceof ZExpression)
			z2 = (ZExpression) o2;
		
		if(o1 instanceof ZConstant)
			c1 = (ZConstant) o1;
		
		if(o2 instanceof ZConstant)
			c2 = (ZConstant) o2;
		
	
		
		if(c1 != null && z2 != null)
			return -1;
		
		if(z1 != null && c2 != null)
			return 1;
		
		if(c1 != null && c2 != null) {
			if(c1.getType() == ZConstant.COLUMNNAME && c2.getType() != ZConstant.COLUMNNAME)
				return -1;
			
			if(c1.getType() != ZConstant.COLUMNNAME && c2.getType() == ZConstant.COLUMNNAME)
				return 1;
			
			if(c1.getType() == ZConstant.COLUMNNAME && c2.getType() == ZConstant.COLUMNNAME)
				return c1.getValue().compareTo(c2.getValue());
			
			return 0;
			
		}
		
		
		
		//if subqueries have same depth we have to decide their depth
		
		if(z1 != null && z2 != null) {
			if(Arrays.asList(QueryUtils.orderList).indexOf(z1.getOperator()) < Arrays.asList(QueryUtils.orderList).indexOf(z2.getOperator()))
				return -1;
			
			if(Arrays.asList(QueryUtils.orderList).indexOf(z2.getOperator()) < Arrays.asList(QueryUtils.orderList).indexOf(z1.getOperator()))
				return 1;
			
			if(Arrays.asList(QueryUtils.orderList).indexOf(z2.getOperator()) == Arrays.asList(QueryUtils.orderList).indexOf(z1.getOperator())) {
				//we have both the same operator and have to decide with a DFS and the first different node (except leafs)
				// if we structure is all the same, then we decide leaf node wise
							
				/*int value = QueryUtils.DFSFirstDifferentNodeValue(z1,z2);
				if(value == 0) {
					//System.out.println("struktur gleich :");
					//System.out.println(z1+" "+z2);
					
					String[] leftLeafs = QueryUtils.getLeafes(z1);
					String[] rightLeafs = QueryUtils.getLeafes(z1);
					
					Arrays.sort(leftLeafs);
					Arrays.sort(rightLeafs);
					
					for(int i=0; i<leftLeafs.length; i++) {
						if(leftLeafs[i].compareTo(rightLeafs[i]) != 0) 
							return leftLeafs[i].compareTo(rightLeafs[i]);
					}
					
				} else {
					return value;
				}*/
				
			}
			
			return 0;
		}
		
		return 0;
	}

}

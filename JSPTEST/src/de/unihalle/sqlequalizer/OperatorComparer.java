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
		
	
		//o1 is constant and o2 is expression => constant has to be first
		if(c1 != null && z2 != null)
			return -1;
		
		//o1 is expression and o2 is constant => constant has to be first
		if(z1 != null && c2 != null)
			return 1;
		
		if(c1 != null && c2 != null) {
			
			// c1 is columnname and c2 is constant => columname has to be firts
			if(c1.getType() == ZConstant.COLUMNNAME && c2.getType() != ZConstant.COLUMNNAME)
				//return -1;
				return 1;
			
			// c1 is constant and c2 is columnanme => columname has to be firts			
			if(c1.getType() != ZConstant.COLUMNNAME && c2.getType() == ZConstant.COLUMNNAME)
				//return 1;
				return -1;
			
			// c1 is columname and c2 is columname
			//if(c1.getType() == ZConstant.COLUMNNAME && c2.getType() == ZConstant.COLUMNNAME)
				return c1.getValue().compareTo(c2.getValue());
			
			//return 0;
			
		}
		
		
		
		//if subqueries have same depth we have to decide their depth
		
		if(z1 != null && z2 != null) {
			if(Arrays.asList(QueryUtils.orderList).indexOf(z1.getOperator()) < Arrays.asList(QueryUtils.orderList).indexOf(z2.getOperator()))
				return -1;
			
			if(Arrays.asList(QueryUtils.orderList).indexOf(z2.getOperator()) < Arrays.asList(QueryUtils.orderList).indexOf(z1.getOperator()))
				return 1;
			
			if(Arrays.asList(QueryUtils.orderList).indexOf(z2.getOperator()) == Arrays.asList(QueryUtils.orderList).indexOf(z1.getOperator())) {
				//iterate both trees and as soon as structure differs
				return QueryUtils.DFSFirstDifferentNodeValue(z1, z2);
				
			}
			
			return 0;
		}
		
		return 0;
	}

}

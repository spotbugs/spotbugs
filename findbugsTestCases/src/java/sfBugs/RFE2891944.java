package sfBugs;

import java.math.BigDecimal;

public class RFE2891944 {
	
	public static void main(String args[]) {
		
		BigDecimal bd = new BigDecimal(0.1);
		System.out.println(bd);
	}

}

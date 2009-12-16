package sfBugs;

import java.math.BigDecimal;

public class RFE2891944 {

	public static void bug1() {
		BigDecimal bd = new BigDecimal(0.1);
		System.out.println(bd);
	}
	public static void bug2() {
		BigDecimal bd = new BigDecimal(100.0);
		System.out.println(bd);
	}
	public static void bug3() {
		BigDecimal bd = new BigDecimal(1.0);
		System.out.println(bd);
	}

}

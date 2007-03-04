

import java.util.Date;

public class MyMonth extends Date{

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Date x=new Date();
		x.setMonth(12);
        x.setMonth(-1);
		
		String month="January";
		
		System.out.println(month.toUpperCase());
		month=month.toUpperCase();
	}
}

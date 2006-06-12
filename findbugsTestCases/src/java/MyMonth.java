

import java.io.File;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Date;

public class MyMonth extends Date{

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Date x=new Date();
		x.setMonth(12);
		
		String month="January";
		
		System.out.println(month.toUpperCase());
		month=month.toUpperCase();
	}
}

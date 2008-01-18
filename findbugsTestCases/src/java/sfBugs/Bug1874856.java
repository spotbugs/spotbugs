package sfBugs;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class Bug1874856 {

	public static void main(String args[]) {
		falsePositive();
	}
		
	public static void falsePositive() {
		  Calendar c = new GregorianCalendar(1993, 4, 23);
		   String s1 = String.format("Duke's Birthday: %1$tm %1$te, %1$tY", c);
		   System.out.println(s1);
		   String s2 = String.format("Duke's Birthday: %1$tm %<te, %<tY", c);
		   System.out.println(s2);
	}
}

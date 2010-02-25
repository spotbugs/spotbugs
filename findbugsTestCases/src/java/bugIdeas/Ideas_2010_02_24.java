package bugIdeas;

import java.util.Date;

import org.joda.time.Instant;

public class Ideas_2010_02_24 {

	public Date bad1(int x) {
		return new Date(x * 1000);
	}
	public Date bad2(int x) {
		return new Date(1000 * x);
	}

	public java.sql.Date bad3(int x) {
		return new java.sql.Date(1000 * x);
	}

	public Instant bad4(int x) {
		return new Instant(x * 1000);
	}

	public static void main(String args[]) {
		long x = System.currentTimeMillis();
		System.out.println(Long.toHexString(x));
		System.out.println(Long.toHexString(x/1000));
		System.out.println(Integer.MAX_VALUE - x/1000);
		System.out.println(new Date(Integer.MIN_VALUE));
		System.out.println(new Date(0));
		System.out.println(new Date(Integer.MAX_VALUE));
		System.out.println(new Date(Integer.MAX_VALUE * 1000L));
	}

}

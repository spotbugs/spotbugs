import java.util.Date;

public class DangerousStoreIntoField {

	Date w;

	int x;

	String y;

	String[] z;

	public void f(int a, String b, String[] c) {
		x = a;
	}

	public void g(int a, String b, String[] c) {
		y = b;
	}

	public void h(int a, String b, String[] c) {
		z = c;
	}

	public void i(int a, String b, String[] c) {
		if (c.length == 0)
			throw new IllegalArgumentException();
		z = c;
	}

	public void j(Date d) {
		if (d.before(new Date()))
			throw new IllegalArgumentException();
		w = d;
	}
}


public class CircularDepsTest {
	public void test1() {
		Circ1 c1 = new Circ1(this);
		Circ2 c2 = new Circ2();
		NoCirc nc = new NoCirc();
	}

	public void test2() {
		System.out.println("woot!");
	}
}

class Circ1 {
	public Circ1(CircularDepsTest cdt) {
		cdt.test2();
	}
}

class Circ2 {
	public Circ2() {
		Circ1 c1 = new Circ1(new CircularDepsTest());
	}
}

class NoCirc {
	public NoCirc() {
	}
}
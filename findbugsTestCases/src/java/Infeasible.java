import java.io.IOException;

class Frotz {
	public void f(int x) throws IOException {
		if (x == 42)
			throw new IOException();
	}
}

class Infeasible {

	int foo() {
		Frotz f = new Frotz();

		Infeasible a;
		a = null;
		try {
			a = new Infeasible();
			f.f(99);
		} catch (IOException e) {
			e.printStackTrace();
			// a should always be initialized here
			return a.hashCode();
		}
		// a should always be initialized here
		return a.hashCode();
	}
}

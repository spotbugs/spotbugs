public class UncalledPrivateMethod {
	String s;

	private void foo(String s) {
		this.s = s;
	}


  private void debug(String s) {
	System.out.println(s);
	}

  private void foobar(int i) {
	}
  private void foobar(double d) {
	}

  public void f(double d) {
	foobar(d);
	}

}

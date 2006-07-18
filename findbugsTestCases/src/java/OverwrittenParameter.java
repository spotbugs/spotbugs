
public class OverwrittenParameter {
	
	public void f(int i, String x, String y, String z) {
		y = Integer.toString(i);
	}
	public void g(long i, String x, String y, String z) {
		y = Long.toString(i);
	}

}

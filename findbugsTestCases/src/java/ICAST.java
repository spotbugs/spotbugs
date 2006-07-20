

public class ICAST {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int x=20;
		byte q=10;
		short s=5;
		s=(short)(q>>>16);
		q=(byte)(q>>>16);
		x=x>>>37;
		x=x<<37;
		
		double y=(double)x;
		y=Math.ceil(y);
		double z=Math.ceil((double)x);	
	}
	
	public int roundUp(int a, int b) {
		return (int) Math.ceil(a/b);
	}
	
	public double doubleDivision(int a, int b) {
		return (double) (a/b);
	}
	public int round(int a, int b) {
		return Math.round(a/b);
	}

}

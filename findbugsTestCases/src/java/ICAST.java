

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
}

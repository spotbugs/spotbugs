

public class MutableMan {
	static public int[] y=new int[1];
	final int[] x=new int[1];
	/**
	 * @param args
	 */
	
	public int[] getX()
	{
		return x;
	}
	
	public void setX(int value)
	{
		x[0]=value;
	}
	
	public MutableMan ()
	{
		//x=y;
	}
	
	public boolean equals(MutableMan m)
	{
		return false;
	}
	
	public static void main(String[] args) {
		System.out.println(new MutableMan().x);
	}

}

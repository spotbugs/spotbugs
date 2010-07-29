package bugIdeas;

public class Ideas_2010_07_29 {
	
	public static void test(byte a[]) {
		byte b = a[0];
		Byte bb = b;
		int b2 = bb;
		if (b2 == 255)
			System.out.println("huh");
	}

}

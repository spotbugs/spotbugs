public class FloatMath {
	public static void main(String[] args) {
		final int START = 1234567890;
		int count = 0;
		for (float f = START; f < START + 50; f++)
			count++;
		System.out.println(count);
	}
}

public class IDiv {
	public static void main(String[] argv) {
		int a = Integer.parseInt(argv[0]);
		int b = Integer.parseInt(argv[1]);

		double value = (double) (a / b);
		System.out.println(value);
	}
}
package npe;

public class NullComplexPath {
	public static void main(String[] argv) {
		if (argv.length < 1)
			return;

		Object o = null;

		if (argv[0].equals("S")) {
			o = new Object();
		}

		if (argv[0].equals("S")) {
			System.out.println("Hash code is " + o.hashCode());
		}
	}
}

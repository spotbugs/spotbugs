public class DeadStore {
	public static void main(String[] args) {
		String live = "Hello, world";
		String dead = "Oops!";

		System.out.println(live);
		args = new String[0];
	}

	public int finalLocalDNR(int a) {
		final int SCAN = 0;    //<<---- complains about this line
		final int STAR = 1;
		final int DONE = 2;
		//int state = SCAN;

		a += SCAN;
		a += STAR;
		a += DONE;

		return a;
	}

	public int storeNullDNR(int a) {
		Object foo = null;
		return a;
	}

	public int storeZeroDNR(int a) {
		int count = 0;

		return a;
	}

	public int killedByStoreDNR(int a) {
		int b = 3;

		if (a > 1) {
			b = 4;
			a += b;
		}

		return a;
	}
}

public class TrickyJSR {

	private static final boolean P = Boolean.getBoolean("P");
	private static final boolean PP = Boolean.getBoolean("PP");
	private static final boolean QQ = Boolean.getBoolean("QQ");
	private static final boolean ZZ = Boolean.getBoolean("ZZ");

	public static void main(String[] argv) throws Throwable {

		int count = 0;

	loop:
		for (int i = 0; i < 10; ++i) {
			try {
				try {
					count++;

					if (PP) {
						System.out.println("MMM");
						throw new Throwable("X");
					}

					if (QQ)
						break loop;

					if (ZZ)
						throw new Throwable("ZX");


				} finally {
					count++;
					count += Math.max(count, ((count ^ 0xff) & 0x3) != 0 ? 10 : 20);
					if (P) throw new Exception("hah!");
				}
			} catch (Exception e) {
				System.out.println("Yeah");
			}
		}	

		System.out.println("count="+count);

	}
}

// vim:ts=4

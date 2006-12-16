package npe;

/**
 * Bug found in analysis of Nutch-0.8.1
 * @author pugh
 *
 */
public class NutchBug {

	Object x;

	static int test(NutchBug b, int y) {

		if (b.x == null)
			b.x = new Object();

		if (b.x.hashCode() == 42) {
		}

		if (b.x == null) {
			return 1;
		}
		return 2;

	}
}

public class TestFloatEquality {
	public static void main(String[] args) {
		final float fConst = 1.0f;
		final double dConst = 1.0d;
		final float fEpsilon = .0000001f;
		final double dEpsilon = .000000000000001d;

		if (0.0f / 0.0f != Float.NaN) {
			System.out.println("float NaN != NaN");
		}

		for (int i = 1; i < 50; ++i) {
			float f = fConst / i * i;
			if (f != fConst) // FE_FLOATING_POINT_EQUALITY
			{
				System.out.println("inexact float: " + i);
			}
			// The following expression should be false.
			if (Math.abs(f - fConst) >= fEpsilon) {
				System.out.println("epsilon too large: " + i);
			}
		}
		for (int i = 50; i < 100; ++i) {
			float f = fConst / i * i;
			if (f == fConst) // FE_FLOATING_POINT_EQUALITY
			{
			} else {
				System.out.println("inexact float: " + i);
			}
			// The following expression should be false.
			if (Math.abs(f - fConst) >= fEpsilon) {
				System.out.println("epsilon too large: " + i);
			}
		}

		if (0.0d / 0.0d != Double.NaN) {
			System.out.println("double NaN != NaN");
		}

		for (int i = 1; i < 50; ++i) {
			double d = dConst / i * i;
			if (d != dConst) // FE_FLOATING_POINT_EQUALITY
			{
				System.out.println("inexact double: " + i);
			}
			// The following expression should be false.
			if (Math.abs(d - dConst) >= dEpsilon) {
				System.out.println("epsilon too large: " + i);
			}
		}
		for (int i = 50; i < 100; ++i) {
			double d = dConst / i * i;
			if (d == dConst) // FE_FLOATING_POINT_EQUALITY
			{
			} else {
				System.out.println("inexact double: " + i);
			}
			// The following expression should be false.
			if (Math.abs(d - dConst) >= dEpsilon) {
				System.out.println("epsilon too large: " + i);
			}
		}
	}
}

import java.math.BigInteger;

public class UnnecessaryMathTest {
	static {
		// Bug 1196294 - Don't report in clinit
		BigInteger check = BigInteger.valueOf((long) Math.floor(Integer.MAX_VALUE / 127.5));
	}

	public void test() {
		double a;

		a = Math.abs(1.5);
		a = Math.acos(0.0);
		a = Math.acos(1.0);
		a = Math.asin(0.0);
		a = Math.asin(1.0);
		a = Math.atan(0.0);
		a = Math.atan(1.0);
		a = Math.cbrt(0.0);
		a = Math.cbrt(1.0);
		a = Math.ceil(1.5);
		a = Math.cos(0.0);
		a = Math.cosh(0.0);
		a = Math.exp(0.0);
		a = Math.exp(1.0);
		a = Math.floor(1.5);
		a = Math.log(0.0);
		a = Math.log(1.0);
		a = Math.log10(0.0);
		a = Math.log10(1.0);
		a = Math.pow(10.0, 0.0);
		a = Math.pow(10.0, 1.0);
		a = Math.rint(1.5);
		a = Math.round(1.5);
		a = Math.sin(0.0);
		a = Math.sinh(0.0);
		a = Math.sqrt(0.0);
		a = Math.sqrt(1.0);
		a = Math.tan(0.0);
		a = Math.tanh(0.0);
		a = Math.toDegrees(0.0);
		a = Math.toDegrees(1.0);
		a = Math.toRadians(0.0);
	}
}
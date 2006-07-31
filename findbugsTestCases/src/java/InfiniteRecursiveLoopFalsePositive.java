public class InfiniteRecursiveLoopFalsePositive {

	InfiniteRecursiveLoopFalsePositive x = new Inner();

	double z;

	double f(double y) {
		z = x.f(y);
		return z;
	}

	static class Inner extends InfiniteRecursiveLoopFalsePositive {

		double f(double y) {
			return y * y;
		}
	}
}

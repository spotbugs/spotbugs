import java.util.HashSet;
import java.util.Set;


public class InfiniteLoopFalsePositive {
	static class InnerClass extends InfiniteLoopFalsePositive {
		@Override
		public int x() {
			return 42;
		}
	}
	int z(Object o) {
		o = (Object) o;
		return ((int[]) o).length;
	}
	public int x() {
		int y = ((HashSet)(Set)new HashSet()).size();
		return ((InnerClass)this).x();
	}

}

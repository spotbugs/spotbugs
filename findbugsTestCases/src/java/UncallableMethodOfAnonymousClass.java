
public class UncallableMethodOfAnonymousClass {
	private class DepFactory {
		public Object getDep() {
			return new Object() {
                public UncallableMethodOfAnonymousClass getDepSetter() {
					return UncallableMethodOfAnonymousClass.this;
				}
			};
        }
	}

}

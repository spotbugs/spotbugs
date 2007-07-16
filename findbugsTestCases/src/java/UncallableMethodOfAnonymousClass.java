import java.util.Comparator;


public class UncallableMethodOfAnonymousClass {
	  private final static Comparator COMPARATOR = new Comparator() { 
	      public int compare(Object o1, Object o2) { 
	         int result = o1.hashCode() - o2.hashCode(); 
	         assert(result > 0); 
	         return result; 
	      } 
	   }; 

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

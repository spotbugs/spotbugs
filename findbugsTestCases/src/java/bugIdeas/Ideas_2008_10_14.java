package bugIdeas;

public class Ideas_2008_10_14 {

	 static int falsePositive(Object key) {
		    int rawHash = key.hashCode();
		    return rawHash == Integer.MIN_VALUE ? 0 : Math.abs(rawHash);
		  }
	 
	 String foo;
	 Ideas_2008_10_14(String f00) {
		 this.foo = foo;
	 }
	 
	 String getFoo() {
		 return foo;
	 }
}

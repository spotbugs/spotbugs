package bugIdeas;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.DesireNoWarning;
import edu.umd.cs.findbugs.annotations.DesireWarning;

public class Ideas_2010_06_16 {
	
	
	@DesireNoWarning("NP")
	static void test2(@Nullable Object... t) {
		for(Object x : t) {
			System.err.println(x);
		}
	}

	public static void main(String args[]) {
		test2("a", "b");
		main2();
	}
	
	@DesireWarning("NP")
	public static void main2() {
		test2((Object[]) null);
		
	}

}

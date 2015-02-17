package sfBugsNew;

import edu.umd.cs.findbugs.annotations.NoWarning;

public class Bug1361 {
    @NoWarning("UC")
	public void test(boolean flag) {
		if(!flag) {
			System.out.println("No flag");
		} else {
			System.out.println("!flag = "+(!flag));
		}
	}
}

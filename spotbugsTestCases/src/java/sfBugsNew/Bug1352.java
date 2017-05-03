package sfBugsNew;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

public class Bug1352 {
	static abstract class Super {
		public void test() {
			System.out.println("ok");
		}
	}
	
	static class ChildOk extends Super {
		
	}
	
	static class ChildUnsupported extends Super {
		@Override
		public void test() {
			throw new UnsupportedOperationException();
		}
	}

	@ExpectWarning("NP_NULL_ON_SOME_PATH")
	public void testMethod(String s, Super c) {
		if(s == null) {
			System.out.println("!");
		}
		c.test();
		System.out.println(s.trim());
	}
}

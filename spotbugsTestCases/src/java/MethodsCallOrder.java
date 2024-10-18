/**
 * @author Tomas Polesovsky
 */
public class MethodsCallOrder {

	static class SimpleTest {
		static {
			staticMethod1();
		}

		public static void staticMethod1() {
			staticMethod2();
		}

		public static void staticMethod2() {
			staticMethod3();
		}

		public static void staticMethod3() {
			staticMethod4();
		}

		public static void staticMethod4() {
			new MethodsCallOrder.SimpleTest();
		}

		public SimpleTest() {
			this(null);
		}

		public SimpleTest(Object o) {
			method1();
		}

		public void method1() {
			method2();
		}

		public void method2() {
			method3();
		}

		public void method3() {
			method4();
		}

		public void method4() {
			System.out.println("HERE WE GO!");
		}
	}

	static class ShortcutPathTest {
		public ShortcutPathTest() {
			methodA();
		}

		public void methodA(){
			if (System.currentTimeMillis() % 2 == 1) {
				methodB();
			} else {
				methodC();
			}
		}

		public void methodB(){
			methodC();
		}

		public void methodC(){
			System.out.println("HERE WE GO!");
		}
	}

}

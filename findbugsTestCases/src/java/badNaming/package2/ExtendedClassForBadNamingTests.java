package badNaming.package2;

import badNaming.package1.BaseClassForBadNamingTests;

public class ExtendedClassForBadNamingTests extends BaseClassForBadNamingTests  implements AnInterface {

	public void test1(A a) {}

	public void test2(A a) {}

	public void test3(A a) {}

	public void callingMethod(A a) {
		test3(a);
	}


}

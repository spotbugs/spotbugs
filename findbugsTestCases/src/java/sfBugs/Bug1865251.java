package sfBugs;

public class Bug1865251 {
	static boolean  foo(Class c) {
		return c == Bug1865251.class;
	}

}

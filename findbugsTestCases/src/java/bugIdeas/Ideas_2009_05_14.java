package bugIdeas;

import java.util.HashSet;
import java.util.Set;

public class Ideas_2009_05_14 {
	static <E> Set<E> newHashSet() {
		return new HashSet<E>();
	}
	static <E> Set<E> newHashSet(int x) {
		return new HashSet<E>();
	}
	
	static Set<String> foo,bar;
	
	static Set<String> getFoo() {
		if (foo == null) {
			foo = newHashSet();
			foo.add("a");
			foo.add("b");
		}
		return foo;
	}
	static Set<String> getFaz() {
		if (foo == null) {
			foo = newHashSet(1);
			foo.add("a");
			foo.add("b");
		}
		return foo;
	}
	static Set<String> getBar() {
		if (bar == null) {
			bar = new HashSet<String>();
			bar.add("a");
			bar.add("b");
		}
		return bar;
	}

}

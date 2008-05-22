package equals;

import java.util.HashSet;

public class DefaultEquals {
	
	public boolean equals(Object o) {
		return this == o;
	}
	public static HashSet<DefaultEquals> set = new  HashSet<DefaultEquals> ();
	
	public static boolean foo(DefaultEquals bar) {
		return set.add(bar);
	}

}

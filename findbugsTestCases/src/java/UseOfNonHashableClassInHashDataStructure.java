import java.util.*;

public class UseOfNonHashableClassInHashDataStructure {

	static HashMap<UseOfNonHashableClassInHashDataStructure, String> m = new HashMap<UseOfNonHashableClassInHashDataStructure, String>();

	public boolean equals(Object o) {
		return hashCode() == o.hashCode();
	}

	public static String add(UseOfNonHashableClassInHashDataStructure b,
			String s) {
		return m.put(b, s);
	}

	public static String get(UseOfNonHashableClassInHashDataStructure b) {
		return m.get(b);
	}
}

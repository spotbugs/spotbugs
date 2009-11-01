package bugIdeas;

import java.util.Collection;
import java.util.Iterator;

public class Ideas_2009_11_01 {
	
	public String getString() {
		return "x";
	}

	public Integer getInteger() {
		return 5;
	}
	
	public int check(Object x) {
		if (getString() == x)
			return 1;
		if (x == getString())
			return 2;
		if (getInteger() == x)
			return 3;
		if (x == getInteger())
			return 4;
		if (getInteger() == getInteger())
			return 5;
		if (getString() == getString())
			return 5;
		return 5;
	}
	
	public static boolean verifyStringCollection(Collection<String> c) {
		for(Iterator i = c.iterator(); i.hasNext(); )
			if (!(i.next() instanceof String))
				return false;
		return true;
	}
}

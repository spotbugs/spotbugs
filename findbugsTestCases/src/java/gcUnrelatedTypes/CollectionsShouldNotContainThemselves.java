package gcUnrelatedTypes;

import java.util.HashSet;
import java.util.Set;

public class CollectionsShouldNotContainThemselves {
	
	public static void main(String args[]) {
		
		Set s = new HashSet();
		
		s.contains(s);
		s.remove(s);
		s.containsAll(s);
		s.retainAll(s);
		s.removeAll(s);
	}

}

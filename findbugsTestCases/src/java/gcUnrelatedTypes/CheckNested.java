package gcUnrelatedTypes;

import java.util.HashSet;
import java.util.Set;

public class CheckNested {
	
	public static void main(String args[]) {
		Set<Set<Integer>> s = new HashSet<Set<Integer>>();
		
		Set<Long> l = new HashSet<Long>();
		
		s.remove(l);
	}

}

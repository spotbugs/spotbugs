package gcUnrelatedTypes;

import java.util.HashSet;
import java.util.List;

public class ShowingEqualsMethodUsed {
	
	HashSet<String> set = new HashSet<String>();
	
	public boolean testByteArray(byte[] b) {
		return set.contains(b);
	}
	public boolean testList(List<String> lst) {
		return set.contains(lst);
	}

}

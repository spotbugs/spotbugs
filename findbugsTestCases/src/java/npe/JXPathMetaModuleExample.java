package npe;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class JXPathMetaModuleExample {

	// A null pointer error very similar to this was reported in
	// http://issues.apache.org/bugzilla/show_bug.cgi?id=28646

	// TODO: generate a NPE warning for this bug
	public Object[] getCollectionAsArray(Collection c) {
		List values = null;
		Iterator i = c.iterator();
		if (i.hasNext()) {
			values = new LinkedList();
		}
		while (i.hasNext()) {
			values.add(i.next());
		}
		Object[] obj = values.toArray();
		return obj;
	}
}

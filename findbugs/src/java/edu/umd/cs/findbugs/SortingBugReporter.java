package edu.umd.cs.findbugs;

import java.util.*;

/**
 * A BugReporter which stores all of the reported bug instances,
 * and sorts them by class name before printing them.
 */
public class SortingBugReporter implements BugReporter {
	private TreeSet<BugInstance> bugInstanceSet = new TreeSet<BugInstance>(new Comparator<BugInstance>() {
		public int compare(BugInstance lhs, BugInstance rhs) {
			ClassAnnotation lca = lhs.getPrimaryClass();
			ClassAnnotation rca = rhs.getPrimaryClass();
			if (lca == null || rca == null)
				throw new IllegalStateException("null class annotation: " + lca + "," + rca);
			return lca.getClassName().compareTo(rca.getClassName());
		}
	});

	public void reportBug(BugInstance bugInstance) {
		if (!bugInstanceSet.contains(bugInstance)) {
			bugInstanceSet.add(bugInstance);
		}
	}

	public void finish() {
		Iterator i = bugInstanceSet.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = (BugInstance) i.next();
			System.out.println(bugInstance.getMessage());
		}
	}
}

// vim:ts=4

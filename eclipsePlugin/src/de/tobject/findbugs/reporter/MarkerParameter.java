package de.tobject.findbugs.reporter;

import de.tobject.findbugs.builder.WorkItem;
import edu.umd.cs.findbugs.BugInstance;

public class MarkerParameter {
	public final BugInstance bug;
	public final WorkItem resource;
	public final Integer primaryLine;
	public final Integer startLine;

	public MarkerParameter(BugInstance bug, WorkItem resource, int startLine, int primaryLine) {
		this.bug = bug;
		this.resource = resource;
		this.startLine = Integer.valueOf(startLine);
		this.primaryLine = Integer.valueOf(primaryLine);
	}
}

package de.tobject.findbugs.reporter;

import org.eclipse.core.resources.IResource;

import edu.umd.cs.findbugs.BugInstance;

public class MarkerParameter {
	public final BugInstance bug;
	public final IResource resource;
	public final Integer primaryLine;
	public final Integer startLine;

	public MarkerParameter(BugInstance bug, IResource resource, int startLine, int primaryLine) {
		this.bug = bug;
		this.resource = resource;
		this.startLine = Integer.valueOf(startLine);
		this.primaryLine = Integer.valueOf(primaryLine);
	}
}

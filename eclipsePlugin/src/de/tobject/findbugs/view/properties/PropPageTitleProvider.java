/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Andrei Loskutov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.tobject.findbugs.view.properties;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.viewers.IStructuredSelection;

import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.view.explorer.BugGroup;
import de.tobject.findbugs.view.explorer.BugLabelProvider;
import edu.umd.cs.findbugs.BugCategory;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;

public class PropPageTitleProvider extends BugLabelProvider {
	@Override
	public String getText(Object element) {
		if(element instanceof IStructuredSelection){
			IStructuredSelection selection = (IStructuredSelection) element;
			if(selection.size() > 1){
				return super.getText(element);
			}
			element = selection.getFirstElement();
		}
		String title = getTitle(element);
		if(title != null){
			return title;
		}
		return super.getText(element);
	}

	String getTitle(Object something){
		if(something instanceof BugGroup){
			return getTitle((BugGroup)something);
		}
		if(something instanceof IMarker){
			return getTitle((IMarker)something);
		}
		return null;
	}

	String getTitle(IMarker marker){
		return getTitle(MarkerUtil.findBugInstanceForMarker(marker));
	}

	String getTitle(BugGroup group){
		switch (group.getType()) {
		case Marker:
			return getTitle((IMarker)group.getData());
		case Pattern:
			return getTitle((BugPattern)group.getData());
		case PatternType:
			return getTitle((BugCode)group.getData());
		case Category:
			return getTitle((BugCategory)group.getData());
		case Priority:
			return getTitle((Integer)group.getData());
		case Package:
			return getTitle((IPackageFragment)group.getData());
		case Project:
			return getTitle((IProject)group.getData());
		case Class:
			return getTitle((IJavaElement)group.getData());

		default:
			break;
		}
		return null;
	}

	String getTitle(IJavaElement elem){
		if(elem == null){
			return null;
		}
		StringBuilder sb = new StringBuilder("Class: ");
		sb.append(elem.getElementName());
		return sb.toString();
	}

	String getTitle(IProject pack){
		if(pack == null){
			return null;
		}
		StringBuilder sb = new StringBuilder("Project: ");
		sb.append(pack.getName());
		return sb.toString();
	}

	String getTitle(IPackageFragment pack){
		if(pack == null){
			return null;
		}
		StringBuilder sb = new StringBuilder("Package: ");
		String name = pack.getElementName();
		if(name == null || name.length() == 0){
			sb.append("default package");
		} else {
			sb.append(name);
		}
		return sb.toString();
	}

	String getTitle(Integer priority){
		if(priority == null){
			return null;
		}
		StringBuilder sb = new StringBuilder("Priority: ");
		sb.append(FindBugsMarker.Priority.label(priority.intValue()).name());
		return sb.toString();
	}

	String getTitle(BugCategory category){
		if(category == null){
			return null;
		}
		StringBuilder sb = new StringBuilder("Category: ");
		sb.append(category.getShortDescription());
		sb.append(" (").append(category.getAbbrev()).append(", ");
		sb.append(category.getCategory()).append(")");
		return sb.toString();
	}

	String getTitle(BugCode type){
		if(type == null){
			return null;
		}
		StringBuilder sb = new StringBuilder("Pattern Type: ");
		sb.append(type.getDescription());
		sb.append(" (").append(type.getAbbrev()).append(")");
		return sb.toString();
	}

	String getTitle(BugPattern pattern){
		if(pattern == null){
			return null;
		}
		StringBuilder sb = new StringBuilder("Pattern: ");
		sb.append(pattern.getShortDescription());
		return sb.toString();
	}

	String getTitle(BugInstance bug){
		if(bug == null){
			return null;
		}
		StringBuilder sb = new StringBuilder("Bug: ");
		sb.append(bug.getAbridgedMessage());
		return sb.toString();
	}
	String getDetails(BugInstance bug){
		if(bug == null){
			return null;
		}
		StringBuilder sb = new StringBuilder();
		BugPattern pattern = bug.getBugPattern();
		sb.append(" (").append(pattern.getType());
		sb.append(", ").append(pattern.getAbbrev()).append(", ");
		sb.append(pattern.getCategory()).append(", ");
		sb.append(bug.getPriorityString());
		sb.append(")");
		return sb.toString();
	}

	String getDetails(BugPattern pattern){
		if(pattern == null){
			return null;
		}
		StringBuilder sb = new StringBuilder("<b>id:</b> ");
		sb.append(pattern.getType());
		sb.append(", <b>type:</b> ").append(pattern.getAbbrev()).append(", <b>category:</b> ");
		sb.append(pattern.getCategory());
		return sb.toString();
	}
}

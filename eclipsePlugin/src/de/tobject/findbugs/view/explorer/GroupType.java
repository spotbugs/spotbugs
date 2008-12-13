/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.view.explorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IWorkingSet;

import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;

/**
 * Type of the bug groups, shown inside the bug explorer
 *
 * @author Andrei
 */
public enum GroupType {

	Workspace(false, MarkerMapper.NO_MAPPING),

	WorkingSet(false, MarkerMapper.NO_MAPPING),

	Project(true, new MarkerMapper<IProject>() {
		@Override
		IProject getIdentifier(IMarker marker) {
			return marker.getResource().getProject();
		}
	}),

	Package(true, new MarkerMapper<IJavaElement>() {
		@Override
		IJavaElement getIdentifier(IMarker marker) {
			IJavaElement javaElement = JavaCore.create(marker.getResource());
			if (javaElement != null) {
				return javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			}
			return null;
		}
	}),

	Class(true, new MarkerMapper<IJavaElement>() {
		@Override
		IJavaElement getIdentifier(IMarker marker) {
			return JavaCore.create(marker.getResource());
		}
	}),

	Priority(true, new MarkerMapper<Integer>() {
		@Override
		Integer getIdentifier(IMarker marker) {
			BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
			return bug == null ? null : Integer.valueOf(bug.getPriority());
		}
	}),

	Category(true, new MarkerMapper<String>() {
		@Override
		String getIdentifier(IMarker marker) {
			BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
			return bug == null ? null : bug.getBugPattern().getCategory();
		}
	}),

	PatternType(true, new MarkerMapper<String>() {
		@Override
		String getIdentifier(IMarker marker) {
			BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
			return bug == null ? null : bug.getAbbrev();
		}
	}),

	Pattern(true, new MarkerMapper<BugPattern>() {
		@Override
		BugPattern getIdentifier(IMarker marker) {
			BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
			return bug == null ? null : bug.getBugPattern();
		}
	}),

	Marker(false, MarkerMapper.NO_MAPPING),

	Undefined(false, MarkerMapper.NO_MAPPING);

	private final boolean visible;
	private final MarkerMapper<?> mapper;

	private GroupType(boolean visible, MarkerMapper<?> mapper) {
		this.visible = visible;
		this.mapper = mapper;
		if(mapper != MarkerMapper.NO_MAPPING) {
			mapper.setType(this);
		}
	}

	public boolean isVisible() {
		return visible;
	}

	/**
	 * @return may return {@link MarkerMapper#NO_MAPPING}, if there is no mapping possible
	 *         for current group type
	 */
	MarkerMapper<?> getMapper() {
		return mapper;
	}

	public static List<GroupType> getVisible(){
		List<GroupType> visible = new ArrayList<GroupType>();
		GroupType[] values = values();
		for (GroupType type : values) {
			if(type.isVisible()){
				visible.add(type);
			}
		}
		return visible;
	}

	public static GroupType getType(Object element) {
		if (element instanceof BugGroup) {
			return ((BugGroup) element).getType();
		}
		if (element instanceof IMarker) {
			return GroupType.Marker;
		}
		if (element instanceof IProject) {
			return GroupType.Project;
		}
		if (element instanceof IWorkingSet) {
			return GroupType.WorkingSet;
		}
		if (element instanceof IWorkspaceRoot) {
			return GroupType.Workspace;
		}
		if (element instanceof IPackageFragment) {
			return GroupType.Package;
		}
		if (element instanceof IJavaElement) {
			return GroupType.Class;
		}
		return GroupType.Undefined;
	}


}

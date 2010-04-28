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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IWorkingSet;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugCategory;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.Priorities;

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
		@Override
		String getShortDescription(Object id) {
			return ((IProject)id).getName();
		}
	}),


	Package(true, new MarkerMapper<IPackageFragment>() {
		@Override
		IPackageFragment getIdentifier(IMarker marker) {
			IJavaElement javaElement = MarkerUtil.findJavaElementForMarker(marker);
			if(javaElement == null){
				javaElement = JavaCore.create(marker.getResource());
			}
			if (javaElement != null) {
				return (IPackageFragment) javaElement
						.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			}
			return null;
		}

		@Override
		String getShortDescription(Object id) {
			String name = ((IPackageFragment)id).getElementName();
			if(name == null || name.length() == 0){
				name = "default package";
			}
			return name;
		}
	}),

	Class(true, new MarkerMapper<IJavaElement>() {
		@Override
		IJavaElement getIdentifier(IMarker marker) {
			IJavaElement javaElement = MarkerUtil.findJavaElementForMarker(marker);
			if(javaElement != null){
				return javaElement;
			}
			return JavaCore.create(marker.getResource());
		}

		@Override
		String getShortDescription(Object id) {
			return ((IJavaElement)id).getElementName();
		}
	}),

	Priority(true, new MarkerMapper<Integer>() {
		@Override
		Integer getIdentifier(IMarker marker) {
			try {
				Object attribute = marker.getAttribute(IMarker.PRIORITY);
				if(attribute instanceof Integer){
					Integer prio = (Integer) attribute;
					switch (prio.intValue()) {
					case IMarker.PRIORITY_HIGH:
						return Integer.valueOf(Priorities.HIGH_PRIORITY);
					case IMarker.PRIORITY_NORMAL:
						return Integer.valueOf(Priorities.NORMAL_PRIORITY);
					default:
						return Integer.valueOf(Priorities.LOW_PRIORITY);
					}
				}
			} catch (CoreException e) {
				FindbugsPlugin.getDefault().logException(e, "Missing priority attribute in marker");
			}
			BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
			return bug == null ? null : Integer.valueOf(bug.getPriority());
		}

		@Override
		String getShortDescription(Object id) {
			return FindBugsMarker.Priority.label(((Integer)id).intValue()).name() + " priority";
		}
	}),

	Category(true, new MarkerMapper<BugCategory>() {
		@Override
		BugCategory getIdentifier(IMarker marker) {
			BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
			if(bug == null){
				return null;
			}
			return I18N.instance().getBugCategory(bug.getBugPattern().getCategory());
		}

		@Override
		String getShortDescription(Object id) {
			return ((BugCategory)id).getShortDescription();
		}
	}),

	PatternType(true, new MarkerMapper<BugCode>() {
		@Override
		BugCode getIdentifier(IMarker marker) {
			BugCode code = MarkerUtil.findBugCodeForMarker(marker);
			if(code == null){
				// can happen only if project was analysed with older plugin version then 1.3.8
				BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
				if(bug == null){
					return null;
				}
				return I18N.instance().getBugCode(bug.getAbbrev());
			}
			return code;
		}

		@Override
		String getShortDescription(Object id) {
			return ((BugCode)id).getDescription();
		}
	}),

	Pattern(true, new MarkerMapper<BugPattern>() {
		@Override
		BugPattern getIdentifier(IMarker marker) {
			BugPattern bug = MarkerUtil.findBugPatternForMarker(marker);
			return bug;
		}

		@Override
		String getShortDescription(Object id) {
			return ((BugPattern)id).getShortDescription();
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
		if (element instanceof BugCode) {
			return GroupType.PatternType;
		}
		if (element instanceof BugPattern) {
			return GroupType.Pattern;
		}
		if (element instanceof BugCategory) {
			return GroupType.Category;
		}
		if (element instanceof Integer) {
			return GroupType.Priority;
		}
		return GroupType.Undefined;
	}


}

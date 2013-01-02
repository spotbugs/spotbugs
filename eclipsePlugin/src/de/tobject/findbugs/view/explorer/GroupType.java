/*
 * Contributions to FindBugs
 * Copyright (C) 2012, Andrey Loskutov
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
import static de.tobject.findbugs.marker.FindBugsMarker.*;

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

import de.tobject.findbugs.marker.FindBugsMarker.MarkerConfidence;
import de.tobject.findbugs.marker.FindBugsMarker.MarkerRank;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugCategory;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Plugin;

/**
 * Type of the bug groups, shown inside the bug explorer
 *
 * @author Andrei
 */
public enum GroupType {

    Workspace(false, MarkerMapper.NO_MAPPING),

    WorkingSet(false, MarkerMapper.NO_MAPPING),

    DetectorPlugin(true, new MarkerMapper<Plugin>() {
        @Override
        Plugin getIdentifier(IMarker marker) {
            return MarkerUtil.findDetectorPluginFor(marker);
        }

        @Override
        String getShortDescription(Plugin id) {
            return id.getProvider();
        }

        @Override
        String getDebugDescription(IMarker marker) throws CoreException {
            return "plugin id: " + marker.getAttribute(DETECTOR_PLUGIN_ID);
        }
    }),

    Project(true, new MarkerMapper<IProject>() {
        @Override
        IProject getIdentifier(IMarker marker) {
            return marker.getResource().getProject();
        }

        @Override
        String getShortDescription(IProject id) {
            return id.getName();
        }

        @Override
        String getDebugDescription(IMarker marker) {
            return "project of resource: " + marker.getResource();
        }
    }),

    Package(true, new MarkerMapper<IPackageFragment>() {
        @Override
        IPackageFragment getIdentifier(IMarker marker) {
            IJavaElement javaElement = MarkerUtil.findJavaElementForMarker(marker);
            if (javaElement == null) {
                javaElement = JavaCore.create(marker.getResource());
            }
            if (javaElement != null) {
                return (IPackageFragment) javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
            }
            return null;
        }

        @Override
        String getShortDescription(IPackageFragment id) {
            String name = id.getElementName();
            if (name == null || name.length() == 0) {
                name = "default package";
            }
            return name;
        }

        @Override
        String getDebugDescription(IMarker marker) throws CoreException {
            return "package of element with unique Java id: " + marker.getAttribute(UNIQUE_JAVA_ID);
        }
    }),

    Class(true, new MarkerMapper<IJavaElement>() {
        @Override
        IJavaElement getIdentifier(IMarker marker) {
            IJavaElement javaElement = MarkerUtil.findJavaElementForMarker(marker);
            if (javaElement != null) {
                return javaElement;
            }
            return JavaCore.create(marker.getResource());
        }

        @Override
        String getShortDescription(IJavaElement id) {
            return id.getElementName();
        }

        @Override
        String getDebugDescription(IMarker marker) throws CoreException {
            return "class of element with unique Java id: " + marker.getAttribute(UNIQUE_JAVA_ID);
        }
    }),

    Confidence(true, new MarkerMapper<MarkerConfidence>() {
        @Override
        MarkerConfidence getIdentifier(IMarker marker) {
            return MarkerUtil.findConfidenceForMarker(marker);
        }

        @Override
        String getShortDescription(MarkerConfidence id) {
            return id.name() + " confidence";
        }

        @Override
        String getDebugDescription(IMarker marker) throws CoreException {
            return "confidence: " + marker.getAttribute(PRIO_AKA_CONFIDENCE);
        }
    }),

    BugRank(true, new MarkerMapper<MarkerRank>() {
        @Override
        MarkerRank getIdentifier(IMarker marker) {
            return MarkerRank.getRank(MarkerUtil.findBugRankForMarker(marker));
        }

        @Override
        String getShortDescription(MarkerRank id) {
            return id.toString();
        }

        @Override
        String getDebugDescription(IMarker marker) throws CoreException {
            return "rank: " + marker.getAttribute(RANK);
        }

    }),


    Category(true, new MarkerMapper<BugCategory>() {
        @Override
        BugCategory getIdentifier(IMarker marker) {
            BugPattern bugPattern = MarkerUtil.findBugPatternForMarker(marker);
            if(bugPattern == null){
                BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
                if (bug == null) {
                    return null;
                }
                return DetectorFactoryCollection.instance().getBugCategory(bug.getBugPattern().getCategory());
            }
            return DetectorFactoryCollection.instance().getBugCategory(bugPattern.getCategory());
        }

        @Override
        String getShortDescription(BugCategory id) {
            return id.getShortDescription();
        }

        @Override
        String getDebugDescription(IMarker marker) throws CoreException {
            return "category of: " + marker.getAttribute(UNIQUE_ID) + "/"
                    + marker.getAttribute(BUG_TYPE);
        }
    }),

    PatternType(true, new MarkerMapper<BugCode>() {
        @Override
        BugCode getIdentifier(IMarker marker) {
            BugCode code = MarkerUtil.findBugCodeForMarker(marker);
            if (code == null) {
                // can happen only if project was analysed with older plugin
                // version then 1.3.8
                BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
                if (bug == null) {
                    return null;
                }
                return DetectorFactoryCollection.instance().getBugCode(bug.getAbbrev());
            }
            return code;
        }

        @Override
        String getShortDescription(BugCode id) {
            return id.getDescription();
        }

        @Override
        String getDebugDescription(IMarker marker) throws CoreException {
            return "pattern type: " + marker.getAttribute(PATTERN_TYPE);
        }
    }),

    Pattern(true, new MarkerMapper<BugPattern>() {
        @Override
        BugPattern getIdentifier(IMarker marker) {
            return MarkerUtil.findBugPatternForMarker(marker);
        }

        @Override
        String getShortDescription(BugPattern id) {
            return id.getShortDescription();
        }

        @Override
        String getDebugDescription(IMarker marker) throws CoreException {
            return "pattern: " + marker.getAttribute(BUG_TYPE);
        }
    }),

    Marker(false, MarkerMapper.NO_MAPPING),

    Undefined(false, MarkerMapper.NO_MAPPING);

    private final boolean visible;

    private final MarkerMapper<?> mapper;

    private GroupType(boolean visible, MarkerMapper<?> mapper) {
        this.visible = visible;
        this.mapper = mapper;
        if (mapper != MarkerMapper.NO_MAPPING) {
            mapper.setType(this);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * @return may return {@link MarkerMapper#NO_MAPPING}, if there is no
     *         mapping possible for current group type
     */
    MarkerMapper<?> getMapper() {
        return mapper;
    }

    public static List<GroupType> getVisible() {
        List<GroupType> visible = new ArrayList<GroupType>();
        GroupType[] values = values();
        for (GroupType type : values) {
            if (type.isVisible()) {
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
        if (element instanceof MarkerConfidence) {
            return GroupType.Confidence;
        }
        if (element instanceof MarkerRank) {
            return GroupType.BugRank;
        }
        if (element instanceof String) {
            GroupType[] values = values();
            for (GroupType type : values) {
                if(type.toString().equals(element)) {
                    return type;
                }
            }
            // legacy name for compatibility if restoring from saved
            if("Priority".equals(element)) {
                return GroupType.Confidence;
            }
        }
        return GroupType.Undefined;
    }

}

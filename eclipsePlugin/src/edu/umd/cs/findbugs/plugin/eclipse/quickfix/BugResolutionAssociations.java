/*
 * Contributions to FindBugs
 * Copyright (C) 2006, Institut for Software
 * An Institut of the University of Applied Sciences Rapperswil
 *
 * Author: Thierry Wyss, Marco Busarello
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
package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ConditionCheck.checkForNull;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.IMarkerResolution;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * The <CODE>BugResolutionAssociations</CODE> is the container for the loaded
 * bug-resolutions. For each registred bug-type, at least one resolution-class
 * has to be specified. Also an instance of a bug-resolution can be associated
 * with a bug-type.
 *
 * @see BugResolutionAssociations#getBugResolutions(String)
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:g1zgragg@hsr.ch">Guido Zgraggen</a>
 */
public class BugResolutionAssociations {

    private final Map<String, Set<Class<? extends IMarkerResolution>>> resolutionClasses;

    protected BugResolutionAssociations(Map<String, Set<Class<? extends IMarkerResolution>>> resolutionClasses,
            Map<String, Set<IMarkerResolution>> resolutions) {
        super();
        if (resolutionClasses == null) {
            resolutionClasses = new Hashtable<String, Set<Class<? extends IMarkerResolution>>>();
        }
        this.resolutionClasses = resolutionClasses;
    }

    protected BugResolutionAssociations(Map<String, Set<Class<? extends IMarkerResolution>>> resolutionClasses) {
        this(resolutionClasses, null);
    }

    public BugResolutionAssociations() {
        this(null);
    }

    public boolean registerBugResolution(String bugType, Class<? extends IMarkerResolution> resolutionClass) {
        checkForNull(bugType, "bug type");
        Set<Class<? extends IMarkerResolution>> classes = new HashSet<Class<? extends IMarkerResolution>>();
        classes.add(resolutionClass);
        return registerBugResolutions(bugType, classes);
    }

    protected boolean registerBugResolutions(String bugType, Set<Class<? extends IMarkerResolution>> rclasses) {
        Assert.isNotNull(bugType);
        Assert.isNotNull(rclasses);
        if (rclasses.isEmpty()) {
            return false;
        }

        Set<Class<? extends IMarkerResolution>> classes = this.resolutionClasses.get(bugType);
        if (classes != null) {
            return classes.addAll(rclasses);
        }

        this.resolutionClasses.put(bugType, rclasses);
        return true;
    }

    protected boolean addBugResolutions(String bugType, Set<IMarkerResolution> resolutions) {
        Assert.isNotNull(bugType);
        Assert.isNotNull(resolutions);
        if (resolutions.isEmpty()) {
            return false;
        }

        Set<Class<? extends IMarkerResolution>> resolutionClazzes = new HashSet<Class<? extends IMarkerResolution>>();
        for (IMarkerResolution bugFix : resolutions) {
            resolutionClazzes.add(bugFix.getClass());
        }
        return registerBugResolutions(bugType, resolutionClazzes);
    }

    public IMarkerResolution[] getBugResolutions(String bugType) {
        Assert.isNotNull(bugType);
        return createBugResolutions(bugType);
    }

    public boolean containsBugResolution(String bugType) {
        Assert.isNotNull(bugType);
        return resolutionClasses.containsKey(bugType);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Entry<String, Set<Class<? extends IMarkerResolution>>> entry : resolutionClasses.entrySet()) {
            final String bugType = entry.getKey();
            sb.append(bugType);
            sb.append(" { ");
            for (Class<? extends IMarkerResolution> resolutionClass : entry.getValue()) {
                sb.append(resolutionClass.getName());
                sb.append(", ");
            }
            sb.replace(sb.length() - 2, sb.length(), " }\n");
        }
        // sb.trimToSize();
        return sb.toString();
    }

    private IMarkerResolution[] createBugResolutions(String bugType) {
        Assert.isNotNull(bugType);
        Set<Class<? extends IMarkerResolution>> classes = resolutionClasses.get(bugType);
        if (classes == null) {
            return new IMarkerResolution[0];
        }

        Set<IMarkerResolution> fixes = instantiateBugResolutions(classes);
        return fixes.toArray(new IMarkerResolution[fixes.size()]);
    }

    private static Set<IMarkerResolution> instantiateBugResolutions(Set<Class<? extends IMarkerResolution>> classes) {
        Assert.isNotNull(classes);
        Set<IMarkerResolution> fixes = new HashSet<IMarkerResolution>();
        for (Class<? extends IMarkerResolution> resolutionClass : classes) {
            IMarkerResolution fixer = instantiateBugResolution(resolutionClass);
            if (fixer != null) {
                fixes.add(fixer);
            }
        }
        return fixes;
    }

    @CheckForNull
    private static <F extends IMarkerResolution> F instantiateBugResolution(Class<F> resolutionClass) {
        try {
            return resolutionClass.newInstance();
        } catch (InstantiationException e) {
            FindbugsPlugin.getDefault().logException(e,
                    "Failed to instantiate bug-resolution '" + resolutionClass.getName() + "'.");
            return null;
        } catch (IllegalAccessException e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to create bug-resolution '" + resolutionClass.getName() + "'.");
            return null;
        }
    }

}

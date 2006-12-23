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
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.ui.IMarkerResolution;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * The <CODE>BugResolutionAssociations</CODE> is the container for the loaded
 * bug-resolutions. For each registred bug-type, at least one resolution-class
 * has to be specified. Also an instance of a bug-resolution can be associated
 * with a bug-type.
 * 
 * @see BugResolutionAssociations#getBugResolutions(String)
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 */
public class BugResolutionAssociations {

    private final Map<String, Set<Class<? extends IMarkerResolution>>> resolutionClasses;

    private final Map<String, Set<IMarkerResolution>> resolutions;

    // -------------------------------------------------------------------------

    protected BugResolutionAssociations(Map<String, Set<Class<? extends IMarkerResolution>>> resolutionClasses, Map<String, Set<IMarkerResolution>> resolutions) {
        super();
        if (resolutionClasses == null) {
            resolutionClasses = new Hashtable<String, Set<Class<? extends IMarkerResolution>>>();
        }
        if (resolutions == null) {
            resolutions = new Hashtable<String, Set<IMarkerResolution>>();
        }
        this.resolutionClasses = resolutionClasses;
        this.resolutions = resolutions;
    }

    protected BugResolutionAssociations(Map<String, Set<Class<? extends IMarkerResolution>>> resolutionClasses) {
        this(resolutionClasses, null);
    }

    public BugResolutionAssociations() {
        this(null);
    }

    // -------------------------------------------------------------------------

    public boolean registerBugResolution(String bugType, Class<? extends IMarkerResolution> resolutionClass) {
        return registerBugResolutions(bugType, resolutionClass);
    }

    public boolean deregisterBugResolution(String bugType, Class<? extends IMarkerResolution> resolutionClass) {
        return deregisterBugResolutions(bugType, resolutionClass);
    }

    public boolean registerBugResolutions(String bugType, Class<? extends IMarkerResolution>... resolutionClasses) {
        checkForNull(bugType, "bug type");
        if (resolutionClasses.length == 0) {
            return false;
        }

        Set<Class<? extends IMarkerResolution>> classes = new HashSet<Class<? extends IMarkerResolution>>();
        for (Class<? extends IMarkerResolution> resolutionClass : resolutionClasses) {
            classes.add(resolutionClass);
        }
        return registerBugResolutions(bugType, classes);
    }

    public boolean deregisterBugResolutions(String bugType, Class<? extends IMarkerResolution>... resolutionClasses) {
        checkForNull(bugType, "bug type");

        Set<Class<? extends IMarkerResolution>> classes;
        if (resolutionClasses.length > 0) {
            classes = new HashSet<Class<? extends IMarkerResolution>>();
            for (Class<? extends IMarkerResolution> resolutionClass : resolutionClasses) {
                classes.add(resolutionClass);
            }
        } else {
            classes = this.resolutionClasses.get(bugType);
        }
        return deregisterBugResolutions(bugType, classes);
    }

    protected boolean registerBugResolutions(String bugType, Set<Class<? extends IMarkerResolution>> resolutionClasses) {
        assert bugType != null;
        assert resolutionClasses != null;
        if (resolutionClasses.isEmpty()) {
            return false;
        }

        Set<Class<? extends IMarkerResolution>> classes = this.resolutionClasses.get(bugType);
        if (classes != null) {
            return classes.addAll(resolutionClasses);
        }

        this.resolutionClasses.put(bugType, resolutionClasses);
        return true;
    }

    protected boolean deregisterBugResolutions(String bugType, Set<Class<? extends IMarkerResolution>> resolutionClasses) {
        assert bugType != null;
        assert resolutionClasses != null;
        if (resolutionClasses.isEmpty()) {
            return false;
        }

        Set<Class<? extends IMarkerResolution>> classes = this.resolutionClasses.get(bugType);
        if (classes == null) {
            return false;
        }

        classes.removeAll(resolutionClasses);
        if (classes.isEmpty()) {
            this.resolutionClasses.remove(bugType);
        }
        return true;
    }

    // -------------------------------------------------------------------------

    public boolean addBugResolution(String bugType, IMarkerResolution bugFix) {
        return addBugResolutions(bugType, bugFix);
    }

    public boolean addBugResolutions(String bugType, IMarkerResolution... resolutions) {
        assert bugType != null;
        if (resolutions.length == 0) {
            return false;
        }

        Set<IMarkerResolution> fixes = new HashSet<IMarkerResolution>();
        for (IMarkerResolution bugFix : resolutions) {
            fixes.add(bugFix);
        }
        return addBugResolutions(bugType, fixes);
    }

    protected boolean addBugResolutions(String bugType, Set<IMarkerResolution> resolutions) {
        assert bugType != null;
        assert resolutions != null;
        if (resolutions.isEmpty()) {
            return false;
        }

        Set<Class<? extends IMarkerResolution>> resolutionClasses = new HashSet<Class<? extends IMarkerResolution>>();
        for (IMarkerResolution bugFix : resolutions) {
            resolutionClasses.add(bugFix.getClass());
        }
        registerBugResolutions(bugType, resolutionClasses);

        Set<IMarkerResolution> fixes = this.resolutions.get(bugType);
        if (fixes != null) {
            return fixes.addAll(resolutions);
        }

        this.resolutions.put(bugType, resolutions);
        return true;
    }

    // -------------------------------------------------------------------------

    public IMarkerResolution[] getBugResolutions(String bugType) {
        assert bugType != null;
        Set<? extends IMarkerResolution> resolutionSet = resolutions.get(bugType);
        if (resolutionSet != null) {
            return resolutionSet.toArray(new IMarkerResolution[resolutionSet.size()]);
        }
        return createBugResolutions(bugType);
    }

    // -------------------------------------------------------------------------

    public boolean containsBugResolution(String bugType) {
        assert bugType != null;
        return resolutions.containsKey(bugType) || resolutionClasses.containsKey(bugType);
    }

    // -------------------------------------------------------------------------

    public boolean removeBugResolution(String bugType, IMarkerResolution bugFix) {
        return removeBugResolutions(bugType, bugFix);
    }

    public boolean removeBugResolutions(String bugType, IMarkerResolution... resolutions) {
        assert bugType != null;

        Set<IMarkerResolution> resolutionSet;
        if (resolutions.length > 0) {
            resolutionSet = new HashSet<IMarkerResolution>();
            for (IMarkerResolution resolution : resolutions) {
                resolutionSet.add(resolution);
            }
        } else {
            resolutionSet = this.resolutions.get(bugType);
        }
        return removeBugResolutions(bugType, resolutionSet);
    }

    protected boolean removeBugResolutions(String bugType, Set<IMarkerResolution> resolutions) {
        assert bugType != null;
        assert resolutions != null;
        if (resolutions.isEmpty()) {
            return false;
        }

        Set<Class<? extends IMarkerResolution>> resolutionClasses = new HashSet<Class<? extends IMarkerResolution>>();
        for (IMarkerResolution resolution : resolutions) {
            resolutionClasses.add(resolution.getClass());
        }

        Set<IMarkerResolution> resolutionSet = this.resolutions.get(bugType);
        if (resolutionSet == null) {
            return false;
        }

        resolutionSet.removeAll(resolutions);
        if (resolutionSet.isEmpty()) {
            this.resolutions.remove(bugType);
        }

        return deregisterBugResolutions(bugType, resolutionClasses);
    }

    // -------------------------------------------------------------------------

    public void clear() {
        resolutionClasses.clear();
        resolutions.clear();
    }

    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------

    private IMarkerResolution[] createBugResolutions(String bugType) {
        assert bugType != null;
        Set<Class<? extends IMarkerResolution>> classes = resolutionClasses.get(bugType);
        if (classes == null) {
            return new IMarkerResolution[0];
        }

        Set<IMarkerResolution> fixes = instantiateBugResolutions(classes);
        resolutions.put(bugType, fixes);
        return fixes.toArray(new IMarkerResolution[fixes.size()]);
    }

    private Set<IMarkerResolution> instantiateBugResolutions(Set<Class<? extends IMarkerResolution>> classes) {
        assert classes != null;
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
    private <F extends IMarkerResolution> F instantiateBugResolution(Class<F> resolutionClass) {
        try {
            return resolutionClass.newInstance();
        } catch (InstantiationException e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to instantiate bug-resolution '" + resolutionClass.getName() + "'.");
            return null;
        } catch (IllegalAccessException e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to create bug-resolution '" + resolutionClass.getName() + "'.");
            return null;
        }
    }

}

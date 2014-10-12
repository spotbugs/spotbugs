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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.IMarkerResolution;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * The <CODE>BugResolutionAssociations</CODE> is the container for the loaded
 * bug-resolutions. For each registred bug pattern, at least one resolution-class
 * has to be specified. Also an instance of a bug resolution can be associated
 * with a bug pattern.
 *
 * @see BugResolutionAssociations#getBugResolutions(String)
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:g1zgragg@hsr.ch">Guido Zgraggen</a>
 */
public class BugResolutionAssociations {

    private final Map<String, List<QuickFixContribution>> quickFixes;

    protected BugResolutionAssociations(Map<String, List<QuickFixContribution>> quickFixes) {
        super();
        this.quickFixes = quickFixes;
    }

    public boolean containsBugResolution(String bugType) {
        Assert.isNotNull(bugType);
        return quickFixes.containsKey(bugType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<Entry<String, List<QuickFixContribution>>> set = quickFixes.entrySet();
        for (Entry<String, List<QuickFixContribution>> entry : set) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
        }
        if(sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }

    public IMarkerResolution[] createBugResolutions(String bugType, IMarker marker) {
        Assert.isNotNull(bugType);
        Assert.isNotNull(marker);
        List<QuickFixContribution> classes = quickFixes.get(bugType);
        if (classes == null) {
            return new IMarkerResolution[0];
        }

        Set<BugResolution> fixes = instantiateBugResolutions(classes);
        for (Iterator<BugResolution> iterator = fixes.iterator(); iterator.hasNext();) {
            BugResolution fix = iterator.next();
            if (fix.isApplicable(marker)) {
                fix.setMarker(marker);
            } else {
                iterator.remove();
            }
        }
        return fixes.toArray(new IMarkerResolution[fixes.size()]);
    }

    private static Set<BugResolution> instantiateBugResolutions(List<QuickFixContribution> quicks) {
        Assert.isNotNull(quicks);
        Set<BugResolution> fixes = new HashSet<>();
        for (QuickFixContribution qf : quicks) {
            BugResolution fixer = instantiateBugResolution(qf);
            if (fixer != null) {
                fixes.add(fixer);
            }
        }
        return fixes;
    }

    @CheckForNull
    private static BugResolution instantiateBugResolution(QuickFixContribution qf) {
        try {
            BugResolution br = qf.producer.call();
            br.setLabel(qf.label);
            br.setBugPattern(qf.pattern);
            br.setOptions(qf.args);
            return br;
        } catch (Throwable e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to create bug-resolution '" + qf + "'.");
            return null;
        }
    }

}

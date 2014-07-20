/*
 * Contributions to FindBugs
 * Copyright (C) 2014, Andrey Loskutov
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * Helper class to read contributions for the "detectorPlugins" extension point
 */
public class QuickFixesExtensionHelper {

    private static final String EXTENSION_POINT_ID = FindbugsPlugin.PLUGIN_ID + ".findbugsQuickFixes";

    private static final String ARGUMENTS = "arguments";
    private static final String PATTERN = "pattern";
    private static final String LABEL = "label";
    private static final String CLASS_FQN = "class";

    /** key is the pattern id, value is the corresponding contribution */
    private static Map<String, List<QuickFixContribution>> contributedQuickFixes;

    /** key is the pattern id, the value is the corresponding contribution */
    public static synchronized Map<String, List<QuickFixContribution>> getContributedQuickFixes() {
        if (contributedQuickFixes != null) {
            return contributedQuickFixes;
        }
        HashMap<String, List<QuickFixContribution>> set = new HashMap<>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXTENSION_POINT_ID);
        if (point == null) {
            return Collections.EMPTY_MAP;
        }
        IExtension[] extensions = point.getExtensions();
        for (IExtension extension : extensions) {
            IConfigurationElement[] elements = extension.getConfigurationElements();
            for (IConfigurationElement configElt : elements) {
                addContribution(set, configElt);
            }
        }
        Set<Entry<String, List<QuickFixContribution>>> entrySet = set.entrySet();
        for (Entry<String, List<QuickFixContribution>> entry : entrySet) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        contributedQuickFixes = Collections.unmodifiableMap(set);
        return contributedQuickFixes;
    }

    private static void addContribution(Map<String, List<QuickFixContribution>> set, final IConfigurationElement configElt) {
        IContributor contributor = null;
        try {
            contributor = configElt.getContributor();
            if (contributor == null) {
                throw new IllegalArgumentException("Null contributor");
            }
            String clazzFqn = configElt.getAttribute(CLASS_FQN);
            if (isEmpty(clazzFqn)) {
                throw new IllegalArgumentException("Missing '" + CLASS_FQN + "' attribute");
            }
            String label = configElt.getAttribute(LABEL);
            if (isEmpty(label)) {
                throw new IllegalArgumentException("Missing '" + LABEL + "' attribute");
            }
            String pattern = configElt.getAttribute(PATTERN);
            if (isEmpty(pattern)) {
                throw new IllegalArgumentException("Missing '" + PATTERN + "' attribute");
            }


            String arg = configElt.getAttribute(ARGUMENTS);
            Set<String> args;
            if (arg == null) {
                args = Collections.EMPTY_SET;
            } else {
                String[] strings = arg.split(",\\s*");
                args = new HashSet<>();
                for (String string : strings) {
                    args.add(string);
                }
            }
            QuickFixContribution qf = createQuickFix(configElt, clazzFqn, label, pattern, args);
            List<QuickFixContribution> list = set.get(pattern);
            if(list == null) {
                list = new ArrayList<>();
                set.put(pattern, list);
            }
            if(list.contains(qf)) {
                throw new IllegalArgumentException("Duplicated quick fix contribution for pattern '"
                        + pattern + "': " + qf  + ".");
            }
            list.add(qf);
        } catch (Throwable e) {
            String cName = contributor != null ? contributor.getName() : "unknown contributor";
            String message = "Failed to read contribution for '" + EXTENSION_POINT_ID
                    + "' extension point from " + cName;
            FindbugsPlugin.getDefault().logException(e, message);
        }
    }

    private static QuickFixContribution createQuickFix(final IConfigurationElement configElt, String clazzFqn, String label,
            String pattern, Set<String> args) {
        return new QuickFixContribution(clazzFqn, label, pattern, args, new Callable<BugResolution>() {
            @Override
            public BugResolution call() throws Exception {
                return (BugResolution) configElt.createExecutableExtension(CLASS_FQN);
            }
        });
    }


    static boolean isEmpty(String s){
        return s == null || s.isEmpty();
    }


}

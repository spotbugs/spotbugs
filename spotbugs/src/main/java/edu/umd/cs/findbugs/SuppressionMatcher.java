/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class SuppressionMatcher implements Matcher {
    private final Map<ClassAnnotation, Collection<WarningSuppressor>> suppressedWarnings = new HashMap<>();

    private final Map<String, Collection<WarningSuppressor>> suppressedPackageWarnings = new HashMap<>();

    private final Set<WarningSuppressor> matched = new HashSet<>();

    int count = 0;

    public void addPackageSuppressor(PackageWarningSuppressor suppressor) {
        String packageName = suppressor.getPackageName();

        Collection<WarningSuppressor> c = suppressedPackageWarnings.computeIfAbsent(packageName,
                k -> new LinkedList<>());
        c.add(suppressor);
    }

    public void addSuppressor(ClassWarningSuppressor suppressor) {
        ClassAnnotation clazz = suppressor.getClassAnnotation().getTopLevelClass();
        Collection<WarningSuppressor> c = suppressedWarnings.computeIfAbsent(clazz, k -> new LinkedList<>());
        c.add(suppressor);
    }

    public int count() {
        return count;
    }

    @Override
    public boolean match(BugInstance b) {
        ClassAnnotation primaryClazz = b.getPrimaryClass();
        if (primaryClazz != null) {
            ClassAnnotation clazz = b.getPrimaryClass().getTopLevelClass();
            Collection<WarningSuppressor> c = suppressedWarnings.get(clazz);
            if (c != null) {
                for (WarningSuppressor w : c) {
                    if (w.match(b)) {
                        count++;
                        matched.add(w);
                        return true;
                    }
                }
            }
        }
        for (Collection<WarningSuppressor> c2 : suppressedPackageWarnings.values()) {
            for (WarningSuppressor w : c2) {
                if (w.match(b)) {
                    count++;
                    matched.add(w);
                    return true;
                }
            }
        }
        return false;
    }

    public void validateSuppressionUsage(BugReporter bugReporter) {
        Stream.concat(suppressedWarnings.values().stream(), suppressedPackageWarnings.values().stream())
                .flatMap(Collection::stream)
                .filter(w -> !matched.contains(w))
                .map(WarningSuppressor::buildUselessSuppressionBugInstance)
                .forEach(bugReporter::reportBug);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        // no-op; these aren't saved to XML
    }

}

/*
 * Generate HTML file containing bug descriptions
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.tools.html;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;

public abstract class PrintBugDescriptions {
    public void print() throws IOException {
        // Ensure bug patterns are loaded
        DetectorFactoryCollection factories = DetectorFactoryCollection.instance();

        // Find all bug patterns reported by at least one non-disabled detector.
        Collection<BugPattern> enabledPatternSet = new HashSet<BugPattern>();
        for (Iterator<DetectorFactory> i = factories.factoryIterator(); i.hasNext();) {
            DetectorFactory factory = i.next();
            if (isEnabled(factory)) {
                enabledPatternSet.addAll(factory.getReportedBugPatterns());
            }
        }

        prologue();

        Iterator<BugPattern> i = DetectorFactoryCollection.instance().bugPatternIterator();
        while (i.hasNext()) {
            BugPattern bugPattern = i.next();
            if (!enabledPatternSet.contains(bugPattern)) {
                continue;
            }
            emit(bugPattern);
        }

        epilogue();
    }

    protected boolean isEnabled(DetectorFactory factory) {
        return factory.isDefaultEnabled();
    }

    protected abstract void prologue() throws IOException;

    protected abstract void emit(BugPattern bugPattern) throws IOException;

    protected abstract void epilogue() throws IOException;
}


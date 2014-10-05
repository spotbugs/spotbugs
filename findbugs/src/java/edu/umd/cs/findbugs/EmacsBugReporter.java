/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2006 University of Maryland
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import javax.annotation.CheckForNull;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * BugReporter to output warnings in Emacs format.
 *
 * @author David Li
 */
public class EmacsBugReporter extends TextUIBugReporter {

    private final HashSet<BugInstance> seenAlready = new HashSet<BugInstance>();

    private final HashMap<String, String> sourceFileNameCache = new HashMap<String, String>();

    @Override
    public void observeClass(ClassDescriptor classDescriptor) {
        try {
            JavaClass javaClass = AnalysisContext.currentAnalysisContext().lookupClass(classDescriptor.toDottedClassName());
            String sourceFileName = fileNameFor(javaClass.getPackageName(), javaClass.getSourceFileName());
            sourceFileNameCache.put(javaClass.getClassName(), sourceFileName);
        } catch (ClassNotFoundException e) {
            // Ignore - should not happen
        }
    }

    private String fileNameFor(final String packageName, final String sourceName) {
        String result;
        SourceFinder sourceFinder = AnalysisContext.currentAnalysisContext().getSourceFinder();
        try {
            result = sourceFinder.findSourceFile(packageName, sourceName).getFullFileName();
        } catch (IOException e) {
            result = packageName.replace('.', File.separatorChar) + File.separatorChar + sourceName;
        }
        return result;
    }

    @Override
    protected void printBug(BugInstance bugInstance) {
        int lineStart = 0;
        int lineEnd = 0;
        String fullPath = "???";

        SourceLineAnnotation line = bugInstance.getPrimarySourceLineAnnotation();

        lineStart = line.getStartLine();
        lineEnd = line.getEndLine();
        SourceFinder sourceFinder = AnalysisContext.currentAnalysisContext().getSourceFinder();
        String pkgName = line.getPackageName();
        try {
            fullPath = sourceFinder.findSourceFile(pkgName, line.getSourceFile()).getFullFileName();
        } catch (IOException e) {
            if ("".equals(pkgName)) {
                fullPath = line.getSourceFile();
            } else {
                fullPath = pkgName.replace('.', '/') + "/" + line.getSourceFile();
            }
        }
        outputStream.print(fullPath + ":" + lineStart + ":" + lineEnd + " " + bugInstance.getMessage());

        switch (bugInstance.getPriority()) {
        case Priorities.EXP_PRIORITY:
            outputStream.print(" (E) ");
            break;
        case Priorities.LOW_PRIORITY:
            outputStream.print(" (L) ");
            break;
        case Priorities.NORMAL_PRIORITY:
            outputStream.print(" (M) ");
            break;
        case Priorities.HIGH_PRIORITY:
            outputStream.print(" (H) ");
            break;
        default:
            throw new IllegalArgumentException("Invalid priority " + bugInstance.getPriority() + " for " + bugInstance);
        }

        outputStream.println();
    }

    @Override
    protected void doReportBug(BugInstance bugInstance) {
        if (seenAlready.add(bugInstance)) {
            printBug(bugInstance);
            notifyObservers(bugInstance);
        }
    }

    @Override
    public void finish() {
        outputStream.close();
    }

    @Override
    public @CheckForNull
    BugCollection getBugCollection() {
        return null;
    }

}

/*
 * Local Variables: eval: (c-set-style "bsd") End:
 */

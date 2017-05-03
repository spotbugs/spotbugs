/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, Mike Fagan <mfagan@tde.com>
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
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * Class to store package bug statistics.
 *
 * @author Mike Fagan
 * @author Jay Dunning
 */

class BugCounts {
    protected int[] nBugs;

    @OverridingMethodsMustInvokeSuper
    public void addError(BugInstance bug) {
        ensureNonnullBugCounts();
        ++nBugs[bug.getPriority()];
        ++nBugs[0];
    }

    protected void ensureNonnullBugCounts() {
        if (nBugs == null) {
            nBugs = new int[] { 0, 0, 0, 0, 0 };
        }

    }
    public final int getTotalBugs() {
        if (nBugs == null) {
            return 0;
        }
        return nBugs[0];
    }

    public final int getBugsAtPriority(int p) {
        if (nBugs == null) {
            return 0;
        }
        return nBugs[p];
    }

    public void clearBugCounts() {
        nBugs = null;

    }

    /**
     * Add priority attributes to a started tag. Each priority at offset n,
     * where n &gt; 0, is output using attribute priority_n if the value at
     * offset n is greater than zero.
     *
     * @param xmlOutput
     *            an output stream for which startTag has been called but
     *            stopTag has not.
     */
    public  void writeBugPriorities(XMLOutput xmlOutput) throws IOException {
        if (nBugs == null) {
            return;
        }
        writeBugPriorities(xmlOutput, nBugs);
    }
    public static void writeBugPriorities(XMLOutput xmlOutput, @Nonnull int nBugs[]) throws IOException {
        int i = nBugs.length;
        while (--i > 0) {
            if (nBugs[i] > 0) {
                xmlOutput.addAttribute("priority_" + i, String.valueOf(nBugs[i]));
            }
        }
    }
}

public class PackageStats extends BugCounts implements XMLWriteable {


    public static class ClassStats extends BugCounts implements XMLWriteable, Cloneable {
        private final String name;

        private final String sourceFile;

        private boolean isInterface;

        private int size;

        public ClassStats(String name, String sourceFile) {
            this.name = name;
            this.sourceFile = sourceFile;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                // can't happen
                throw new AssertionError(e);
            }
        }

        public void setInterface(boolean isInterface) {
            this.isInterface = isInterface;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int size() {
            return size;
        }

        public String getName() {
            return name;
        }

        public @CheckForNull
        String getSourceFile() {
            return sourceFile;
        }

        @Override
        public void writeXML(XMLOutput xmlOutput) throws IOException {
            if (size == 0) {
                return;
            }
            xmlOutput.startTag("ClassStats");

            xmlOutput.addAttribute("class", name);
            if (sourceFile != null) {
                xmlOutput.addAttribute("sourceFile", sourceFile);
            }
            xmlOutput.addAttribute("interface", String.valueOf(isInterface));
            xmlOutput.addAttribute("size", String.valueOf(size));
            xmlOutput.addAttribute("bugs", String.valueOf(getTotalBugs()));
            writeBugPriorities(xmlOutput);

            xmlOutput.stopTag(true);
        }

        /**
         *
         */

    }

    public static final String ELEMENT_NAME = "PackageStats";

    public static final int ALL_ERRORS = 0;

    private final String packageName;
    private int size;

    private int numClasses;

    @Override
    public String toString() {
        return String.format("%s, %d classes, %d ncss", packageName, numClasses, size);
    }

    // list of errors for this package
    // private LinkedList<BugInstance> packageErrors = new
    // LinkedList<BugInstance>();

    // all classes and interfaces in this package
    private final Map<String, ClassStats> packageMembers = new HashMap<String, ClassStats>(5);

    public PackageStats(String packageName) {
        this.packageName = packageName;
    }

    public PackageStats(String packageName, int numClasses, int size) {
        this(packageName);
        this.numClasses = numClasses;
        this.size = size;
    }

    public Collection<ClassStats> getClassStats() {
        return packageMembers.values();
    }

    public int size() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }


    private ClassStats getClassStats(String name, String sourceFile) {
        ClassStats result = packageMembers.get(name);
        if (result == null) {
            result = new ClassStats(name, sourceFile);
            packageMembers.put(name, result);
            numClasses = packageMembers.size();
        }

        return result;
    }

    public @CheckForNull ClassStats getClassStatsOrNull(String name) {
        ClassStats result = packageMembers.get(name);
        return result;
    }

    @Override
    public void addError(BugInstance bug) {
        super.addError(bug);
        SourceLineAnnotation source = bug.getPrimarySourceLineAnnotation();
        // see bug https://sourceforge.net/tracker/index.php?func=detail&aid=3322583&group_id=96405&atid=614693
        // always add class stats to see useful details in package stats fancy.xsl output
        getClassStats(source.getClassName(), source.getSourceFile()).addError(bug);
    }

    public void addClass(String name, String sourceFile, boolean isInterface, int size) {
        addClass(name, sourceFile, isInterface, size, true);
    }

    public void addClass(String name, String sourceFile, boolean isInterface, int size, boolean updatePackageStats) {
        ClassStats classStats = getClassStats(name, sourceFile);
        classStats.setInterface(isInterface);
        classStats.setSize(size);
        addClass(classStats, updatePackageStats);
    }

    public void addClass(ClassStats classStats) {
        addClass(classStats, true);
    }

    public void addClass(ClassStats classStats, boolean updatePackageStats) {
        if (packageMembers.isEmpty()) {
            this.size = 0;
            this.numClasses = 0;
        }
        packageMembers.put(classStats.getName(), classStats);
        if (updatePackageStats) {
            size += classStats.size();
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public int getNumClasses() {
        return numClasses;
    }

    public void setNumClasses(int numClasses) {
        this.numClasses = numClasses;
    }

    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        if (size == 0) {
            return;
        }

        xmlOutput.startTag(ELEMENT_NAME);

        xmlOutput.addAttribute("package", packageName);
        xmlOutput.addAttribute("total_bugs", String.valueOf(getTotalBugs()));
        int numClasses = packageMembers.size();
        if (numClasses == 0) {
            numClasses = this.numClasses;
        }
        xmlOutput.addAttribute("total_types", String.valueOf(numClasses));
        xmlOutput.addAttribute("total_size", String.valueOf(size));
        writeBugPriorities(xmlOutput);

        xmlOutput.stopTag(false);

        for (ClassStats classStats : getSortedClassStats()) {
            classStats.writeXML(xmlOutput);
        }

        xmlOutput.closeTag(ELEMENT_NAME);
    }

    public Collection<ClassStats> getSortedClassStats() {
        SortedMap<String, ClassStats> sorted = new TreeMap<String, ClassStats>(packageMembers);
        return sorted.values();

    }



    public void recomputeFromClassStats() {
        super.clearBugCounts();
        size = 0;
        numClasses = packageMembers.size();
        ensureNonnullBugCounts();
        for (ClassStats classStats : packageMembers.values()) {
            for (int i = 0; i < nBugs.length; i++) {
                nBugs[i] += classStats.getBugsAtPriority(i);
            }
            size += classStats.size;
        }
    }

    /**
     *
     */
    @Override
    public void clearBugCounts() {
        super.clearBugCounts();

        for (ClassStats classStats : packageMembers.values()) {
            classStats.clearBugCounts();
        }

    }

    /**
     * @param classPattern
     */
    public void purgeClassesThatDontMatch(Pattern classPattern) {
        for (Iterator<Map.Entry<String, ClassStats>> i = packageMembers.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, ClassStats> e = i.next();
            if (!classPattern.matcher(e.getKey()).find()) {
                i.remove();
            }
        }
    }
}


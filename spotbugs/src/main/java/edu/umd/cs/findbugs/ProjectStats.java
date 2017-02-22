/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, Mike Fagan <mfagan@tde.com>
 * Copyright (C) 2003-2008 University of Maryland
 * Copyright (C) 2008 Google
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.WillClose;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import edu.umd.cs.findbugs.PackageStats.ClassStats;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.workflow.FileBugHash;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * Statistics resulting from analyzing a project.
 */
public class ProjectStats implements XMLWriteable, Cloneable {
    private static final String TIMESTAMP_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

    private static final boolean OMIT_PACKAGE_STATS = SystemProperties.getBoolean("findbugs.packagestats.omit");

    private final SortedMap<String, PackageStats> packageStatsMap;

    private final int[] totalErrors = new int[] { 0, 0, 0, 0, 0 };

    private int totalClasses;

    private int referencedClasses;

    private int totalSize;

    private int totalSizeFromPackageStats;

    private int totalClassesFromPackageStats;

    private Date analysisTimestamp;

    private boolean hasClassStats;

    private boolean hasPackageStats;

    private final Footprint baseFootprint;

    private final String java_version = SystemProperties.getProperty("java.version");
    private String java_vm_version = SystemProperties.getProperty("java.vm.version");

    private final Profiler profiler;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getNumClasses()).append(" classes: ");
        for (PackageStats pStats : getPackageStats()) {
            for (ClassStats cStats : pStats.getSortedClassStats()) {
                buf.append(cStats.getName()).append(" ");
            }
        }
        return buf.toString();
    }

    /**
     * Constructor. Creates an empty object.
     */
    public ProjectStats() {
        this.packageStatsMap = new TreeMap<String, PackageStats>();
        this.totalClasses = 0;
        this.analysisTimestamp = new Date();
        this.baseFootprint = new Footprint();
        this.profiler = new Profiler();
    }

    public boolean hasClassStats() {
        return hasClassStats;
    }

    public boolean hasPackageStats() {
        return hasPackageStats;
    }

    @Override
    public ProjectStats clone() {
        try {
            return (ProjectStats) super.clone();
        } catch (CloneNotSupportedException e) {
            // can't happen
            throw new AssertionError(e);
        }
    }

    public int getCodeSize() {
        if (totalSizeFromPackageStats > 0) {
            return totalSizeFromPackageStats;
        }
        return totalSize;

    }

    public int getTotalBugs() {
        return totalErrors[0];
    }

    public int getBugsOfPriority(int priority) {
        return totalErrors[priority];
    }

    /**
     * Set the timestamp for this analysis run.
     *
     * @param timestamp
     *            the time of the analysis run this ProjectStats represents, as
     *            previously reported by writeXML.
     */
    public void setTimestamp(String timestamp) throws ParseException {
        this.analysisTimestamp = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.ENGLISH).parse(timestamp);
    }

    public void setTimestamp(long timestamp) {
        this.analysisTimestamp = new Date(timestamp);
    }

    public void setVMVersion(String vm_version) {
        this.java_vm_version = vm_version;
    }

    /**
     * Get the number of classes analyzed.
     */
    public int getNumClasses() {
        if (totalClassesFromPackageStats > 0) {
            return totalClassesFromPackageStats;
        }
        return totalClasses;
    }

    /**
     * @return Returns the baseFootprint.
     */
    public Footprint getBaseFootprint() {
        return baseFootprint;
    }

    /**
     * Report that a class has been analyzed.
     *
     * @param className
     *            the full name of the class
     * @param sourceFile
     *            TODO
     * @param isInterface
     *            true if the class is an interface
     * @param size
     *            a normalized class size value; see
     *            detect/FindBugsSummaryStats.
     */
    public void addClass(@DottedClassName String className, @CheckForNull String sourceFile, boolean isInterface, int size) {
        addClass(className, sourceFile, isInterface, size, true);
    }

    /**
     * Report that a class has been analyzed.
     *
     * @param className
     *            the full name of the class
     * @param sourceFile
     *            TODO
     * @param isInterface
     *            true if the class is an interface
     * @param size
     *            a normalized class size value; see
     *            detect/FindBugsSummaryStats.
     * @param updatePackageStats TODO
     */
    public void addClass(@DottedClassName String className, @CheckForNull String sourceFile, boolean isInterface, int size, boolean updatePackageStats) {
        if(!hasClassStats) {
            // totalClasses/totalSize might be set from FindBugsSummary before when parsing XML: reset them
            totalClasses = 0;
            totalSize = 0;
        }
        hasClassStats = true;
        String packageName;
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 0) {
            packageName = "";
        } else {
            packageName = className.substring(0, lastDot);
        }
        PackageStats stat = getPackageStats(packageName);
        stat.addClass(className, sourceFile, isInterface, size, updatePackageStats);
        totalClasses++;
        totalSize += size;
        totalClassesFromPackageStats = 0;
        totalSizeFromPackageStats = 0;
    }

    /**
     * Report that a class has been analyzed.
     *
     * @param className
     *            the full name of the class
     */
    public @CheckForNull
    ClassStats getClassStats(@DottedClassName String className) {
        if (hasClassStats) {
            return null;
        }
        String packageName;
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 0) {
            packageName = "";
        } else {
            packageName = className.substring(0, lastDot);
        }
        PackageStats stat = getPackageStats(packageName);
        return stat.getClassStatsOrNull(className);
    }

    /**
     * Called when a bug is reported.
     */
    public void addBug(BugInstance bug) {

        SourceLineAnnotation source = bug.getPrimarySourceLineAnnotation();
        PackageStats stat = getPackageStats(source.getPackageName());
        stat.addError(bug);
        ++totalErrors[0];
        int priority = bug.getPriority();
        if (priority >= 1) {
            ++totalErrors[Math.min(priority, totalErrors.length - 1)];
        }
    }

    /**
     * Clear bug counts
     */
    public void clearBugCounts() {
        for (int i = 0; i < totalErrors.length; i++) {
            totalErrors[i] = 0;
        }
        for (PackageStats stats : packageStatsMap.values()) {
            stats.clearBugCounts();
        }
    }

    public void purgeClassesThatDontMatch(Pattern classPattern) {
        if (hasClassStats) {
            for (Iterator<Map.Entry<String, PackageStats>> i = packageStatsMap.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, PackageStats> e = i.next();
                PackageStats stats = e.getValue();
                stats.purgeClassesThatDontMatch(classPattern);
                if (stats.getClassStats().isEmpty()) {
                    i.remove();
                }
            }
        } else if (hasPackageStats) {
            boolean matchAny = false;
            for (String packageName : packageStatsMap.keySet()) {
                Matcher m = classPattern.matcher(packageName);
                if (m.lookingAt()) {
                    matchAny = true;
                    break;
                }
            }
            if (matchAny) {
                for (Iterator<String> i = packageStatsMap.keySet().iterator(); i.hasNext();) {
                    String packageName = i.next();
                    Matcher m = classPattern.matcher(packageName);
                    if (!m.lookingAt()) {
                        i.remove();
                    }

                }
            }
        }
    }

    public void purgeClassStats() {
        hasClassStats = false;
        if (totalClassesFromPackageStats == 0) {
            totalClassesFromPackageStats = totalClasses;
        }
        if (totalSizeFromPackageStats == 0) {
            totalSizeFromPackageStats = totalSize;
        }

        for (PackageStats ps : getPackageStats()) {
            ps.getClassStats().clear();
        }
    }

    public void purgePackageStats() {
        hasPackageStats = false;
        if (totalClassesFromPackageStats == 0) {
            totalClassesFromPackageStats = totalClasses;
        }
        if (totalSizeFromPackageStats == 0) {
            totalSizeFromPackageStats = totalSize;
        }

        getPackageStats().clear();
    }

    public void recomputeFromComponents() {
        if (!hasClassStats && !hasPackageStats) {
            return;
        }
        for (int i = 0; i < totalErrors.length; i++) {
            totalErrors[i] = 0;
        }
        totalSize = 0;
        totalClasses = 0;
        totalSizeFromPackageStats = 0;
        totalClassesFromPackageStats = 0;

        for (PackageStats stats : packageStatsMap.values()) {
            if (hasClassStats) {
                stats.recomputeFromClassStats();
            }
            totalSize += stats.size();
            totalClasses += stats.getNumClasses();
            for (int i = 0; i < totalErrors.length; i++) {
                totalErrors[i] += stats.getBugsAtPriority(i);
            }
        }
    }

    FileBugHash fileBugHashes;

    public void computeFileStats(BugCollection bugs) {
        if (bugs.getProjectStats() != this) {
            throw new IllegalArgumentException("Collection doesn't own stats");
        }
        fileBugHashes = FileBugHash.compute(bugs);
    }

    /**
     * Output as XML.
     */
    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        writeXML(xmlOutput, true);
    }

    /**
     * Output as XML.
     */
    public void writeXML(XMLOutput xmlOutput, boolean withMessages) throws IOException {
        xmlOutput.startTag("FindBugsSummary");

        xmlOutput.addAttribute("timestamp", new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.ENGLISH).format(analysisTimestamp));
        xmlOutput.addAttribute("total_classes", String.valueOf(getNumClasses()));
        xmlOutput.addAttribute("referenced_classes", String.valueOf(referencedClasses));

        xmlOutput.addAttribute("total_bugs", String.valueOf(totalErrors[0]));
        xmlOutput.addAttribute("total_size", String.valueOf(getCodeSize()));
        xmlOutput.addAttribute("num_packages", String.valueOf(packageStatsMap.size()));

        if (java_version != null) {
            xmlOutput.addAttribute("java_version", java_version);
        }
        if (java_vm_version != null) {
            xmlOutput.addAttribute("vm_version", java_vm_version);
        }
        Footprint delta = new Footprint(baseFootprint);
        NumberFormat twoPlaces = NumberFormat.getInstance(Locale.ENGLISH);
        twoPlaces.setMinimumFractionDigits(2);
        twoPlaces.setMaximumFractionDigits(2);
        twoPlaces.setGroupingUsed(false);
        long cpuTime = delta.getCpuTime(); // nanoseconds
        if (cpuTime >= 0) {
            xmlOutput.addAttribute("cpu_seconds", twoPlaces.format(cpuTime / 1000000000.0));
        }
        long clockTime = delta.getClockTime(); // milliseconds
        if (clockTime >= 0) {
            xmlOutput.addAttribute("clock_seconds", twoPlaces.format(clockTime / 1000.0));
        }
        long peakMemory = delta.getPeakMemory(); // bytes
        if (peakMemory >= 0) {
            xmlOutput.addAttribute("peak_mbytes", twoPlaces.format(peakMemory / (1024.0 * 1024)));
        }
        xmlOutput.addAttribute("alloc_mbytes", twoPlaces.format(Runtime.getRuntime().maxMemory() / (1024.0 * 1024)));
        long gcTime = delta.getCollectionTime(); // milliseconds
        if (gcTime >= 0) {
            xmlOutput.addAttribute("gc_seconds", twoPlaces.format(gcTime / 1000.0));
        }

        BugCounts.writeBugPriorities(xmlOutput, totalErrors);

        xmlOutput.stopTag(false);

        if (withMessages && fileBugHashes != null) {
            for (String sourceFile : new TreeSet<String>(fileBugHashes.getSourceFiles())) {
                xmlOutput.startTag("FileStats");
                xmlOutput.addAttribute("path", sourceFile);
                xmlOutput.addAttribute("bugCount", String.valueOf(fileBugHashes.getBugCount(sourceFile)));
                xmlOutput.addAttribute("size", String.valueOf(fileBugHashes.getSize(sourceFile)));

                String hash = fileBugHashes.getHash(sourceFile);
                if (hash != null) {
                    xmlOutput.addAttribute("bugHash", hash);
                }
                xmlOutput.stopTag(true);

            }
        }

        if (!OMIT_PACKAGE_STATS) {
            for (PackageStats stats : packageStatsMap.values()) {
                stats.writeXML(xmlOutput);
            }
        }

        getProfiler().writeXML(xmlOutput);
        xmlOutput.closeTag("FindBugsSummary");
    }

    public Map<String, String> getFileHashes(BugCollection bugs) {
        if (bugs.getProjectStats() != this) {
            throw new IllegalArgumentException("Collection doesn't own stats");
        }

        if (fileBugHashes == null) {
            computeFileStats(bugs);
        }

        HashMap<String, String> result = new HashMap<String, String>();
        for (String sourceFile : fileBugHashes.getSourceFiles()) {
            result.put(sourceFile, fileBugHashes.getHash(sourceFile));
        }
        return result;

    }

    /**
     * Report statistics as an XML document to given output stream.
     */
    public void reportSummary(@WillClose OutputStream out) throws IOException {
        XMLOutput xmlOutput = new OutputStreamXMLOutput(out);
        try {
            writeXML(xmlOutput);
        } finally {
            xmlOutput.finish();
        }
    }

    /**
     * Transform summary information to HTML.
     *
     * @param htmlWriter
     *            the Writer to write the HTML output to
     */
    public void transformSummaryToHTML(Writer htmlWriter) throws IOException, TransformerException {

        ByteArrayOutputStream summaryOut = new ByteArrayOutputStream(8096);
        reportSummary(summaryOut);

        StreamSource in = new StreamSource(new ByteArrayInputStream(summaryOut.toByteArray()));
        StreamResult out = new StreamResult(htmlWriter);
        InputStream xslInputStream = this.getClass().getClassLoader().getResourceAsStream("summary.xsl");
        if (xslInputStream == null) {
            throw new IOException("Could not load summary stylesheet");
        }
        StreamSource xsl = new StreamSource(xslInputStream);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer(xsl);
        transformer.transform(in, out);

        Reader rdr = in.getReader();
        if (rdr != null) {
            rdr.close();
        }
        htmlWriter.close();
        InputStream is = xsl.getInputStream();
        if (is != null) {
            is.close();
        }
    }

    public Collection<PackageStats> getPackageStats() {
        return packageStatsMap.values();
    }

    private PackageStats getPackageStats(String packageName) {
        PackageStats stat = packageStatsMap.get(packageName);
        if (stat == null) {
            stat = new PackageStats(packageName);
            packageStatsMap.put(packageName, stat);
        }
        return stat;
    }

    public void putPackageStats(String packageName, int numClasses, int size) {
        hasPackageStats = true;
        PackageStats stat = packageStatsMap.get(packageName);
        if (stat == null) {
            stat = new PackageStats(packageName, numClasses, size);
            totalSizeFromPackageStats += size;
            totalClassesFromPackageStats += numClasses;
            packageStatsMap.put(packageName, stat);

        } else {
            totalSizeFromPackageStats += size - stat.size();
            totalClassesFromPackageStats += numClasses - stat.getNumClasses();

            stat.setNumClasses(numClasses);
            stat.setSize(size);
        }
    }

    /**
     * @param stats2
     */
    public void addStats(ProjectStats stats2) {
        if (totalSize == totalSizeFromPackageStats) {
            totalSizeFromPackageStats += stats2.getCodeSize();
        }
        totalSize += stats2.getCodeSize();
        if (totalClasses == totalClassesFromPackageStats) {
            totalClassesFromPackageStats += stats2.getNumClasses();
        }
        totalClasses += stats2.getNumClasses();
        for (int i = 0; i < totalErrors.length; i++) {
            totalErrors[i] += stats2.totalErrors[i];
        }

        if (stats2.hasPackageStats) {
            hasPackageStats = true;
        }
        if (stats2.hasClassStats) {
            hasClassStats = true;
        }

        for (Map.Entry<String, PackageStats> entry : stats2.packageStatsMap.entrySet()) {
            String key = entry.getKey();
            PackageStats pkgStats2 = entry.getValue();
            if (packageStatsMap.containsKey(key)) {
                PackageStats pkgStats = packageStatsMap.get(key);
                for (ClassStats classStats : pkgStats2.getClassStats()) {
                    pkgStats.addClass(classStats, true);
                }
            } else {
                packageStatsMap.put(key, pkgStats2);
            }
        }
    }

    /**
     * @param size
     */
    public void setReferencedClasses(int size) {
        this.referencedClasses = size;
    }

    public int getReferencedClasses() {
        return this.referencedClasses;
    }

    /**
     * @return Returns the project profiler instance, never null
     */
    public Profiler getProfiler() {
        return profiler;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }
}

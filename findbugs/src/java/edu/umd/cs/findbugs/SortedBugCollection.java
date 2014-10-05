/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;
import javax.xml.transform.TransformerException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.CloudFactory;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.model.ClassFeatureSet;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.xml.Dom4JXMLOutput;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutputUtil;

/**
 * An implementation of {@link BugCollection} that keeps the BugInstances sorted
 * by class (using the native comparison ordering of BugInstance's compareTo()
 * method as a tie-breaker).
 *
 * @see BugInstance
 * @author David Hovemeyer
 */
public class SortedBugCollection implements BugCollection {

    private static final Logger LOGGER = Logger.getLogger(SortedBugCollection.class.getName());

    private static final boolean REPORT_SUMMARY_HTML = SystemProperties.getBoolean("findbugs.report.SummaryHTML");

    long analysisTimestamp = System.currentTimeMillis();

    String analysisVersion = Version.RELEASE;

    boolean earlyStats = SystemProperties.getBoolean("findbugs.report.summaryFirst");

    boolean bugsPopulated = false;

    private boolean withMessages = false;

    private boolean minimalXML = false;

    private boolean applySuppressions = false;

    private @CheckForNull Cloud cloud;

    boolean shouldNotUsePlugin;

    long timeStartedLoading, timeFinishedLoading;

    String dataSource = "";

    private Map<String, String> xmlCloudDetails = Collections.emptyMap();

    private final Comparator<BugInstance> comparator;

    private final TreeSet<BugInstance> bugSet;

    private final LinkedHashSet<AnalysisError> errorList;

    private final TreeSet<String> missingClassSet;

    @CheckForNull
    private String summaryHTML;

    final Project project;

    private final ProjectStats projectStats;

    private final Map<String, ClassFeatureSet> classFeatureSetMap;

    private final List<AppVersion> appVersionList;

    private boolean preciseHashOccurrenceNumbersAvailable = false;

    /**
     * Sequence number of the most-recently analyzed version of the code.
     */
    private long sequence;

    /**
     * Release name of the analyzed application.
     */
    private String releaseName;

    /**
     * Current timestamp for the code being analyzed
     */
    private long timestamp;


    public long getTimeStartedLoading() {
        return timeStartedLoading;
    }

    public long getTimeFinishedLoading() {
        return timeFinishedLoading;
    }

    public String getDataSource() {
        return dataSource;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public @CheckForNull Cloud getCloudLazily() {
        if (cloud != null && bugsPopulated) {
            cloud.bugsPopulated();
        }
        return cloud;
    }

    @Override
    public @Nonnull Cloud getCloud() {
        if (shouldNotUsePlugin) {
            return CloudFactory.getPlainCloud(this);
        }
        Cloud result = cloud;
        if (result == null) {
            IGuiCallback callback = getProject().getGuiCallback();
            result = cloud = CloudFactory.createCloudWithoutInitializing(this);
            try {
                CloudFactory.initializeCloud(this, result);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not load cloud plugin "+ result.getCloudName(), e);
                callback.showMessageDialog("Unable to connect to " + result.getCloudName() + ": " + Util.getNetworkErrorMessage(e));
                if (CloudFactory.FAIL_ON_CLOUD_ERROR) {
                    throw new IllegalStateException("Could not load FindBugs Cloud plugin - to avoid this message, " +
                            "set -D" + CloudFactory.FAIL_ON_CLOUD_ERROR_PROP + "=false", e);
                }
                result = cloud = CloudFactory.getPlainCloud(this);
            }
            callback.registerCloud(getProject(), this, result);


        }
        if (!result.isInitialized()) {
            LOGGER.log(Level.SEVERE, "Cloud " + result.getCloudName() + " is not initialized ");
        }
        if (bugsPopulated) {
            result.bugsPopulated();
        }
        return result;
    }

    @Override
    public boolean isApplySuppressions() {
        return applySuppressions;
    }

    @Override
    public void setApplySuppressions(boolean applySuppressions) {
        this.applySuppressions = applySuppressions;
    }

    @Override
    public long getAnalysisTimestamp() {
        return analysisTimestamp;
    }

    @Override
    public void setAnalysisTimestamp(long timestamp) {
        analysisTimestamp = timestamp;
    }

    /**
     * Add a Collection of BugInstances to this BugCollection object. This just
     * calls add(BugInstance) for each instance in the input collection.
     *
     * @param collection
     *            the Collection of BugInstances to add
     */
    public void addAll(Collection<BugInstance> collection) {
        for (BugInstance bug : collection) {
            add(bug);
        }
    }

    /**
     * Add a Collection of BugInstances to this BugCollection object.
     *
     * @param collection
     *            the Collection of BugInstances to add
     * @param updateActiveTime
     *            true if active time of added BugInstances should be updated to
     *            match collection: false if not
     */
    public void addAll(Collection<BugInstance> collection, boolean updateActiveTime) {
        for (BugInstance warning : collection) {
            add(warning, updateActiveTime);
        }
    }

    /**
     * Add a BugInstance to this BugCollection. This just calls add(bugInstance,
     * true).
     *
     * @param bugInstance
     *            the BugInstance
     * @return true if the BugInstance was added, or false if a matching
     *         BugInstance was already in the BugCollection
     */
    @Override
    public boolean add(BugInstance bugInstance) {
        return add(bugInstance,
                bugInstance.getFirstVersion() == 0L && bugInstance.getLastVersion() == 0L);
    }

    /**
     * Add an analysis error.
     *
     * @param message
     *            the error message
     */
    @Override
    public void addError(String message) {
        addError(message, null);
    }

    /**
     * Get the current AppVersion.
     */
    @Override
    public AppVersion getCurrentAppVersion() {
        return new AppVersion(getSequenceNumber()).setReleaseName(getReleaseName()).setTimestamp(getTimestamp())
                .setNumClasses(getProjectStats().getNumClasses()).setCodeSize(getProjectStats().getCodeSize());
    }

    /**
     * Read XML data from given file into this object, populating given Project
     * as a side effect.
     *
     * @param fileName
     *            name of the file to read
     */
    @Override
    public void readXML(String fileName) throws IOException, DocumentException {
        readXML(new File(fileName));
    }

    /**
     * Read XML data from given file into this object, populating given Project
     * as a side effect.
     *
     * @param file
     *            the file
     */
    public void readXML(File file) throws IOException, DocumentException {
        project.setCurrentWorkingDirectory(file.getParentFile());
        dataSource = file.getAbsolutePath();
        InputStream in = progessMonitoredInputStream(file, "Loading analysis");
        try {
            readXML(in, file);
        } catch (IOException e) {
            throw newIOException(file, e);
        } catch (DocumentException e) {
            throw new DocumentException("Failing reading " + file, e);
        }
    }

    private static IOException newIOException(Object file, IOException e) {
        IOException result = new IOException("Failing reading " + file);
        result.initCause(e);
        return result;
    }

    public void readXML(URL u) throws IOException, DocumentException {
        InputStream in = progessMonitoredInputStream(u.openConnection(), "Loading analysis");
        dataSource = u.toString();
        try {
            readXML(in);
        } catch (IOException e) {
            throw newIOException(u, e);
        } catch (DocumentException e) {
            throw new DocumentException("Failing reading " + u, e);
        }
    }

    /**
     * Read XML data from given input stream into this object, populating the
     * Project as a side effect. An attempt will be made to close the input
     * stream (even if an exception is thrown).
     *
     * @param in
     *            the InputStream
     */
    public void readXML(@WillClose InputStream in, File base) throws IOException, DocumentException {
        try {
            doReadXML(in, base);
        } finally {
            in.close();
        }
    }

    @Override
    public void readXML(@WillClose InputStream in) throws IOException, DocumentException {
        assert project != null;
        assert in != null;
        doReadXML(in, null);
    }

    @Override
    public void readXML(@WillClose Reader reader) throws IOException, DocumentException {
        assert project != null;
        assert reader != null;
        doReadXML(reader, null);
    }

    private void doReadXML(@WillClose InputStream in, @CheckForNull File base) throws IOException, DocumentException {
        try {
            checkInputStream(in);
            Reader reader = Util.getReader(in);
            doReadXML(reader, base);
        } catch (RuntimeException e) {
            in.close();
            throw e;
        } catch (IOException e) {
            in.close();
            throw e;
        }
    }

    private void doReadXML(@WillClose Reader reader, @CheckForNull File base) throws IOException, DocumentException {
        timeStartedLoading = System.currentTimeMillis();

        SAXBugCollectionHandler handler = new SAXBugCollectionHandler(this, base);
        Profiler profiler = getProjectStats().getProfiler();
        profiler.start(handler.getClass());
        try {

            XMLReader xr;
            try {
                xr = XMLReaderFactory.createXMLReader();
            } catch (SAXException e) {
                AnalysisContext.logError("Couldn't create XMLReaderFactory", e);
                throw new DocumentException("Sax error ", e);
            }
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);

            xr.parse(new InputSource(reader));
        } catch (SAXParseException e) {
            if (base != null) {
                throw new DocumentException("Parse error at line " + e.getLineNumber() + " : " + e.getColumnNumber() + " of "
                        + base, e);
            }
            throw new DocumentException("Parse error at line " + e.getLineNumber() + " : " + e.getColumnNumber(), e);
        } catch (SAXException e) {
            // FIXME: throw SAXException from method?
            if (base != null) {
                throw new DocumentException("Sax error while parsing " + base, e);
            }
            throw new DocumentException("Sax error ", e);
        } finally {
            Util.closeSilently(reader);
            profiler.end(handler.getClass());
        }
        timeFinishedLoading = System.currentTimeMillis();
        bugsPopulated();
        // Presumably, project is now up-to-date
        project.setModified(false);
    }


    @Override
    public void writeXML(OutputStream out) throws IOException {
        writeXML(UTF8.writer(out));
    }

    /**
     * Write this BugCollection to a file as XML.
     *
     * @param fileName
     *            the file to write to
     */
    @Override
    public void writeXML(String fileName) throws IOException {
        OutputStream out = new FileOutputStream(fileName);
        if (fileName.endsWith(".gz")) {
            out = new GZIPOutputStream(out);
        }
        writeXML(out);
    }

    /**
     * Write this BugCollection to a file as XML.
     *
     * @param file
     *            the file to write to
     */
    public void writeXML(File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        if (file.getName().endsWith(".gz")) {
            out = new GZIPOutputStream(out);
        }
        writeXML(out);
    }

    /**
     * Convert the BugCollection into a dom4j Document object.
     *
     * @return the Document representing the BugCollection as a dom4j tree
     */
    @Override
    public Document toDocument() {
        // if (project == null) throw new NullPointerException("No project");
        assert project != null;

        DocumentFactory docFactory = new DocumentFactory();
        Document document = docFactory.createDocument();
        Dom4JXMLOutput treeBuilder = new Dom4JXMLOutput(document);

        try {
            writeXML(treeBuilder);
        } catch (IOException e) {
            // Can't happen
        }
        return document;
    }

    /**
     * Write the BugCollection to given output stream as XML. The output stream
     * will be closed, even if an exception is thrown.
     *
     * @param out
     *            the OutputStream to write to
     */
    @Override
    public void writeXML(@WillClose Writer out) throws IOException {
        assert project != null;
        bugsPopulated();
        XMLOutput xmlOutput;
        // if (project == null) throw new NullPointerException("No project");


        if (withMessages && cloud != null) {
            cloud.bugsPopulated();
            cloud.initiateCommunication();
            cloud.waitUntilIssueDataDownloaded();
            String token = SystemProperties.getProperty("findbugs.cloud.token");
            if (token != null && token.trim().length() > 0) {
                LOGGER.info("Cloud token specified - uploading new issues, if necessary...");
                cloud.waitUntilNewIssuesUploaded();
            }
            xmlOutput = new OutputStreamXMLOutput(out, "http://findbugs.sourceforge.net/xsl/default.xsl");
        } else {
            xmlOutput = new OutputStreamXMLOutput(out);
        }

        writeXML(xmlOutput);
    }

    @Override
    public void writePrologue(XMLOutput xmlOutput) throws IOException {
        xmlOutput.beginDocument();
        xmlOutput.openTag(
                ROOT_ELEMENT_NAME,
                new XMLAttributeList().addAttribute("version", analysisVersion)
                .addAttribute("sequence", String.valueOf(getSequenceNumber()))
                .addAttribute("timestamp", String.valueOf(getTimestamp()))
                .addAttribute("analysisTimestamp", String.valueOf(getAnalysisTimestamp()))
                .addAttribute("release", getReleaseName()));
        project.writeXML(xmlOutput, null, this);
    }

    public void computeBugHashes() {
        if (preciseHashOccurrenceNumbersAvailable) {
            return;
        }
        invalidateHashes();
        HashMap<String, Integer> seen = new HashMap<String, Integer>();

        for (BugInstance bugInstance : getCollection()) {
            String hash = bugInstance.getInstanceHash();

            Integer count = seen.get(hash);
            if (count == null) {
                bugInstance.setInstanceOccurrenceNum(0);
                seen.put(hash, 0);
            } else {
                bugInstance.setInstanceOccurrenceNum(count + 1);
                seen.put(hash, count + 1);
            }
        }
        for (BugInstance bugInstance : getCollection()) {
            bugInstance.setInstanceOccurrenceMax(seen.get(bugInstance.getInstanceHash()));
        }
        preciseHashOccurrenceNumbersAvailable = true;
    }

    /**
     * Write the BugCollection to an XMLOutput object. The finish() method of
     * the XMLOutput object is guaranteed to be called.
     *
     * <p>
     * To write the SummaryHTML element, set property
     * findbugs.report.SummaryHTML to "true".
     * </p>
     *
     * @param xmlOutput
     *            the XMLOutput object
     */
    @Override
    public void writeXML(@WillClose XMLOutput xmlOutput) throws IOException {
        assert project != null;
        try {
            writePrologue(xmlOutput);
            if (withMessages) {
                computeBugHashes();
                getProjectStats().computeFileStats(this);
                String commonBase = null;
                for (String s : project.getSourceDirList()) {
                    if (commonBase == null) {
                        commonBase = s;
                    } else {
                        commonBase = commonBase.substring(0, commonPrefix(commonBase, s));
                    }

                }
                if (commonBase != null && commonBase.length() > 0) {
                    if (commonBase.indexOf("/./") > 0) {
                        commonBase = commonBase.substring(0, commonBase.indexOf("/."));
                    }
                    File base = new File(commonBase);
                    if (base.exists() && base.isDirectory() && base.canRead()) {
                        SourceLineAnnotation.generateRelativeSource(base, project);
                    }
                }
            }
            if (earlyStats && !minimalXML) {
                getProjectStats().writeXML(xmlOutput, withMessages);
            }

            // Write BugInstances
            for (BugInstance bugInstance : getCollection()) {
                if (!applySuppressions || !project.getSuppressionFilter().match(bugInstance)) {
                    bugInstance.writeXML(xmlOutput, this, withMessages);
                }
            }

            writeEpilogue(xmlOutput);

        } finally {
            xmlOutput.finish();
            SourceLineAnnotation.clearGenerateRelativeSource();
        }
    }

    int commonPrefix(String s1, String s2) {
        int pos = 0;
        while (pos < s1.length() && pos < s2.length() && s1.charAt(pos) == s2.charAt(pos)) {
            pos++;
        }
        return pos;
    }

    @Override
    public void writeEpilogue(XMLOutput xmlOutput) throws IOException {
        if (withMessages) {
            writeBugCategories(xmlOutput);
            writeBugPatterns(xmlOutput);
            writeBugCodes(xmlOutput);
        }
        // Errors, missing classes
        if (!minimalXML) {
            emitErrors(xmlOutput);
        }

        if (!earlyStats && !minimalXML) {
            // Statistics
            getProjectStats().writeXML(xmlOutput, withMessages);
        }

        // // Class and method hashes
        // xmlOutput.openTag(CLASS_HASHES_ELEMENT_NAME);
        // for (Iterator<ClassHash> i = classHashIterator(); i.hasNext();) {
        // ClassHash classHash = i.next();
        // classHash.writeXML(xmlOutput);
        // }
        // xmlOutput.closeTag(CLASS_HASHES_ELEMENT_NAME);

        // Class features
        xmlOutput.openTag("ClassFeatures");
        for (Iterator<ClassFeatureSet> i = classFeatureSetIterator(); i.hasNext();) {
            ClassFeatureSet classFeatureSet = i.next();
            classFeatureSet.writeXML(xmlOutput);
        }
        xmlOutput.closeTag("ClassFeatures");

        // AppVersions
        xmlOutput.openTag(HISTORY_ELEMENT_NAME);
        for (Iterator<AppVersion> i = appVersionIterator(); i.hasNext();) {
            AppVersion appVersion = i.next();
            appVersion.writeXML(xmlOutput);
        }
        xmlOutput.closeTag(HISTORY_ELEMENT_NAME);

        // Summary HTML
        if (REPORT_SUMMARY_HTML) {
            String html = getSummaryHTML();
            if (html != null && !"".equals(html)) {
                xmlOutput.openTag(SUMMARY_HTML_ELEMENT_NAME);
                xmlOutput.writeCDATA(html);
                xmlOutput.closeTag(SUMMARY_HTML_ELEMENT_NAME);
            }
        }
        xmlOutput.closeTag(ROOT_ELEMENT_NAME);
    }

    private void writeBugPatterns(XMLOutput xmlOutput) throws IOException {
        // Find bug types reported
        Set<String> bugTypeSet = new HashSet<String>();
        for (Iterator<BugInstance> i = iterator(); i.hasNext();) {
            BugInstance bugInstance = i.next();
            BugPattern bugPattern = bugInstance.getBugPattern();
            bugTypeSet.add(bugPattern.getType());
        }
        // Emit element describing each reported bug pattern
        for (String bugType : bugTypeSet) {
            BugPattern bugPattern = DetectorFactoryCollection.instance().lookupBugPattern(bugType);
            if (bugPattern == null) {
                continue;
            }

            XMLAttributeList attributeList = new XMLAttributeList();
            attributeList.addAttribute("type", bugType);
            attributeList.addAttribute("abbrev", bugPattern.getAbbrev());
            attributeList.addAttribute("category", bugPattern.getCategory());
            if (bugPattern.getCWEid() != 0) {
                attributeList.addAttribute("cweid", Integer.toString(bugPattern.getCWEid()));
            }
            xmlOutput.openTag("BugPattern", attributeList);

            xmlOutput.openTag("ShortDescription");
            xmlOutput.writeText(bugPattern.getShortDescription());
            xmlOutput.closeTag("ShortDescription");

            xmlOutput.openTag("Details");
            xmlOutput.writeCDATA(bugPattern.getDetailText());
            xmlOutput.closeTag("Details");

            xmlOutput.closeTag("BugPattern");
        }
    }

    private void writeBugCodes(XMLOutput xmlOutput) throws IOException {
        // Find bug codes reported
        Set<String> bugCodeSet = new HashSet<String>();
        for (Iterator<BugInstance> i = iterator(); i.hasNext();) {
            BugInstance bugInstance = i.next();
            String bugCode = bugInstance.getAbbrev();
            if (bugCode != null) {
                bugCodeSet.add(bugCode);
            }
        }
        // Emit element describing each reported bug code
        for (String bugCodeAbbrev : bugCodeSet) {
            BugCode bugCode = DetectorFactoryCollection.instance().getBugCode(bugCodeAbbrev);
            String bugCodeDescription = bugCode.getDescription();
            if (bugCodeDescription == null) {
                continue;
            }

            XMLAttributeList attributeList = new XMLAttributeList();
            attributeList.addAttribute("abbrev", bugCodeAbbrev);
            if (bugCode.getCWEid() != 0) {
                attributeList.addAttribute("cweid", Integer.toString(bugCode.getCWEid()));
            }
            xmlOutput.openTag("BugCode", attributeList);

            xmlOutput.openTag("Description");
            xmlOutput.writeText(bugCodeDescription);
            xmlOutput.closeTag("Description");

            xmlOutput.closeTag("BugCode");
        }
    }

    private void writeBugCategories(XMLOutput xmlOutput) throws IOException {
        // Find bug categories reported
        Set<String> bugCatSet = new HashSet<String>();
        for (Iterator<BugInstance> i = iterator(); i.hasNext();) {
            BugInstance bugInstance = i.next();
            BugPattern bugPattern = bugInstance.getBugPattern();
            bugCatSet.add(bugPattern.getCategory());
        }
        // Emit element describing each reported bug code
        for (String bugCat : bugCatSet) {
            String bugCatDescription = I18N.instance().getBugCategoryDescription(bugCat);
            if (bugCatDescription == null) {
                continue;
            }

            XMLAttributeList attributeList = new XMLAttributeList();
            attributeList.addAttribute("category", bugCat);

            xmlOutput.openTag("BugCategory", attributeList);

            xmlOutput.openTag("Description");
            xmlOutput.writeText(bugCatDescription);
            xmlOutput.closeTag("Description");

            xmlOutput.closeTag("BugCategory");
        }
    }

    private void emitErrors(XMLOutput xmlOutput) throws IOException {
        // System.err.println("Writing errors to XML output");
        XMLAttributeList attributeList = new XMLAttributeList();
        attributeList.addAttribute("errors", Integer.toString(errorList.size()));
        attributeList.addAttribute("missingClasses", Integer.toString(missingClassSet.size()));
        xmlOutput.openTag(ERRORS_ELEMENT_NAME, attributeList);

        // Emit Error elements describing analysis errors
        for (AnalysisError error : getErrors()) {
            xmlOutput.openTag(ERROR_ELEMENT_NAME);

            xmlOutput.openTag(ERROR_MESSAGE_ELEMENT_NAME);
            xmlOutput.writeText(error.getMessage());
            xmlOutput.closeTag(ERROR_MESSAGE_ELEMENT_NAME);

            if (error.getExceptionMessage() != null) {
                xmlOutput.openTag(ERROR_EXCEPTION_ELEMENT_NAME);
                xmlOutput.writeText(error.getExceptionMessage());
                xmlOutput.closeTag(ERROR_EXCEPTION_ELEMENT_NAME);

                String stackTrace[] = error.getStackTrace();
                if (stackTrace != null) {
                    for (String aStackTrace : stackTrace) {
                        xmlOutput.openTag(ERROR_STACK_TRACE_ELEMENT_NAME);
                        xmlOutput.writeText(aStackTrace);
                        xmlOutput.closeTag(ERROR_STACK_TRACE_ELEMENT_NAME);
                    }
                }

                //                if (false && error.getNestedExceptionMessage() != null) {
                //                    xmlOutput.openTag(ERROR_EXCEPTION_ELEMENT_NAME);
                //                    xmlOutput.writeText(error.getNestedExceptionMessage());
                //                    xmlOutput.closeTag(ERROR_EXCEPTION_ELEMENT_NAME);
                //
                //                    stackTrace = error.getNestedStackTrace();
                //                    if (stackTrace != null) {
                //                        for (String aStackTrace : stackTrace) {
                //                            xmlOutput.openTag(ERROR_STACK_TRACE_ELEMENT_NAME);
                //                            xmlOutput.writeText(aStackTrace);
                //                            xmlOutput.closeTag(ERROR_STACK_TRACE_ELEMENT_NAME);
                //                        }
                //                    }
                //                }
            }
            xmlOutput.closeTag(ERROR_ELEMENT_NAME);
        }

        // Emit missing classes
        XMLOutputUtil.writeElementList(xmlOutput, MISSING_CLASS_ELEMENT_NAME, missingClassIterator());

        xmlOutput.closeTag(ERRORS_ELEMENT_NAME);
    }

    private static void checkInputStream(@WillNotClose InputStream in) throws IOException {
        if (!in.markSupported()) {
            return;
        }

        byte[] buf = new byte[200];
        in.mark(buf.length);

        int numRead = 0;

        boolean isEOF = false;
        while (numRead < buf.length && !isEOF) {
            int n = in.read(buf, numRead, buf.length - numRead);
            if (n < 0) {
                isEOF = true;
            } else {
                numRead += n;
            }
        }

        in.reset();

        BufferedReader reader = new BufferedReader(Util.getReader(new ByteArrayInputStream(buf)));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<BugCollection")) {
                    return;
                }
            }
        } finally {
            reader.close();
        }

        throw new IOException("XML does not contain saved bug data");
    }

    /**
     * Clone all of the BugInstance objects in the source Collection and add
     * them to the destination Collection.
     *
     * @param dest
     *            the destination Collection
     * @param source
     *            the source Collection
     */
    public static void cloneAll(Collection<BugInstance> dest, Collection<BugInstance> source) {
        for (BugInstance obj : source) {
            dest.add((BugInstance) obj.clone());
        }
    }

    private static final class BoundedLinkedHashSet extends LinkedHashSet<AnalysisError> {
        @Override
        public boolean add(AnalysisError a) {
            if (this.size() > 1000) {
                return false;
            }
            return super.add(a);
        }
    }

    public static class BugInstanceComparator implements Comparator<BugInstance> {

        private BugInstanceComparator() {
        }

        @Override
        public int compare(BugInstance lhs, BugInstance rhs) {
            ClassAnnotation lca = lhs.getPrimaryClass();
            ClassAnnotation rca = rhs.getPrimaryClass();
            if (lca == null || rca == null) {
                throw new IllegalStateException("null class annotation: " + lca + "," + rca);
            }
            int cmp = lca.getClassName().compareTo(rca.getClassName());
            if (cmp != 0) {
                return cmp;
            }
            return lhs.compareTo(rhs);
        }

        public static final BugInstanceComparator instance = new BugInstanceComparator();
    }

    public static class MultiversionBugInstanceComparator extends BugInstanceComparator {

        private MultiversionBugInstanceComparator(){
        }

        @Override
        public int compare(BugInstance lhs, BugInstance rhs) {
            int result = super.compare(lhs, rhs);
            if (result != 0) {
                return result;
            }
            long diff = lhs.getFirstVersion() - rhs.getFirstVersion();
            if (diff == 0) {
                diff = lhs.getLastVersion() - rhs.getLastVersion();
            }
            if (diff < 0) {
                return -1;
            }
            if (diff > 0) {
                return 1;
            }
            return 0;
        }

        public static final MultiversionBugInstanceComparator instance = new MultiversionBugInstanceComparator();
    }

    public SortedBugCollection(Project project) {
        this(new ProjectStats(), MultiversionBugInstanceComparator.instance, project);
    }

    public SortedBugCollection(File f) throws IOException, DocumentException {
        this();
        this.readXML(f);
    }

    /**
     * Constructor. Creates an empty object.
     */
    public SortedBugCollection() {
        this(new ProjectStats());
    }

    /**
     * Constructor. Creates an empty object.
     */
    public SortedBugCollection(Comparator<BugInstance> comparator) {
        this(new ProjectStats(), comparator);
    }

    /**
     * Constructor. Creates an empty object given an existing ProjectStats.
     *
     * @param projectStats
     *            the ProjectStats
     */
    public SortedBugCollection(ProjectStats projectStats) {
        this(projectStats, MultiversionBugInstanceComparator.instance);
    }

    public SortedBugCollection(ProjectStats projectStats, Project project) {
        this(projectStats, MultiversionBugInstanceComparator.instance, project);
    }

    /**
     * Constructor. Creates an empty object given an existing ProjectStats.
     *
     * @param projectStats
     *            the ProjectStats
     * @param comparator
     *            to use for sorting bug instances
     */
    public SortedBugCollection(ProjectStats projectStats, Comparator<BugInstance> comparator) {
        this(projectStats, comparator, new Project());
    }

    public SortedBugCollection(ProjectStats projectStats, Comparator<BugInstance> comparator, Project project) {
        this.projectStats = projectStats;
        this.comparator = comparator;
        this.project = project;
        bugSet = new TreeSet<BugInstance>(comparator);
        errorList = new BoundedLinkedHashSet();
        missingClassSet = new TreeSet<String>();
        summaryHTML = null;
        classFeatureSetMap = new TreeMap<String, ClassFeatureSet>();
        sequence = 0L;
        appVersionList = new LinkedList<AppVersion>();
        releaseName = "";
        timestamp = -1L;
    }

    @Override
    public boolean add(BugInstance bugInstance, boolean updateActiveTime) {
        assert !bugsPopulated;

        if (bugsPopulated) {
            AnalysisContext.logError("Bug collection marked as populated, but bugs added",
                    new RuntimeException("Bug collection marked as populated, but bugs added"));
            bugsPopulated = false;
        }
        preciseHashOccurrenceNumbersAvailable = false;
        if (updateActiveTime) {
            bugInstance.setFirstVersion(sequence);
        }

        invalidateHashes();
        if (!bugInstance.isDead()) {
            projectStats.addBug(bugInstance);
        }
        return bugSet.add(bugInstance);
    }

    private void invalidateHashes() {
        preciseHashOccurrenceNumbersAvailable = false;
    }

    public boolean remove(BugInstance bugInstance) {
        invalidateHashes();
        return bugSet.remove(bugInstance);
    }

    @Override
    public Iterator<BugInstance> iterator() {
        return bugSet.iterator();
    }

    @Override
    public Collection<BugInstance> getCollection() {
        return Collections.unmodifiableCollection(bugSet);
    }

    public void addError(String message, Throwable exception) {
        if (exception instanceof MissingClassException) {
            MissingClassException e = (MissingClassException) exception;
            addMissingClass(AbstractBugReporter.getMissingClassName(e.getClassNotFoundException()));
            return;
        }
        if (exception instanceof ClassNotFoundException) {
            ClassNotFoundException e = (ClassNotFoundException) exception;
            addMissingClass(AbstractBugReporter.getMissingClassName(e));
            return;
        }
        if (exception instanceof edu.umd.cs.findbugs.classfile.MissingClassException) {
            edu.umd.cs.findbugs.classfile.MissingClassException e = (edu.umd.cs.findbugs.classfile.MissingClassException) exception;
            addMissingClass(AbstractBugReporter.getMissingClassName(e.toClassNotFoundException()));
            return;
        }
        errorList.add(new AnalysisError(message, exception));
    }

    @Override
    public void addError(AnalysisError error) {
        errorList.add(error);
    }

    public void clearErrors() {
        errorList.clear();
    }

    @Override
    public void addMissingClass(String className) {
        if (className == null || className.length() == 0) {
            return;
        }
        if (className.startsWith("[")) {
            assert false : "Bad class name " + className;
        return;
        }
        if (className.endsWith(";")) {
            addError("got signature rather than classname: " + className, new IllegalArgumentException());
        } else {
            missingClassSet.add(className);
        }
    }

    public Collection<? extends AnalysisError> getErrors() {
        return errorList;
    }

    public Iterator<String> missingClassIterator() {
        return missingClassSet.iterator();
    }

    public boolean contains(BugInstance bugInstance) {
        return bugSet.contains(bugInstance);
    }

    public BugInstance getMatching(BugInstance bugInstance) {
        SortedSet<BugInstance> tailSet = bugSet.tailSet(bugInstance);
        if (tailSet.isEmpty()) {
            return null;
        }
        BugInstance first = tailSet.first();
        return bugInstance.equals(first) ? first : null;
    }

    public String getSummaryHTML() throws IOException {
        if (summaryHTML == null) {
            try {
                StringWriter writer = new StringWriter();
                ProjectStats stats = getProjectStats();
                stats.transformSummaryToHTML(writer);
                summaryHTML = writer.toString();
            } catch (final TransformerException e) {
                IOException ioe = new IOException("Couldn't generate summary HTML");
                ioe.initCause(e);
                throw ioe;
            }
        }
        return summaryHTML;
    }

    @Override
    public ProjectStats getProjectStats() {
        return projectStats;
    }

    @Override
    @Deprecated
    public BugInstance lookupFromUniqueId(String uniqueId) {
        for (BugInstance bug : bugSet) {
            if (bug.getInstanceHash().equals(uniqueId)) {
                return bug;
            }
        }
        return null;
    }

    /** Returns whether this bug collection contains results from multiple analysis runs,
     * either of different version of the software or from different versions of FindBugs.
     */
    @Override
    public boolean isMultiversion() {
        return sequence > 0;
    }

    @Override
    public boolean hasDeadBugs() {
        if (sequence == 0) {
            return false;
        }
        for(BugInstance b : bugSet) {
            if (b.isDead()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getSequenceNumber() {
        return sequence;
    }

    @Override
    public void setSequenceNumber(long sequence) {
        this.sequence = sequence;
    }

    public SortedBugCollection duplicate() {
        SortedBugCollection dup = createEmptyCollectionWithMetadata();
        SortedBugCollection.cloneAll(dup.bugSet, this.bugSet);
        return dup;
    }

    @Override
    public SortedBugCollection createEmptyCollectionWithMetadata() {
        SortedBugCollection dup = new SortedBugCollection(projectStats.clone(), comparator, project);
        dup.projectStats.clearBugCounts();
        dup.errorList.addAll(this.errorList);
        dup.missingClassSet.addAll(this.missingClassSet);
        dup.summaryHTML = this.summaryHTML;
        dup.classFeatureSetMap.putAll(this.classFeatureSetMap);
        dup.sequence = this.sequence;
        dup.analysisVersion = this.analysisVersion;
        dup.analysisTimestamp = this.analysisTimestamp;
        dup.timestamp = this.timestamp;
        dup.releaseName = this.releaseName;
        for (AppVersion appVersion : appVersionList) {
            dup.appVersionList.add((AppVersion) appVersion.clone());
        }
        return dup;
    }

    public void clearBugInstances() {
        bugSet.clear();
        invalidateHashes();
    }

    @Override
    public void clearMissingClasses() {
        missingClassSet.clear();
    }

    @Override
    public String getReleaseName() {
        if (releaseName == null) {
            return "";
        }
        return releaseName;
    }

    @Override
    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    @Override
    public Iterator<AppVersion> appVersionIterator() {
        return appVersionList.iterator();
    }

    @Override
    public void addAppVersion(AppVersion appVersion) {
        appVersionList.add(appVersion);
    }

    @Override
    public void clearAppVersions() {
        appVersionList.clear();
        sequence = 0;
    }

    public void trimAppVersions(long numberToRetain) {
        while (appVersionList.size() > numberToRetain) {
            appVersionList.remove(appVersionList.size() - 1);
        }
        sequence = appVersionList.size();
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public ClassFeatureSet getClassFeatureSet(String className) {
        return classFeatureSetMap.get(className);
    }

    @Override
    public void setClassFeatureSet(ClassFeatureSet classFeatureSet) {
        classFeatureSetMap.put(classFeatureSet.getClassName(), classFeatureSet);
    }

    public Iterator<ClassFeatureSet> classFeatureSetIterator() {
        return classFeatureSetMap.values().iterator();
    }

    @Override
    public void clearClassFeatures() {
        classFeatureSetMap.clear();
    }

    @Override
    public void setWithMessages(boolean withMessages) {
        this.withMessages = withMessages;
    }

    @Override
    public boolean getWithMessages() {
        return withMessages;
    }

    @Override
    public AppVersion getAppVersionFromSequenceNumber(long target) {
        for (AppVersion av : appVersionList) {
            if (av.getSequenceNumber() == target) {
                return av;
            }
        }
        if (target == this.getSequenceNumber()) {
            return this.getCurrentAppVersion();
        }
        return null;
    }

    @Override
    public BugInstance findBug(String instanceHash, String bugType, int lineNumber) {
        for (BugInstance bug : bugSet) {
            if (bug.getInstanceHash().equals(instanceHash) && bug.getBugPattern().getType().equals(bugType)
                    && bug.getPrimarySourceLineAnnotation().getStartLine() == lineNumber) {
                return bug;
            }
        }
        return null;
    }

    @Override
    public void setAnalysisVersion(String version) {
        this.analysisVersion = version;
    }

    public String getAnalysisVersion() {
        return this.analysisVersion;
    }

    public InputStream progessMonitoredInputStream(File f, String msg) throws IOException {
        long length = f.length();
        if (length > Integer.MAX_VALUE){
            throw new IllegalArgumentException("File " + f + " is too big at " + length + " bytes");
        }
        InputStream in = new FileInputStream(f);
        return wrapGzip(progressMonitoredInputStream(in, (int) length, msg), f);
    }

    public InputStream progessMonitoredInputStream(URLConnection c, String msg) throws IOException {
        InputStream in = c.getInputStream();
        int length = c.getContentLength();
        return wrapGzip(progressMonitoredInputStream(in, length, msg), c.getURL());
    }

    public InputStream progressMonitoredInputStream(InputStream in, int length, String msg) {
        if (GraphicsEnvironment.isHeadless()) {
            return in;
        }
        IGuiCallback guiCallback = project.getGuiCallback();
        return guiCallback.getProgressMonitorInputStream(in, length, msg);
    }

    public InputStream wrapGzip(InputStream in, Object source) {
        try {
            if (source instanceof File) {
                File f = (File) source;
                if (f.getName().endsWith(".gz")) {
                    return new GZIPInputStream(in);
                }
            } else if (source instanceof URL) {
                URL u = (URL) source;
                if (u.getPath().endsWith(".gz")) {
                    return new GZIPInputStream(in);
                }

            }
        } catch (IOException e) {
            assert true;
        }
        return in;
    }

    public void clearCloud() {
        Cloud oldCloud = cloud;
        IGuiCallback callback = project.getGuiCallback();
        if (oldCloud != null) {
            callback.unregisterCloud(project, this, oldCloud);
            oldCloud.shutdown();
        }
        cloud = null;
    }

    @Override
    public @Nonnull Cloud reinitializeCloud() {
        Cloud oldCloud = cloud;
        IGuiCallback callback = project.getGuiCallback();
        if (oldCloud != null) {
            callback.unregisterCloud(project, this, oldCloud);
            oldCloud.shutdown();
        }
        cloud = null;
        @Nonnull Cloud newCloud = getCloud();
        assert newCloud == cloud;
        assert cloud != null;
        assert cloud.isInitialized();
        if (bugsPopulated ) {
            cloud.bugsPopulated();
            cloud.initiateCommunication();
        }
        return cloud;
    }

    @Override
    public void setXmlCloudDetails(Map<String, String> map) {
        this.xmlCloudDetails = map;
    }

    @Override
    public Map<String, String> getXmlCloudDetails() {
        return xmlCloudDetails;
    }

    @Override
    public void setMinimalXML(boolean minimalXML) {
        this.minimalXML = minimalXML;
    }

    public void setDoNotUseCloud(boolean b) {
        this.shouldNotUsePlugin = b;
    }

    @Override
    public void bugsPopulated() {
        bugsPopulated = true;
    }

}


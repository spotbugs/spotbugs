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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
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
import java.util.zip.GZIPInputStream;

import javax.annotation.WillClose;
import javax.xml.transform.TransformerException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.model.ClassFeatureSet;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.xml.Dom4JXMLOutput;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutputUtil;

/**
 * An implementation of {@link BugCollection} that keeps the BugInstances
 * sorted by class (using the native comparison ordering of BugInstance's
 * compareTo() method as a tie-breaker).
 *
 * @see BugInstance
 * @author David Hovemeyer
 */
public class SortedBugCollection implements BugCollection {
	long analysisTimestamp = System.currentTimeMillis();
	String analysisVersion = Version.RELEASE;
	private boolean withMessages = false;

	private static final boolean REPORT_SUMMARY_HTML =
		SystemProperties.getBoolean("findbugs.report.SummaryHTML");

	public long getAnalysisTimestamp() {
		return analysisTimestamp;
	}

	public void setAnalysisTimestamp(long timestamp) {
		analysisTimestamp = timestamp;
	}

	/**
	 * Add a Collection of BugInstances to this BugCollection object.
	 * This just calls add(BugInstance) for each instance in the input collection.
	 *
	 * @param collection the Collection of BugInstances to add
	 */
	public void addAll(Collection<BugInstance> collection) {
		for (BugInstance aCollection : collection) {
			add(aCollection);
		}
	}

	/**
	 * Add a Collection of BugInstances to this BugCollection object.
	 *
	 * @param collection       the Collection of BugInstances to add
	 * @param updateActiveTime true if active time of added BugInstances should
	 *                         be updated to match collection: false if not
	 */
	public void addAll(Collection<BugInstance> collection, boolean updateActiveTime) {
		for (BugInstance warning : collection) {
			add(warning, updateActiveTime);
		}
	}

	/**
	 * Add a BugInstance to this BugCollection.
	 * This just calls add(bugInstance, true).
	 *
	 * @param bugInstance the BugInstance
	 * @return true if the BugInstance was added, or false if a matching
	 *         BugInstance was already in the BugCollection
	 */
	public boolean add(BugInstance bugInstance) {
		return add(bugInstance, true);
	}

	/**
	 * Add an analysis error.
	 *
	 * @param message the error message
	 */
	public void addError(String message) {
		addError(message, null);
	}

	/**
	 * Get the current AppVersion.
	 */
	public AppVersion getCurrentAppVersion() {
		return new AppVersion(getSequenceNumber())
			.setReleaseName(getReleaseName())
			.setTimestamp(getTimestamp())
			.setNumClasses(getProjectStats().getNumClasses())
			.setCodeSize(getProjectStats().getCodeSize());
	}

	/**
	 * Read XML data from given file into this object,
	 * populating given Project as a side effect.
	 *
	 * @param fileName name of the file to read
	 * @param project  the Project
	 */
	public void readXML(String fileName, Project project)
			throws IOException, DocumentException {
		readXML(new File(fileName), project);
	}

	/**
	 * Read XML data from given file into this object,
	 * populating given Project as a side effect.
	 *
	 * @param file    the file
	 * @param project the Project
	 */
	public void readXML(File file, Project project)
			throws IOException, DocumentException {
		project.setCurrentWorkingDirectory(file.getParentFile());
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		if (file.getName().endsWith(".gz"))
			in = new GZIPInputStream(in);
		readXML(in, project, file);
	}

	/**
	 * Read XML data from given input stream into this
	 * object, populating the Project as a side effect.
	 * An attempt will be made to close the input stream
	 * (even if an exception is thrown).
	 *
	 * @param in      the InputStream
	 * @param project the Project
	 */
	public void readXML(InputStream in, Project project, File base)
			throws IOException, DocumentException {
		if (in == null) throw new IllegalArgumentException();

		try {
			if (project == null) throw new IllegalArgumentException();
			doReadXML(in, project, base);
		} finally {
			in.close();
		}
	}
	public void readXML(@WillClose InputStream in, Project project)
			throws IOException, DocumentException {
			doReadXML(in, project, null);

	}

	private void doReadXML(@WillClose InputStream in, Project project, File base) throws IOException, DocumentException {

		checkInputStream(in);

		try {
			SAXBugCollectionHandler handler = new SAXBugCollectionHandler(this, project, base);


			XMLReader xr = null;
			if (true) try { 
				xr = XMLReaderFactory.createXMLReader();
			  } catch (SAXException e) {
				AnalysisContext.logError("Couldn't create XMLReaderFactory", e);   
			  }

//			if (xr == null) {
//				xr = new org.dom4j.io.aelfred.SAXDriver();
//				}
			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);

			Reader reader = Util.getReader(in);

			xr.parse(new InputSource(reader));
		} catch (SAXParseException e) {
			throw new DocumentException("Parse error at line " + e.getLineNumber()
					+ " : " + e.getColumnNumber(), e);
		} catch (SAXException e) {
			// FIXME: throw SAXException from method?
			throw new DocumentException("Sax error ", e);
		}
		finally {
			in.close();
		}

		// Presumably, project is now up-to-date
		project.setModified(false);
	}

	/**
	 * Write this BugCollection to a file as XML.
	 *
	 * @param fileName the file to write to
	 * @param project  the Project from which the BugCollection was generated
	 */
	public void writeXML(String fileName, Project project) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
		writeXML(out, project);
	}

	/**
	 * Write this BugCollection to a file as XML.
	 *
	 * @param file    the file to write to
	 * @param project the Project from which the BugCollection was generated
	 */
	public void writeXML(File file, Project project) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		writeXML(out, project);
	}

	/**
	 * Convert the BugCollection into a dom4j Document object.
	 *
	 * @param project the Project from which the BugCollection was generated
	 * @return the Document representing the BugCollection as a dom4j tree
	 */
	public Document toDocument(Project project) {
		if (project == null) throw new NullPointerException("No project");
		DocumentFactory docFactory = new DocumentFactory();
		Document document = docFactory.createDocument();
		Dom4JXMLOutput treeBuilder = new Dom4JXMLOutput(document);

		try {
			writeXML(treeBuilder, project);
		} catch (IOException e) {
			// Can't happen
		}

		return document;
	}

	/**
	 * Write the BugCollection to given output stream as XML.
	 * The output stream will be closed, even if an exception is thrown.
	 *
	 * @param out     the OutputStream to write to
	 * @param project the Project from which the BugCollection was generated
	 */
	public void writeXML(@WillClose OutputStream out, Project project) throws IOException {
		XMLOutput xmlOutput;
		if (project == null) throw new NullPointerException("No project");
		if (withMessages) xmlOutput= new OutputStreamXMLOutput(out, "http://findbugs.sourceforge.net/xsl/default.xsl");
		else xmlOutput= new OutputStreamXMLOutput(out);

		writeXML(xmlOutput, project);
	}

	public void writePrologue(XMLOutput xmlOutput, Project project) throws IOException {
		xmlOutput.beginDocument();
		xmlOutput.openTag(ROOT_ELEMENT_NAME,
			new XMLAttributeList()
				.addAttribute("version", analysisVersion)
				.addAttribute("sequence",String.valueOf(getSequenceNumber()))
				.addAttribute("timestamp", String.valueOf(getTimestamp()))
				.addAttribute("analysisTimestamp", String.valueOf(getAnalysisTimestamp()))

				.addAttribute("release", getReleaseName())
		);
		project.writeXML(xmlOutput);
	}

//	private String getQuickInstanceHash(BugInstance bugInstance) {
//		String hash = bugInstance.getInstanceHash();
//		if (hash != null) return hash;
//		MessageDigest digest = null;
//		try { digest = MessageDigest.getInstance("MD5");
//		} catch (Exception e2) {
//			// OK, we won't digest
//			assert true;
//		}
//		hash = bugInstance.getInstanceKey();
//		if (digest != null) {
//			byte [] data = digest.digest(hash.getBytes());
//			String tmp = new BigInteger(1,data).toString(16);
//			if (false) System.out.println(hash + " -> " + tmp);
//			hash = tmp;
//		}
//		bugInstance.setInstanceHash(hash);
//		return hash;
//	}

	public void computeBugHashes() {
		if (preciseHashOccurrenceNumbersAvailable) return;
		invalidateHashes();
		MessageDigest digest = null;
		try { digest = MessageDigest.getInstance("MD5");
		} catch (Exception e2) {
			// OK, we won't digest
		}

		HashMap<String, Integer> seen = new HashMap<String, Integer>();

		for(BugInstance bugInstance : getCollection()) {
			String hash = bugInstance.getInstanceHash();
			if (hash == null) {
				hash = bugInstance.getInstanceKey();

				if (digest != null) {
					byte [] data = digest.digest(hash.getBytes());
					String tmp = new BigInteger(1,data).toString(16);
					if (false) System.out.println(hash + " -> " + tmp);
					hash = tmp;
				}
				bugInstance.setInstanceHash(hash);
			}
			Integer count = seen.get(hash);
			if (count == null) {
				bugInstance.setInstanceOccurrenceNum(0);
				seen.put(hash,0);
			} else {
				bugInstance.setInstanceOccurrenceNum(count+1);
				seen.put(hash, count+1);
			}
		}
		for(BugInstance bugInstance : getCollection()) 
			bugInstance.setInstanceOccurrenceMax(seen.get(bugInstance.getInstanceHash()));
		preciseHashOccurrenceNumbersAvailable = true;
	}
	/**
	 * Write the BugCollection to an XMLOutput object.
	 * The finish() method of the XMLOutput object is guaranteed
	 * to be called.
	 *
	 * <p>
	 * To write the SummaryHTML element, set property
	 * findbugs.report.SummaryHTML to "true".
	 * </p>
	 *
	 * @param xmlOutput the XMLOutput object
	 * @param project   the Project from which the BugCollection was generated
	 */
	public void writeXML(XMLOutput xmlOutput, @NonNull Project project) throws IOException {
		if (project == null) throw new NullPointerException("No project");
		try {
			writePrologue(xmlOutput, project);
			if (withMessages) {
				computeBugHashes();
				String commonBase = null;
				for(String s : project.getSourceDirList()) {
					if (commonBase == null) commonBase = s;
					else commonBase = commonBase.substring(0, commonPrefix(commonBase, s));
					
				}
				if (commonBase != null && commonBase.length() > 0) {
					if (commonBase.indexOf("/./") > 0)
						commonBase = commonBase.substring(0,commonBase.indexOf("/."));
					File base = new File(commonBase);
					if (base.exists() && base.isDirectory() && base.canRead())
						SourceLineAnnotation.generateRelativeSource(base, project);
				}
			}

			// Write BugInstances
			for(BugInstance bugInstance : getCollection())
				bugInstance.writeXML(xmlOutput, withMessages, false);

			writeEpilogue(xmlOutput);
			
		} finally {
			xmlOutput.finish();
			SourceLineAnnotation.clearGenerateRelativeSource();
		}
	}

	
	int commonPrefix(String s1, String s2) {
		int pos = 0;
		while (pos < s1.length() && pos < s2.length() && s1.charAt(pos) == s2.charAt(pos)) pos++;
		return pos;
	}
	public void writeEpilogue(XMLOutput xmlOutput) throws IOException {
		if (withMessages) {
			writeBugCategories( xmlOutput);
			writeBugPatterns( xmlOutput);
			writeBugCodes( xmlOutput);
		}
		// Errors, missing classes
		emitErrors(xmlOutput);

		// Statistics
		getProjectStats().writeXML(xmlOutput);

//		// Class and method hashes
//		xmlOutput.openTag(CLASS_HASHES_ELEMENT_NAME);
//		for (Iterator<ClassHash> i = classHashIterator(); i.hasNext();) {
//			ClassHash classHash = i.next();
//			classHash.writeXML(xmlOutput);
//		}
//		xmlOutput.closeTag(CLASS_HASHES_ELEMENT_NAME);

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
		if ( REPORT_SUMMARY_HTML ) {
			String html = getSummaryHTML();
			if (html != null && !html.equals("")) {
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
			if (bugPattern != null) {
				bugTypeSet.add(bugPattern.getType());
			}
		}
		// Emit element describing each reported bug pattern
		for (String bugType : bugTypeSet) {
			BugPattern bugPattern = I18N.instance().lookupBugPattern(bugType);
			if (bugPattern == null)
				continue;

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
			BugCode bugCode = I18N.instance().getBugCode(bugCodeAbbrev);
			String bugCodeDescription = bugCode.getDescription();
			if (bugCodeDescription == null)
				continue;

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
			if (bugPattern != null) {
				bugCatSet.add(bugPattern.getCategory());
			}
		}
		// Emit element describing each reported bug code
		for (String bugCat : bugCatSet) {
			String bugCatDescription = I18N.instance().getBugCategoryDescription(bugCat);
			if (bugCatDescription == null)
				continue;

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
		//System.err.println("Writing errors to XML output");

		xmlOutput.openTag(ERRORS_ELEMENT_NAME);

		// Emit Error elements describing analysis errors
		for (Iterator<AnalysisError> i = errorIterator(); i.hasNext(); ) {
			AnalysisError error = i.next();
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

				if (false && error.getNestedExceptionMessage() != null) {
					xmlOutput.openTag(ERROR_EXCEPTION_ELEMENT_NAME);
					xmlOutput.writeText(error.getNestedExceptionMessage());
					xmlOutput.closeTag(ERROR_EXCEPTION_ELEMENT_NAME);

					stackTrace = error.getNestedStackTrace();
					if (stackTrace != null) {
						for (String aStackTrace : stackTrace) {
							xmlOutput.openTag(ERROR_STACK_TRACE_ELEMENT_NAME);
							xmlOutput.writeText(aStackTrace);
							xmlOutput.closeTag(ERROR_STACK_TRACE_ELEMENT_NAME);
						}
					}
				}
			}
			xmlOutput.closeTag(ERROR_ELEMENT_NAME);
		}

		// Emit missing classes
		XMLOutputUtil.writeElementList(xmlOutput, MISSING_CLASS_ELEMENT_NAME,
			missingClassIterator());

		xmlOutput.closeTag(ERRORS_ELEMENT_NAME);
	}

	
	
	private void checkInputStream(InputStream in) throws IOException {
		if (in.markSupported()) {
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
	}

	/**
	 * Clone all of the BugInstance objects in the source Collection
	 * and add them to the destination Collection.
	 *
	 * @param dest   the destination Collection
	 * @param source the source Collection
	 */
	public static void cloneAll(Collection<BugInstance> dest, Collection<BugInstance> source) {
		for (BugInstance obj : source) {
			dest.add((BugInstance) obj.clone());
		}
	}

	public static class BugInstanceComparator implements Comparator<BugInstance> {
		private BugInstanceComparator() {}
		public int compare(BugInstance lhs, BugInstance rhs) {
			ClassAnnotation lca = lhs.getPrimaryClass();
			ClassAnnotation rca = rhs.getPrimaryClass();
			if (lca == null || rca == null)
				throw new IllegalStateException("null class annotation: " + lca + "," + rca);
			int cmp = lca.getClassName().compareTo(rca.getClassName());
			if (cmp != 0)
				return cmp;
			return lhs.compareTo(rhs);
		}
		public static final BugInstanceComparator instance = new BugInstanceComparator();
	}

	public static class MultiversionBugInstanceComparator extends BugInstanceComparator {
		@Override
		public int compare(BugInstance lhs, BugInstance rhs) {
			int result = super.compare(lhs,rhs);
			if (result != 0) return result;
			long diff  = lhs.getFirstVersion() - rhs.getFirstVersion();
			if (diff == 0)
				diff = lhs.getLastVersion() - rhs.getLastVersion();
			if (diff < 0) return -1;
			if (diff > 0) return 1;
			return 0;
		}
		public static final MultiversionBugInstanceComparator instance = new MultiversionBugInstanceComparator();
	}
	private Comparator<BugInstance> comparator;
	private TreeSet<BugInstance> bugSet;
	private LinkedHashSet<AnalysisError> errorList;
	private TreeSet<String> missingClassSet;
	@CheckForNull private String summaryHTML;
	private ProjectStats projectStats;
//	private Map<String, ClassHash> classHashMap;
	private Map<String, ClassFeatureSet> classFeatureSetMap;
	private List<AppVersion> appVersionList;


	private boolean preciseHashOccurrenceNumbersAvailable = false;
	/**
	 * Sequence number of the most-recently analyzed version
	 * of the code.
	 */
	private long sequence;
	/**
	 * Release name of the analyzed application.
	 */
	private String releaseName;
	/**
	 * Current analysis timestamp.
	 */
	private long timestamp;

	/**
	 * Constructor.
	 * Creates an empty object.
	 */
	public SortedBugCollection() {
		this(new ProjectStats());
	}

	/**
	 * Constructor.
	 * Creates an empty object.
	 */
	public SortedBugCollection(Comparator<BugInstance> comparator) {
		this(new ProjectStats(), comparator);
	}

	/**
	 * Constructor.
	 * Creates an empty object given an existing ProjectStats.
	 * 
	 * @param projectStats the ProjectStats
	 */
	public SortedBugCollection(ProjectStats projectStats) {
		this(projectStats, MultiversionBugInstanceComparator.instance);
	}
	/**
	 * Constructor.
	 * Creates an empty object given an existing ProjectStats.
	 * 
	 * @param projectStats the ProjectStats
	 * @param comparator to use for sorting bug instances
	 */
	public SortedBugCollection(ProjectStats projectStats, Comparator<BugInstance> comparator) {
		this.projectStats = projectStats;
		this.comparator = comparator;
		bugSet = new TreeSet<BugInstance>(comparator);
		errorList = new LinkedHashSet<AnalysisError>() { 
			@Override public boolean add(AnalysisError a) {
				if (this.size() > 1000) return false;
				return super.add(a);
			}
		};
		missingClassSet = new TreeSet<String>();
		summaryHTML = null;
		classFeatureSetMap = new TreeMap<String, ClassFeatureSet>();
		sequence = 0L;
		appVersionList = new LinkedList<AppVersion>();
		releaseName = "";
		timestamp = -1L;
	}

	public boolean add(BugInstance bugInstance, boolean updateActiveTime) {
		preciseHashOccurrenceNumbersAvailable = false;
		if (updateActiveTime) {
			bugInstance.setFirstVersion(sequence);
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

	public Iterator<BugInstance> iterator() {
		return bugSet.iterator();
	}

	public Collection<BugInstance> getCollection() {
		return bugSet;
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


	public void addError(AnalysisError error) {
		errorList.add(error);
	}

	public void addMissingClass(String className) {
		if (className.length() == 0) return;
		if (className.startsWith("[")) {
			assert false : "Bad class name " + className;
			return;
		}
		missingClassSet.add(className);
	}

	public Iterator<AnalysisError> errorIterator() {
		return errorList.iterator();
	}

	public Iterator<String> missingClassIterator() {
		return missingClassSet.iterator();
	}

	public boolean contains(BugInstance bugInstance) {
		return bugSet.contains(bugInstance);
	}

	public BugInstance getMatching(BugInstance bugInstance) {
		SortedSet<BugInstance> tailSet = bugSet.tailSet(bugInstance);
		if (tailSet.isEmpty())
			return null;
		BugInstance first = tailSet.first();
		return bugInstance.equals(first) ? first : null;
	}

	public String getSummaryHTML() throws IOException {
		if ( summaryHTML == null ) {
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

	public ProjectStats getProjectStats() {
		return projectStats;
	}

	/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.BugCollection#lookupFromUniqueId(java.lang.String)
		 */
	@Deprecated
	public BugInstance lookupFromUniqueId(String uniqueId) {
		for(BugInstance bug : bugSet)
			if (bug.getInstanceHash().equals(uniqueId)) return bug;
		return null;
	}

		public long getSequenceNumber() {
		return sequence;
	}

	public void setSequenceNumber(long sequence) {
		this.sequence = sequence;
	}



	public SortedBugCollection duplicate() {
		SortedBugCollection dup = new SortedBugCollection((ProjectStats) projectStats.clone(), comparator);

		SortedBugCollection.cloneAll(dup.bugSet, this.bugSet);
		dup.errorList.addAll(this.errorList);
		dup.missingClassSet.addAll(this.missingClassSet);
		dup.summaryHTML = this.summaryHTML;
//		dup.classHashMap.putAll(this.classHashMap);
		dup.classFeatureSetMap.putAll(this.classFeatureSetMap);
		dup.sequence = this.sequence;
		dup.timestamp = this.timestamp;
		dup.releaseName = this.releaseName;
		for (AppVersion appVersion : appVersionList) {
			dup.appVersionList.add((AppVersion) appVersion.clone());
		}

		return dup;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#clearBugInstances()
	 */

	public void clearBugInstances() {
		bugSet.clear();
		invalidateHashes();

	}

	/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.BugCollection#getReleaseName()
		 */

	public String getReleaseName() {
		if (releaseName == null) return "";
		return releaseName;
	}

	/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.BugCollection#setReleaseName(java.lang.String)
		 */

	public void setReleaseName(String releaseName) {
		this.releaseName = releaseName;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#appVersionIterator()
	 */

	public Iterator<AppVersion> appVersionIterator() {
		return appVersionList.iterator();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#addAppVersion(edu.umd.cs.findbugs.AppVersion)
	 */

	public void addAppVersion(AppVersion appVersion) {
		appVersionList.add(appVersion);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#clearAppVersions()
	 */

	public void clearAppVersions() {
		appVersionList.clear();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#createEmptyCollectionWithMetadata()
	 */

	public SortedBugCollection createEmptyCollectionWithMetadata() {
		SortedBugCollection dup = new SortedBugCollection((ProjectStats) projectStats.clone(), comparator);	
		dup.errorList.addAll(this.errorList);
		dup.missingClassSet.addAll(this.missingClassSet);
		dup.summaryHTML = this.summaryHTML;
		dup.classFeatureSetMap.putAll(this.classFeatureSetMap);
		dup.sequence = this.sequence;
		dup.analysisVersion = this.analysisVersion;
		dup.analysisTimestamp = dup.analysisTimestamp;
		dup.timestamp = this.timestamp;
		dup.releaseName = this.releaseName;
		for (AppVersion appVersion : appVersionList) {
			dup.appVersionList.add((AppVersion) appVersion.clone());
		}

		return dup;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#setTimestamp(long)
	 */

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#getTimestamp()
	 */

	public long getTimestamp() {
		return timestamp;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#getClassFeatureSet(java.lang.String)
	 */

	public ClassFeatureSet getClassFeatureSet(String className) {
		return classFeatureSetMap.get(className);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#setClassFeatureSet(edu.umd.cs.findbugs.model.ClassFeatureSet)
	 */

	public void setClassFeatureSet(ClassFeatureSet classFeatureSet) {
		classFeatureSetMap.put(classFeatureSet.getClassName(), classFeatureSet);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#classFeatureSetIterator()
	 */

	public Iterator<ClassFeatureSet> classFeatureSetIterator() {
		return classFeatureSetMap.values().iterator();
	}

	/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.BugCollection#clearClassFeatures()
		 */
	public void clearClassFeatures() {
		classFeatureSetMap.clear();
	}

	/**
	 * @param withMessages The withMessages to set.
	 */
	public void setWithMessages(boolean withMessages) {
		this.withMessages = withMessages;
	}

	/**
	 * @return Returns the withMessages.
	 */
	public boolean getWithMessages() {
		return withMessages;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#getAppVersionFromSequenceNumber(int)
	 */
	public AppVersion getAppVersionFromSequenceNumber(long target) {
		for(AppVersion av : appVersionList)
			if (av.getSequenceNumber() == target) return av;
		if(target == this.getSequenceNumber())
			return this.getCurrentAppVersion();
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#findBug(java.lang.String, java.lang.String, int)
	 */
	public BugInstance findBug(String instanceHash, String bugType,
			int lineNumber) {
		for(BugInstance bug : bugSet) 
			if (bug.getInstanceHash().equals(instanceHash) && bug.getBugPattern().getType().equals(bugType)
					&&  bug.getPrimarySourceLineAnnotation().getStartLine() == lineNumber) return bug;
		return null;
	}

	/**
	 * @param version
	 */
	public void setAnalysisVersion(String version) {
		this.analysisVersion = version;

	}
}

// vim:ts=4

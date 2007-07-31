/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

/*
 * Project.java
 *
 * Created on March 30, 2003, 2:22 PM
 */

package edu.umd.cs.findbugs;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.dom4j.DocumentException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.URLClassPath;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutputUtil;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * A project in the GUI.
 * This consists of some number of Jar files to analyze for bugs, and optionally
 * <p/>
 * <ul>
 * <li> some number of source directories, for locating the program's
 * source code
 * <li> some number of auxiliary classpath entries, for locating classes
 * referenced by the program which the user doesn't want to analyze
 * <li> some number of boolean options
 * </ul>
 *
 * @author David Hovemeyer
 */
public class Project implements XMLWriteable {
	private static final boolean DEBUG = SystemProperties.getBoolean("findbugs.project.debug");

	private File currentWorkingDirectory;
	/**
	 * Project filename.
	 */
	@Deprecated
	private String projectFileName;

	private String projectName;

	/**
	 * Options.
	 */
	private Map<String, Boolean> optionsMap;

	/**
	 * The list of project files.
	 */
	private List<String> fileList;

	/**
	 * The list of source directories.
	 */
	private List<String> srcDirList;

	/**
	 * The list of auxiliary classpath entries.
	 */
	private List<String> auxClasspathEntryList;

	/**
	 * Flag to indicate that this Project has been modified.
	 */
	private boolean isModified;

	/**
	 * Constant used to name anonymous projects.
	 */
	public static final String UNNAMED_PROJECT = "<<unnamed project>>";

	private long timestamp = 0L;
	
	@NonNull private Filter suppressionFilter = new Filter();

	/**
	 * Create an anonymous project.
	 */
	public Project() {
		this.projectFileName = UNNAMED_PROJECT;
		optionsMap = new HashMap<String, Boolean>();
		optionsMap.put(RELATIVE_PATHS, Boolean.FALSE);
		fileList = new LinkedList<String>();
		srcDirList = new LinkedList<String>();
		auxClasspathEntryList = new LinkedList<String>();
		isModified = false;
	}

	/**
	 * Return an exact copy of this Project.
	 */
	public Project duplicate() {
		Project dup = new Project();
		dup.projectFileName = this.projectFileName;
		dup.currentWorkingDirectory = this.currentWorkingDirectory;
		dup.optionsMap.clear();
		dup.optionsMap.putAll(this.optionsMap);
		dup.fileList.addAll(this.fileList);
		dup.srcDirList.addAll(this.srcDirList);
		dup.auxClasspathEntryList.addAll(this.auxClasspathEntryList);
		dup.timestamp = timestamp;

		return dup;
	}
	/**
	 * add information from project2 to this project
	 */
	public void add(Project project2) {
		optionsMap.putAll(project2.optionsMap);
		fileList = appendWithoutDuplicates(fileList, project2.fileList);
		srcDirList = appendWithoutDuplicates(srcDirList, project2.srcDirList);
		auxClasspathEntryList = appendWithoutDuplicates(auxClasspathEntryList, project2.auxClasspathEntryList);

	}

	public static <T> List<T> appendWithoutDuplicates(List<T> lst1, List<T> lst2) {
		LinkedHashSet<T> joined = new LinkedHashSet<T>(lst1);
		joined.addAll(lst2);
		return new ArrayList<T>(joined);
		
	}
	public void setCurrentWorkingDirectory(File f) {
		this.currentWorkingDirectory = f;
	}
	/**
	 * Return whether or not this Project has unsaved modifications.
	 */
	public boolean isModified() {
		return isModified;
	}

	/**
	 * Set whether or not this Project has unsaved modifications.
	 */
	public void setModified(boolean isModified) {
		this.isModified = isModified;
	}

	/**
	 * Get the project filename.
	 */
	@Deprecated
	public String getProjectFileName() {
		return projectFileName;
	}

	/**
	 * Set the project filename.
	 *
	 * @param projectFileName the new filename
	 */
	@Deprecated
	public void setProjectFileName(String projectFileName) {
		this.projectFileName = projectFileName;
	}

	/**
	 * Add a file to the project.
	 *
	 * @param fileName the file to add
	 * @return true if the file was added, or false if the
	 *         file was already present
	 */
	public boolean addFile(String fileName) {
		return addToListInternal(fileList, makeAbsoluteCWD(fileName));
	}

	/**
	 * Add a source directory to the project.
	 * @param dirName the directory to add
	 * @return true if the source directory was added, or false if the
	 *   source directory was already present
	 */
	public boolean addSourceDir(String dirName) {
		return addToListInternal(srcDirList, makeAbsoluteCWD(dirName));
	}

	/**
	 * Retrieve the Options value.
	 *
	 * @param option the name of option to get
	 * @return the value of the option
	 */
	public boolean getOption(String option) {
		Boolean value = optionsMap.get(option);
		return value != null && value.booleanValue();
	}

	/**
	 * Get the number of files in the project.
	 *
	 * @return the number of files in the project
	 */
	public int getFileCount() {
		return fileList.size();
	}

	/**
	 * Get the given file in the list of project files.
	 *
	 * @param num the number of the file in the list of project files
	 * @return the name of the file
	 */
	public String getFile(int num) {
		return fileList.get(num);
	}

	/**
	 * Remove file at the given index in the list of project files
	 *
	 * @param num index of the file to remove in the list of project files
	 */
	public void removeFile(int num) {
		fileList.remove(num);
		isModified = true;
	}

	/**
	 * Get the list of files, directories, and zip files in the project.
	 */
	public List<String> getFileList() {
		return fileList;
	}

	/**
	 * Get the number of source directories in the project.
	 *
	 * @return the number of source directories in the project
	 */
	public int getNumSourceDirs() {
		return srcDirList.size();
	}

	/**
	 * Get the given source directory.
	 *
	 * @param num the number of the source directory
	 * @return the source directory
	 */
	public String getSourceDir(int num) {
		return srcDirList.get(num);
	}

	/**
	 * Remove source directory at given index.
	 *
	 * @param num index of the source directory to remove
	 */
	public void removeSourceDir(int num) {
		srcDirList.remove(num);
		isModified = true;
	}

	/**
	 * Get project files as an array of Strings.
	 */
	public String[] getFileArray() {
		return fileList.toArray(new String[fileList.size()]);
	}

	/**
	 * Get source dirs as an array of Strings.
	 */
	public String[] getSourceDirArray() {
		return srcDirList.toArray(new String[srcDirList.size()]);
	}

	/**
	 * Get the source dir list.
	 */
	public List<String> getSourceDirList() {
		return srcDirList;
	}

	/**
	 * Add an auxiliary classpath entry
	 *
	 * @param auxClasspathEntry the entry
	 * @return true if the entry was added successfully, or false
	 *         if the given entry is already in the list
	 */
	public boolean addAuxClasspathEntry(String auxClasspathEntry) {
		return addToListInternal(auxClasspathEntryList, makeAbsoluteCWD(auxClasspathEntry));
	}

	/**
	 * Get the number of auxiliary classpath entries.
	 */
	public int getNumAuxClasspathEntries() {
		return auxClasspathEntryList.size();
	}

	/**
	 * Get the n'th auxiliary classpath entry.
	 */
	public String getAuxClasspathEntry(int n) {
		return auxClasspathEntryList.get(n);
	}

	/**
	 * Remove the n'th auxiliary classpath entry.
	 */
	public void removeAuxClasspathEntry(int n) {
		auxClasspathEntryList.remove(n);
		isModified = true;
	}

	/**
	 * Return the list of aux classpath entries.
	 */
	public List<String> getAuxClasspathEntryList() {
		return auxClasspathEntryList;
	}

	/**
	 * Worklist item for finding implicit classpath entries.
	 */
	private static class WorkListItem {
		private URL url;

		/**
		 * Constructor.
		 *
		 * @param url the URL of the Jar or Zip file
		 */
		public WorkListItem(URL url) {
			this.url = url;
		}

		/**
		 * Get URL of Jar/Zip file.
		 */
		public URL getURL() {
			return this.url;
		}
	}

	/**
	 * Worklist for finding implicit classpath entries.
	 */
	private static class WorkList {
		private LinkedList<WorkListItem> itemList;
		private HashSet<String> addedSet;

		/**
		 * Constructor.
		 * Creates an empty worklist.
		 */
		public WorkList() {
			this.itemList = new LinkedList<WorkListItem>();
			this.addedSet = new HashSet<String>();
		}

		/**
		 * Create a URL from a filename specified in the project file.
		 */
		public URL createURL(String fileName) throws MalformedURLException {
			String protocol = URLClassPath.getURLProtocol(fileName);
			if (protocol == null) {
				fileName = "file:" + fileName;
			}
			return new URL(fileName);
		}

		/**
		 * Create a URL of a file relative to another URL.
		 */
		public URL createRelativeURL(URL base, String fileName) throws MalformedURLException {
			return new URL(base, fileName);
		}

		/**
		 * Add a worklist item.
		 *
		 * @param item the WorkListItem representing a zip/jar file to be examined
		 * @return true if the item was added, false if not (because it was
		 *         examined already)
		 */
		public boolean add(WorkListItem item) {
			if (DEBUG) System.out.println("Adding " + item.getURL().toString());
			if (!addedSet.add(item.getURL().toString())) {
				if (DEBUG) System.out.println("\t==> Already processed");
				return false;
			}

			itemList.add(item);
			return true;
		}

		/**
		 * Return whether or not the worklist is empty.
		 */
		public boolean isEmpty() {
			return itemList.isEmpty();
		}

		/**
		 * Get the next item in the worklist.
		 */
		public WorkListItem getNextItem() {
			return itemList.removeFirst();
		}
	}

	/**
	 * Return the list of implicit classpath entries.  The implicit
	 * classpath is computed from the closure of the set of jar files
	 * that are referenced by the <code>"Class-Path"</code> attribute
	 * of the manifest of the any jar file that is part of this project
	 * or by the <code>"Class-Path"</code> attribute of any directly or
	 * indirectly referenced jar.  The referenced jar files that exist
	 * are the list of implicit classpath entries.
	 * 
	 * @deprecated FindBugs2 and ClassPathBuilder take care of this automatically
	 */
	public List<String> getImplicitClasspathEntryList() {
		final LinkedList<String> implicitClasspath = new LinkedList<String>();
		WorkList workList = new WorkList();

		// Prime the worklist by adding the zip/jar files
		// in the project.
		for (String fileName : fileList) {
			try {
				URL url = workList.createURL(fileName);
				WorkListItem item = new WorkListItem(url);
				workList.add(item);
			} catch (MalformedURLException ignore) {
				// Ignore
			}
		}

		// Scan recursively.
		while (!workList.isEmpty()) {
			WorkListItem item = workList.getNextItem();
			processComponentJar(item.getURL(), workList, implicitClasspath);
		}

		return implicitClasspath;
	}

	/**
	 * Examine the manifest of a single zip/jar file for implicit
	 * classapth entries.
	 *
	 * @param jarFileURL        URL of the zip/jar file
	 * @param workList          worklist of zip/jar files to examine
	 * @param implicitClasspath list of implicit classpath entries found
	 */
	private void processComponentJar(URL jarFileURL, WorkList workList,
		List<String> implicitClasspath) {

		if (DEBUG) System.out.println("Processing " + jarFileURL.toString());

		if (!jarFileURL.toString().endsWith(".zip") && !jarFileURL.toString().endsWith(".jar"))
			return;

		try {
			URL manifestURL = new URL("jar:" + jarFileURL.toString() + "!/META-INF/MANIFEST.MF");

			InputStream in = null;
			try {
				in = manifestURL.openStream();
				Manifest manifest = new Manifest(in);

				Attributes mainAttrs = manifest.getMainAttributes();
				String classPath = mainAttrs.getValue("Class-Path");
				if (classPath != null) {
					String[] fileList = classPath.split("\\s+");

					for (String jarFile : fileList) {
						URL referencedURL = workList.createRelativeURL(jarFileURL, jarFile);
						if (workList.add(new WorkListItem(referencedURL))) {
							implicitClasspath.add(referencedURL.toString());
							if (DEBUG) System.out.println("Implicit jar: " + referencedURL.toString());
						}
					}
				}
			} finally {
				if (in != null) {
					in.close();
				}
			}
		} catch (IOException ignore) {
			// Ignore
		}
	}

	private static final String OPTIONS_KEY = "[Options]";
	private static final String JAR_FILES_KEY = "[Jar files]";
	private static final String SRC_DIRS_KEY = "[Source dirs]";
	private static final String AUX_CLASSPATH_ENTRIES_KEY = "[Aux classpath entries]";

	// Option keys
	public static final String RELATIVE_PATHS = "relative_paths";

	/**
	 * Save the project to an output file.
	 *
	 * @param outputFile       name of output file
	 * @param useRelativePaths true if the project should be written
	 *                         using only relative paths
	 * @param relativeBase     if useRelativePaths is true,
	 *                         this file is taken as the base directory in terms of which
	 *                         all files should be made relative
	 * @throws IOException if an error occurs while writing
	 */
	@Deprecated
	public void write(String outputFile, boolean useRelativePaths, String relativeBase)
			throws IOException {
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
		try {
			writer.println(JAR_FILES_KEY);
			for (String jarFile : fileList) {
				if (useRelativePaths)
					jarFile = convertToRelative(jarFile, relativeBase);
				writer.println(jarFile);
			}

			writer.println(SRC_DIRS_KEY);
			for (String srcDir : srcDirList) {
				if (useRelativePaths)
					srcDir = convertToRelative(srcDir, relativeBase);
				writer.println(srcDir);
			}

			writer.println(AUX_CLASSPATH_ENTRIES_KEY);
			for (String auxClasspathEntry : auxClasspathEntryList) {
				if (useRelativePaths)
					auxClasspathEntry = convertToRelative(auxClasspathEntry, relativeBase);
				writer.println(auxClasspathEntry);
			}

			if (useRelativePaths) {
				writer.println(OPTIONS_KEY);
				writer.println(RELATIVE_PATHS + "=true");
			}
		} finally {
			writer.close();
		}

		// Project successfully saved
		isModified = false;
	}

	public static Project readXML(File f) throws IOException, DocumentException, SAXException {
		Project project = new Project();
		InputStream in = new BufferedInputStream(new FileInputStream(f));
		String tag = Util.getXMLType(in);
		SAXBugCollectionHandler handler;
		if (tag.equals("Project")) {
			handler = new SAXBugCollectionHandler(project, f);
		} else if (tag.equals("BugCollection")) {
			SortedBugCollection bugs = new SortedBugCollection();
			handler = new SAXBugCollectionHandler(bugs, project, f);
		} else throw new IOException("Can't load a project from a " + tag + " file");
		try {
			XMLReader xr = null;
			if (true) try { 
				xr = XMLReaderFactory.createXMLReader();
			} catch (SAXException e) {
				AnalysisContext.logError("Couldn't create XMLReaderFactory", e);   
			}

			if (xr == null) {
				xr = new org.dom4j.io.aelfred.SAXDriver();
			}
			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);

			Reader reader = Util.getReader(in);

			xr.parse(new InputSource(reader));
		}
		finally {
			in.close();
		}

		// Presumably, project is now up-to-date
		project.setModified(false);

		return project;

	}
	public  void writeXML(File f) throws IOException {
		OutputStream out  = new FileOutputStream(f);
		XMLOutput xmlOutput = new OutputStreamXMLOutput(out);
		try {
			writeXML(xmlOutput);
		} finally {
			xmlOutput.finish();
		}
	}

	/**
	 * Read the project from an input file.
	 * This method should only be used on an empty Project
	 * (created with the default constructor).
	 *
	 * @param inputFile name of the input file to read the project from
	 * @throws IOException if an error occurs while reading
	 */
	public void read(String inputFile) throws IOException {
		if (isModified)
			throw new IllegalStateException("Reading into a modified Project!");

		// Make the input file absolute, if necessary
		File file = new File(inputFile);
		if (!file.isAbsolute())
			inputFile = file.getAbsolutePath();

		// Store the project filename
		setProjectFileName(inputFile);

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(Util.getFileReader(inputFile));
			String line;
			line = getLine(reader);

			if (line == null || !line.equals(JAR_FILES_KEY))
				throw new IOException("Bad format: missing jar files key");
			while ((line = getLine(reader)) != null && !line.equals(SRC_DIRS_KEY)) {
				addToListInternal(fileList, line);
			}

			if (line == null)
				throw new IOException("Bad format: missing source dirs key");
			while ((line = getLine(reader)) != null && !line.equals(AUX_CLASSPATH_ENTRIES_KEY)) {
				addToListInternal(srcDirList, line);
			}

			// The list of aux classpath entries is optional
			if (line != null) {
				while ((line = getLine(reader)) != null) {
					if (line.equals(OPTIONS_KEY))
						break;
					addToListInternal(auxClasspathEntryList, line);
				}
			}

			// The Options section is also optional
			if (line != null && line.equals(OPTIONS_KEY)) {
				while ((line = getLine(reader)) != null && !line.equals(JAR_FILES_KEY))
					parseOption(line);
			}

			// If this project has the relative paths option set,
			// resolve all internal relative paths into absolute
			// paths, using the absolute path of the project
			// file as a base directory.
			if (getOption(RELATIVE_PATHS)) {
				makeListAbsoluteProject(fileList);
				makeListAbsoluteProject(srcDirList);
				makeListAbsoluteProject(auxClasspathEntryList);
			}

			// Clear the modification flag set by the various "add" methods.
			isModified = false;
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	
	
	/**
	 * Read a line from a BufferedReader, ignoring blank lines
	 * and comments.
	 */
	private static String getLine(BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!line.equals("") && !line.startsWith("#"))
				break;
		}
		return line;
	}


	public String projectNameFromProjectFileName() {
		String name = projectFileName;
		int lastSep = name.lastIndexOf(File.separatorChar);
		if (lastSep >= 0)
			name = name.substring(lastSep + 1);
		int dot = name.lastIndexOf('.');
		if (dot >= 0)
			name = name.substring(0, dot);
		return name;

	}
	/**
	 * Convert to a string in a nice (displayable) format.
	 */
	@Override
	public String toString() {
		String name = projectFileName;
		int lastSep = name.lastIndexOf(File.separatorChar);
		if (lastSep >= 0)
			name = name.substring(lastSep + 1);
		//int dot = name.lastIndexOf('.');
		//Don't hide every suffix--some are informative and/or disambiguative.
		int dot = (name.endsWith(".fb") ? name.length()-3 : -1);
		if (dot >= 0)
			name = name.substring(0, dot);
		return name;
	}

	/**
	 * Transform a user-entered filename into a proper filename,
	 * by adding the ".fb" file extension if it isn't already present.
	 */
	public static String transformFilename(String fileName) {
		if (!fileName.endsWith(".fb"))
			fileName = fileName + ".fb";
		return fileName;
	}

	static final String JAR_ELEMENT_NAME = "Jar";
	static final String AUX_CLASSPATH_ENTRY_ELEMENT_NAME = "AuxClasspathEntry";
	static final String SRC_DIR_ELEMENT_NAME = "SrcDir";
	static final String FILENAME_ATTRIBUTE_NAME = "filename";
	static final String PROJECTNAME_ATTRIBUTE_NAME = "projectName";

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList().addAttribute(FILENAME_ATTRIBUTE_NAME, getProjectFileName());
		if (getProjectName() != null)
			attributeList = attributeList.addAttribute(PROJECTNAME_ATTRIBUTE_NAME, getProjectName());
		xmlOutput.openTag(
				BugCollection.PROJECT_ELEMENT_NAME,
				attributeList
				);

		XMLOutputUtil.writeElementList(xmlOutput, JAR_ELEMENT_NAME, fileList);
		XMLOutputUtil.writeElementList(xmlOutput, AUX_CLASSPATH_ENTRY_ELEMENT_NAME, auxClasspathEntryList);
		XMLOutputUtil.writeElementList(xmlOutput, SRC_DIR_ELEMENT_NAME, srcDirList);

		if (suppressionFilter != null && !suppressionFilter.isEmpty()) {
			xmlOutput.openTag("SuppressionFilter");
			suppressionFilter.writeBodyAsXML(xmlOutput);
			xmlOutput.closeTag("SuppressionFilter");
		}
		xmlOutput.closeTag(BugCollection.PROJECT_ELEMENT_NAME);
	}

	/**
	 * Parse one line in the [Options] section.
	 *
	 * @param option one line in the [Options] section
	 */
	private void parseOption(String option) throws IOException {
		int equalPos = option.indexOf("=");
		if (equalPos < 0)
			throw new IOException("Bad format: invalid option format");
		String name = option.substring(0, equalPos);
		String value = option.substring(equalPos + 1);
		optionsMap.put(name, Boolean.valueOf(value));
	}

	/**
	 * Hack for whether files are case insensitive.
	 * For now, we'll assume that Windows is the only
	 * case insensitive OS.  (OpenVMS users,
	 * feel free to submit a patch :-)
	 */
	private static final boolean FILE_IGNORE_CASE =
			SystemProperties.getProperty("os.name", "unknown").startsWith("Windows");

	/**
	 * Converts a full path to a relative path if possible
	 *
	 * @param srcFile path to convert
	 * @return the converted filename
	 */
	private String convertToRelative(String srcFile, String base) {
		String slash = SystemProperties.getProperty("file.separator");

		if (FILE_IGNORE_CASE) {
			srcFile = srcFile.toLowerCase();
			base = base.toLowerCase();
		}

		if (base.equals(srcFile))
			return ".";

		if (!base.endsWith(slash))
			base = base + slash;

		if (base.length() <= srcFile.length()) {
			String root = srcFile.substring(0, base.length());
			if (root.equals(base)) {
				// Strip off the base directory, make relative
				return "." + SystemProperties.getProperty("file.separator") + srcFile.substring(base.length());
			}
		}

		//See if we can build a relative path above the base using .. notation
		int slashPos = srcFile.indexOf(slash);
		int branchPoint;
		if (slashPos >= 0) {
			String subPath = srcFile.substring(0, slashPos);
			if ((subPath.length() == 0) || base.startsWith(subPath)) {
				branchPoint = slashPos + 1;
				slashPos = srcFile.indexOf(slash, branchPoint);
				while (slashPos >= 0) {
					subPath = srcFile.substring(0, slashPos);
					if (base.startsWith(subPath))
						branchPoint = slashPos + 1;
					else
						break;
					slashPos = srcFile.indexOf(slash, branchPoint);
				}

				int slashCount = 0;
				slashPos = base.indexOf(slash, branchPoint);
				while (slashPos >= 0) {
					slashCount++;
					slashPos = base.indexOf(slash, slashPos + 1);
				}

				StringBuffer path = new StringBuffer();
				String upDir = ".." + slash;
				for (int i = 0; i < slashCount; i++)
					path.append(upDir);
				path.append(srcFile.substring(branchPoint));
				return path.toString();
			}
		}


		return srcFile;

	}

	/**
	 * Converts a relative path to an absolute path if possible.
	 *
	 * @param fileName path to convert
	 * @return the converted filename
	 */
	private String convertToAbsolute(String fileName) throws IOException {
		// At present relative paths are only calculated if the fileName is
		// below the project file. This need not be the case, and we could use ..
		// syntax to move up the tree. (To Be Added)

		File file = new File(fileName);

		if (!file.isAbsolute()) {
			// Only try to make the relative path absolute
			// if the project file is absolute.
			File projectFile = new File(projectFileName);
			if (projectFile.isAbsolute()) {
				// Get base directory (parent of the project file)
				String base = new File(projectFileName).getParent();

				// Make the file absolute in terms of the parent directory
				fileName = new File(base, fileName).getCanonicalPath();
			}
		}
		return fileName;
	}


	/**
	 * Make the given filename absolute relative to the
	 * current working directory.
	 */
	private  String makeAbsoluteCWD(String fileName) {

		boolean hasProtocol = (URLClassPath.getURLProtocol(fileName) != null);
		if (hasProtocol) return fileName;

		if (new File(fileName).isAbsolute()) return fileName;
		return new File(currentWorkingDirectory, fileName).getAbsolutePath();

	}

	/**
	 * Add a value to given list, making the Project modified
	 * if the value is not already present in the list.
	 *
	 * @param list  the list
	 * @param value the value to be added
	 * @return true if the value was not already present in the list,
	 *         false otherwise
	 */
	private boolean addToListInternal(Collection<String> list, String value) {
		if (!list.contains(value)) {
			list.add(value);
			isModified = true;
			return true;
		} else
			return false;
	}

	/**
	 * Make the given list of pathnames absolute relative
	 * to the absolute path of the project file.
	 */
	private void makeListAbsoluteProject(List<String> list) throws IOException {
		List<String> replace = new LinkedList<String>();
		for (String fileName : list) {
			fileName = convertToAbsolute(fileName);
			replace.add(fileName);
		}

		list.clear();
		list.addAll(replace);
	}

	/**
	 * @param timestamp The timestamp to set.
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void addTimestamp(long timestamp) {
		if (this.timestamp < timestamp)
			this.timestamp = timestamp;
	}
	/**
	 * @return Returns the timestamp.
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @param projectName The projectName to set.
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/**
	 * @return Returns the projectName.
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
     * @param suppressionFilter The suppressionFilter to set.
     */
    public void setSuppressionFilter(Filter suppressionFilter) {
	    this.suppressionFilter = suppressionFilter;
    }

	/**
     * @return Returns the suppressionFilter.
     */
    public Filter getSuppressionFilter() {
    	if (suppressionFilter == null)
    		suppressionFilter = new Filter();
	    return suppressionFilter;
    }
}

// vim:ts=4

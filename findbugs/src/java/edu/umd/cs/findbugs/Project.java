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

/*
 * Project.java
 *
 * Created on March 30, 2003, 2:22 PM
 */

package edu.umd.cs.findbugs;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import org.dom4j.DocumentException;
import org.dom4j.Element;

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
public class Project {
	/**
	 * Project filename.
	 */
	private String projectFileName;

	/**
	 * Options.
	 */
	private Map<String, Boolean> optionsMap;

	/**
	 * The list of jar files.
	 */
	private LinkedList<String> jarList;

	/**
	 * The list of source directories.
	 */
	private LinkedList<String> srcDirList;

	/**
	 * The list of auxiliary classpath entries.
	 */
	private LinkedList<String> auxClasspathEntryList;

	/**
	 * Flag to indicate that this Project has been modified.
	 */
	private boolean isModified;

	/**
	 * Constant used to name anonymous projects.
	 */
	public static final String UNNAMED_PROJECT = "<<unnamed project>>";

	/**
	 * Create an anonymous project.
	 */
	public Project() {
		this.projectFileName = UNNAMED_PROJECT;
		optionsMap = new HashMap<String, Boolean>();
		optionsMap.put(RELATIVE_PATHS, Boolean.FALSE);
		jarList = new LinkedList<String>();
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
		dup.optionsMap.putAll(this.optionsMap);
		dup.jarList.addAll(this.jarList);
		dup.srcDirList.addAll(this.srcDirList);
		dup.auxClasspathEntryList.addAll(this.auxClasspathEntryList);

		return dup;
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
	public String getFileName() {
		return projectFileName;
	}

	/**
	 * Set the project filename.
	 *
	 * @param projectFileName the new filename
	 */
	public void setFileName(String projectFileName) {
		this.projectFileName = projectFileName;
	}

	/**
	 * Add a Jar file to the project.
	 *
	 * @param fileName the jar file to add
	 * @return true if the jar file was added, or false if the jar
	 *         file was already present
	 */
	public boolean addJar(String fileName) {
		return addToListInternal(jarList, makeAbsoluteCWD(fileName));
	}

	/*
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
	 * Get the number of jar files in the project.
	 *
	 * @return the number of jar files in the project
	 */
	public int getNumJarFiles() {
		return jarList.size();
	}

	/**
	 * Get the given Jar file.
	 *
	 * @param num the number of the jar file
	 * @return the name of the jar file
	 */
	public String getJarFile(int num) {
		return jarList.get(num);
	}

	/**
	 * Remove jar file at given index.
	 *
	 * @param num index of the jar file to remove
	 */
	public void removeJarFile(int num) {
		jarList.remove(num);
		isModified = true;
	}

	/**
	 * Get the list of jar files, directories, and zip file.
	 */
	public List<String> getJarFileList() {
		return jarList;
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
	 * Get Jar files as an array of Strings.
	 */
	public String[] getJarFileArray() {
		return (String[]) jarList.toArray(new String[jarList.size()]);
	}

	/**
	 * Get source dirs as an array of Strings.
	 */
	public String[] getSourceDirArray() {
		return (String[]) srcDirList.toArray(new String[srcDirList.size()]);
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
	 * Return the list of implicit classpath entries.  The implicit
	 * classpath is computed from the closure of the set of jar files
	 * that are referenced by the <code>"Class-Path"</code> attribute
	 * of the manifest of the any jar file that is part of this project
	 * or by the <code>"Class-Path"</code> attribute of any directly or
	 * indirectly referenced jar.  The referenced jar files that exist
	 * are the list of implicit classpath entries.
	 */
	public List getImplicitClasspathEntryList() {
		final HashSet<File> processedJars = new HashSet<File>();
		final LinkedList<String> implicitClasspath = new LinkedList<String>();

		for (Iterator<String> i = jarList.iterator(); i.hasNext();) {
			String fileName = i.next();

			if (!fileName.endsWith(".zip") && !fileName.endsWith(".jar"))
				continue;

			final File jarFile = new File(fileName);

			if (jarFile.isFile() && !processedJars.contains(jarFile)) {
				// Add the name of the jar to the list of processed jars to
				// avoid recursion.
				processedJars.add(jarFile);

				processComponentJar(jarFile, processedJars, implicitClasspath);
			}
		}

		return implicitClasspath;
	}

	/**
	 * Get the parent directory of the specified file.  The method attempts
	 * to determine the canonical name of the specified path and then
	 * returns the parent directory.  If unable to determine the canonical
	 * name of the path, then the absolute form of the path is used.  If
	 * no parent directory can be determined from the specified path, then
	 * the current directory of the JVM, as specified in the system property
	 * <code>"user.dir"</code>, is used.
	 *
	 * @param file the file whose parent directory is to be determined
	 * @return the file for the parent directory
	 */
	private static File getParentFile(File file) {
		try {
			file = file.getCanonicalFile();
		} catch (IOException unused) {
			file = file.getAbsoluteFile();
		}

		File parent = file.getParentFile();

		if (parent == null) {
			parent = new File(System.getProperty("user.dir", "."));
		}

		return parent;
	}

	/**
	 * Get the <code>Class-Path</code> attribute for the specified jar file.
	 *
	 * @param jarFile the file to process
	 * @return the classpath for the specified jar file, may be
	 *         <code>null</code>
	 */
	private static String getClassPath(final File jarFile) {
		String result = null;

		if (jarFile.isFile()) {
			try {
				final JarFile jar = new JarFile(jarFile);
				final Manifest manifest = jar.getManifest();
				if (manifest != null) {
					final Attributes mainAttrs = manifest.getMainAttributes();
					result = mainAttrs.getValue("Class-Path");
				}
			} catch (IOException ioExc) {
				System.err.println("Unable to access Jar file: " + jarFile +
				        ", exc = " + ioExc);
			}
		} else {
			System.err.println("Missing Jar file: " + jarFile);
		}

		return result;
	}

	/**
	 * Process the specified jar file and add any new jar files on which it
	 * depends to the list of implicit classpath entries.
	 */
	private void processComponentJar(final File jar, final HashSet<File> processedJars,
	                                 final LinkedList<String> implicitClasspath) {
		String jarClassPath = getClassPath(jar);

		if (jarClassPath != null) {
			final File baseDir = getParentFile(jar);

			jarClassPath = jarClassPath.trim();
			if (jarClassPath.length() > 0) {
				final String[] jarList = jarClassPath.split("\\s+");

				for (int i = 0; i < jarList.length; ++i) {
					final String curJarName = jarList[i];
					final File curJar = new File(baseDir, curJarName);

					if (curJar.isFile() && !processedJars.contains(curJar)) {
						final String curJarPath = curJar.toString();

						processedJars.add(curJar);
						if (!implicitClasspath.contains(curJarPath)) {
							implicitClasspath.add(curJarPath);
						}
						processComponentJar(curJar, processedJars, implicitClasspath);
					}
				}
			}
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
	public void write(String outputFile, boolean useRelativePaths, String relativeBase)
	        throws IOException {
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
		try {
			writer.println(JAR_FILES_KEY);
			for (Iterator<String> i = jarList.iterator(); i.hasNext();) {
				String jarFile = i.next();
				if (useRelativePaths)
					jarFile = convertToRelative(jarFile, relativeBase);
				writer.println(jarFile);
			}

			writer.println(SRC_DIRS_KEY);
			for (Iterator<String> i = srcDirList.iterator(); i.hasNext();) {
				String srcDir = i.next();
				if (useRelativePaths)
					srcDir = convertToRelative(srcDir, relativeBase);
				writer.println(srcDir);
			}

			writer.println(AUX_CLASSPATH_ENTRIES_KEY);
			for (Iterator<String> i = auxClasspathEntryList.iterator(); i.hasNext();) {
				String auxClasspathEntry = i.next();
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
		setFileName(inputFile);

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(inputFile));
			String line;
			line = getLine(reader);

			if (line == null || !line.equals(JAR_FILES_KEY))
				throw new IOException("Bad format: missing jar files key");
			while ((line = getLine(reader)) != null && !line.equals(SRC_DIRS_KEY)) {
				addToListInternal(jarList, line);
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
				makeListAbsoluteProject(jarList);
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

	/**
	 * Convert to a string in a nice (displayable) format.
	 */
	public String toString() {
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
	 * Transform a user-entered filename into a proper filename,
	 * by adding the ".fb" file extension if it isn't already present.
	 */
	public static String transformFilename(String fileName) {
		if (!fileName.endsWith(".fb"))
			fileName = fileName + ".fb";
		return fileName;
	}

	private static final String JAR_ELEMENT_NAME = "Jar";
	private static final String AUX_CLASSPATH_ENTRY_ELEMENT_NAME = "AuxClasspathEntry";
	private static final String SRC_DIR_ELEMENT_NAME = "SrcDir";
	private static final String FILENAME_ATTRIBUTE_NAME = "filename";

	/**
	 * Populate Project object from a <code>&lt;Project&gt;</code> element
	 * in a FindBugs saved bugs XML file.
	 */
	public void readElement(Element element) throws DocumentException {
		Iterator i = element.elements().iterator();

		String projectName = element.attributeValue(FILENAME_ATTRIBUTE_NAME);
		if (projectName != null)
			projectFileName = projectName;
		else
			projectFileName = UNNAMED_PROJECT;

		while (i.hasNext()) {
			Element child = (Element) i.next();
			String name = child.getName();
			String text = child.getText();
			if (name.equals(JAR_ELEMENT_NAME))
				addJar(text);
			else if (name.equals(AUX_CLASSPATH_ENTRY_ELEMENT_NAME))
				addAuxClasspathEntry(text);
			else if (name.equals(SRC_DIR_ELEMENT_NAME))
				addSourceDir(text);
			else
				throw new DocumentException("Unknown project node: " + name);
		}
	}

	/**
	 * Popular an XML Element from Project object.
	 */
	public void writeElement(Element element) {
		element.addAttribute(FILENAME_ATTRIBUTE_NAME, projectFileName);

		for (Iterator<String> i = jarList.iterator(); i.hasNext();) {
			element.addElement(JAR_ELEMENT_NAME).setText(i.next());
		}

		for (Iterator<String> i = auxClasspathEntryList.iterator(); i.hasNext();) {
			element.addElement(AUX_CLASSPATH_ENTRY_ELEMENT_NAME).setText(i.next());
		}

		for (Iterator<String> i = srcDirList.iterator(); i.hasNext();) {
			element.addElement(SRC_DIR_ELEMENT_NAME).setText(i.next());
		}
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
	        System.getProperty("os.name", "unknown").startsWith("Windows");

	/**
	 * Converts a full path to a relative path if possible
	 *
	 * @param srcFile path to convert
	 * @return the converted filename
	 */
	private String convertToRelative(String srcFile, String base) {
		String slash = System.getProperty("file.separator");

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
				return "." + System.getProperty("file.separator") + srcFile.substring(base.length());
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
	private static String makeAbsoluteCWD(String fileName) {
		File file = new File(fileName);
		boolean hasProtocol = (FindBugs.getURLProtocol(fileName) != null);
		if (!hasProtocol && !file.isAbsolute())
			fileName = file.getAbsolutePath();
		return fileName;
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
	private boolean addToListInternal(List<String> list, String value) {
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
		for (Iterator<String> i = list.iterator(); i.hasNext();) {
			String fileName = i.next();
			fileName = convertToAbsolute(fileName);
			replace.add(fileName);
		}

		list.clear();
		list.addAll(replace);
	}
}

// vim:ts=4

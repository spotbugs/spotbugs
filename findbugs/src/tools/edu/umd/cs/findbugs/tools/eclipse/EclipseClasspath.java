/*
 * Generate a Java classpath from an Eclipse plugin.xml file
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

package edu.umd.cs.findbugs.tools.eclipse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;

import org.dom4j.io.SAXReader;

/**
 * Starting from an Eclipse plugin, finds all required plugins
 * (in an Eclipse installation) and recursively finds the classpath
 * required to compile the original plugin.  Different Eclipse
 * releases will generally have different version numbers on the
 * plugins they contain, which makes this task slightly difficult.
 *
 * <p> Basically, this is a big complicated hack to allow compilation
 * of the FindBugs Eclipse plugin outside of the Eclipse workspace,
 * in a way that doesn't depend on any specific release of Eclipse.
 *
 * @author David Hovemeyer
 */
public class EclipseClasspath {

	public static class EclipseClasspathException extends Exception {
		public EclipseClasspathException(String msg) {
			super(msg);
		}
	}

	/**
	 * A customized Reader for Eclipse plugin descriptor files.
	 * The problem is that Eclipse uses invalid XML in the form of
	 * directives like
	 * <pre>
	 * &lt;?eclipse version="3.0"?&gt;
	 * </pre>
	 * outside of the root element.  This Reader class
	 * filters out this crap.
	 */
	private static class EclipseXMLReader extends Reader {
		private BufferedReader reader;
		private LinkedList<String> lineList;

		public EclipseXMLReader(Reader reader) {
			this.reader = new BufferedReader(reader);
			this.lineList = new LinkedList<String>();
		}

		public int read(char[] cbuf, int off, int len) throws IOException {
			if (!fill())
				return -1;
			String line = lineList.getFirst();
			if (len > line.length())
				len = line.length();
			for (int i = 0; i < len; ++i)
				cbuf[i+off] = line.charAt(i);
			if (len == line.length())
				lineList.removeFirst();
			else
				lineList.set(0, line.substring(len));
			return len;
		}

		public void close() throws IOException {
			reader.close();
		}

		private boolean fill() throws IOException {
			if (!lineList.isEmpty())
				return true;

			String line;
			do {
				line = reader.readLine();
				if (line == null)
					return false;
			} while (isIllegal(line));
			lineList.add(line+"\n");
			return true;
		}

		private boolean isIllegal(String line) {
			return line.startsWith("<?eclipse");
		}
	}

	private class Plugin {
		private String directory;
		private Document document;
		private boolean isDependent;
		private String pluginId;
		private String pluginVersion;
		private List<String> requiredPluginIdList;
		private List<String> exportedLibraryList;

		public Plugin(String directory, Document document, boolean isDependent)
				throws DocumentException, EclipseClasspathException {
			this.directory = directory;
			this.document = document;
			this.isDependent = isDependent;

			// Get the plugin id
			Node plugin = document.selectSingleNode("/plugin");
			if (plugin == null)
				throw new EclipseClasspathException("No plugin node in plugin descriptor");

			if (!plugin.valueOf("@id").equals("")) {
				parseOldPluginDescriptor(directory, plugin, isDependent);
			} else {
				parseNewPluginDescriptor(directory, plugin, isDependent);
			}
		}

		public String getDirectory() {
			return directory;
		}

		public boolean isDependent() {
			return isDependent;
		}

		public String getId() {
			return pluginId;
		}

		public String getVersion() {
			return pluginVersion;
		}

		public Iterator<String> requiredPluginIdIterator() {
			return requiredPluginIdList.iterator();
		}

		public Iterator<String> exportedLibraryIterator() {
			return exportedLibraryList.iterator();
		}

		private void parseOldPluginDescriptor(String directory, Node plugin, boolean isDependent)
			throws DocumentException, EclipseClasspathException {
			// In Eclipse 2.1.x, all of the information we need
			// is in plugin.xml.

			pluginId = plugin.valueOf("@id");
			//System.out.println("Plugin id is " + pluginId);
			pluginVersion = plugin.valueOf("@version");
			if (pluginVersion.equals(""))
				throw new EclipseClasspathException("Cannot determine plugin version");

			// Extract required plugins
			requiredPluginIdList = new LinkedList<String>();
			List requiredPluginNodeList = document.selectNodes("/plugin/requires/import");
			for (Iterator i = requiredPluginNodeList.iterator(); i.hasNext(); ) {
				Node node = (Node) i.next();
				String requiredPluginId = node.valueOf("@plugin");
				if (requiredPluginId.equals(""))
					throw new EclipseClasspathException("Import has no plugin id");
				//System.out.println(" Required plugin ==> " + requiredPluginId);
				requiredPluginIdList.add(requiredPluginId);
			}

			// Extract exported libraries
			exportedLibraryList = new LinkedList<String>();
			List exportedLibraryNodeList = document.selectNodes("/plugin/runtime/library");
			for (Iterator i = exportedLibraryNodeList.iterator(); i.hasNext(); ) {
				Node node = (Node) i.next();
				String jarName = node.valueOf("@name");
				if (jarName.equals(""))
					throw new EclipseClasspathException("Could not get name of exported library");

				jarName = replaceSpecial(jarName);
				File jarFile = new File(jarName);
				if (!jarFile.isAbsolute()) {
					// Make relative to plugin directory
					jarFile = new File(directory, jarName);
				}
				exportedLibraryList.add(jarFile.getPath());
			}
		}

		private void parseNewPluginDescriptor(String directory, Node plugin, boolean isDependent)
			throws DocumentException, EclipseClasspathException {
			// In Eclipse 3.x, we need to parse the plugin's MANIFEST.MF

			throw new EclipseClasspathException(
				"FIXME: support parsing Eclipse 3.0 plugin manifest for " + directory);
		}
	}

	private static final Pattern pluginDirPattern = Pattern.compile("^(\\w+(\\.\\w+)*)_(\\w+(\\.\\w+)*)$");
	private static final int PLUGIN_ID_GROUP = 1;

	private String eclipseDir;
	private String rootPluginDir;
	private Map<String, File> pluginDirectoryMap;
	private Map<String, String> varMap;
	private Plugin rootPlugin;
	private List<String> importList;

	public EclipseClasspath(String eclipseDir, String rootPluginDir) {
		this.eclipseDir = eclipseDir;
		this.rootPluginDir = rootPluginDir;
		this.pluginDirectoryMap = new HashMap<String, File>();
		this.varMap = new HashMap<String, String>();
	}

	public void addRequiredPlugin(String pluginId, String pluginDir) {
		pluginDirectoryMap.put(pluginId, new File(pluginDir));
	}

	private static class WorkListItem {
		private String directory;
		private boolean isDependent;

		public WorkListItem(String directory, boolean isDependent) {
			this.directory = directory;
			this.isDependent = isDependent;
		}

		public String getDirectory() { return directory; }
		public boolean isDependent() { return isDependent; }

		public String getDescriptorFileName() {
			return new File(directory, "plugin.xml").getPath();
		}
	}

	public EclipseClasspath execute() throws EclipseClasspathException, DocumentException, IOException {

		// Build plugin directory map
		File pluginDir = new File(eclipseDir, "plugins");
		File[] dirList = pluginDir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		if (dirList == null)
			throw new EclipseClasspathException("Could not list plugins in directory " + pluginDir);
		for (int i = 0; i < dirList.length; ++i) {
			String dirName = dirList[i].getName();
			String pluginId = getPluginId(dirName);
			if (pluginId != null) {
				//System.out.println(pluginId + " ==> " + dirList[i]);
				pluginDirectoryMap.put(pluginId, dirList[i]);

				// HACK - see if we can deduce the value of the special "ws" variable.
				if (pluginId.startsWith("org.eclipse.swt.")) {
					String ws = pluginId.substring("org.eclipse.swt.".length());
					varMap.put("ws", new File(dirList[i] + File.separator + "ws" + File.separator + ws).getPath().replace('\\', '/'));
				}
			}
		}

		Map<String, Plugin> requiredPluginMap = new HashMap<String, Plugin>();

		LinkedList<WorkListItem> workList = new LinkedList<WorkListItem>();
		workList.add(new WorkListItem(rootPluginDir, false));

		while (!workList.isEmpty()) {
			WorkListItem item = workList.removeFirst();

			// Read the plugin file
			SAXReader reader = new SAXReader();
			Document doc = reader.read(new EclipseXMLReader(new FileReader(item.getDescriptorFileName())));

			// Add to the map
			Plugin plugin = new Plugin(item.getDirectory(), doc, item.isDependent());
			requiredPluginMap.put(plugin.getId(), plugin);

			if (!plugin.isDependent()) {
				if (rootPlugin != null) throw new IllegalStateException("multiple root plugins");
				this.rootPlugin = plugin;
			}

			// Add unresolved required plugins to the worklist
			for (Iterator<String> i = plugin.requiredPluginIdIterator(); i.hasNext(); ) {
				String requiredPluginId = i.next();
				if (requiredPluginMap.get(requiredPluginId) == null) {
					// Find the plugin's directory
					File requiredPluginDir = pluginDirectoryMap.get(requiredPluginId);
					if (requiredPluginDir == null)
						throw new EclipseClasspathException("Unable to find plugin " + requiredPluginId);
					workList.add(new WorkListItem(requiredPluginDir.getPath(), true));
				}
			}
		}

		//System.out.println("Found " + requiredPluginMap.size() + " required plugins");

		importList = new LinkedList<String>();
		for (Iterator<Plugin> i = requiredPluginMap.values().iterator(); i.hasNext(); ) {
			Plugin plugin = i.next();
			if (plugin.isDependent()) {
				for (Iterator<String> j = plugin.exportedLibraryIterator(); j.hasNext(); ) {
					//System.out.println("Import: " + j.next());
					importList.add(j.next());
				}
			}
		}

		return this;
	}

	public Iterator<String> importListIterator() {
		return importList.iterator();
	}

	public String getClasspath() {
		if (importList == null) throw new IllegalStateException("execute() has not been called");

		StringBuffer buf = new StringBuffer();
		boolean first = true;

		Iterator <String> i = importListIterator();
		while (i.hasNext()) {
			if (first)
				first = false;
			else
				buf.append(File.pathSeparator);

			buf.append(i.next());
		}

		// Convert backslashes to forward slashes,
		// since raw backslashes cause problems in .properties files.
		String s = buf.toString();
		s = s.replace('\\', '/');

		return s;
	}

	public Plugin getRootPlugin() {
		return rootPlugin;
	}

	/**
	 * Get the plugin id for given directory name.
	 * Returns null if the directory name does not seem to
	 * be a plugin.
	 */
	private String getPluginId(String dirName) {
		Matcher m = pluginDirPattern.matcher(dirName);
		return m.matches() ? m.group(PLUGIN_ID_GROUP) : null;
	}

	/**
	 * Expand variables of the form $varname$ in library names.
	 * This is used to handle the "$ws$" substitution used to refer to
	 * the plugin containing swt.jar.
	 */
	private String replaceSpecial(String value) {
		if (!varMap.isEmpty()) {
			for (Iterator<String> i = varMap.keySet().iterator(); i.hasNext(); ) {
				String key = i.next();
				String replace = varMap.get(key);
				Pattern pat = Pattern.compile("\\$\\Q" + key + "\\E\\$");
				Matcher m = pat.matcher(value);
				StringBuffer buf = new StringBuffer();
	
				while (m.find())
					m.appendReplacement(buf, replace);
				m.appendTail(buf);
	
				value = buf.toString();
			}
		}
		return value;
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length < 2 || (argv.length % 2 != 0)) {
			System.err.println("Usage: " + EclipseClasspath.class.getName() +
				" <eclipse dir> <root plugin directory> [<required plugin id> <required plugin dir> ...]");
			System.exit(1);
		}

		EclipseClasspath ec = new EclipseClasspath(argv[0], argv[1]);
		for (int i = 2; i < argv.length; i += 2) {
			ec.addRequiredPlugin(argv[i], new File(argv[i+1]).getAbsolutePath());
		}

		// Generate a build.properties file which communicates to Ant:
		//   - what the build classpath should be
		//   - what the plugin id and version are
		ec.execute();
		System.out.println("plugin.build.classpath=" + ec.getClasspath());
		System.out.println("plugin.id=" + ec.getRootPlugin().getId());
		System.out.println("plugin.version=" + ec.getRootPlugin().getVersion());
	}
}

// vim:ts=4

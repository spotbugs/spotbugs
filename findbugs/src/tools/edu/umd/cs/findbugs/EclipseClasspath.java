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

package edu.umd.cs.findbugs;

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

	private static class Plugin {
		private Document document;
		private String pluginId;
		private List<String> requiredPluginIdList;

		public Plugin(Document document) throws DocumentException, EclipseClasspathException {
			this.document = document;

			// Get the plugin id
			Node plugin = document.selectSingleNode("/plugin");
			if (plugin == null)
				throw new EclipseClasspathException("No plugin node in plugin descriptor");
			pluginId = plugin.valueOf("@id");
			if (pluginId.equals(""))
				throw new EclipseClasspathException("Cannot determine plugin id");
			System.out.println("Plugin id is " + pluginId);

			// Extract required plugins
			requiredPluginIdList = new LinkedList<String>();
			List requiredPluginNodeList = document.selectNodes("/plugin/requires/import");
			for (Iterator i = requiredPluginNodeList.iterator(); i.hasNext(); ) {
				Node node = (Node) i.next();
				String requiredPluginId = node.valueOf("@plugin");
				if (requiredPluginId.equals(""))
					throw new EclipseClasspathException("Import has no plugin id");
				System.out.println(" Required plugin ==> " + requiredPluginId);
				requiredPluginIdList.add(requiredPluginId);
			}
		}

		public String getId() {
			return pluginId;
		}

		public Iterator<String> requiredPluginIdIterator() {
			return requiredPluginIdList.iterator();
		}
	}

	private static final Pattern pluginDirPattern = Pattern.compile("^(\\w+(\\.\\w+)*)_(\\w+(\\.\\w+)*)$");
	private static final int PLUGIN_ID_GROUP = 1;

	private String eclipseDir;
	private String pluginFile;
	private Map<String, File> pluginDirectoryMap;

	public EclipseClasspath(String eclipseDir, String pluginFile) {
		this.eclipseDir = eclipseDir;
		this.pluginFile = pluginFile;
		this.pluginDirectoryMap = new HashMap<String, File>();
	}

	public void addRequiredPlugin(String pluginId, String pluginDir) {
		pluginDirectoryMap.put(pluginId, new File(pluginDir));
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
			String pluginId = getPluginId(dirList[i].getName());
			if (pluginId != null) {
				//System.out.println(pluginId + " ==> " + dirList[i]);
				pluginDirectoryMap.put(pluginId, dirList[i]);
			}
		}

		Map<String, Plugin> requiredPluginMap = new HashMap<String, Plugin>();

		LinkedList<String> workList = new LinkedList<String>();
		workList.add(pluginFile);

		while (!workList.isEmpty()) {
			String descriptor = workList.removeFirst();

			// Read the plugin file
			SAXReader reader = new SAXReader();
			Document doc = reader.read(new EclipseXMLReader(new FileReader(descriptor)));

			// Add to the map
			Plugin plugin = new Plugin(doc);
			requiredPluginMap.put(plugin.getId(), plugin);

			// Add unresolved required plugins to the worklist
			for (Iterator<String> i = plugin.requiredPluginIdIterator(); i.hasNext(); ) {
				String requiredPluginId = i.next();
				if (requiredPluginMap.get(requiredPluginId) == null) {
					// Find the plugin in the Eclipse directory
					File requiredPluginDir = pluginDirectoryMap.get(requiredPluginId);
					if (requiredPluginDir == null)
						throw new EclipseClasspathException("Unable to find plugin " + requiredPluginId);
					workList.add(new File(requiredPluginDir, "plugin.xml").getPath());
				}
			}
		}

		System.out.println("Found " + requiredPluginMap.size() + " required plugins");

		return this;
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

	public static void main(String[] argv) throws Exception {
		if (argv.length < 2 || (argv.length % 2 != 0)) {
			System.err.println("Usage: " + EclipseClasspath.class.getName() +
				" <eclipse dir> <plugin file> [<required plugin id> <required plugin dir> ...]");
			System.exit(1);
		}

		EclipseClasspath ec = new EclipseClasspath(argv[0], argv[1]);
		for (int i = 2; i < argv.length; i += 2) {
			ec.addRequiredPlugin(argv[i], argv[i+1]);
		}
		ec.execute();
	}
}

// vim:ts=4

/*
 * Generate a Java classpath from an Eclipse plugin.xml file
 * Copyright (C) 2003, University of Maryland
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

import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

	private static class Plugin {
		private Document document;
		private String pluginId;
		private List<String> requiredPluginIdList;

		public Plugin(Document document) throws DocumentException {
			this.document = document;

			// Get the plugin id
			Node plugin = document.selectSingleNode("//plugin");
			if (plugin == null)
				throw new DocumentException("No plugin node in plugin descriptor");
			pluginId = plugin.valueOf("@id");
			if (pluginId.equals(""))
				throw new DocumentException("Cannot determine plugin id");

			// Extract required plugins
			requiredPluginIdList = new LinkedList<String>();
			List requiredPluginNodeList = document.selectNodes("//plugin/requires/import");
			for (Iterator i = requiredPluginIdList.iterator(); i.hasNext(); ) {
				Node node = (Node) i.next();
				String requiredPluginId = node.valueOf("@plugin");
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

	private String eclipseDir;
	private String pluginFile;

	public EclipseClasspath(String eclipseDir, String pluginFile) {
		this.eclipseDir = eclipseDir;
		this.pluginFile = pluginFile;
	}

	public EclipseClasspath execute() throws DocumentException, MalformedURLException {
		Map<String, Plugin> pluginMap = new HashMap<String, Plugin>();

		List workList = new LinkedList<String>();
		workList.add(pluginFile);

		while (!workList.isEmpty()) {
			// Read the plugin file
			SAXReader reader = new SAXReader();
			Document doc = reader.read(new URL("file:" + pluginFile));

			// Add to the map
			Plugin plugin = new Plugin(doc);
			pluginMap.put(plugin.getId(), plugin);
		}

		return this;
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 2) {
			System.err.println("Usage: " + EclipseClasspath.class.getName() +
				" <eclipse dir> <plugin file>");
			System.exit(1);
		}

		EclipseClasspath ec = new EclipseClasspath(argv[0], argv[1]);
		ec.execute();
	}
}

// vim:ts=4

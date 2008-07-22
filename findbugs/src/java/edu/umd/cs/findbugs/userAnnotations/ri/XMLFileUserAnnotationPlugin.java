/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.userAnnotations.ri;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.userAnnotations.Plugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Reference implementation of user annotation plugin.
 * Persists user annotations using instance hash as a key,
 * storing the user annotations in an XML file.
 * This plugin is mostly a proof of concept, although it
 * might be more generally useful.
 * 
 * @author David Hovemeyer
 */
public class XMLFileUserAnnotationPlugin implements Plugin {
	private Map<String, String> properties;
	
	public XMLFileUserAnnotationPlugin() {
		this.properties = new HashMap<String, String>();
	}

	public Set<String> getPropertyNames() {
		TreeSet<String> names = new TreeSet<String>();
		names.add("filename");
		return names;
	}

	public boolean setProperties(Map<String, String> properties) {
		this.properties.clear();
		this.properties.putAll(properties);
		return true;
	}

	public void loadUserAnnotations(BugCollection bugs) {
		try {
			Document document = readXMLFile();
			
			// Build map of instance hashes to BugInstances
			Map<String, BugInstance> instanceHashToBugInstanceMap = new HashMap<String, BugInstance>();
			for (Iterator<BugInstance> i = bugs.iterator(); i.hasNext(); ) {
				BugInstance bugInstance = i.next();
				instanceHashToBugInstanceMap.put(bugInstance.getInstanceHash(), bugInstance);
			}

			// Read entries from the XML file.
			// Load the designations and annotations and
			// apply them to the BugInstances.
			List<?> entries = document.selectNodes("/FindBugsUserAnnotations/Entry");
			Iterator<?> i = entries.iterator();
			while (i.hasNext()) {
				Element entry = (Element) i.next();
				
				String instanceHash = safeGetAttribute(entry, "hash");
				String designationKey = safeGetAttribute(entry, "designation");
				String annotationText = safeGetText(entry);
				
				if (!I18N.instance().getUserDesignationKeys().contains(designationKey)) {
					designationKey = "UNCLASSIFIED";
				}
				
				BugInstance bugInstance = instanceHashToBugInstanceMap.get(instanceHash);
				if (bugInstance != null) {
					BugDesignation bugDesignation = bugInstance.getNonnullUserDesignation();
					bugDesignation.setDesignationKey(designationKey);
					bugDesignation.setAnnotationText(annotationText);
				}
			}
		} catch (FileNotFoundException e) {
			// File does not exist.
			// So, nothing happens, since this is equivalent to saying that
			// there are no saved user annotations.
			// (Should something happen?)
			return;
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Could not parse XML file", e);
		} catch (DocumentException e) {
			throw new IllegalArgumentException("Could not parse XML file", e);
		}
	}

	public void storeUserAnnotation(BugInstance bug) {
		Map<String, BugDesignation> toUpdate = new HashMap<String, BugDesignation>();
		toUpdate.put(bug.getInstanceHash(), bug.getUserDesignation());
		
		storeAnnotations(toUpdate);
	}

	public void storeUserAnnotations(BugCollection bugs) {
		Map<String, BugDesignation> toUpdate = new HashMap<String, BugDesignation>();
		for (Iterator<BugInstance> i = bugs.iterator(); i.hasNext(); ){
			BugInstance bugInstance = i.next();
			if (bugInstance.getUserDesignation() != null) {
				toUpdate.put(bugInstance.getInstanceHash(), bugInstance.getUserDesignation());
			}
		}
		
		storeAnnotations(toUpdate);
	}

	private Document readXMLFile() throws MalformedURLException, DocumentException, FileNotFoundException {
		Document document;

		if (!properties.containsKey("filename")) {
			throw new IllegalArgumentException("Required property 'filename' is not set");
		}
		
		File f = new File(properties.get("filename"));
		if (!f.exists()) {
			throw new FileNotFoundException("Could not open XML file " + f.getPath());
		}
		
		SAXReader reader = new SAXReader();
		URL url = new URL("file:///" + properties.get("filename"));
		document = reader.read(url);

		return document;
	}

	private String safeGetAttribute(Element entry, String attrName) {
		Node attrNode = entry.selectSingleNode("@" + attrName);
		return attrNode != null ? attrNode.getText() : "";
	}
	
	private String safeGetText(Node node) {
		String text = node.getText();
		return text != null ? text : "";
	}

	private void storeAnnotations(Map<String, BugDesignation> toUpdate) {
		Document document;
		
		try {
			document = readXMLFile();
		} catch (FileNotFoundException f) {
			// Create empty document
			document = DocumentHelper.createDocument();
			document.addElement("FindBugsUserAnnotations");
		} catch (DocumentException e) {
			throw new IllegalArgumentException("Could not parse XML file", e);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Could not parse XML file", e);
		}
		
		// Update all of the existing entries
		Set<String> updatedEntries = new HashSet<String>();
		List<?> entries = document.selectNodes("/FindBugsUserAnnotations/Entry");
		for (Iterator<?> i = entries.iterator(); i.hasNext(); ) {
			Element entry = (Element) i.next();

			String instanceHash = safeGetAttribute(entry, "hash");
			
			if (toUpdate.containsKey(instanceHash)) {
				BugDesignation bugDesignation = toUpdate.get(instanceHash);
				entry.addAttribute("designation", bugDesignation.getDesignationKey());
				entry.setText(bugDesignation.getAnnotationText());
				updatedEntries.add(instanceHash);
			}

		}
		
		// Add new entries for the new instance hashes (not previously recorded)
		for (Map.Entry<String, BugDesignation> updateEntry : toUpdate.entrySet()) {
			String instanceHash = updateEntry.getKey();
			if (!updatedEntries.contains(instanceHash)) {
				BugDesignation bugDesignation = updateEntry.getValue();
				
				Element entry = document.getRootElement().addElement("Entry");
				entry.addAttribute("hash", instanceHash);
				entry.addAttribute("designation", bugDesignation.getDesignationKey());
				entry.setText(bugDesignation.getAnnotationText());
			}
		}
		
		// Save the file (destructively overwriting previous file)
		XMLWriter writer = null;
		try {
			FileWriter fw = new FileWriter(properties.get("filename"));
			OutputFormat format = OutputFormat.createPrettyPrint();
			writer = new XMLWriter(fw, format);
			writer.write(document);
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not write XML file", e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

}

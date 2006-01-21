/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Report warnings as an XML document.
 * To the extent possible, warnings are written incrementally.
 * So, if the analysis is terminated unexpectedly, the output
 * may still be salvaged.
 * 
 * @author David Hovemeyer
 */
public class XMLBugReporter extends BugCollectionBugReporter {
	private boolean addMessages;
	private boolean started;
	private boolean sorted = true;
	private XMLOutput xmlOutput;

	public XMLBugReporter(Project project) {
		super(project);
		this.addMessages = false;
		this.started = false;
	}

	public void setAddMessages(boolean enable) {
		this.addMessages = enable;
	}

	//@Override
	public void doReportBug(BugInstance bugInstance) {
		// Add it to the bug collection and notify observers
		super.doReportBug(bugInstance);
		
		// Write it to output
		if (!sorted) 
			try {
			getReady();
			bugInstance.writeXML(xmlOutput, addMessages);
		} catch (IOException e) {
			throw new FatalException("Error writing XML output", e);
		}
	}

	private void getReady() throws IOException {
		if (!started) {
			started = true;
			xmlOutput = new OutputStreamXMLOutput(outputStream);
			getBugCollection().writePrologue(xmlOutput, getProject());
		}
	}
	
	public void computeBugHashes() {
		MessageDigest digest = null;
		try { digest = MessageDigest.getInstance("MD5");
		} catch (Exception e2) {
			// OK, we won't digest
		}
		
		HashMap<String, Integer> seen = new HashMap<String, Integer>();
		for(BugInstance bugInstance : getBugCollection().getCollection()) {
			String hash = bugInstance.getInstanceKey();
			if (digest != null) {
				byte [] data = digest.digest(hash.getBytes());
				hash = new BigInteger(1,data).toString(16);
			}
			bugInstance.setInstanceHash(hash);
			Integer count = seen.get(hash);
			if (count == null) {
				bugInstance.setInstanceOccurrenceNum(0);
				seen.put(hash,1);
			} else {
				bugInstance.setInstanceOccurrenceNum(count);
				seen.put(hash, count+1);
			}
		}
	
	}
	public void finish() {
		try {
			getReady(); // If no warnings were issued, then nothing has been written yet
			if (sorted) {
				if (addMessages) computeBugHashes();
				for(BugInstance bugInstance : getBugCollection().getCollection())
					bugInstance.writeXML(xmlOutput, addMessages);
			}
			if (addMessages) {
				writeBugCategories();
				writeBugPatterns();
				writeBugCodes();
			}
			
			getBugCollection().writeEpilogue(xmlOutput);
		} catch (IOException e) {
			throw new FatalException("Error writing XML output", e);
		} finally {
			try {
				xmlOutput.finish();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	private void writeBugPatterns() throws IOException {
		// Find bug types reported
		Set<String> bugTypeSet = new HashSet<String>();
		for (Iterator<BugInstance> i = getBugCollection().iterator(); i.hasNext();) {
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

	private void writeBugCodes() throws IOException {
		// Find bug codes reported
		Set<String> bugCodeSet = new HashSet<String>();
		for (Iterator<BugInstance> i = getBugCollection().iterator(); i.hasNext();) {
			BugInstance bugInstance = i.next();
			String bugCode = bugInstance.getAbbrev();
			if (bugCode != null) {
				bugCodeSet.add(bugCode);
			}
		}
		// Emit element describing each reported bug code
		for (String bugCode : bugCodeSet) {
			String bugCodeDescription = I18N.instance().getBugTypeDescription(bugCode);
			if (bugCodeDescription == null)
				continue;

			XMLAttributeList attributeList = new XMLAttributeList();
			attributeList.addAttribute("abbrev", bugCode);

			xmlOutput.openTag("BugCode", attributeList);

			xmlOutput.openTag("Description");
			xmlOutput.writeText(bugCodeDescription);
			xmlOutput.closeTag("Description");

			xmlOutput.closeTag("BugCode");
		}
	}

	private void writeBugCategories() throws IOException {
		// Find bug categories reported
		Set<String> bugCatSet = new HashSet<String>();
		for (Iterator<BugInstance> i = getBugCollection().iterator(); i.hasNext();) {
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
}

// vim:ts=4

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
	private XMLOutput xmlOutput;

	public XMLBugReporter(Project project) {
		super(project);
		this.addMessages = false;
		this.started = false;
		this.xmlOutput = new OutputStreamXMLOutput(outputStream);
	}

	public void setAddMessages(boolean enable) {
		this.addMessages = enable;
	}

	//@Override
	public void doReportBug(BugInstance bugInstance) {
		try {
			if (!started) {
				started = true;
				getBugCollection().writePrologue(xmlOutput, getProject());
			}
			
			bugInstance.writeXML(xmlOutput, addMessages);
		} catch (IOException e) {
			throw new FatalException("Error writing XML output", e);
		}
		
		super.doReportBug(bugInstance);
	}
	
	
	public void finish() {
		generateSummary();
		
		try {
			if (addMessages) {
				writeBugPatterns();
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
		for (Iterator<String> i = bugTypeSet.iterator(); i.hasNext();) {
			String bugType = i.next();
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
}

// vim:ts=4

/*
 * FindBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.ba.SignatureConverter;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * Add human-readable messages to a dom4j tree containing
 * FindBugs XML output.  This transformation makes it easier
 * to generate reports (such as HTML) from the XML.
 *
 * @see BugCollection
 * @author David Hovemeyer
 */
public class AddMessages {
	private BugCollection bugCollection;
	private Document document;

	/**
	 * Constructor.
	 *
	 * @param bugCollection the BugCollection the dom4j was generated from
	 * @param document      the dom4j tree
	 */
	public AddMessages(BugCollection bugCollection, Document document) {
		this.bugCollection = bugCollection;
		this.document = document;
	}

	/**
	 * Add messages to the dom4j tree.
	 */
	public void execute() {
		Iterator elementIter = document.selectNodes("/BugCollection/BugInstance").iterator();
		Iterator<BugInstance> bugInstanceIter = bugCollection.iterator();

		Set<String> bugTypeSet = new HashSet<String>();

		// Add short and long descriptions to BugInstance elements.
		// We rely on the Document and the BugCollection storing
		// the bug instances in the same order.
		while (elementIter.hasNext() && bugInstanceIter.hasNext()) {
			Element element = (Element) elementIter.next();
			BugInstance bugInstance = bugInstanceIter.next();

			String bugType = bugInstance.getType();
			bugTypeSet.add(bugType);

			BugPattern bugPattern = bugInstance.getBugPattern();

			element.addElement("ShortMessage").addText(
				bugPattern != null
					? bugPattern.getShortDescription()
					: bugInstance.toString());
			element.addElement("LongMessage").addText(bugInstance.getMessage());

			// Add pre-formatted display strings in "Message"
			// elements for all bug annotations.
			Iterator annElementIter = element.elements().iterator();
			Iterator<BugAnnotation> annIter = bugInstance.annotationIterator();
			while (annElementIter.hasNext() && annIter.hasNext()) {
				Element annElement = (Element) annElementIter.next();
				BugAnnotation ann = annIter.next();
				annElement.addElement("Message").addText(ann.toString());
			}
		}

		// Add BugPattern elements for each referenced bug types.
		Element root = document.getRootElement();
		for (Iterator<String> bugTypeIter = bugTypeSet.iterator(); bugTypeIter.hasNext(); ) {
			String bugType = bugTypeIter.next();
			BugPattern bugPattern = I18N.instance().lookupBugPattern(bugType);
			if (bugPattern == null)
				continue;
			Element details = root.addElement("BugPattern");
			details
				.addAttribute("type", bugType)
				.addAttribute("abbrev", bugPattern.getAbbrev())
				.addAttribute("category", bugPattern.getCategory());
			details
				.addElement("ShortDescription")
				.addText(bugPattern.getShortDescription());
			details
				.addElement("Details")
				.addCDATA(bugPattern.getDetailText());
		}
	}
}

// vim:ts=4

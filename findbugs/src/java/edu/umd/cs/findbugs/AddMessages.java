/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 University of Maryland
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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.xml.XMLUtil;

/**
 * Add human-readable messages to a dom4j tree containing FindBugs XML output.
 * This transformation makes it easier to generate reports (such as HTML) from
 * the XML.
 *
 * @see BugCollection
 * @author David Hovemeyer
 */
public class AddMessages {
    private final BugCollection bugCollection;

    private final Document document;

    /**
     * Constructor.
     *
     * @param bugCollection
     *            the BugCollection the dom4j was generated from
     * @param document
     *            the dom4j tree
     */
    public AddMessages(BugCollection bugCollection, Document document) {
        this.bugCollection = bugCollection;
        this.document = document;
    }

    /**
     * Add messages to the dom4j tree.
     */
    public void execute() {
        Iterator<?> elementIter = XMLUtil.selectNodes(document, "/BugCollection/BugInstance").iterator();
        Iterator<BugInstance> bugInstanceIter = bugCollection.iterator();

        Set<String> bugTypeSet = new HashSet<String>();
        Set<String> bugCategorySet = new HashSet<String>();
        Set<String> bugCodeSet = new HashSet<String>();

        // Add short and long descriptions to BugInstance elements.
        // We rely on the Document and the BugCollection storing
        // the bug instances in the same order.
        while (elementIter.hasNext() && bugInstanceIter.hasNext()) {
            Element element = (Element) elementIter.next();
            BugInstance bugInstance = bugInstanceIter.next();

            String bugType = bugInstance.getType();
            bugTypeSet.add(bugType);

            BugPattern bugPattern = bugInstance.getBugPattern();

            bugCategorySet.add(bugPattern.getCategory());
            bugCodeSet.add(bugPattern.getAbbrev());

            element.addElement("ShortMessage").addText(bugPattern.getShortDescription());
            element.addElement("LongMessage").addText(bugInstance.getMessage());

            // Add pre-formatted display strings in "Message"
            // elements for all bug annotations.
            Iterator<?> annElementIter = element.elements().iterator();
            Iterator<BugAnnotation> annIter = bugInstance.annotationIterator();
            while (annElementIter.hasNext() && annIter.hasNext()) {
                Element annElement = (Element) annElementIter.next();
                BugAnnotation ann = annIter.next();
                annElement.addElement("Message").addText(ann.toString());
            }
        }

        // Add BugPattern elements for each referenced bug types.
        addBugCategories(bugCategorySet);
        addBugPatterns(bugTypeSet);
        addBugCodes(bugCodeSet);
    }

    /**
     * Add BugCategory elements.
     *
     * @param bugCategorySet
     *            all bug categories referenced in the BugCollection
     */
    private void addBugCategories(Set<String> bugCategorySet) {
        Element root = document.getRootElement();
        for (String category : bugCategorySet) {
            Element element = root.addElement("BugCategory");
            element.addAttribute("category", category);
            Element description = element.addElement("Description");
            description.setText(I18N.instance().getBugCategoryDescription(category));

            BugCategory bc = DetectorFactoryCollection.instance().getBugCategory(category);
            if (bc != null) { // shouldn't be null
                String s = bc.getAbbrev();
                if (s != null) {
                    Element abbrev = element.addElement("Abbreviation");
                    abbrev.setText(s);
                }
                s = bc.getDetailText();
                if (s != null) {
                    Element details = element.addElement("Details");
                    details.setText(s);
                }
            }
        }
    }

    /**
     * Add BugCode elements.
     *
     * @param bugCodeSet
     *            all bug codes (abbrevs) referenced in the BugCollection
     */
    private void addBugCodes(Set<String> bugCodeSet) {
        Element root = document.getRootElement();
        for (String bugCode : bugCodeSet) {
            Element element = root.addElement("BugCode");
            element.addAttribute("abbrev", bugCode);
            Element description = element.addElement("Description");
            description.setText(I18N.instance().getBugTypeDescription(bugCode));
        }
    }

    private void addBugPatterns(Set<String> bugTypeSet) {
        Element root = document.getRootElement();
        for (String bugType : bugTypeSet) {
            BugPattern bugPattern = DetectorFactoryCollection.instance().lookupBugPattern(bugType);
            if (bugPattern == null) {
                continue;
            }
            Element details = root.addElement("BugPattern");
            details.addAttribute("type", bugType).addAttribute("abbrev", bugPattern.getAbbrev())
            .addAttribute("category", bugPattern.getCategory());
            details.addElement("ShortDescription").addText(bugPattern.getShortDescription());
            details.addElement("Details").addCDATA(bugPattern.getDetailText());
        }
    }

    @SuppressFBWarnings("DM_EXIT")
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: " + AddMessages.class.getName() + " <input collection> <output collection>");
            System.exit(1);
        }

        // Load plugins, in order to get message files
        DetectorFactoryCollection.instance();

        String inputFile = args[0];
        String outputFile = args[1];
        Project project = new Project();

        SortedBugCollection inputCollection = new SortedBugCollection(project);
        inputCollection.readXML(inputFile);

        Document document = inputCollection.toDocument();

        AddMessages addMessages = new AddMessages(inputCollection, document);
        addMessages.execute();

        XMLWriter writer = new XMLWriter(new BufferedOutputStream(new FileOutputStream(outputFile)),
                OutputFormat.createPrettyPrint());
        writer.write(document);
        writer.close();
    }
}


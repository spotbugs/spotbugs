/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, Garvin LeClaire <garvin.leclaire@insightbb.com>
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
import java.io.Writer;
import java.util.Iterator;

import javax.annotation.Nonnull;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * BugReporter to output warnings in xdocs format for Maven.
 *
 * @author Garvin LeClaire
 */
public class XDocsBugReporter extends TextUIBugReporter {
    final private SortedBugCollection bugCollection;

    final private Project project;

    private final Document document;

    private final Element root;

    private static final String ROOT_ELEMENT_NAME = "BugCollection";

    private static final String PROJECT_ELEMENT_NAME = "Project";

    private static final String ERRORS_ELEMENT_NAME = "Errors";

    private static final String ANALYSIS_ERROR_ELEMENT_NAME = "AnalysisError";

    private static final String MISSING_CLASS_ELEMENT_NAME = "MissingClass";

    private static final String SUMMARY_HTML_ELEMENT_NAME = "SummaryHTML";

    private static final String ELEMENT_NAME = "BugInstance";

    private static final String FILE_ELEMENT_NAME = "file";

    public XDocsBugReporter(Project project) {
        this.project = project;
        this.bugCollection = new SortedBugCollection(project);

        this.document = DocumentHelper.createDocument();
        this.root = document.addElement(ROOT_ELEMENT_NAME);

    }

    @Override
    public void observeClass(ClassDescriptor classDescriptor) {
    }

    @Override
    public void logError(String message) {
        bugCollection.addError(message);
        super.logError(message);
    }

    @Override
    public void reportMissingClass(ClassNotFoundException ex) {
        String missing = AbstractBugReporter.getMissingClassName(ex);
        if (!isValidMissingClassMessage(missing)) {
            return;
        }
        bugCollection.addMissingClass(missing);
        super.reportMissingClass(ex);
    }

    @Override
    public void doReportBug(BugInstance bugInstance) {
        if (bugCollection.add(bugInstance)) {
            printBug(bugInstance);
            notifyObservers(bugInstance);
        }
    }

    @Override
    protected void printBug(BugInstance bugInstance) {
        try {
            toElement(bugInstance);
        } catch (Exception e) {
            logError("Couldn't add Element", e);
        }
    }

    @Override
    public void finish() {

        try {
            writeXML(outputStream, project);
        } catch (Exception e) {
            logError("Couldn't write XML output", e);
        }
        outputStream.flush();
    }

    private void writeXML(Writer out, Project project) throws IOException {
        Document document = endDocument(project);

        XMLWriter writer = new XMLWriter(out, OutputFormat.createPrettyPrint());
        writer.write(document);
    }

    private Document endDocument(Project project) {

        // Save the error information
        Element errorsElement = root.addElement(ERRORS_ELEMENT_NAME);
        for (AnalysisError analysisError : bugCollection.getErrors()) {
            errorsElement.addElement(ANALYSIS_ERROR_ELEMENT_NAME).setText(analysisError.getMessage());
        }
        for (Iterator<String> i = bugCollection.missingClassIterator(); i.hasNext();) {
            errorsElement.addElement(MISSING_CLASS_ELEMENT_NAME).setText(i.next());
        }

        return document;
    }

    private static String xmlEscape(String theString) {
        // Replaces characters '>', '<', '"', '&', ''' with XML equivalents
        StringBuilder buf = new StringBuilder();
        int len = theString.length();
        char theChar;
        for (int i = 0; i < len; i++) {
            theChar = theString.charAt(i);
            switch (theChar) {
            case '>':
                buf.append("&gt;");
                break;
            case '<':
                buf.append("&lt;");
                break;
            case '"':
                buf.append("&quot;");
                break;
            case '&':
                buf.append("&amp;");
                break;
            case '\'':
                buf.append("&apos;");
                break;
            default:
                buf.append(theChar);
            }
        }
        return buf.toString();
    }

    public void toElement(BugInstance bugInstance) {

        String className = bugInstance.getPrimaryClass().getClassName();
        Element element = (Element) root.selectSingleNode(FILE_ELEMENT_NAME + "[@classname='" + className + "']");

        if (element == null) {
            element = root.addElement(FILE_ELEMENT_NAME);
            element.addAttribute("classname", className);
        }

        element = element.addElement(ELEMENT_NAME);

        element.addAttribute("type", bugInstance.getType());

        switch (bugInstance.getPriority()) {
        case Priorities.EXP_PRIORITY:
            element.addAttribute("priority", "Experimental");
            break;
        case Priorities.LOW_PRIORITY:
            element.addAttribute("priority", "Low");
            break;
        case Priorities.NORMAL_PRIORITY:
            element.addAttribute("priority", "Normal");
            break;
        case Priorities.HIGH_PRIORITY:
            element.addAttribute("priority", "High");
            break;
        default:
            assert false;
        }

        element.addAttribute("message", xmlEscape(bugInstance.getMessage()));

        SourceLineAnnotation line = bugInstance.getPrimarySourceLineAnnotation();
        element.addAttribute("line", Integer.toString(line.getStartLine()));


    }

    /*
     * public static void main(String args[]) { String x =
     * "Less than: < Greater than: > Ampersand: & Quotation mark: \" Apostrophe: '"
     * ; String y = xmlEscape(x); System.out.println(x); System.out.println(y);
     * }
     */

    @Override
    public @Nonnull
    BugCollection getBugCollection() {
        return bugCollection;
    }

}


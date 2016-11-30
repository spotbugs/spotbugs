/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.io.DocumentSource;

public class HTMLBugReporter extends BugCollectionBugReporter {
    private final String stylesheet;

    private Exception fatalException;

    public HTMLBugReporter(Project project, String stylesheet) {
        super(project);
        this.stylesheet = stylesheet;
    }

    @Override
    public void finish() {
        try {
            BugCollection bugCollection = getBugCollection();
            bugCollection.setWithMessages(true);
            // Decorate the XML with messages to display
            Document document = bugCollection.toDocument();
            // new AddMessages(bugCollection, document).execute();

            // Get the stylesheet as a StreamSource.
            // First, try to load the stylesheet from the filesystem.
            // If that fails, try loading it as a resource.
            InputStream xslInputStream = getStylesheetStream(stylesheet);
            StreamSource xsl = new StreamSource(xslInputStream);
            xsl.setSystemId(stylesheet);

            // Create a transformer using the stylesheet
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xsl);

            // Source document is the XML generated from the BugCollection
            DocumentSource source = new DocumentSource(document);

            // Write result to output stream
            StreamResult result = new StreamResult(outputStream);

            // Do the transformation
            transformer.transform(source, result);
        } catch (Exception e) {
            logError("Could not generate HTML output", e);
            fatalException = e;
            if (FindBugs.DEBUG) {
                e.printStackTrace();
            }
        }
        outputStream.close();
    }

    public Exception getFatalException() {
        return fatalException;
    }

    private static InputStream getStylesheetStream(String stylesheet) throws IOException {
        if (FindBugs.DEBUG) {
            System.out.println("Attempting to load stylesheet " + stylesheet);
        }
        try {
            URL u = new URL(stylesheet);
            return u.openStream();
        } catch (Exception e) {
            assert true; // ignore it
        }
        try {
            return new BufferedInputStream(new FileInputStream(stylesheet));
        } catch (Exception fnfe) {
            assert true; // ignore it
        }
        InputStream xslInputStream = HTMLBugReporter.class.getResourceAsStream("/" + stylesheet);
        if (xslInputStream == null) {
            throw new IOException("Could not load HTML generation stylesheet " + stylesheet);
        }
        return xslInputStream;
    }
}


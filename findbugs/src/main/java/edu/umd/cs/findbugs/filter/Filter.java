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

package edu.umd.cs.findbugs.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.IdentityHashMap;
import java.util.Iterator;

import javax.annotation.WillClose;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SAXBugCollectionHandler;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Filter to match a subset of BugInstances. The filter criteria are read from
 * an XML file.
 *
 * @author David Hovemeyer
 */

public class Filter extends OrMatcher {
    private static final boolean DEBUG = SystemProperties.getBoolean("filter.debug");

    private final IdentityHashMap<Matcher, Boolean> disabled = new IdentityHashMap<Matcher, Boolean>();

    /**
     * Constructor for empty filter
     *
     */
    public Filter() {

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((disabled == null) ? 0 : disabled.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Filter)) {
            return false;
        }
        final Filter other = (Filter) obj;
        if (disabled == null) {
            if (other.disabled != null) {
                return false;
            }
        } else if (!disabled.equals(other.disabled)) {
            return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return super.numberChildren() == 0;
    }

    public void setEnabled(Matcher m, boolean value) {
        if (value) {
            enable(m);
        } else {
            disable(m);
        }
    }

    public void disable(Matcher m) {
        disabled.put(m, true);
    }

    public boolean isEnabled(Matcher m) {
        return !disabled.containsKey(m);
    }

    public void enable(Matcher m) {
        disabled.remove(m);
    }

    public static Filter parseFilter(String fileName) throws IOException {
        return new Filter(fileName);
    }

    /**
     * Constructor.
     *
     * @param fileName
     *            name of the filter file
     * @throws IOException
     */
    public Filter(String fileName) throws IOException {
        try {
            parse(fileName);
        } catch (SAXException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Constructor.
     *
     * @param stream
     *            content of the filter file
     * @throws IOException
     */
    public Filter(InputStream stream) throws IOException {
        try {
            parse("", stream);
        } catch (SAXException e) {
            throw new IOException(e.getMessage());
        }
    }

    public boolean contains(Matcher child) {
        return children.contains(child);
    }

    /**
     * Add if not present, but do not enable if already present and disabled
     *
     * @param child
     */
    public void softAdd(Matcher child) {
        super.addChild(child);
    }

    @Override
    public void addChild(Matcher child) {
        super.addChild(child);
        enable(child);
    }

    @Override
    public void removeChild(Matcher child) {
        enable(child);// Remove from disabled before removing it
        super.removeChild(child);
    }

    @Override
    public void clear() {
        disabled.clear();
        super.clear();
    }

    @Override
    public boolean match(BugInstance bugInstance) {
        Iterator<Matcher> i = childIterator();
        while (i.hasNext()) {
            Matcher child = i.next();
            if (isEnabled(child) && child.match(bugInstance)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse and load the given filter file.
     *
     * @param fileName
     *            name of the filter file
     * @throws IOException
     * @throws SAXException
     * @throws FilterException
     */
    private void parse(String fileName) throws IOException, SAXException {
        FileInputStream fileInputStream = new FileInputStream(new File(fileName));
        parse(fileName, fileInputStream);
    }

    /**
     * Parse and load the given filter file.
     *
     * @param fileName
     *            name of the filter file
     * @throws IOException
     * @throws SAXException
     * @throws FilterException
     */
    private void parse(String fileName, @WillClose InputStream stream) throws IOException, SAXException {
        try {
            SAXBugCollectionHandler handler = new SAXBugCollectionHandler(this, new File(fileName));
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            Reader reader = Util.getReader(stream);
            xr.parse(new InputSource(reader));
        } finally {
            Util.closeSilently(stream);
        }
    }

    public static void main(String[] argv) {
        try {
            if (argv.length != 1) {
                System.err.println("Usage: " + Filter.class.getName() + " <filename>");
                System.exit(1);
            }

            Filter filter = new Filter(argv[0]);
            filter.writeAsXML(System.out);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void writeAsXML(@WillClose OutputStream out) throws IOException {
        XMLOutput xmlOutput = new OutputStreamXMLOutput(out);

        try {
            xmlOutput.beginDocument();
            xmlOutput.openTag("FindBugsFilter");
            writeBodyAsXML(xmlOutput);
            xmlOutput.closeTag("FindBugsFilter");
        } finally {
            xmlOutput.finish();
        }
    }

    public void writeEnabledMatchersAsXML(@WillClose OutputStream out) throws IOException {

        XMLOutput xmlOutput = new OutputStreamXMLOutput(out);

        try {
            xmlOutput.beginDocument();
            xmlOutput.openTag("FindBugsFilter");
            Iterator<Matcher> i = childIterator();
            while (i.hasNext()) {
                Matcher child = i.next();
                if (!disabled.containsKey(child)) {
                    child.writeXML(xmlOutput, false);
                }
            }
            xmlOutput.closeTag("FindBugsFilter");
        } finally {
            xmlOutput.finish();
        }
    }

    public void writeBodyAsXML(XMLOutput xmlOutput) throws IOException {
        Iterator<Matcher> i = childIterator();
        while (i.hasNext()) {
            Matcher child = i.next();
            child.writeXML(xmlOutput, disabled.containsKey(child));
        }
    }

}


/*
 * XML input/output support for FindBugs
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

package edu.umd.cs.findbugs.xml;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Utility routines for writing to XMLOutput.
 *
 * @see XMLOutput
 * @author David Hovemeyer
 */
public abstract class XMLOutputUtil {
    /**
     * Write a list of Strings to document as elements with given tag name.
     *
     * @param xmlOutput
     *            the XMLOutput object to write to
     * @param tagName
     *            the tag name
     * @param listValues
     *            Collection of String values to write
     */
    public static void writeElementList(XMLOutput xmlOutput, String tagName, Iterable<String> listValues) throws IOException {
        writeElementList(xmlOutput, tagName, listValues.iterator());
    }

    /**
     * Write a list of Strings to document as elements with given tag name.
     *
     * @param xmlOutput
     *            the XMLOutput object to write to
     * @param tagName
     *            the tag name
     * @param listValueIterator
     *            Iterator over String values to write
     */
    public static void writeElementList(XMLOutput xmlOutput, String tagName, Iterator<String> listValueIterator)
            throws IOException {
        while (listValueIterator.hasNext()) {
            xmlOutput.openTag(tagName);
            xmlOutput.writeText(listValueIterator.next());
            xmlOutput.closeTag(tagName);
        }
    }

    /**
     * Write a list of Strings to document as elements with given tag name.
     *
     * @param xmlOutput
     *            the XMLOutput object to write to
     * @param tagName
     *            the tag name
     * @param listValues
     *            Collection of String values to write
     */
    public static void writeFileList(XMLOutput xmlOutput, String tagName, Iterable<File> listValues) throws IOException {
        if (listValues != null) {
            writeFileList(xmlOutput, tagName, listValues.iterator());
        }
    }

    /**
     * Write a list of Strings to document as elements with given tag name.
     *
     * @param xmlOutput
     *            the XMLOutput object to write to
     * @param tagName
     *            the tag name
     * @param listValueIterator
     *            Iterator over String values to write
     */
    public static void writeFileList(XMLOutput xmlOutput, String tagName, Iterator<File> listValueIterator) throws IOException {
        while (listValueIterator.hasNext()) {
            xmlOutput.openTag(tagName);
            xmlOutput.writeText(listValueIterator.next().getPath());
            xmlOutput.closeTag(tagName);
        }
    }

    /**
     * Write a Collection of XMLWriteable objects.
     *
     * @param xmlOutput
     *            the XMLOutput object to write to
     * @param collection
     *            Collection of XMLWriteable objects
     */
    public static void writeCollection(XMLOutput xmlOutput, Collection<? extends XMLWriteable> collection) throws IOException {
        for (XMLWriteable obj : collection) {
            obj.writeXML(xmlOutput);
        }
    }
}

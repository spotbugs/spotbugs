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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.dom4j.Branch;
import org.dom4j.Element;

/**
 * XMLOutput class to build all or part of a dom4j tree.
 *
 * @see XMLOutput
 * @author David Hovemeyer
 */
public class Dom4JXMLOutput implements XMLOutput {
    private final LinkedList<Branch> stack;

    /**
     * Constructor.
     *
     * @param topLevel
     *            the Document or Element that is the root of the tree to be
     *            built
     */
    public Dom4JXMLOutput(Branch topLevel) {
        this.stack = new LinkedList<>();
        stack.addLast(topLevel);
    }

    @Override
    public void beginDocument() {
    }

    @Override
    public void openTag(String tagName) {
        Branch top = stack.getLast();
        Element element = top.addElement(tagName);
        stack.addLast(element);
    }

    @Override
    public void openTag(String tagName, XMLAttributeList attributeList) {
        Branch top = stack.getLast();
        Element element = top.addElement(tagName);
        stack.addLast(element);

        for (Iterator<XMLAttributeList.NameValuePair> i = attributeList.iterator(); i.hasNext();) {
            XMLAttributeList.NameValuePair pair = i.next();
            element.addAttribute(pair.getName(), pair.getValue());
        }
    }

    @Override
    public void openCloseTag(String tagName) {
        openTag(tagName);
        closeTag(tagName);
    }

    @Override
    public void openCloseTag(String tagName, XMLAttributeList attributeList) {
        openTag(tagName, attributeList);
        closeTag(tagName);
    }

    @Override
    public void startTag(String tagName) {
        Branch top = stack.getLast();
        Element element = top.addElement(tagName);
        stack.addLast(element);
    }

    @Override
    public void addAttribute(String name, String value) {
        Element element = (Element) stack.getLast();
        element.addAttribute(name, value);
    }

    @Override
    public void stopTag(boolean close) {
        if (close) {
            closeTag(null);
        }
    }

    @Override
    public void closeTag(String tagName) {
        stack.removeLast();
    }

    @Override
    public void writeText(String text) {
        Element top = (Element) stack.getLast();
        top.addText(text);
    }

    @Override
    public void writeCDATA(String cdata) {
        Element top = (Element) stack.getLast();
        top.addCDATA(cdata);
    }

    /**
     * Add a list of Strings to document as elements with given tag name to the
     * tree.
     *
     * @param tagName
     *            the tag name
     * @param listValues
     *            Collection of String values to add
     */
    public void writeElementList(String tagName, Collection<String> listValues) {
        for (String listValue : listValues) {
            openTag(tagName);
            writeText(listValue);
            closeTag(tagName);
        }
    }

    /**
     * Add given object to the tree.
     *
     * @param obj
     *            the object
     */
    public void write(XMLWriteable obj) {
        try {
            obj.writeXML(this);
        } catch (java.io.IOException e) {
            // Can't really happen
        }
    }

    /**
     * Add a Collection of XMLWriteable objects to the tree.
     *
     * @param collection
     *            Collection of XMLWriteable objects
     */
    public void writeCollection(Collection<? extends XMLWriteable> collection) {
        for (XMLWriteable obj : collection) {
            write(obj);
        }
    }

    @Override
    public void finish() {
    }
}

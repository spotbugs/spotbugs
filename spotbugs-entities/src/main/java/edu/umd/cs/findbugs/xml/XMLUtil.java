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

package edu.umd.cs.findbugs.xml;

import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;

import org.dom4j.Node;
import org.dom4j.io.SAXReader;


/**
 * @author pugh
 */
public class XMLUtil {

    @SuppressWarnings("unchecked")
    public static <T> List<T> selectNodes(Node node, String arg0) {
        return (List<T>) node.selectNodes(arg0);
    }

    public static SAXReader buildSAXReader() {
        SAXReader reader = new SAXReader();

        try {
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception e) {
            throw new RuntimeException("Error while disabling XML external entities", e);
        }

        return reader;
    }

    public static TransformerFactory buildTransformerFactory() {
        TransformerFactory factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (Exception e) {
            throw new RuntimeException("Error while disabling XML external entities", e);
        }

        return factory;
    }
}

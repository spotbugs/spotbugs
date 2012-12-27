/*
 * Contributions to FindBugs
 * Copyright (C) 2012, Andrey Loskutov
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
package de.tobject.findbugs.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.eclipse.core.runtime.Assert;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import edu.umd.cs.findbugs.util.Strings;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLAttributeList.NameValuePair;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Write XML or plain text to an output stream.
 *
 * @see OutputStreamXMLOutput
 * @author Andrei Loskutov
 */
public class ConfigurableXmlOutputStream implements XMLOutput {
    private static final String OPENING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    private final Writer out;

    private int nestingLevel;

    private boolean newLine;

    private final boolean plainText;

    /**
     * Constructor.
     *
     * @param os
     *            OutputStream to write XML output to
     * @param plainText
     *            to use plain text instead of xml
     */
    public ConfigurableXmlOutputStream(OutputStream os, boolean plainText) {
        this.plainText = plainText;
        this.out = new OutputStreamWriter(os, Charset.forName("UTF-8"));
        this.nestingLevel = 0;
        this.newLine = true;
    }

    public void beginDocument() throws IOException {
        if (!plainText) {
            out.write(OPENING);
        }
        out.write("\n");
        newLine = true;
    }

    public void openTag(String tagName) throws IOException {
        emitTag(tagName, false);
    }

    public void openTag(String tagName, XMLAttributeList attributeList) throws IOException {
        if (!plainText) {
            emitTag(tagName, attributeList.toString(), false);
        } else {
            StringBuilder sb = new StringBuilder();
            Iterator<NameValuePair> iterator = attributeList.iterator();
            while (iterator.hasNext()) {
                XMLAttributeList.NameValuePair nameValuePair = iterator.next();
                sb.append("\n");
                for (int i = 0; i < nestingLevel; ++i) {
                    sb.append("  ");
                }
                sb.append(nameValuePair.getName()).append(" = ").append(nameValuePair.getName());
            }
            emitTag(tagName, sb.toString(), false);
        }
    }

    public void openCloseTag(String tagName) throws IOException {
        emitTag(tagName, true);
    }

    public void openCloseTag(String tagName, XMLAttributeList attributeList) throws IOException {
        if (!plainText) {
            emitTag(tagName, attributeList.toString(), true);
        } else {
            StringBuilder sb = new StringBuilder();
            Iterator<NameValuePair> iterator = attributeList.iterator();
            while (iterator.hasNext()) {
                XMLAttributeList.NameValuePair nameValuePair = iterator.next();
                sb.append("\n");
                for (int i = 0; i < nestingLevel; ++i) {
                    sb.append("  ");
                }
                sb.append(nameValuePair.getName()).append(" = ").append(nameValuePair.getName());
            }
            emitTag(tagName, sb.toString(), true);
        }
    }

    public void startTag(String tagName) throws IOException {
        indent();
        ++nestingLevel;
        if (!plainText) {
            out.write("<");
        }
        out.write(tagName);
    }

    public void addAttribute(String name, String value) throws IOException {
        if (plainText) {
            out.write("\n");
            for (int i = 0; i < nestingLevel; ++i) {
                out.write("  ");
            }
        }
        out.write(' ');
        out.write(name);
        out.write('=');
        out.write('"');
        out.write(XMLAttributeList.getQuotedAttributeValue(value));
        out.write('"');
    }

    public void stopTag(boolean close) throws IOException {
        if (close) {
            if (!plainText) {
                out.write("/>");
            }
            out.write("\n");
            --nestingLevel;
            newLine = true;
        } else {
            if (!plainText) {
                out.write(">");
            } else {
                out.write("\n");
            }
            newLine = false;
        }
    }

    private void emitTag(String tagName, boolean close) throws IOException {
        startTag(tagName);
        stopTag(close);
    }

    private void emitTag(String tagName, String attributes, boolean close) throws IOException {
        startTag(tagName);
        attributes = attributes.trim();
        if (attributes.length() > 0) {
            out.write(" ");
            out.write(attributes);
        }
        stopTag(close);
    }

    public void closeTag(String tagName) throws IOException {
        --nestingLevel;
        if (newLine) {
            indent();
        }
        if (!plainText) {
            out.write("</" + tagName + ">");
        }
        out.write("\n");
        newLine = true;
    }

    public void writeText(String text) throws IOException {
        out.write(Strings.escapeXml(text));
    }

    public void writeCDATA(String cdata) throws IOException {
        // FIXME: We just trust fate that the characters being written
        // don't contain the string "]]>"
        Assert.isTrue(cdata.indexOf("]]") == -1);
        if (!plainText) {
            out.write("<![CDATA[");
        } else {
            out.write("\n");
        }
        out.write(cdata);
        if (!plainText) {
            out.write("]]>");
        } else {
            out.write("\n");
        }
        newLine = false;
    }

    @DischargesObligation
    public void finish() throws IOException {
        out.flush();
        out.close();
    }

    private void indent() throws IOException {
        if (!newLine) {
            out.write("\n");
        }
        // if(plainText){
        // out.write("\n");
        // }
        for (int i = 0; i < nestingLevel; ++i) {
            out.write("  ");
        }
    }
}

// vim:ts=4

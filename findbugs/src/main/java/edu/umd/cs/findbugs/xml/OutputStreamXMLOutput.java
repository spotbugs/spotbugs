/*
 * XML input/output support for FindBugs
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

package edu.umd.cs.findbugs.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.annotation.WillCloseWhenClosed;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.util.Strings;

/**
 * Write XML to an output stream.
 *
 * @author David Hovemeyer
 */
public class OutputStreamXMLOutput implements XMLOutput {
    private static final String OPENING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    private static String getStylesheetCode(String stylesheet) {
        if (stylesheet == null) {
            return "";
        }
        return "<?xml-stylesheet type=\"text/xsl\" href=\"" + stylesheet + "\"?>\n";
    }

    private final Writer out;

    private int nestingLevel;

    private boolean newLine;

    private final String stylesheet;

    /**
     * Constructor.
     *
     * @param os
     *            OutputStream to write XML output to
     */
    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    public OutputStreamXMLOutput(@WillCloseWhenClosed OutputStream os) {
        this(os, null);
    }
    /**
     * Constructor.
     *
     * @param writer
     *            Writer to write XML output to
     */
    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    public OutputStreamXMLOutput(@WillCloseWhenClosed Writer writer) {
        this(writer, null);
    }
    /**
     * Constructor.
     *
     * @param os
     *            OutputStream to write XML output to
     * @param stylesheet
     *            name of stylesheet
     */
    public OutputStreamXMLOutput(@WillCloseWhenClosed OutputStream os, String stylesheet) {
        this.out = new OutputStreamWriter(os, Charset.forName("UTF-8"));
        this.nestingLevel = 0;
        this.newLine = true;
        this.stylesheet = stylesheet;
    }

    /*
     * @param os
     *            Writer to write XML output to
     * @param stylesheet
     *            name of stylesheet
     */
    public OutputStreamXMLOutput(@WillCloseWhenClosed Writer writer, String stylesheet) {
        this.out = writer;
        this.nestingLevel = 0;
        this.newLine = true;
        this.stylesheet = stylesheet;
    }
    @Override
    public void beginDocument() throws IOException {
        out.write(OPENING);
        out.write(getStylesheetCode(stylesheet));
        out.write("\n");
        newLine = true;
    }

    @Override
    public void openTag(String tagName) throws IOException {
        emitTag(tagName, false);
    }

    @Override
    public void openTag(String tagName, XMLAttributeList attributeList) throws IOException {
        emitTag(tagName, attributeList.toString(), false);
    }

    @Override
    public void openCloseTag(String tagName) throws IOException {
        emitTag(tagName, true);
    }

    @Override
    public void openCloseTag(String tagName, XMLAttributeList attributeList) throws IOException {
        emitTag(tagName, attributeList.toString(), true);
    }

    @Override
    public void startTag(String tagName) throws IOException {
        indent();
        ++nestingLevel;
        out.write("<" + tagName);
    }

    @Override
    public void addAttribute(String name, String value) throws IOException {
        out.write(' ');
        out.write(name);
        out.write('=');
        out.write('"');
        out.write(XMLAttributeList.getQuotedAttributeValue(value));
        out.write('"');
    }

    @Override
    public void stopTag(boolean close) throws IOException {
        if (close) {
            out.write("/>\n");
            --nestingLevel;
            newLine = true;
        } else {
            out.write(">");
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

    @Override
    public void closeTag(String tagName) throws IOException {
        --nestingLevel;
        if (newLine) {
            indent();
        }
        out.write("</" + tagName + ">\n");
        newLine = true;
    }

    @Override
    public void writeText(String text) throws IOException {
        out.write(Strings.escapeXml(text));
    }

    @Override
    public void writeCDATA(String cdata) throws IOException {
        // FIXME: We just trust fate that the characters being written
        // don't contain the string "]]>"
        assert (cdata.indexOf("]]") == -1);
        out.write("<![CDATA[");
        out.write(cdata);
        out.write("]]>");
        newLine = false;
    }

    public void flush() throws IOException {
        out.flush();
    }
    @Override
    @DischargesObligation
    public void finish() throws IOException {
        out.close();
    }

    private void indent() throws IOException {
        if (!newLine) {
            out.write("\n");
        }
        for (int i = 0; i < nestingLevel; ++i) {
            out.write("  ");
        }
    }
}


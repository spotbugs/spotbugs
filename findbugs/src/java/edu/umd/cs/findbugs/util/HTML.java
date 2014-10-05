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

package edu.umd.cs.findbugs.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLWriter;

public class HTML {

    private static final class HTMLtoPlainTextWriter2 extends HTMLWriter {

        boolean inPre = false;

        boolean startingParagraph = false;

        public HTMLtoPlainTextWriter2(Writer w, HTMLDocument doc) {
            super(w, doc);
            setLineLength(80);
            setCanWrapLines(true);
        }

        @Override
        protected void startTag(Element elem) throws IOException {
            String name = elem.getName();
            startingParagraph = true;
            if ("ul".equals(name)) {
                super.incrIndent();
                write("  ");
            } else if ("pre".equals(name)) {
                inPre = true;
            } else if ("li".equals(name)) {
                super.incrIndent();
                write("* ");
            } /*else if (name.equals("p")) {

            }*/
        }

        @Override
        protected void writeEmbeddedTags(AttributeSet attr) throws IOException {

        }

        @Override
        protected void endTag(Element elem) throws IOException {
            String name = elem.getName();
            if ("p".equals(name)) {
                writeLineSeparator();
                indent();
            } else if ("pre".equals(name)) {
                inPre = false;
            } else if ("ul".equals(name)) {
                super.decrIndent();
                writeLineSeparator();
                indent();
            } else if ("li".equals(name)) {
                super.decrIndent();
                writeLineSeparator();
                indent();
            }
        }

        @Override
        protected void incrIndent() {
        }

        @Override
        protected void decrIndent() {
        }

        @Override
        protected void emptyTag(Element elem) throws IOException, BadLocationException {
            if ("content".equals(elem.getName())) {
                super.emptyTag(elem);
            }
        }

        @Override
        protected void text(Element elem) throws IOException, BadLocationException {
            String contentStr = getText(elem);
            if (!inPre) {
                contentStr = contentStr.replaceAll("\\s+", " ");

                if (startingParagraph) {
                    while (contentStr.length() > 0 && contentStr.charAt(0) == ' ') {
                        contentStr = contentStr.substring(1);
                    }
                }

                startingParagraph = false;
            }
            if (contentStr.length() > 0) {

                setCanWrapLines(!inPre);
                write(contentStr);
            }
        }
    }

    private HTML() {
    }

    public static void convertHtmlToText(Reader reader, Writer writer) throws IOException, BadLocationException {

        EditorKit kit = new HTMLEditorKit();
        HTMLDocument doc = new HTMLDocument();
        kit.read(reader, doc, 0);

        HTMLtoPlainTextWriter2 x = new HTMLtoPlainTextWriter2(writer, doc);
        x.write();
        writer.close();

    }

    public static String convertHtmlSnippetToText(String htmlSnippet) throws IOException, BadLocationException {
        StringWriter writer = new StringWriter();
        StringReader reader = new StringReader("<HTML><BODY>" + htmlSnippet + "</BODY></HTML>");
        convertHtmlToText(reader, writer);
        return writer.toString();
    }

}

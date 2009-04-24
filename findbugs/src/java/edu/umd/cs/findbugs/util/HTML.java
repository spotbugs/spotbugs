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
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.MinimalHTMLWriter;

public class HTML {

	private static final class HTMLtoPlainTextWriter extends MinimalHTMLWriter {
		boolean textWritten = false;

		/**
		 * @param w
		 * @param doc
		 */
		private HTMLtoPlainTextWriter(Writer w, StyledDocument doc) {
			super(w, doc);
			setCurrentLineLength(80);
		}

		@Override
		protected void writeHeader() {
		}

		@Override
		protected void writeStartTag(String tag) {
		}

		@Override
		protected void writeEndTag(String tag) {
		}

		@Override
		protected void write(char[] chars, int startIndex, int length) throws IOException {
			textWritten = true;
			super.write(chars, startIndex, length);
		}

		@Override
		protected void writeHTMLTags(AttributeSet attr) {
		}

		@Override
		protected void writeStartParagraph(Element elem) throws IOException {
			String name = elem.getName();
			if (textWritten && name.equals("p"))
				writeLineSeparator();
		}

		@Override
		protected void writeEndParagraph() throws IOException {
			if (textWritten)
				writeLineSeparator();

		}
	}

	private HTML() {
	};

	public static void convertHtmlToText(Reader reader, Writer writer) throws IOException, BadLocationException {

		EditorKit kit = new HTMLEditorKit();
		HTMLDocument doc = new HTMLDocument();
		kit.read(reader, doc, 0);

		MinimalHTMLWriter x = new HTMLtoPlainTextWriter(writer, doc);
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

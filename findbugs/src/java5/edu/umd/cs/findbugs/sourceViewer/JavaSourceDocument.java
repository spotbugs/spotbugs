/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.sourceViewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

import edu.umd.cs.findbugs.gui2.Debug;
import edu.umd.cs.findbugs.gui2.Driver;
import edu.umd.cs.findbugs.ba.SourceFile;

public class JavaSourceDocument {

	static final SimpleAttributeSet commentAttributes = new SimpleAttributeSet();

	static final SimpleAttributeSet javadocAttributes = new SimpleAttributeSet();

	static final SimpleAttributeSet quotesAttributes = new SimpleAttributeSet();

	static final SimpleAttributeSet keywordsAttributes = new SimpleAttributeSet();

	static final SimpleAttributeSet whiteAttributes = new SimpleAttributeSet();

	static Font sourceFont = new Font("Monospaced", Font.PLAIN, (int)Driver.getFontSize());
	final static Color HIGHLIGHT_COLOR = new Color(1f, 1f, .3f);
	TabSet TAB_SET;
	static {
		commentAttributes.addAttribute(StyleConstants.Foreground, new Color(
				0.0f, 0.5f, 0.0f));
		javadocAttributes.addAttribute(StyleConstants.Foreground, new Color(
				0.25f, 0.37f, 0.75f));
		quotesAttributes.addAttribute(StyleConstants.Foreground, new Color(
				0.0f, 0.0f, 1.0f));
		keywordsAttributes.addAttribute(StyleConstants.Foreground, new Color(
				0.5f, 0.0f, 0.5f));
		keywordsAttributes.addAttribute(StyleConstants.Bold, true);

	}

	
	final HighlightInformation highlights = new HighlightInformation();
	final NumberedEditorKit dek = new NumberedEditorKit(highlights);

	final StyleContext styleContext = new StyleContext();

	final Element root;

	final DefaultStyledDocument doc;
	
	final SourceFile sourceFile;

	public HighlightInformation getHighlightInformation() {
		return highlights;
	}
	public StyledDocument getDocument() {
		return doc;
	}
	public NumberedEditorKit getEditorKit() {
		return dek;
	}
	private final String title;
	public String getTitle() {
		return title;
	}
	
	public SourceFile getSourceFile() {
		return sourceFile;
	}
	
	public JavaSourceDocument(String title, Reader in, SourceFile theSource) throws IOException  {
		doc = new DefaultStyledDocument();
		this.title = title;
		this.sourceFile = theSource;
		Debug.println("Created JavaSourceDocument for " + title);
		try {
			dek.read(in, doc, 0);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
		in.close();
		doc.putProperty(Document.TitleProperty, title);
		root = doc.getDefaultRootElement();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		FontMetrics fontMetrics = toolkit.getFontMetrics(sourceFont);
		TabStop[] tabs = new TabStop[50];
		float width = fontMetrics.stringWidth(" ");
			
		for (int i = 0; i < tabs.length; i++)
			tabs[i] = new TabStop(width * (4 + 4 * i));
		TAB_SET = new TabSet(tabs);
		StyleConstants.setTabSet(commentAttributes, TAB_SET);
		StyleConstants.setTabSet(javadocAttributes, TAB_SET);

		StyleConstants.setTabSet(quotesAttributes, TAB_SET);

		StyleConstants.setTabSet(keywordsAttributes, TAB_SET);

		StyleConstants.setTabSet(commentAttributes, TAB_SET);

		StyleConstants.setTabSet(whiteAttributes, TAB_SET);
		StyleConstants.setFontFamily(whiteAttributes, sourceFont.getFamily());
		StyleConstants.setFontSize(whiteAttributes, sourceFont.getSize());
		StyleConstants.setLeftIndent(whiteAttributes, NumberedParagraphView.NUMBERS_WIDTH);
		
		doc.setParagraphAttributes(0, doc.getLength(), whiteAttributes, true);
		JavaScanner parser = new JavaScanner(new DocumentCharacterIterator(doc));
		while (parser.next() != JavaScanner.EOF) {
			int kind = parser.getKind();
			switch (kind) {
			case JavaScanner.COMMENT:
				doc.setCharacterAttributes(parser.getStartPosition(), parser
						.getLength(), commentAttributes, true);

				break;
			case JavaScanner.KEYWORD:
				doc.setCharacterAttributes(parser.getStartPosition(), parser
						.getLength(), keywordsAttributes, true);

				break;
			case JavaScanner.JAVADOC:
				doc.setCharacterAttributes(parser.getStartPosition(), parser
						.getLength(), javadocAttributes, true);

				break;
			case JavaScanner.QUOTE:
				doc.setCharacterAttributes(parser.getStartPosition(), parser
						.getLength(), quotesAttributes, true);

				break;
			}

		}

	}
	private static final long serialVersionUID = 0L;
	public static final JavaSourceDocument UNKNOWNSOURCE;
	static {
		try {
			UNKNOWNSOURCE= new JavaSourceDocument("Unknown source", new StringReader("Unable to find source"), null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
		
}
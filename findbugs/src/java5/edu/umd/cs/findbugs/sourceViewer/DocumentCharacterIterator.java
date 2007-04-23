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

import java.text.CharacterIterator;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

/**
 * A CharacterIterator over a Document.
 * Only a partial implementation.
 */
public class DocumentCharacterIterator implements CharacterIterator {

	private final Document doc;

	private final Segment text;

	/** Position of iterator in document. */
	private int docPos = 0;

	/** Index of end of current segment in document. */
	private int segmentEnd;

	DocumentCharacterIterator(Document doc) {
		this.doc = doc;
		text = new Segment();
		text.setPartialReturn(true);

		try {
			doc.getText(0, doc.getLength(), text);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
		segmentEnd = text.count;
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}

	public char current() {
		return text.current();
	}

	public char first() {
		throw new UnsupportedOperationException();
	}

	public int getBeginIndex() {
		throw new UnsupportedOperationException();
	}

	public int getEndIndex() {
		throw new UnsupportedOperationException();
	}

	public int getIndex() {
		return docPos;
	}

	public char last() {
		throw new UnsupportedOperationException();
	}

	/** Increments the iterator's index by one and returns the character at the new index.
	 * @return the character at the new position, or DONE if the new position is off the end
	 */
	public char next() {
		++docPos;
		if (docPos < segmentEnd || segmentEnd >= doc.getLength()) {
			return text.next();
		}
		try {
			doc.getText(segmentEnd, doc.getLength() - segmentEnd, text);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
		segmentEnd += text.count;
		return text.current();
	}

	public char previous() {
		throw new UnsupportedOperationException();
	}

	public char setIndex(int position) {
		throw new UnsupportedOperationException();
	}

}

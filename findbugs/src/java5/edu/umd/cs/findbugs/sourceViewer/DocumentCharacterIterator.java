package edu.umd.cs.findbugs.sourceViewer;
import java.text.CharacterIterator;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

public class DocumentCharacterIterator implements CharacterIterator {
	DocumentCharacterIterator(Document doc) {
		this.doc = doc;
		size = doc.getLength();
		text = new Segment();
		text.setPartialReturn(true);
		prep();
	}

	public Object clone() {
		throw new UnsupportedOperationException();
	}
	char nextChar = CharacterIterator.DONE;

	int pos = 0;

	private void prep() {
		if (nextChar != CharacterIterator.DONE)
			return;
		nextChar = text.next();
		if (nextChar != CharacterIterator.DONE)
			return;
		if (offset >= size)
			return;
		try {
			doc.getText(offset, size - offset, text);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
		offset += text.count;
		nextChar = text.current();
	}

	Document doc;

	int size;

	int offset = 0;

	Segment text;

	public char current() {
		prep();
		return nextChar;
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
		return pos;
	}

	public char last() {
		throw new UnsupportedOperationException();
	}

	public char next() {
		prep();
		char result = nextChar;
		pos++;
		nextChar = Segment.DONE;
		return result;
	}

	public char previous() {
		throw new UnsupportedOperationException();
	}

	public char setIndex(int position) {
		throw new UnsupportedOperationException();
	}

}

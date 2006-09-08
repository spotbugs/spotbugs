package edu.umd.cs.findbugs.sourceViewer;

import javax.swing.text.BoxView;
import javax.swing.text.Element;

// code from http://forum.java.sun.com/thread.jspa?threadID=622683&messageID=3547713
class NoWrapBoxView extends BoxView {
	public NoWrapBoxView(Element elem, int axis) {
		super(elem, axis);
	}

	public void layout(int width, int height) {
		super.layout(32768, height);
	}

	public float getMinimumSpan(int axis) {
		return super.getPreferredSpan(axis);
	}
}

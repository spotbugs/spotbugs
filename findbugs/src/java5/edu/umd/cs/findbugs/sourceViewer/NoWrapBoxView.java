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

import javax.swing.text.BoxView;
import javax.swing.text.Element;

// code from http://forum.java.sun.com/thread.jspa?threadID=622683&messageID=3547713
class NoWrapBoxView extends BoxView {
	public NoWrapBoxView(Element elem, int axis) {
		super(elem, axis);
	}

	@Override
	public void layout(int width, int height) {
		super.layout(32768, height);
	}

	@Override
	public float getMinimumSpan(int axis) {
		return super.getPreferredSpan(axis);
	}
}

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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;


public class HighlightInformation {

	Map<Integer, Color> map = new HashMap<Integer, Color>();
	private int foundLineNum = -1;

	public void clear() {
		map.clear();
		foundLineNum = -1;
	}
	public void setHighlight(int start, int end, Color color) {
		for(int i = start; i <= end; i++)
			map.put(i, color);
	}

	public void setHighlight(Integer line, Color color) {
		map.put(line, color);
	}

	public void unsetHighlight(Integer line) {
		map.remove(line);
	}

	public void updateFoundLineNum(Integer line) {
		if(foundLineNum != -1)
			unsetHighlight(foundLineNum);
		foundLineNum = line;
	}

	public @CheckForNull Color getHighlight(Integer line) {
		return map.get(line);
	}
}

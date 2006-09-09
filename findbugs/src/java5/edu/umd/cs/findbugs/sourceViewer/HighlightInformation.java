package edu.umd.cs.findbugs.sourceViewer;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;


public class HighlightInformation {

	Map<Integer, Color> map = new HashMap<Integer, Color>();
	
	
	public void clear() {
		map.clear();
	}
	public void setHighlight(int start, int end, Color color) {
		for(int i = start; i <= end; i++)
			map.put(i, color);
	}
	
	public void setHighlight(Integer line, Color color) {
		map.put(line, color);
	}
	public @CheckForNull Color getHighlight(Integer line) {
		return map.get(line);
	}
}

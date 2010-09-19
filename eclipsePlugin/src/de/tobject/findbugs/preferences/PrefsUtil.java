/*
 * Contributions to FindBugs
 * Copyright (C) 2010, Andrei Loskutov
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
package de.tobject.findbugs.preferences;

import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Misc. preferences utility methods
 *
 * @author Andrei Loskutov
 */
public class PrefsUtil {

    public static SortedSet<String> readDetectorPaths(IPreferenceStore prefs) {
        SortedSet<String> set = new TreeSet<String>();
        boolean keyFound = true;
		String keyPrefix = FindBugsConstants.KEY_CUSTOM_DETECTORS;
        int counter = 0;
        while (keyFound) {
            String property = prefs.getString(keyPrefix + counter);
			if (property != null && property.length() > 0) {
                set.add(property);
                counter++;
            } else {
				keyFound = false;
            }
        }
        return set;
	}

    public static void writeDetectorPaths(IPreferenceStore prefs, SortedSet<String> paths) {
        String keyPrefix = FindBugsConstants.KEY_CUSTOM_DETECTORS;
        int counter = 0;
		for (String s : paths) {
            prefs.setValue(keyPrefix + counter, s);
            counter++;
        }
		// remove obsolete keys from the properties file
        boolean keyFound = true;
        while (keyFound) {
            String key = keyPrefix + counter;
			String property = prefs.getString(key);
            if (property == null || property.length() > 0) {
                keyFound = false;
            } else {
				prefs.setValue(key, "");
            }
        }
    }
}

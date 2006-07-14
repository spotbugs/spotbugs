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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Utility methods for working with zip/jar archives.
 * 
 * @author David Hovemeyer
 */
public class Archive {

	/**
	 * File extensions that indicate an archive (zip, jar, or similar).
	 */
	static public final Set<String> ARCHIVE_EXTENSION_SET = new HashSet<String>();
	static {
		ARCHIVE_EXTENSION_SET.add(".jar");
		ARCHIVE_EXTENSION_SET.add(".zip");
		ARCHIVE_EXTENSION_SET.add(".war");
		ARCHIVE_EXTENSION_SET.add(".ear");
		ARCHIVE_EXTENSION_SET.add(".sar");
	}

	/**
	 * Determine whether or not the given filename appears to
	 * identify a zip/jar archive. 
	 * 
	 * @param fileName the filename
	 * @return true if the filename appears to identify an archive,
	 *          false otherwise
	 */
	public static boolean isArchiveFileName(String fileName) {
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot < 0) {
			return false;
		}
		String extension = fileName.substring(lastDot).toLowerCase(Locale.ENGLISH);
		return ARCHIVE_EXTENSION_SET.contains(extension);
	}
}

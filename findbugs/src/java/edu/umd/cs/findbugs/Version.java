/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.findbugs;

/**
 * Version number and release date information.
 */
public class Version {
	/** Major version number. */
	public static final int MAJOR = 0;

	/** Minor version number. */
	public static final int MINOR = 6;

	/** Patch level. */
	public static final int PATCHLEVEL = 5;

	/** Release version string. */
	public static final String RELEASE = MAJOR + "." + MINOR + "." + PATCHLEVEL;

	/** Release date. */
	public static final String DATE = "August 22, 2003";

	public static void main(String[] argv) {
		if (argv.length != 1)
			usage();

		String arg = argv[0];

		if (arg.equals("-release"))
			System.out.println(RELEASE);
		else if (arg.equals("-date"))
			System.out.println(DATE);
		else if (arg.equals("-props")) {
			System.out.println("release.number="+RELEASE);
			System.out.println("release.date="+DATE);
		} else
			usage();
	}

	private static void usage() {
		System.err.println("Usage: " + Version.class.getName() + 
                       "  (-release|-date|-props)");
		System.exit(1);
	}
}

// vim:ts=4

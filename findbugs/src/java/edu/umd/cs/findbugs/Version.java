/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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
	/**
	 * Major version number.
	 */
	public static final int MAJOR = 0;

	/**
	 * Minor version number.
	 */
	public static final int MINOR = 9;

	/**
	 * Patch level.
	 */
	public static final int PATCHLEVEL = 5;

	/**
	 * Development version or release candidate?
	 */
	public static final boolean IS_DEVELOPMENT = true;

	/**
	 * Release candidate number.
	 * "0" indicates that the version is not a release candidate.
	 */
	public static final int RELEASE_CANDIDATE = 0;

	private static final String RELEASE_SUFFIX_WORD =
		(RELEASE_CANDIDATE > 0 ? "rc" + RELEASE_CANDIDATE : "dev");

	/**
	 * Release version string.
	 */
	public static final String RELEASE =
		MAJOR + "." + MINOR + "." + PATCHLEVEL + (IS_DEVELOPMENT ? "-" + RELEASE_SUFFIX_WORD : "");

	/**
	 * Release date.
	 */
	public static final String DATE = "January 19, 2006";

	/**
	 * Version of Eclipse plugin.
	 */
	public static final String ECLIPSE_UI_VERSION =
		"0.0.17" + (IS_DEVELOPMENT ? "." + RELEASE_SUFFIX_WORD: "");

	/**
	 * FindBugs website.
	 */
	public static final String WEBSITE = "http://findbugs.sourceforge.net";

	/**
	 * Downloads website.
	 */
	public static final String DOWNLOADS_WEBSITE = "http://prdownloads.sourceforge.net/findbugs";

	/**
	 * Support email.
	 */
	public static final String SUPPORT_EMAIL = "http://findbugs.sourceforge.net/reportingBugs.html";

	public static void main(String[] argv) {
		if (argv.length != 1)
			usage();

		String arg = argv[0];

		if (arg.equals("-release"))
			System.out.println(RELEASE);
		else if (arg.equals("-date"))
			System.out.println(DATE);
		else if (arg.equals("-props")) {
			System.out.println("release.number=" + RELEASE);
			System.out.println("release.date=" + DATE);
			System.out.println("eclipse.ui.version=" + ECLIPSE_UI_VERSION);
			System.out.println("findbugs.website=" + WEBSITE);
			System.out.println("findbugs.downloads.website=" + DOWNLOADS_WEBSITE);
		} else {
			usage();
			System.exit(1);
		}
	}

	private static void usage() {
		System.err.println("Usage: " + Version.class.getName() +
		        "  (-release|-date|-props)");
	}
}

// vim:ts=4

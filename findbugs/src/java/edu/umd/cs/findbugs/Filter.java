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

import java.util.*;
import java.io.*;
import org.dom4j.*;
import org.dom4j.io.*;

public class Filter {
	private OrMatcher rootMatcher;

	public Filter(String fileName) throws IOException, FilterException {
		rootMatcher = new OrMatcher();

		parse(fileName);
	}

	private void parse(String fileName) throws IOException, FilterException {

		Document filterDoc = null;

		try {
			SAXReader reader = new SAXReader();
			filterDoc = reader.read(new BufferedInputStream(new FileInputStream(fileName)));
		} catch (DocumentException e) {
			throw new FilterException("Couldn't parse filter file " + fileName, e);
		}

		// Iterate over Exclude elements
		Iterator i = filterDoc.selectNodes("/FindBugsFilter/Exclude").iterator();
		while (i.hasNext()) {
			Node excludeNode = (Node) i.next();

			System.out.println("Exclude node");
		}

	}

	public static void main(String[] argv) {
		try {
			if (argv.length != 1) {
				System.err.println("Usage: " + Filter.class.getName() + " <filename>");
				System.exit(1);
			}

			Filter filter = new Filter(argv[0]);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}

// vim:ts=4

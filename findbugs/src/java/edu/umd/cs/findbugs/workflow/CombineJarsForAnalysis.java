/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.workflow;

import java.util.zip.*;
import java.util.*;
import java.io.*;

/**
 * @author pwilliam
 */
public class CombineJarsForAnalysis {

	public static void main(String args[]) throws Exception {
		ZipOutputStream analyzeOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream("analyze.jar")));
		String classPrefix = System.getProperty("prefix","");
		ZipOutputStream auxilaryOut = null;
		if (classPrefix.length() > 0)
			auxilaryOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream("auxilary.jar")));
		
		Set copied = new HashSet();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String fInName = in.readLine();
			if (fInName == null) break;
			System.err.println("Opening " + fInName);
			ZipFile zipInputFile = new ZipFile(fInName);

			byte buffer[] = new byte[8192];
			for (Enumeration e = zipInputFile.entries(); e.hasMoreElements();) {
				ZipEntry ze = (ZipEntry) e.nextElement();

				if (ze == null)
					break;
				if (ze.isDirectory())
					continue;

				String name = ze.getName();
				if (!name.endsWith(".class")) continue;
				if (copied.contains(name)) {
					System.err.println("Skipping duplicate of " + name);
					continue;
				}
				copied.add(name);
				ZipOutputStream out = name.replace('/', '.').startsWith(classPrefix) ? analyzeOut : auxilaryOut;
				analyzeOut.putNextEntry(new ZipEntry(name));

				InputStream zipIn = zipInputFile.getInputStream(ze);

				while (true) {
					int bytesRead = zipIn.read(buffer);
					if (bytesRead < 0)
						break;
					analyzeOut.write(buffer, 0, bytesRead);
				}
				analyzeOut.closeEntry();
			}
			zipInputFile.close();
		}
		analyzeOut.close();
		if (auxilaryOut != null) auxilaryOut.close();
	}
}

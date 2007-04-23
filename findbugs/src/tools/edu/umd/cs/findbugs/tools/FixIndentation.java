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

package edu.umd.cs.findbugs.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author pugh
 */
public class FixIndentation {
	static final String TABS = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t"
		+"\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";
	
	public static void main(String args[]) throws Exception {
		fix(new File(args[0]), true);
	}
	
	
	static String fix(String s) {
		if (s.length() == 0) return s;
		// if (s.trim().length() == 0) return "";
		int spaces = 0;
		boolean seenTabs = false;
		boolean badSpaces = false;
		int pos = 0;
		int indentation = 0;
		while(pos < s.length()) {
			char c = s.charAt(pos);
			if (c == ' ') {
				indentation++;
				spaces++;
			} else if (c == '\t') {
				indentation +=4;
				seenTabs = true;
				if (spaces > 0) badSpaces = true;
			} else break;
			pos++;
		}
		if (badSpaces || spaces > 1) {
			int tabs = (indentation+3)/4;
			String result = TABS.substring(0,tabs) + s.substring(pos);
			return result;
		}
		return s;
		
	}
	static void fix(File fileToUpdate, boolean partial) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fileToUpdate));
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		int consecutiveFixes = 0;
		try {
		while (true) {
			String s = in.readLine();
			if (s == null) break;
			String s2 = fix(s);
			if (s2 != s) {
				consecutiveFixes++;
				if (consecutiveFixes > 3 && partial) {
					s2 = s;
					consecutiveFixes = 0;
				}
			} else
				consecutiveFixes = 0;
			out.println(s2);
		}
		} finally {
		in.close();
		}
		StringReader stringReader = new StringReader(stringWriter.toString());
		FileWriter outFile = new FileWriter(fileToUpdate);
		char[] buffer = new char[1000];
		try {
		while(true) {
			int sz = stringReader.read(buffer);
			if (sz < 0) break;
			outFile.write(buffer,0,sz);
		}
		} finally {
		outFile.close();
		}
		
		
	}

}

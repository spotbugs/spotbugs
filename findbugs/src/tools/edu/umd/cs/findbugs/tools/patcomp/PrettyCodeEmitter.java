/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 University of Maryland
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

package edu.umd.cs.findbugs.tools.patcomp;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;

public class PrettyCodeEmitter implements CodeEmitter {
	private PrintStream out;
	private int indent;
	private String last = "";
	private boolean newline;

	private static final HashSet<String> noSpaceBefore = new HashSet<String>();
	private static final HashSet<String> noSpaceAfter = new HashSet<String>();
	private static final HashSet<String> alwaysSpaceBeforeAndAfter = new HashSet<String>();
	static {
		String[] both = new String[]{ ".", "(", ")", "[", "]"}; 
		String[] before = new String[]{ ",", ";" };
		noSpaceAfter.addAll(Arrays.asList(both));
		noSpaceBefore.addAll(Arrays.asList(both));
		noSpaceBefore.addAll(Arrays.asList(before));

		String[] always = new String[]{ "||","&&","=","?",":" };
		alwaysSpaceBeforeAndAfter.addAll(Arrays.asList(always));
	}

	public PrettyCodeEmitter(PrintStream out) {
		this.out = out;
		this.indent = 0;
		this.newline = true;
	}

	public void emitToken(String token) {
		if (token.equals("}")) {
			newline = true;
			--indent;
		}

		if (newline) {
			out.println();
			indent();
			newline = false;
		} else if (alwaysSpaceBeforeAndAfter.contains(last) || alwaysSpaceBeforeAndAfter.contains(token) ||
					!(noSpaceAfter.contains(last) || noSpaceBefore.contains(token)))
			out.print(" ");

		out.print(token);

		if (token.equals(";") || token.equals("}"))
			newline = true;
		else if (token.equals("{")) {
			newline = true;
			++indent;
		}

		last = token;
	}

	public void finish() throws IOException {
		out.println();
		out.flush();
	}

	private void indent() {
		for (int i = 0; i < indent; ++i)
			out.print("\t");
	}
}

// vim:ts=4

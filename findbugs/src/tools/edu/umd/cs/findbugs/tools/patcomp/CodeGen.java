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

import java.io.PrintStream;

/**
 * From a parsed FindBugs Pattern (.fbp) file,
 * generate Java source code for a bug pattern detector.
 *
 * @author David Hovemeyer
 */
public class CodeGen implements PatternCompilerTreeConstants {
	private PrintStream out;

	public CodeGen() {
	}

	public void generate(SimpleNode root, PrintStream out) {
		this.out = out;
		visit(root);
	}

	private void visit(SimpleNode node) {
		switch (node.getId()) {
		case JJTPRESCREEN:
			generatePrescreen(node);
			break;
		default:
			generateDefault(node);
			break;
		}
	}

	public void generateDefault(SimpleNode node) {
		int numChildren = node.jjtGetNumChildren();
		int numTokens = node.getNumTokens();
		if (numChildren == 0 && numTokens == 0)
			return;

		// If there are no child nodes, just emit all of
		// the tokens.
		if (numChildren == 0) {
			Token t = node.getFirstToken();
			while (t != node.getLastToken()) {
				out.println(t.image);
				t = t.next;
			}
			return;
		}

		// For each child, emit tokens preceeding that
		// child not covered by previous children,
		// then visit the child.
		Token t = node.getFirstToken();
		int childNum = 0;
		do {
			SimpleNode child = (SimpleNode) node.jjtGetChild(childNum);
			while (t != null && t != child.getFirstToken()) {
				out.println(t.image);
				t = t.next;
			}

			visit(child);

			if (child.getNumTokens() > 0)
				t = child.getLastToken().next;

			++childNum;
		} while (childNum < numChildren);

		// Emit rest of tokens
		while (t != null && t != node.getLastToken()) {
			out.println(t.image);
			t = t.next;
		}
	}

	public void generatePrescreen(SimpleNode node) {
		System.out.println("Generating prescreen code");
	}
}

// vim:ts=4

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

/**
 * From a parsed FindBugs Pattern (.fbp) file,
 * generate Java source code for a bug pattern detector.
 *
 * @author David Hovemeyer
 */
public class CodeGen implements PatternCompilerTreeConstants {
	private CodeEmitter emitter;

	public CodeGen() {
	}

	public void generate(SimpleNode root, CodeEmitter emitter) throws IOException {
		this.emitter = emitter;
		visit(root);
		emitter.finish();
	}

	private void visit(SimpleNode node) throws IOException {
		switch (node.getId()) {
		case JJTPRESCREEN:
			generatePrescreen(node);
			break;
		default:
			generateDefault(node);
			break;
		}
	}

	public void generateDefault(SimpleNode node) throws IOException {
//		System.out.println("START " + node.toString() +
//			": first=" + node.getFirstToken() + ", last=" + node.getLastToken());

		int numChildren = node.jjtGetNumChildren();

		SimpleNode child = null;
		Token t;

		// For each child, emit tokens preceeding that
		// child not covered by previous children,
		// then visit the child.
		int childNum = 0;
		while (childNum < numChildren) {
			SimpleNode nextChild = (SimpleNode) node.jjtGetChild(childNum);

			if (child == null)
				t = node.getFirstToken();
			else if (child.getLastToken() != nextChild.getFirstToken())
				t = child.getLastToken().next;
			else
				t = nextChild.getFirstToken();

			while (t != nextChild.getFirstToken()) {
				emitter.emitToken(t.image);
				t = t.next;
			}

			child = nextChild;

			visit(child);

			++childNum;
		}

		// Emit rest of tokens
		if (child == null)
			t = node.getFirstToken();
		else if (child.getLastToken() != node.getLastToken())
			t = child.getLastToken().next;
		else
			t = null;

		while (t != null) {
			emitter.emitToken(t.image);
			if (t == node.getLastToken())
				break;
			t = t.next;
		}

//		System.out.println("FINISH " + node.toString() +
//			": first=" + node.getFirstToken() + ", last=" + node.getLastToken());
	}

	public void generatePrescreen(SimpleNode node) throws IOException {
		System.out.println("Generating prescreen code");
	}
}

// vim:ts=4

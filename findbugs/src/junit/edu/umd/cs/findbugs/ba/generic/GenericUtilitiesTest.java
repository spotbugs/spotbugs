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

package edu.umd.cs.findbugs.ba.generic;

import junit.framework.TestCase;

import org.apache.bcel.generic.Type;

/**
 * @author pugh
 */
public class GenericUtilitiesTest extends TestCase {
	/**
	 * 
	 */
	private static final String SAMPLE_SIGNATURE = "Lcom/sleepycat/persist/EntityJoin<TPK;TE;>.JoinForwardCursor<TV;>;";

	public void testUnmatchedRightAngleBracket() {
		assertEquals(3,GenericUtilities.nextUnmatchedRightAngleBracket("<I>>", 0));
		assertEquals(1,GenericUtilities.nextUnmatchedRightAngleBracket("I><I>", 0));
	}
	public void testNestedSignature() {
		GenericObjectType t = (GenericObjectType) GenericUtilities.getType(SAMPLE_SIGNATURE);
		String s = t.getSignature();
		System.out.println(s);
		assertEquals(1,t.getNumParameters());
	}
	public void testMapSignature() {
		GenericObjectType t = (GenericObjectType) GenericUtilities.getType("Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;");
		String s = t.getSignature();
		System.out.println(s);
		assertEquals(2,t.getNumParameters());
	}
	public void testNestedSignatureParser() {
		GenericSignatureParser parser = new GenericSignatureParser("("+SAMPLE_SIGNATURE+")V");
		assertEquals(1,parser.getNumParameters());
	}
	public void testOKSignaturesThatHaveCausedProblems() {
		GenericUtilities.getType("[Ljava/util/Map$Entry<Ljava/lang/String;[B>");
		GenericUtilities.getType("[Ljava/util/Map<Ljava/lang/String;[Ljava/lang/String;>;");
	}
}

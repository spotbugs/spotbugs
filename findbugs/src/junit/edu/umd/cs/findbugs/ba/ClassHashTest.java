/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author David Hovemeyer
 */
public class ClassHashTest extends TestCase {
	
	byte[] hash;
	String s;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	//@Override
	protected void setUp() throws Exception {
		hash = new byte[]{0x06, 0x04, (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF};
		s = "0604deadbeef";
	}
	
	public void testHashToString() {
		String s2 = ClassHash.hashToString(hash);
		Assert.assertEquals(s, s2);
	}
	
	public void testStringToHash() {
		byte[] hash2 = ClassHash.stringToHash(s);
		Assert.assertTrue(Arrays.equals(hash, hash2));
	}
}

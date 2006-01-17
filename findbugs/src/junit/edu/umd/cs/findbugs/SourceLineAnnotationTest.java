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

package edu.umd.cs.findbugs;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author daveho
 */
public class SourceLineAnnotationTest extends TestCase{
	
	SourceLineAnnotation sl;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	//@Override
	protected void setUp() throws Exception {
		sl = new SourceLineAnnotation("com.foo.Bar", "com/foo/Bar.java", 10, 20, 30, 40);
//		sl.setSurroundingOpcodes("1,2,3,4,5|6,7,8|9,10,11");
	}
	
//	public void testEarlierOpcodes() {
//		Assert.assertEquals(sl.getEarlierOpcodesAsString(5), "1,2,3,4,5");
//		Assert.assertEquals(sl.getEarlierOpcodesAsString(69), "1,2,3,4,5");
//	}
//	
//	public void testSomeEarlierOpcodes() {
//		Assert.assertEquals(sl.getEarlierOpcodesAsString(2), "4,5");
//		Assert.assertEquals(sl.getEarlierOpcodesAsString(1), "5");
//		Assert.assertEquals(sl.getEarlierOpcodesAsString(0), "");
//	}
//	
//	public void testSelectedOpcodes() {
//		Assert.assertEquals(sl.getSelectedOpcodesAsString(), "6,7,8");
//		
//	}
//	
//	public void testLaterOpcodes() {
//		Assert.assertEquals(sl.getLaterOpcodesAsString(5), "9,10,11");
//		Assert.assertEquals(sl.getLaterOpcodesAsString(3), "9,10,11");
//		Assert.assertEquals(sl.getLaterOpcodesAsString(69), "9,10,11");
//	}
//	
//	public void testSomeLaterOpcodes() {
//		Assert.assertEquals(sl.getLaterOpcodesAsString(2), "9,10");
//		Assert.assertEquals(sl.getLaterOpcodesAsString(1), "9");
//		Assert.assertEquals(sl.getLaterOpcodesAsString(0), "");
//	}
}

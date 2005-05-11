package edu.umd.cs.findbugs.ba.npe;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NullParamPropertyTest extends TestCase {
	
	NullParamProperty empty;
	NullParamProperty nonEmpty;
	NullParamProperty extremes;
	
	//@Override
	protected void setUp() throws Exception {
		empty = new NullParamProperty();
		
		nonEmpty = new NullParamProperty();
		nonEmpty.setParamMightBeNull(11, true);
		nonEmpty.setParamMightBeNull(25, true);
		
		extremes = new NullParamProperty();
		extremes.setParamMightBeNull(0, true);
		extremes.setParamMightBeNull(31, true);
	}
	
	public void testEmpty() {
		for (int i = 0; i < 32; ++i) {
			Assert.assertFalse(empty.paramMightBeNull(i));
		}
	}
	
	public void testNonEmpty() {
		Assert.assertTrue(nonEmpty.paramMightBeNull(11));
		Assert.assertTrue(nonEmpty.paramMightBeNull(25));
		Assert.assertFalse(nonEmpty.paramMightBeNull(5));
	}
	
	public void testExtremes() {
		Assert.assertTrue(extremes.paramMightBeNull(0));
		Assert.assertTrue(extremes.paramMightBeNull(31));
		Assert.assertFalse(extremes.paramMightBeNull(10));
	}
	
	public void testOutOfBounds() {
		Assert.assertFalse(nonEmpty.paramMightBeNull(-1));
		Assert.assertFalse(nonEmpty.paramMightBeNull(32));
	}
}

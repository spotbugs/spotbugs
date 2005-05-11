package edu.umd.cs.findbugs.ba.npe;

import junit.framework.Assert;
import junit.framework.TestCase;

public class UnconditionalDerefPropertyTest extends TestCase {
	
	UnconditionalDerefProperty empty;
	UnconditionalDerefProperty nonEmpty;
	UnconditionalDerefProperty extremes;
	
	//@Override
	protected void setUp() throws Exception {
		empty = new UnconditionalDerefProperty();
		
		nonEmpty = new UnconditionalDerefProperty();
		nonEmpty.setParamUnconditionalDeref(11, true);
		nonEmpty.setParamUnconditionalDeref(25, true);
		
		extremes = new UnconditionalDerefProperty();
		extremes.setParamUnconditionalDeref(0, true);
		extremes.setParamUnconditionalDeref(31, true);
	}
	
	public void testEmpty() {
		for (int i = 0; i < 32; ++i) {
			Assert.assertFalse(empty.paramUnconditionalDeref(i));
		}
	}
	
	public void testIsEmpty() {
		Assert.assertTrue(empty.isEmpty());
		Assert.assertFalse(nonEmpty.isEmpty());
		Assert.assertFalse(extremes.isEmpty());
	}
	
	public void testNonEmpty() {
		Assert.assertTrue(nonEmpty.paramUnconditionalDeref(11));
		Assert.assertTrue(nonEmpty.paramUnconditionalDeref(25));
		Assert.assertFalse(nonEmpty.paramUnconditionalDeref(5));
	}
	
	public void testExtremes() {
		Assert.assertTrue(extremes.paramUnconditionalDeref(0));
		Assert.assertTrue(extremes.paramUnconditionalDeref(31));
		Assert.assertFalse(extremes.paramUnconditionalDeref(10));
	}
	
	public void testOutOfBounds() {
		Assert.assertFalse(nonEmpty.paramUnconditionalDeref(-1));
		Assert.assertFalse(nonEmpty.paramUnconditionalDeref(32));
	}
}

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
		nonEmpty.setUnconditionalDeref(11, true);
		nonEmpty.setUnconditionalDeref(25, true);
		
		extremes = new UnconditionalDerefProperty();
		extremes.setUnconditionalDeref(0, true);
		extremes.setUnconditionalDeref(31, true);
	}
	
	public void testEmpty() {
		for (int i = 0; i < 32; ++i) {
			Assert.assertFalse(empty.isUnconditionalDeref(i));
		}
	}
	
	public void testIsEmpty() {
		Assert.assertTrue(empty.isEmpty());
		Assert.assertFalse(nonEmpty.isEmpty());
		Assert.assertFalse(extremes.isEmpty());
	}
	
	public void testNonEmpty() {
		Assert.assertTrue(nonEmpty.isUnconditionalDeref(11));
		Assert.assertTrue(nonEmpty.isUnconditionalDeref(25));
		Assert.assertFalse(nonEmpty.isUnconditionalDeref(5));
	}
	
	public void testExtremes() {
		Assert.assertTrue(extremes.isUnconditionalDeref(0));
		Assert.assertTrue(extremes.isUnconditionalDeref(31));
		Assert.assertFalse(extremes.isUnconditionalDeref(10));
	}
	
	public void testOutOfBounds() {
		Assert.assertFalse(nonEmpty.isUnconditionalDeref(-1));
		Assert.assertFalse(nonEmpty.isUnconditionalDeref(32));
	}
}

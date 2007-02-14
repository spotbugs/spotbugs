package edu.umd.cs.findbugs.ba.npe;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NonNullParamPropertyTest extends TestCase {
	
	ParameterNullnessProperty empty;
	ParameterNullnessProperty nonEmpty;
	ParameterNullnessProperty extremes;
	
	
	@Override
         protected void setUp() throws Exception {
		empty = new ParameterNullnessProperty();
		
		nonEmpty = new ParameterNullnessProperty();
		nonEmpty.setNonNull(11, true);
		nonEmpty.setNonNull(25, true);
		
		extremes = new ParameterNullnessProperty();
		extremes.setNonNull(0, true);
		extremes.setNonNull(31, true);
	}
	
	public void testEmpty() {
		for (int i = 0; i < 32; ++i) {
			Assert.assertFalse(empty.isNonNull(i));
		}
	}
	
	public void testIsEmpty() {
		Assert.assertTrue(empty.isEmpty());
		Assert.assertFalse(nonEmpty.isEmpty());
		Assert.assertFalse(extremes.isEmpty());
	}
	
	public void testNonEmpty() {
		Assert.assertTrue(nonEmpty.isNonNull(11));
		Assert.assertTrue(nonEmpty.isNonNull(25));
		Assert.assertFalse(nonEmpty.isNonNull(5));
	}
	
	public void testExtremes() {
		Assert.assertTrue(extremes.isNonNull(0));
		Assert.assertTrue(extremes.isNonNull(31));
		Assert.assertFalse(extremes.isNonNull(10));
	}
	
	public void testOutOfBounds() {
		Assert.assertFalse(nonEmpty.isNonNull(-1));
		Assert.assertFalse(nonEmpty.isNonNull(32));
	}
}

package edu.umd.cs.findbugs;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TimestampIntervalCollectionTest extends TestCase {
	TimestampIntervalCollection c1;
	TimestampIntervalCollection c2;
	
	@Override
	protected void setUp() throws Exception {
		c1 = TimestampIntervalCollection.decode("0-4,6-10");
		c2 = TimestampIntervalCollection.decode("20-30,5-10,8-15");
	}
	
	public void testContains() {
		Assert.assertTrue(c1.contains(0L));
		Assert.assertTrue(c1.contains(3L));
		Assert.assertTrue(c1.contains(4L));
		Assert.assertTrue(c1.contains(6L));
		Assert.assertTrue(c1.contains(7L));
		Assert.assertTrue(c1.contains(10L));
	}
	
	public void testNotContains() {
		Assert.assertFalse(c1.contains(5L));
		Assert.assertFalse(c1.contains(11L));
	}
	
	public void testSimplify() {
		Assert.assertEquals(TimestampIntervalCollection.encode(c2), "5-15,20-30");
		Assert.assertTrue(c2.contains(5L));
		Assert.assertTrue(c2.contains(10L));
		Assert.assertTrue(c2.contains(15L));
		Assert.assertTrue(c2.contains(20L));
		Assert.assertTrue(c2.contains(25L));
		Assert.assertTrue(c2.contains(30L));
		Assert.assertFalse(c2.contains(1L));
		Assert.assertFalse(c2.contains(17L));
		Assert.assertFalse(c2.contains(31L));
	}
}

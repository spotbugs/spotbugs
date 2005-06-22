package edu.umd.cs.findbugs;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TimestampIntervalTest extends TestCase {
	
	SequenceInterval t1;
	SequenceInterval t1Copy;
	SequenceInterval t2;
	SequenceInterval t3;
	SequenceInterval t4;
	SequenceInterval t4SameBegin;
	
	// @Override
	protected void setUp() throws Exception {
		t1 = new SequenceInterval(0L, 4L);
		t1Copy = new SequenceInterval(0L, 4L);
		t2 = new SequenceInterval(1L, 3L);
		t3 = new SequenceInterval(4L, 10L);
		t4 = new SequenceInterval(5L, 10L);
		t4SameBegin = new SequenceInterval(5L, 11L);
	}
	
	public void testAccess() {
		Assert.assertEquals(t1.getBegin(), 0L);
		Assert.assertEquals(t1.getEnd(), 4L);
	}
	
	public void testOverlapSelf() {
		Assert.assertTrue(SequenceInterval.overlap(t1, t1));
	}
	
	public void testOverlapOther() {
		Assert.assertTrue(SequenceInterval.overlap(t1, t2));
		Assert.assertTrue(SequenceInterval.overlap(t2, t1));
	}
	
	public void testNotOverlapOther() {
		Assert.assertFalse(SequenceInterval.overlap(t1, t4));
		Assert.assertFalse(SequenceInterval.overlap(t4, t1));
	}
	
	public void testOverlapEdgeCase() {
		Assert.assertTrue(SequenceInterval.overlap(t1, t3));
		Assert.assertTrue(SequenceInterval.overlap(t3, t1));
	}
	
	public void testInteriorMerge() {
		SequenceInterval m = SequenceInterval.merge(t1, t2);
		Assert.assertEquals(t1, m);
		Assert.assertEquals(m, t1);
	}
	
	public void testMerge() {
		SequenceInterval m = SequenceInterval.merge(t1, t3);
		Assert.assertEquals(m.getBegin(), 0L);
		Assert.assertEquals(m.getEnd(), 10L);
	}
	
	public void testEquals() {
		Assert.assertEquals(t1, t1);
		Assert.assertEquals(t1, t1Copy);
	}
	
	public void testCompare() {
		Assert.assertTrue(t1.compareTo(t2) < 0);
		Assert.assertTrue(t2.compareTo(t1) > 0);
		Assert.assertTrue(t1.compareTo(t1) == 0);
	}
	
	public void testCompareSameBegin() {
		Assert.assertTrue(t4.compareTo(t4SameBegin) < 0);
	}
	
	public void testDecodeMoment() throws InvalidSequenceIntervalException {
		SequenceInterval m = SequenceInterval.decode("1");
		Assert.assertEquals(m.getBegin(), 1L);
		Assert.assertEquals(m.getEnd(), 1L);
	}
	
	public void testDecodeInterval() throws InvalidSequenceIntervalException {
		SequenceInterval m = SequenceInterval.decode("1-4");
		Assert.assertEquals(m.getBegin(), 1L);
		Assert.assertEquals(m.getEnd(), 4L);
	}
	
	public void testContains() {
		Assert.assertTrue(t1.contains(0L));
		Assert.assertTrue(t1.contains(1L));
		Assert.assertTrue(t1.contains(4L));
	}
	
	public void testNotContains() {
		Assert.assertFalse(t1.contains(5L));
		Assert.assertFalse(t3.contains(1L));
	}
}

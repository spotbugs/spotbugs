package edu.umd.cs.findbugs;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TimestampIntervalTest extends TestCase {
	
	TimestampInterval t1;
	TimestampInterval t1Copy;
	TimestampInterval t2;
	TimestampInterval t3;
	TimestampInterval t4;
	
	@Override
	protected void setUp() throws Exception {
		t1 = new TimestampInterval(0L, 4L);
		t1Copy = new TimestampInterval(0L, 4L);
		t2 = new TimestampInterval(1L, 3L);
		t3 = new TimestampInterval(4L, 10L);
		t4 = new TimestampInterval(5L, 10L);
	}
	
	public void testAccess() {
		Assert.assertEquals(t1.getBegin(), 0L);
		Assert.assertEquals(t1.getEnd(), 4L);
	}
	
	public void testOverlapSelf() {
		Assert.assertTrue(TimestampInterval.overlap(t1, t1));
	}
	
	public void testOverlapOther() {
		Assert.assertTrue(TimestampInterval.overlap(t1, t2));
		Assert.assertTrue(TimestampInterval.overlap(t2, t1));
	}
	
	public void testNotOverlapOther() {
		Assert.assertFalse(TimestampInterval.overlap(t1, t4));
		Assert.assertFalse(TimestampInterval.overlap(t4, t1));
	}
	
	public void testOverlapEdgeCase() {
		Assert.assertTrue(TimestampInterval.overlap(t1, t3));
		Assert.assertTrue(TimestampInterval.overlap(t3, t1));
	}
	
	public void testInteriorMerge() {
		TimestampInterval m = TimestampInterval.merge(t1, t2);
		Assert.assertEquals(t1, m);
		Assert.assertEquals(m, t1);
	}
	
	public void testMerge() {
		TimestampInterval m = TimestampInterval.merge(t1, t3);
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
	
	public void testDecodeMoment() {
		TimestampInterval m = TimestampInterval.decode("1");
		Assert.assertEquals(m.getBegin(), 1L);
		Assert.assertEquals(m.getEnd(), 1L);
	}
	
	public void testDecodeInterval() {
		TimestampInterval m = TimestampInterval.decode("1-4");
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

package edu.umd.cs.findbugs.ba;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

public class LongRangeSetTest {

    @Test
    public void testEQ() {
        LongRangeSet set = new LongRangeSet("I");
        set = set.eq(100);
        assertTrue("{100}".equals(set.toString()));
    }

    @Test
    public void testNE() {
        LongRangeSet set = new LongRangeSet("I");
        set = set.ne(100);
        assertTrue(("[" + Integer.MIN_VALUE + ", 99]+[101, " + Integer.MAX_VALUE + "]").equals(set.toString()));
    }

    @Test
    public void testLT() {
        LongRangeSet set = new LongRangeSet("I");
        set = set.lt(100);
        assertTrue(("[" + Integer.MIN_VALUE + ", 99]").equals(set.toString()));
    }

    @Test
    public void testLE() {
        LongRangeSet set = new LongRangeSet("I");
        set = set.le(100);
        assertTrue(("[" + Integer.MIN_VALUE + ", 100]").equals(set.toString()));
    }

    @Test
    public void testGT() {
        LongRangeSet set = new LongRangeSet("I");
        set = set.gt(100);
        assertTrue(("[101, " + Integer.MAX_VALUE + "]").equals(set.toString()));
    }

    @Test
    public void testGE() {
        LongRangeSet set = new LongRangeSet("I");
        set = set.ge(100);
        assertTrue(("[100, " + Integer.MAX_VALUE + "]").equals(set.toString()));
    }

    @Test
    public void testBorders1() {
        LongRangeSet set = new LongRangeSet("J").ne(0);
        Set<Long> borders = set.getBorders();
        assertTrue(borders.size() == 2);
        assertTrue(borders.contains(Long.MIN_VALUE));
        assertTrue(borders.contains(Long.MAX_VALUE));
    }

    @Test
    public void testBorders2() {
        LongRangeSet set = new LongRangeSet("J").le(100).ne(0);
        Set<Long> borders = set.getBorders();
        assertTrue(borders.size() == 3);
        assertTrue(borders.contains(Long.MIN_VALUE));
        assertTrue(borders.contains(Long.valueOf(100)));
        assertTrue(borders.contains(Long.valueOf(101)));
    }

    @Test
    public void testBorders3() {
        LongRangeSet set = new LongRangeSet("J").ge(100).ne(0);
        Set<Long> borders = set.getBorders();
        assertTrue(borders.size() == 3);
        assertTrue(borders.contains(Long.valueOf(99)));
        assertTrue(borders.contains(Long.valueOf(100)));
        assertTrue(borders.contains(Long.MAX_VALUE));
    }

    @Test
    public void testBorders4() {
        LongRangeSet set = new LongRangeSet("J").ge(-100).le(100).ne(0);
        Set<Long> borders = set.getBorders();
        assertTrue(borders.size() == 4);
        assertTrue(borders.contains(Long.valueOf(-101)));
        assertTrue(borders.contains(Long.valueOf(-100)));
        assertTrue(borders.contains(Long.valueOf(100)));
        assertTrue(borders.contains(Long.valueOf(101)));
    }

    @Test
    public void testGetTypeMinMax() {
        LongRangeSet set = new LongRangeSet("S");
        assertTrue(set.getTypeMin() == Short.MIN_VALUE);
        assertTrue(set.getTypeMax() == Short.MAX_VALUE);
    }

    @Test
    public void testInTypeRange() {
        LongRangeSet set = new LongRangeSet("B").ne(0);
        assertTrue(set.inTypeRange(Byte.MIN_VALUE));
        assertTrue(set.inTypeRange(Byte.MAX_VALUE));
        assertTrue(set.inTypeRange(0));
        assertFalse(set.inTypeRange(Byte.MIN_VALUE - 1));
        assertFalse(set.inTypeRange(Byte.MAX_VALUE + 1));
    }

    @Test
    public void testContains() {
        LongRangeSet set = new LongRangeSet("B").ne(0);
        assertTrue(set.contains(Byte.MIN_VALUE));
        assertTrue(set.contains(Byte.MAX_VALUE));
        assertFalse(set.contains(0));
        assertFalse(set.contains(Byte.MIN_VALUE - 1));
        assertFalse(set.contains(Byte.MAX_VALUE + 1));
    }

    @Test
    public void testIntersects1() {
        LongRangeSet set1 = new LongRangeSet("I").le(0);
        LongRangeSet set2 = new LongRangeSet("I").ge(0);
        assertTrue(set1.intersects(set2));
    }

    @Test
    public void testIntersects2() {
        LongRangeSet set1 = new LongRangeSet("I").le(0);
        LongRangeSet set2 = new LongRangeSet("I").gt(0);
        assertFalse(set1.intersects(set2));
    }

    @Test
    public void testIntersects3() {
        LongRangeSet set1 = new LongRangeSet("I").gt(0).ne(10).lt(20);
        LongRangeSet set2 = new LongRangeSet("I").ge(15).lt(25);
        assertTrue(set1.intersects(set2));
    }

    @Test
    public void testIntersects4() {
        LongRangeSet set1 = new LongRangeSet("I").gt(0).ne(3).lt(6);
        LongRangeSet set2 = new LongRangeSet("I").ge(3).ne(4).ne(5).le(6);
        assertFalse(set1.intersects(set2));
    }

    @Test
    public void testRestrict() {
        LongRangeSet set = new LongRangeSet("J");
        set.restrict("I");
        assertTrue(set.contains(Integer.MIN_VALUE));
        assertTrue(set.contains(Integer.MAX_VALUE));
        assertTrue(set.contains(0));
        assertFalse(set.contains(Integer.MIN_VALUE - 1l));
        assertFalse(set.contains(Integer.MAX_VALUE + 1l));
    }

    @Test
    public void testIsEmpty() {
        LongRangeSet set = new LongRangeSet("S");
        assertFalse(set.isEmpty());
        set = set.eq(0);
        assertFalse(set.isEmpty());
        set = set.ne(0);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testIsFull() {
        LongRangeSet set = new LongRangeSet("S");
        assertTrue(set.isFull());
        set = set.ne(0);
        assertFalse(set.isFull());
    }

    @Test
    public void testAdd1() {
        LongRangeSet set1 = new LongRangeSet("I").ge(0).le(10);
        LongRangeSet set2 = new LongRangeSet("I").ge(12).le(20);
        LongRangeSet set3 = set1.add(set2);
        assertTrue("[0, 10]+[12, 20]".equals(set3.toString()));
    }

    @Test
    public void testAdd2() {
        LongRangeSet set1 = new LongRangeSet("I").ge(0).le(10);
        LongRangeSet set2 = new LongRangeSet("I").ge(11).le(20);
        LongRangeSet set3 = set1.add(set2);
        assertTrue("[0, 20]".equals(set3.toString()));
    }

    @Test
    public void testAdd3() {
        LongRangeSet set1 = new LongRangeSet("I").ge(0).le(10);
        LongRangeSet set2 = new LongRangeSet("I").ge(10).le(20);
        LongRangeSet set3 = set1.add(set2);
        assertTrue("[0, 20]".equals(set3.toString()));
    }

    @Test
    public void testAdd4() {
        LongRangeSet set1 = new LongRangeSet("I").ge(0).le(11);
        LongRangeSet set2 = new LongRangeSet("I").ge(9).le(20);
        LongRangeSet set3 = set1.add(set2);
        assertTrue("[0, 20]".equals(set3.toString()));
    }

    @Test
    public void testAdd5() {
        LongRangeSet set1 = new LongRangeSet("I").ge(0).le(9);
        LongRangeSet set2 = new LongRangeSet("I").eq(10);
        LongRangeSet set3 = new LongRangeSet("I").ge(11).le(20);
        LongRangeSet set4 = set1.add(set2).add(set3);
        assertTrue("[0, 20]".equals(set4.toString()));
    }
}

package edu.umd.cs.findbugs;

import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

public class BugInstanceTest extends TestCase {
	static {
		// Load detector plugins
		DetectorFactoryCollection.instance();
	}
	
	BugInstance b;
	
	//@Override
	protected void setUp() throws Exception {
		b = new BugInstance("NP_NULL_DEREF", Detector.NORMAL_PRIORITY);
		b.setProperty("A", "a");
		b.setProperty("B", "b");
		b.setProperty("C", "c");
	}
	
	public void testPropertyIterator() {
		checkPropertyIterator(b.propertyIterator(),
				new String[]{"A","B","C"}, new String[]{"a","b","c"});
	}
	
	public void testRemoveThroughIterator1() {
		removeThroughIterator(b.propertyIterator(), "A");
		checkPropertyIterator(b.propertyIterator(), new String[]{"B","C"}, new String[]{"b","c"});
	}
	
	public void testRemoveThroughIterator2() {
		removeThroughIterator(b.propertyIterator(), "B");
		checkPropertyIterator(b.propertyIterator(), new String[]{"A","C"}, new String[]{"a","c"});
	}
	
	public void testRemoveThroughIterator3() {
		removeThroughIterator(b.propertyIterator(), "C");
		checkPropertyIterator(b.propertyIterator(), new String[]{"A","B"}, new String[]{"a","b"});
	}

	private void checkPropertyIterator(Iterator<BugProperty> iter, String[] names, String[] values) {
		if (names.length != values.length)
			throw new IllegalArgumentException();
		for (int i = 0; i < names.length; ++i) {
			Assert.assertTrue(iter.hasNext());
			String name = names[i];
			String value = values[i];
			checkProperty(iter.next(), name, value);
		}
		Assert.assertFalse(iter.hasNext());
	}
	
	private void checkProperty(BugProperty property, String name, String value) {
		Assert.assertEquals(property.getName(), name);
		Assert.assertEquals(property.getValue(), value);
	}
	
	private void removeThroughIterator(Iterator<BugProperty> iter, String name) {
		while (iter.hasNext()) {
			BugProperty prop = iter.next();
			if (prop.getName().equals(name))
				iter.remove();
		}
	}
}

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
		Iterator<BugProperty> i = b.propertyIterator();
		Assert.assertTrue(i.hasNext());
		checkProperty(i.next(), "A", "a");
		Assert.assertTrue(i.hasNext());
		checkProperty(i.next(), "B", "b");
		Assert.assertTrue(i.hasNext());
		checkProperty(i.next(), "C", "c");
		Assert.assertFalse(i.hasNext());
	}

	private void checkProperty(BugProperty property, String name, String value) {
		Assert.assertEquals(property.getName(), name);
		Assert.assertEquals(property.getValue(), value);
	}
}

package edu.umd.cs.findbugs.ba.type;

import junit.framework.Assert;
import junit.framework.TestCase;

public class XTopTypeTest extends TestCase {
	private XTopType top;

	protected void setUp() {
		top = new XTopType();
	}

	public void testNotReferenceType() {
		Assert.assertFalse(top.isReferenceType());
	}

	public void testNotBasicType() {
		Assert.assertFalse(top.isBasicType());
	}

	public void testNotValidArrayElementType() {
		Assert.assertFalse(top.isValidArrayElementType());
	}

	public void testEquals() {
		XTopType otherTop = new XTopType();
		XBottomType bottom = new XBottomType();

		Assert.assertEquals(top, otherTop);
		Assert.assertFalse(top.equals(bottom));
	}
}

// vim:ts=4

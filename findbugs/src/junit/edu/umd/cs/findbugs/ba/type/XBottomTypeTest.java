package edu.umd.cs.findbugs.ba.type;

import junit.framework.Assert;
import junit.framework.TestCase;

public class XBottomTypeTest extends TestCase {
	private XBottomType bottom;

	protected void setUp() {
		bottom = new XBottomType();
	}

	public void testNotReferenceType() {
		Assert.assertFalse(bottom.isReferenceType());
	}

	public void testNotBasicType() {
		Assert.assertFalse(bottom.isBasicType());
	}

	public void testNotValidArrayElementType() {
		Assert.assertFalse(bottom.isValidArrayElementType());
	}

	public void testEquals() {
		XBottomType otherBottom = new XBottomType();
		XTopType top = new XTopType();

		Assert.assertEquals(bottom, otherBottom);
		Assert.assertFalse(bottom.equals(top));
	}
}

// vim:ts=4

package edu.umd.cs.findbugs.ba.type;

import junit.framework.Assert;
import junit.framework.TestCase;

public class XNullTypeTest extends TestCase {
	private XNullType nullType;

	protected void setUp() {
		nullType = new XNullType();
	}

	public void testIsReferenceType() {
		Assert.assertTrue(nullType.isReferenceType());
	}

	public void testNotBasicType() {
		Assert.assertFalse(nullType.isBasicType());
	}

	public void testNotValidArrayElementType() {
		Assert.assertFalse(nullType.isValidArrayElementType());
	}

	public void testEquals() {
		XNullType otherNull = new XNullType();
		XBottomType bottom = new XBottomType();

		Assert.assertEquals(nullType, otherNull);
		Assert.assertFalse(nullType.equals(bottom));
	}
}

// vim:ts=4

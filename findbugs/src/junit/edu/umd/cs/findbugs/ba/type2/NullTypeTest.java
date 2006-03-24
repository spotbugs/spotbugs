package edu.umd.cs.findbugs.ba.type2;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NullTypeTest extends TestCase {
	private NullType nullType;

	@Override
         protected void setUp() {
		nullType = new NullType();
	}

	public void testGetSignature() {
		Assert.assertEquals(nullType.getSignature(), SpecialTypeSignatures.NULL_TYPE_SIGNATURE);
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

	public void testNotValidArrayBaseType() {
		Assert.assertFalse(nullType.isValidArrayBaseType());
	}

	public void testEquals() {
		NullType otherNull = new NullType();
		BottomType bottom = new BottomType();

		Assert.assertEquals(nullType, otherNull);
		Assert.assertFalse(nullType.equals(bottom));
	}
}

// vim:ts=4

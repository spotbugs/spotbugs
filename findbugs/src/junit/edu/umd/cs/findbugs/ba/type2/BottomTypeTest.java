package edu.umd.cs.findbugs.ba.type2;

import junit.framework.Assert;
import junit.framework.TestCase;

public class BottomTypeTest extends TestCase {
	private BottomType bottom;

	@Override
         protected void setUp() {
		bottom = new BottomType();
	}

	public void testGetSignature() {
		Assert.assertEquals(bottom.getSignature(), SpecialTypeSignatures.BOTTOM_TYPE_SIGNATURE);
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

	public void testNotValidArrayBaseType() {
		Assert.assertFalse(bottom.isValidArrayBaseType());
	}

	public void testEquals() {
		BottomType otherBottom = new BottomType();
		TopType top = new TopType();

		Assert.assertEquals(bottom, otherBottom);
		Assert.assertFalse(bottom.equals(top));
	}
}

// vim:ts=4

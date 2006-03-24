package edu.umd.cs.findbugs.ba.type2;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TopTypeTest extends TestCase {
	private TopType top;

	@Override
         protected void setUp() {
		top = new TopType();
	}

	public void testGetSignature() {
		Assert.assertEquals(top.getSignature(), SpecialTypeSignatures.TOP_TYPE_SIGNATURE);
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

	public void testNotValidArrayBaseType() {
		Assert.assertFalse(top.isValidArrayBaseType());
	}

	public void testEquals() {
		TopType otherTop = new TopType();
		BottomType bottom = new BottomType();

		Assert.assertEquals(top, otherTop);
		Assert.assertFalse(top.equals(bottom));
	}
}

// vim:ts=4

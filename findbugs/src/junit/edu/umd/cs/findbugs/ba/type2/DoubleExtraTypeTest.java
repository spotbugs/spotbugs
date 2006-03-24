package edu.umd.cs.findbugs.ba.type2;

import junit.framework.Assert;
import junit.framework.TestCase;
import edu.umd.cs.findbugs.ba.type.ExtendedTypes;

public class DoubleExtraTypeTest extends TestCase {
	private DoubleExtraType doubleExtraType;

	@Override
         protected void setUp() {
		doubleExtraType = new DoubleExtraType();
	}

	public void testGetSignature() {
		Assert.assertEquals(doubleExtraType.getSignature(), SpecialTypeSignatures.DOUBLE_EXTRA_TYPE_SIGNATURE);
	}

	public void testGetTypeCode() {
		Assert.assertTrue(doubleExtraType.getTypeCode() == ExtendedTypes.T_DOUBLE_EXTRA);
	}

	public void testNotReferenceType() {
		Assert.assertFalse(doubleExtraType.isReferenceType());
	}

	public void testNotBasicType() {
		Assert.assertFalse(doubleExtraType.isBasicType());
	}

	public void testNotValidArrayElementType() {
		Assert.assertFalse(doubleExtraType.isValidArrayElementType());
	}

	public void testNotValidArrayBaseType() {
		Assert.assertFalse(doubleExtraType.isValidArrayBaseType());
	}

	public void testEquals() {
		DoubleExtraType otherDoubleExtra = new DoubleExtraType();
		BottomType bottom = new BottomType();

		Assert.assertEquals(doubleExtraType, otherDoubleExtra);
		Assert.assertFalse(doubleExtraType.equals(bottom));
	}
}

// vim:ts=4

package edu.umd.cs.findbugs.ba.type;

import edu.umd.cs.findbugs.ba.ExtendedTypes;

import junit.framework.Assert;
import junit.framework.TestCase;

public class LongExtraTypeTest extends TestCase {
	private LongExtraType longExtraType;

	protected void setUp() {
		longExtraType = new LongExtraType();
	}

	public void testGetSignature() {
		Assert.assertEquals(longExtraType.getSignature(), SpecialTypeSignatures.LONG_EXTRA_TYPE_SIGNATURE);
	}

	public void testGetTypeCode() {
		Assert.assertTrue(longExtraType.getTypeCode() == ExtendedTypes.T_LONG_EXTRA);
	}

	public void testNotReferenceType() {
		Assert.assertFalse(longExtraType.isReferenceType());
	}

	public void testNotBasicType() {
		Assert.assertFalse(longExtraType.isBasicType());
	}

	public void testNotValidArrayElementType() {
		Assert.assertFalse(longExtraType.isValidArrayElementType());
	}

	public void testNotValidArrayBaseType() {
		Assert.assertFalse(longExtraType.isValidArrayBaseType());
	}

	public void testEquals() {
		LongExtraType otherLongExtra = new LongExtraType();
		BottomType bottom = new BottomType();

		Assert.assertEquals(longExtraType, otherLongExtra);
		Assert.assertFalse(longExtraType.equals(bottom));
	}
}

// vim:ts=4

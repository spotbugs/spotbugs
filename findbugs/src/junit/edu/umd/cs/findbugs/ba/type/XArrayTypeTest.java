package edu.umd.cs.findbugs.ba.type;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bcel.Constants;

public class XArrayTypeTest extends TestCase {
	private XBasicType byteType;
	private XBasicType intType;
	private XClassType stringType;

	private XArrayType byteArray1;
	private XArrayType byteArray2;

	private XArrayType intArray2;

	private XArrayType stringArray3;

	protected void setUp() throws InvalidSignatureException {
		byteType = new XBasicType(Constants.T_BYTE);
		intType = new XBasicType(Constants.T_INT);
		stringType = new XClassType("Ljava/lang/String;");
		byteArray1 = new XArrayType(1, byteType);
		byteArray2 = new XArrayType(2, byteType);
		intArray2 = new XArrayType(2, intType);
		stringArray3 = new XArrayType(3, stringType);
	}

	public void testByteArraySig() {
		Assert.assertEquals(byteArray1.getSignature(), "[B");
		Assert.assertEquals(byteArray2.getSignature(), "[[B");
		Assert.assertEquals(intArray2.getSignature(), "[[I");
		Assert.assertEquals(stringArray3.getSignature(), "[[[Ljava/lang/String;");
	}

	public void testNumDimensions() {
		Assert.assertEquals(byteArray1.getNumDimensions(), 1);
		Assert.assertEquals(byteArray2.getNumDimensions(), 2);
		Assert.assertEquals(intArray2.getNumDimensions(), 2);
		Assert.assertEquals(stringArray3.getNumDimensions(), 3);
	}

	public void testGetBaseType() {
		Assert.assertEquals(byteArray1.getBaseType(), byteType);
		Assert.assertEquals(byteArray2.getBaseType(), byteType);
		Assert.assertEquals(intArray2.getBaseType(), intType);
		Assert.assertEquals(stringArray3.getBaseType(), stringType);
	}

	public void testEquals() {
		XArrayType byteArray1Dup = new XArrayType(1, byteType);
		XArrayType byteArray2Dup = new XArrayType(2, byteType);
		XArrayType intArray2Dup = new XArrayType(2, intType);
		XArrayType stringArray3Dup = new XArrayType(3, stringType);

		Assert.assertEquals(byteArray1, byteArray1Dup);
		Assert.assertEquals(byteArray2, byteArray2Dup);
		Assert.assertEquals(intArray2, intArray2Dup);
		Assert.assertEquals(stringArray3, stringArray3Dup);
	}

	public void testNotEquals() {
		Assert.assertFalse(byteArray1.equals(byteArray2));
		Assert.assertFalse(stringArray3.equals(intArray2));
	}

	public void testIsReferenceType() {
		Assert.assertTrue(byteArray1.isReferenceType());
		Assert.assertTrue(byteArray2.isReferenceType());
		Assert.assertTrue(intArray2.isReferenceType());
		Assert.assertTrue(stringArray3.isReferenceType());
	}

	public void testNotBasicType() {
		Assert.assertFalse(byteArray1.isBasicType());
		Assert.assertFalse(byteArray2.isBasicType());
		Assert.assertFalse(intArray2.isBasicType());
		Assert.assertFalse(stringArray3.isBasicType());
	}

	public void testIsValidArrayElementType() {
		Assert.assertTrue(byteArray1.isValidArrayElementType());
		Assert.assertTrue(byteArray2.isValidArrayElementType());
		Assert.assertTrue(intArray2.isValidArrayElementType());
		Assert.assertTrue(stringArray3.isValidArrayElementType());
	}
}

// vim:ts=4

package edu.umd.cs.findbugs.ba.type2;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bcel.Constants;

public class ArrayTypeTest extends TestCase {
	private BasicType byteType;
	private BasicType intType;
	private ClassType stringType;

	private ArrayType byteArray1;
	private ArrayType byteArray2;

	private ArrayType intArray2;

	private ArrayType stringArray3;

	@Override
         protected void setUp() throws InvalidSignatureException {
		byteType = new BasicType(Constants.T_BYTE);
		intType = new BasicType(Constants.T_INT);
		stringType = new ClassType("Ljava/lang/String;");
		byteArray1 = new ArrayType(1, byteType);
		byteArray2 = new ArrayType(2, byteType);
		intArray2 = new ArrayType(2, intType);
		stringArray3 = new ArrayType(3, stringType);
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
		ArrayType byteArray1Dup = new ArrayType(1, byteType);
		ArrayType byteArray2Dup = new ArrayType(2, byteType);
		ArrayType intArray2Dup = new ArrayType(2, intType);
		ArrayType stringArray3Dup = new ArrayType(3, stringType);

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

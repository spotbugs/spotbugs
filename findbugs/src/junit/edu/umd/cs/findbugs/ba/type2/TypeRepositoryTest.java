package edu.umd.cs.findbugs.ba.type2;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.ba.type.ExtendedTypes;

/**
 * Unit tests for TypeRepository.
 */
public class TypeRepositoryTest extends TestCase {

	/**
	 * A do-nothing class resolver that just throws
	 * ClassNotFoundException.
	 */
	static class NullClassResolver implements ClassResolver {
		public void resolveClass(ClassType type, TypeRepository repos) throws ClassNotFoundException {
			throw new ClassNotFoundException("TypeRepositoryTest does not use a ClassResolver");
		}
	}

	TypeRepository repos;

	BasicType booleanType;
	BasicType byteType;
	BasicType charType;
	BasicType shortType;
	BasicType intType;
	BasicType longType;
	BasicType floatType;
	BasicType doubleType;
	BasicType voidType;

	Type topType;
	Type bottomType;
	Type nullType;
	Type longExtraType;
	Type doubleExtraType;

	ClassType javaLangObjectType;
	ClassType javaIoSerializableType;
	ClassType javaLangCloneableType;

	ClassType myClassType;
	ClassType mySuperclassType;
	ClassType unrelatedThingType;
	ClassType myInterfaceType;
	ClassType mySubinterfaceType;

	ArrayType myClassArrayType;
	ArrayType myClassArray2Type;
	ArrayType mySuperclassArrayType;
	ArrayType mySuperclassArray2Type;
	ArrayType myInterfaceArrayType;
	ArrayType mySubinterfaceArrayType;

	ArrayType javaLangObjectArray1Type;
	ArrayType javaLangObjectArray2Type;

	ArrayType booleanArray1Type;
	ArrayType booleanArray2Type;

	void setClass(ClassType type) {
		type.setIsInterface(false);
	}

	void setInterface(ClassType type) {
		type.setIsInterface(true);
		repos.addInterfaceLink(type, javaLangObjectType);
	}

	boolean checkUnidirectionalSubtype(ObjectType subtype, ObjectType supertype) throws ClassNotFoundException {
		return repos.isSubtype(subtype, supertype)
			&& !repos.isSubtype(supertype, subtype);
	}

	void checkFirstCommonSuperclass(ObjectType a, ObjectType b, ObjectType expectedCommonSuperclass)
		throws ClassNotFoundException {

		//System.out.println("Want common superclass of " + a + " and " + b + " ==> " + expectedCommonSuperclass);

		// Make sure the operation is commutative
		Assert.assertEquals(repos.getFirstCommonSuperclass(a, b), expectedCommonSuperclass);
		Assert.assertEquals(repos.getFirstCommonSuperclass(b, a), expectedCommonSuperclass);
	}

	@Override
         protected void setUp() {
		if (Boolean.getBoolean("tr.debug")) System.out.println("----- Set up -----");

		// In this test, all class types are explicitly marked as
		// class vs. interface, and supertypes explicitly identified.
		// So, dynamic class resolution should never be required.
		ClassResolver resolver = new NullClassResolver();
		this.repos = new TypeRepository(resolver);

		// Create the common class library types needed
		javaLangObjectType = repos.classTypeFromDottedClassName("java.lang.Object");
		setClass(javaLangObjectType);
		javaIoSerializableType = repos.classTypeFromDottedClassName("java.io.Serializable");
		setInterface(javaIoSerializableType);
		javaLangCloneableType = repos.classTypeFromDottedClassName("java.lang.Cloneable");
		setInterface(javaLangCloneableType);

		// Basic types
		booleanType = repos.basicTypeFromTypeCode(Constants.T_BOOLEAN);
		byteType = repos.basicTypeFromTypeCode(Constants.T_BYTE);
		charType = repos.basicTypeFromTypeCode(Constants.T_CHAR);
		shortType = repos.basicTypeFromTypeCode(Constants.T_SHORT);
		intType = repos.basicTypeFromTypeCode(Constants.T_INT);
		longType = repos.basicTypeFromTypeCode(Constants.T_LONG);
		floatType = repos.basicTypeFromTypeCode(Constants.T_FLOAT);
		doubleType = repos.basicTypeFromTypeCode(Constants.T_DOUBLE);
		voidType = repos.basicTypeFromTypeCode(Constants.T_VOID);

		// Special types
		topType = repos.getTopType();
		bottomType = repos.getBottomType();
		nullType = repos.getNullType();
		longExtraType = repos.getLongExtraType();
		doubleExtraType = repos.getDoubleExtraType();

		// Fake hierarchy classes
		myClassType = repos.classTypeFromSlashedClassName("com/foobar/MyClass");
		setClass(myClassType);
		mySuperclassType = repos.classTypeFromDottedClassName("com.foobar.MySuperClass");
		setClass(mySuperclassType);
		myInterfaceType = repos.classTypeFromDottedClassName("com.foobar.MyInterface");
		setInterface(myInterfaceType);
		mySubinterfaceType = repos.classTypeFromDottedClassName("com.foobar.SubInterface");
		setInterface(mySubinterfaceType);
		repos.addSuperclassLink(myClassType, mySuperclassType);
		repos.addSuperclassLink(mySuperclassType, javaLangObjectType);
		repos.addInterfaceLink(mySuperclassType, myInterfaceType);
		repos.addInterfaceLink(mySubinterfaceType, myInterfaceType);

		unrelatedThingType = repos.classTypeFromDottedClassName("com.foobar.UnrelatedThing");
		setClass(unrelatedThingType);
		repos.addSuperclassLink(unrelatedThingType, javaLangObjectType);

		// Array classes
		myClassArrayType = repos.arrayTypeFromDimensionsAndBaseType(1, myClassType);
		myClassArray2Type = repos.arrayTypeFromDimensionsAndBaseType(2, myClassType);
		mySuperclassArrayType = repos.arrayTypeFromDimensionsAndBaseType(1, mySuperclassType);
		mySuperclassArray2Type = repos.arrayTypeFromDimensionsAndBaseType(2, mySuperclassType);
		myInterfaceArrayType = repos.arrayTypeFromDimensionsAndBaseType(1, myInterfaceType);
		mySubinterfaceArrayType = repos.arrayTypeFromDimensionsAndBaseType(1, mySubinterfaceType);
		javaLangObjectArray1Type = repos.arrayTypeFromDimensionsAndBaseType(1, javaLangObjectType);
		javaLangObjectArray2Type = repos.arrayTypeFromDimensionsAndBaseType(2, javaLangObjectType);
		booleanArray1Type = repos.arrayTypeFromDimensionsAndBaseType(1, booleanType);
		booleanArray2Type = repos.arrayTypeFromDimensionsAndBaseType(2, booleanType);
	}

	public void testClassFromSlashedClassName() {
		Assert.assertEquals(myClassType.getSignature(), "Lcom/foobar/MyClass;");
		Assert.assertEquals(myClassType.getClassName(), "com.foobar.MyClass");
	}

	public void testClassFromDottedClassName() {
		ClassType myClassType = repos.classTypeFromDottedClassName("com.foobar.MyClass");
		Assert.assertEquals(myClassType.getSignature(), "Lcom/foobar/MyClass;");
		Assert.assertEquals(myClassType.getClassName(), "com.foobar.MyClass");
	}

	public void testClassTypeUnique() {
		ClassType myClassType2 = repos.classTypeFromDottedClassName("com.foobar.MyClass");
		ClassType myClassType3 = repos.classTypeFromSlashedClassName("com/foobar/MyClass");

		Assert.assertTrue(myClassType == myClassType2);
		Assert.assertTrue(myClassType == myClassType3);
	}

	public void testIsInterface() {
		Assert.assertTrue(myInterfaceType.isInterface());
		Assert.assertTrue(mySubinterfaceType.isInterface());
		Assert.assertTrue(javaIoSerializableType.isInterface());
		Assert.assertTrue(javaLangCloneableType.isInterface());
	}

	public void testIsNotInterface() {
		Assert.assertFalse(myClassType.isInterface());
		Assert.assertFalse(mySuperclassType.isInterface());
		Assert.assertFalse(myClassArrayType.isInterface());
	}

	public void testIsDirectClassSubtype() throws ClassNotFoundException {
		Assert.assertTrue(checkUnidirectionalSubtype(myClassType, mySuperclassType));
		Assert.assertTrue(checkUnidirectionalSubtype(mySuperclassType, javaLangObjectType));
	}

	public void testIndirectClassSubtype() throws ClassNotFoundException {
		Assert.assertTrue(checkUnidirectionalSubtype(myClassType, javaLangObjectType));
	}

	public void testArrayIsSubtypeOfObject() throws ClassNotFoundException {
		Assert.assertTrue(checkUnidirectionalSubtype(myClassArrayType, javaLangObjectType));
	}

	public void testArrayIsSubtypeOfArraysOfElementSupertypes() throws ClassNotFoundException {
		Assert.assertTrue(checkUnidirectionalSubtype(myClassArrayType, mySuperclassArrayType));
		Assert.assertTrue(checkUnidirectionalSubtype(myClassArrayType, myInterfaceArrayType));
		Assert.assertTrue(checkUnidirectionalSubtype(mySubinterfaceArrayType, myInterfaceArrayType));

		Assert.assertTrue(checkUnidirectionalSubtype(myClassArray2Type, mySuperclassArray2Type));
	}

	public void testArrayTypeIsSerializable() throws ClassNotFoundException {
		Assert.assertTrue(checkUnidirectionalSubtype(myClassArrayType, javaIoSerializableType));
		Assert.assertTrue(checkUnidirectionalSubtype(mySuperclassArrayType, javaIoSerializableType));
		Assert.assertTrue(checkUnidirectionalSubtype(myInterfaceArrayType, javaIoSerializableType));
	}

	public void testArrayTypeIsCloneable() throws ClassNotFoundException {
		Assert.assertTrue(checkUnidirectionalSubtype(myClassArrayType, javaLangCloneableType));
		Assert.assertTrue(checkUnidirectionalSubtype(mySuperclassArrayType, javaLangCloneableType));
		Assert.assertTrue(checkUnidirectionalSubtype(myInterfaceArrayType, javaLangCloneableType));
	}

	public void testNoInvalidSubtypes() throws ClassNotFoundException {
		Assert.assertFalse(checkUnidirectionalSubtype(mySuperclassType, myClassType));
		Assert.assertFalse(checkUnidirectionalSubtype(mySuperclassArrayType, myClassArrayType));
		Assert.assertFalse(checkUnidirectionalSubtype(myInterfaceType, mySuperclassType));
		Assert.assertFalse(checkUnidirectionalSubtype(javaLangObjectType, mySuperclassType));
	}

	public void testInterfaceIsSubtypeOfObject() throws ClassNotFoundException {
		Assert.assertTrue(checkUnidirectionalSubtype(myInterfaceType, javaLangObjectType));
		Assert.assertTrue(checkUnidirectionalSubtype(javaIoSerializableType, javaLangObjectType));
		Assert.assertTrue(checkUnidirectionalSubtype(javaLangCloneableType, javaLangObjectType));
	}

	public void testArrayOfObjectSupertypes() throws ClassNotFoundException {
		Assert.assertTrue(checkUnidirectionalSubtype(javaLangObjectArray2Type, javaLangObjectArray1Type));
		Assert.assertTrue(checkUnidirectionalSubtype(javaLangObjectArray1Type, javaLangObjectType));
	}

	public void testArrayFromSignature() throws ClassNotFoundException, InvalidSignatureException {
		ArrayType javaLangObjectArray1TypeCopy = repos.arrayTypeFromSignature("[Ljava/lang/Object;");
		ArrayType javaLangObjectArray2TypeCopy = repos.arrayTypeFromSignature("[[Ljava/lang/Object;");

		Assert.assertTrue(javaLangObjectArray1TypeCopy == javaLangObjectArray1Type);
		Assert.assertTrue(javaLangObjectArray2TypeCopy == javaLangObjectArray2Type);
	}

	public void testBasicTypes() {
		Assert.assertEquals(booleanType.getSignature(),"Z");
		Assert.assertEquals(byteType.getSignature(),"B");
		Assert.assertEquals(charType.getSignature(),"C");
		Assert.assertEquals(shortType.getSignature(),"S");
		Assert.assertEquals(intType.getSignature(),"I");
		Assert.assertEquals(longType.getSignature(),"J");
		Assert.assertEquals(floatType.getSignature(),"F");
		Assert.assertEquals(doubleType.getSignature(),"D");
		Assert.assertEquals(voidType.getSignature(),"V");
	}

	public void testBasicTypesUnique() {
		Assert.assertTrue(booleanType == repos.basicTypeFromTypeCode(Constants.T_BOOLEAN));
		Assert.assertTrue(floatType == repos.basicTypeFromTypeCode(Constants.T_FLOAT));
	}

	public void testBasicTypesFromSignature() throws InvalidSignatureException {
		Assert.assertTrue(intType == repos.basicTypeFromSignature("I"));
		Assert.assertTrue(longType == repos.basicTypeFromSignature("J"));
		Assert.assertTrue(voidType == repos.basicTypeFromSignature("V"));
	}

	public void testBasicArrayType() throws ClassNotFoundException {
		Assert.assertTrue(checkUnidirectionalSubtype(booleanArray1Type, javaLangObjectType));
		Assert.assertTrue(checkUnidirectionalSubtype(booleanArray2Type, javaLangObjectArray1Type));
		Assert.assertTrue(checkUnidirectionalSubtype(booleanArray2Type, javaLangObjectType));
		Assert.assertTrue(checkUnidirectionalSubtype(booleanArray1Type, javaIoSerializableType));
		Assert.assertTrue(checkUnidirectionalSubtype(booleanArray1Type, javaLangCloneableType));
	}

	public void testClassTypeCommonSuperclass() throws ClassNotFoundException {
		checkFirstCommonSuperclass(myClassType, mySuperclassType, mySuperclassType);
		checkFirstCommonSuperclass(myClassType, unrelatedThingType, javaLangObjectType);
	}

	public void testArrayTypeCommonSuperclass() throws ClassNotFoundException {
		checkFirstCommonSuperclass(myClassArrayType, mySuperclassArrayType, mySuperclassArrayType);
		checkFirstCommonSuperclass(myClassArray2Type, mySuperclassArray2Type, mySuperclassArray2Type);

		checkFirstCommonSuperclass(myClassArrayType, myClassArray2Type, javaLangObjectArray1Type);

		checkFirstCommonSuperclass(myClassArrayType, booleanArray2Type, javaLangObjectArray1Type);
	}

	public void testSpecialTypes() {
		Assert.assertTrue(topType.getTypeCode() == ExtendedTypes.T_TOP);
		Assert.assertTrue(bottomType.getTypeCode() == ExtendedTypes.T_BOTTOM);
		Assert.assertTrue(nullType.getTypeCode() == ExtendedTypes.T_NULL);
		Assert.assertTrue(longExtraType.getTypeCode() == ExtendedTypes.T_LONG_EXTRA);
		Assert.assertTrue(doubleExtraType.getTypeCode() == ExtendedTypes.T_DOUBLE_EXTRA);
	}

}

// vim:ts=4

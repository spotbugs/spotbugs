package edu.umd.cs.findbugs.ba.type;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TypeRepositoryTest extends TestCase {

	/**
	 * A do-nothing class resolver that just throws
	 * ClassNotFoundExceptions.
	 */
	static class NullClassResolver implements ClassResolver {
		public void resolveClass(ClassType type, TypeRepository repos) throws ClassNotFoundException {
			throw new ClassNotFoundException("TypeRepositoryTest does not use a ClassResolver");
		}
	}

	TypeRepository repos;

	ClassType javaLangObjectType;
	ClassType javaIoSerializableType;
	ClassType javaLangCloneableType;

	ClassType myClassType;
	ClassType mySuperclassType;
	ClassType myInterfaceType;
	ClassType mySubinterfaceType;

	ArrayType myClassArrayType;
	ArrayType mySuperclassArrayType;
	ArrayType myInterfaceArrayType;
	ArrayType mySubinterfaceArrayType;

	ArrayType javaLangObjectArray1Type;
	ArrayType javaLangObjectArray2Type;

	void setClass(ClassType type) {
		type.setIsInterface(false);
	}

	void setInterface(ClassType type) {
		type.setIsInterface(true);
		repos.addInterfaceLink(type, javaLangObjectType);
	}

	protected void setUp() {
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

		// Class types should have a unique representation
		myClassType = repos.classTypeFromSlashedClassName("com/foobar/MyClass");
		setClass(myClassType);

		// Fake hierarchy classes
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

		// Array classes
		myClassArrayType = repos.arrayTypeFromDimensionsAndElementType(1, myClassType);
		mySuperclassArrayType = repos.arrayTypeFromDimensionsAndElementType(1, mySuperclassType);
		myInterfaceArrayType = repos.arrayTypeFromDimensionsAndElementType(1, myInterfaceType);
		mySubinterfaceArrayType = repos.arrayTypeFromDimensionsAndElementType(1, mySubinterfaceType);
		javaLangObjectArray1Type = repos.arrayTypeFromDimensionsAndElementType(1, javaLangObjectType);
		javaLangObjectArray2Type = repos.arrayTypeFromDimensionsAndElementType(2, javaLangObjectType);
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
		Assert.assertTrue(repos.isSubtype(myClassType, mySuperclassType));
		Assert.assertTrue(repos.isSubtype(mySuperclassType, javaLangObjectType));
	}

	public void testIndirectClassSubtype() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(myClassType, javaLangObjectType));
	}

	public void testArrayIsSubtypeOfObject() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(myClassArrayType, javaLangObjectType));
	}

	public void testArrayIsSubtypeOfArraysOfElementSupertypes() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(myClassArrayType, mySuperclassArrayType));
		Assert.assertTrue(repos.isSubtype(myClassArrayType, myInterfaceArrayType));
		Assert.assertTrue(repos.isSubtype(mySubinterfaceArrayType, myInterfaceArrayType));
	}

	public void testArrayTypeIsSerializable() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(myClassArrayType, javaIoSerializableType));
		Assert.assertTrue(repos.isSubtype(mySuperclassArrayType, javaIoSerializableType));
		Assert.assertTrue(repos.isSubtype(myInterfaceArrayType, javaIoSerializableType));
	}

	public void testArrayTypeIsCloneable() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(myClassArrayType, javaLangCloneableType));
		Assert.assertTrue(repos.isSubtype(mySuperclassArrayType, javaLangCloneableType));
		Assert.assertTrue(repos.isSubtype(myInterfaceArrayType, javaLangCloneableType));
	}

	public void testNoInvalidSubtypes() throws ClassNotFoundException {
		Assert.assertFalse(repos.isSubtype(mySuperclassType, myClassType));
		Assert.assertFalse(repos.isSubtype(mySuperclassArrayType, myClassArrayType));
		Assert.assertFalse(repos.isSubtype(myInterfaceType, mySuperclassType));
		Assert.assertFalse(repos.isSubtype(javaLangObjectType, mySuperclassType));
	}

	public void testInterfaceIsSubtypeOfObject() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(myInterfaceType, javaLangObjectType));
		Assert.assertTrue(repos.isSubtype(javaIoSerializableType, javaLangObjectType));
		Assert.assertTrue(repos.isSubtype(javaLangCloneableType, javaLangObjectType));
	}

	public void testArrayOfObjectSupertypes() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(javaLangObjectArray2Type, javaLangObjectArray1Type));
		Assert.assertTrue(repos.isSubtype(javaLangObjectArray1Type, javaLangObjectType));
	}

}

// vim:ts=4

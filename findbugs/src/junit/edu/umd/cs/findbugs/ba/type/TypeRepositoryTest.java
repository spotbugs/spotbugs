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

	boolean checkUnidirectionalSubtype(ObjectType subtype, ObjectType supertype) throws ClassNotFoundException {
		return repos.isSubtype(subtype, supertype)
			&& !repos.isSubtype(supertype, subtype);
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

}

// vim:ts=4

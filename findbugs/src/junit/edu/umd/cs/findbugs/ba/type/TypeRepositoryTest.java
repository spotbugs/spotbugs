package edu.umd.cs.findbugs.ba.type;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bcel.Repository;

import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

public class TypeRepositoryTest extends TestCase {

	TypeRepository repos;

	ClassType javaLangObjectType;
	ClassType javaIoSerializableType;
	ClassType javaLangCloneableType;

	ClassType myClassType;
	ClassType mySuperclassType;
	ClassType myInterfaceType;

	ArrayType myClassArrayType;
	ArrayType mySuperclassArrayType;
	ArrayType myInterfaceArrayType;

	void setClass(ClassType type) {
		type.setIsInterface(false);
	}

	void setInterface(ClassType type) {
		type.setIsInterface(true);
		repos.addInterfaceLink(type, javaLangObjectType);
	}

	protected void setUp() {
		// The BCEL resolver is an easy way to test stuff
		// that is accessible on the system classpath.
		ClassResolver resolver = new BCELRepositoryClassResolver();

		// Clear the BCEL repository
		Repository.clearCache();
		SyntheticRepository synthRepos = SyntheticRepository.getInstance(ClassPath.SYSTEM_CLASS_PATH);
		Repository.setRepository(synthRepos);

		this.repos = new TypeRepository(resolver);

		javaLangObjectType = repos.classTypeFromDottedClassName("java.lang.Object");
		setClass(javaLangObjectType);
		javaIoSerializableType = repos.classTypeFromDottedClassName("java.io.Serializable");
		setInterface(javaIoSerializableType);
		javaLangCloneableType = repos.classTypeFromDottedClassName("java.lang.Cloneable");
		setClass(javaLangCloneableType);

		// Class types should have a unique representation
		myClassType = repos.classTypeFromSlashedClassName("com/foobar/MyClass");
		setClass(myClassType);

		// Fake hierarchy classes
		mySuperclassType = repos.classTypeFromDottedClassName("com.foobar.MySuperClass");
		setClass(mySuperclassType);
		myInterfaceType = repos.classTypeFromDottedClassName("com.foobar.MyInterface");
		setInterface(myInterfaceType);
		repos.addSuperclassLink(myClassType, mySuperclassType);
		repos.addSuperclassLink(mySuperclassType, javaLangObjectType);
		repos.addInterfaceLink(mySuperclassType, myInterfaceType);

		// Array classes
		myClassArrayType = repos.arrayTypeFromDimensionsAndElementType(1, myClassType);
		mySuperclassArrayType = repos.arrayTypeFromDimensionsAndElementType(1, mySuperclassType);
		myInterfaceArrayType = repos.arrayTypeFromDimensionsAndElementType(1, myInterfaceType);
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
		Assert.assertTrue(!myClassType.isInterface());
		Assert.assertTrue(!mySuperclassType.isInterface());
		Assert.assertTrue(myInterfaceType.isInterface());
	}

	public void testIsDirectClassSubtype() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(myClassType, mySuperclassType));
		Assert.assertTrue(repos.isSubtype(mySuperclassType, javaLangObjectType));
	}

	public void testArrayIsSubtypeOfObject() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(myClassArrayType, javaLangObjectType));
	}

	public void testArrayIsSubtypeOfArraysOfElementSupertypes() throws ClassNotFoundException {
		Assert.assertTrue(repos.isSubtype(myClassArrayType, mySuperclassArrayType));
		Assert.assertTrue(repos.isSubtype(myClassArrayType, myInterfaceArrayType));
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
	
}

// vim:ts=4

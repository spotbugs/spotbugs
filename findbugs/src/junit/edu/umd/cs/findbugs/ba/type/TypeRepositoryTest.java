package edu.umd.cs.findbugs.ba.type;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bcel.Repository;

import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

public class TypeRepositoryTest extends TestCase {

	TypeRepository repos;

	ClassType javaLangObjectType;

	ClassType myClassType;
	ClassType myClassType2;
	ClassType myClassType3;
	ClassType myClassType4;

	ClassType mySuperclassType;
	ClassType myInterfaceType;

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

		// Class types should have a unique representation
		myClassType = repos.classTypeFromSlashedClassName("com/foobar/MyClass");
		myClassType2 = repos.classTypeFromDottedClassName("com.foobar.MyClass");
		myClassType3 = repos.classTypeFromSlashedClassName("com/foobar/MyClass");
		myClassType4 = repos.classTypeFromDottedClassName("com.foobar.MyClass");
		setClass(myClassType);

		// Fake hierarchy classes
		mySuperclassType = repos.classTypeFromDottedClassName("com.foobar.MySuperClass");
		setClass(mySuperclassType);
		myInterfaceType = repos.classTypeFromDottedClassName("com.foobar.MyInterface");
		setInterface(myInterfaceType);
		repos.addSuperclassLink(myClassType, mySuperclassType);
		repos.addSuperclassLink(mySuperclassType, javaLangObjectType);
		repos.addInterfaceLink(mySuperclassType, myInterfaceType);
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
		Assert.assertTrue(myClassType == myClassType2);
		Assert.assertTrue(myClassType == myClassType3);
		Assert.assertTrue(myClassType == myClassType4);
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
	
}

// vim:ts=4

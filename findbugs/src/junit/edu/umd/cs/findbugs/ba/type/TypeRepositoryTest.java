package edu.umd.cs.findbugs.ba.type;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bcel.Repository;

import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

public class TypeRepositoryTest extends TestCase {

	TypeRepository repos;

	protected void setUp() {
		// The BCEL resolver is an easy way to test stuff
		// that is accessible on the system classpath.
		ClassResolver resolver = new BCELRepositoryClassResolver();

		// Clear the BCEL repository
		Repository.clearCache();
		SyntheticRepository synthRepos = SyntheticRepository.getInstance(ClassPath.SYSTEM_CLASS_PATH);
		Repository.setRepository(synthRepos);

		this.repos = new TypeRepository(resolver);
	}

	public void testClassFromSlashedClassName() {
		ClassType type = repos.classTypeFromSlashedClassName("com/foobar/MyClass");
		Assert.assertEquals(type.getSignature(), "Lcom/foobar/MyClass;");
		Assert.assertEquals(type.getClassName(), "com.foobar.MyClass");
	}

	public void testClassFromDottedClassName() {
		ClassType type = repos.classTypeFromDottedClassName("com.foobar.MyClass");
		Assert.assertEquals(type.getSignature(), "Lcom/foobar/MyClass;");
		Assert.assertEquals(type.getClassName(), "com.foobar.MyClass");
	}

	public void testUniqueClass() {
		ClassType type1 = repos.classTypeFromSlashedClassName("com/foobar/MyClass");
		ClassType type2 = repos.classTypeFromDottedClassName("com.foobar.MyClass");
		ClassType type3 = repos.classTypeFromSlashedClassName("com/foobar/MyClass");
		ClassType type4 = repos.classTypeFromDottedClassName("com.foobar.MyClass");
		Assert.assertTrue(type1 == type2);
		Assert.assertTrue(type1 == type3);
		Assert.assertTrue(type1 == type4);
	}

	
}

// vim:ts=4

package edu.umd.cs.findbugs.ba.npe;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.JavaClassAndMethodChooser;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * This callback can be passed to Hierarchy.findInvocationLeastUpperBound
 * to find the @NonNull or @CheckForNull method return value annotation
 * which should be applied at a call site.
 * 
 * @author David Hovemeyer
 * @deprecated
 */
public class NonNullReturnValueAnnotationChecker implements JavaClassAndMethodChooser {
	private static final boolean DEBUG = SystemProperties.getBoolean("fnd.debug.nullreturn");
	
	private MayReturnNullPropertyDatabase database;
	private JavaClassAndMethod annotatedMethod;
	private Boolean property;

	/** @deprecated */
	public NonNullReturnValueAnnotationChecker(MayReturnNullPropertyDatabase database) {
		this.database = database;
	}
	
	public Boolean getProperty() {
		return property;
	}
	
	public JavaClassAndMethod getAnnotatedMethod() {
		return annotatedMethod;
	}
	
	public boolean choose(JavaClassAndMethod javaClassAndMethod) {
		XMethod xmethod = javaClassAndMethod.toXMethod();
		if (DEBUG) {
			System.out.print("Checking " + xmethod + " for @NonNull or @CheckForNull...");
		}
		Boolean prop = database.getProperty(xmethod);
		if (prop != null) {
			this.property = prop;
			this.annotatedMethod = javaClassAndMethod;
			if (DEBUG) {
				System.out.println(prop.booleanValue() ? "@CheckForNull" : "@NonNull");
			}
			return true;
		}
		if (DEBUG) {
			System.out.println("not found");
		}
		return false;
	}
}

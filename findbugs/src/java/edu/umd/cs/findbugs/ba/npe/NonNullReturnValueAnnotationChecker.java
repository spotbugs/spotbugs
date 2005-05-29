package edu.umd.cs.findbugs.ba.npe;

import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.JavaClassAndMethodChooser;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * This callback can be passed to Hierarchy.findInvocationLeastUpperBound
 * to find the @NonNull or @PossiblyNull method return value annotation
 * which should be applied at a call site.
 * 
 * @author David Hovemeyer
 */
public class NonNullReturnValueAnnotationChecker implements JavaClassAndMethodChooser {
	private MayReturnNullPropertyDatabase database;
	private Boolean property;
	
	public NonNullReturnValueAnnotationChecker(MayReturnNullPropertyDatabase database) {
		this.database = database;
	}
	
	public Boolean getProperty() {
		return property;
	}
	
	public boolean choose(JavaClassAndMethod javaClassAndMethod) {
		XMethod xmethod = javaClassAndMethod.toXMethod();
		Boolean prop = database.getProperty(xmethod);
		if (prop != null) {
			this.property = prop;
			return true;
		}
		return false;
	}
}

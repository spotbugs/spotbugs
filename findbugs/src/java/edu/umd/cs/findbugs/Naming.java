package edu.umd.cs.findbugs;
import java.util.*;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;
import edu.umd.cs.pugh.visitclass.PreorderVisitor;

public class Naming extends PreorderVisitor implements Detector, Constants2 {
  String baseClassName;

  HashMap<String, String> classes = new HashMap<String, String>();
  HashMap<String, String> canonicalNames = new HashMap<String, String>();
  HashMap<String, String> canonicalSigs = new HashMap<String, String>();
  HashSet<String> reported = new HashSet<String>();

  private BugReporter bugReporter;

  public Naming(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

  public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
	}

  public void report() { }

  public void visit(JavaClass obj)     {
	super.visit(obj);
	String[] parts = betterClassName.split("[.]");
        baseClassName = parts[parts.length-1];
	}

    public void visit(Method obj) {
	String allSmall = methodName.toLowerCase() + methodSig;
	classes.put(methodName, betterClassName);
	String old = canonicalNames.put(allSmall, methodName);
	String oldSig = canonicalSigs.put(allSmall, methodSig);
	if (old != null && !old.equals(methodName) && !reported.contains(allSmall)) {
		reported.add(allSmall);
		String oldClass = classes.get(old);
		String type = betterSuperclassName.equals(oldClass) ? "NM_VERY_CONFUSING" : "NM_CONFUSING";
		bugReporter.reportBug(new BugInstance(type, NORMAL_PRIORITY)
			.addClass(betterClassName)
			.addMethod(betterClassName, methodName, methodSig)
			.addClass(classes.get(old))
			.addMethod(classes.get(old), old, oldSig));
		}
	// FIXME: I think that the "baseClassName" field is broken.
	// Need to look at the code that sets it.
	//System.out.println("methodName="+methodName+", baseClassName="+baseClassName);
	if (methodName.equals(baseClassName)) 
		bugReporter.reportBug(new BugInstance("NM_CONFUSING_METHOD_NAME", NORMAL_PRIORITY)
			.addClassAndMethod(this));
	if (methodName.equals("hashcode") && methodSig.equals("()I")) 
		bugReporter.reportBug(new BugInstance("NM_LCASE_HASHCODE", NORMAL_PRIORITY)
			.addClassAndMethod(this));
	if (methodName.equals("tostring") && methodSig.equals("()Ljava/lang/String;")) 
		bugReporter.reportBug(new BugInstance("NM_LCASE_TOSTRING", NORMAL_PRIORITY)
			.addClassAndMethod(this));
	}


}

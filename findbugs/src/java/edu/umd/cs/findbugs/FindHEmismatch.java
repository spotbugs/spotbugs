package edu.umd.cs.findbugs;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class FindHEmismatch extends BytecodeScanningDetector implements   Constants2 {
   String prevClassName = " none ";
   boolean firstTime = true;
   boolean hasFields = false;
   boolean hasHashCode = false;
   boolean hasEqualsObject = false;
   boolean hasCompareToObject = false;
   boolean hasEqualsSelf = false;
   boolean hasCompareToSelf = false;
   boolean isInterface = false;
   boolean extendsObject = false;
   private BugReporter bugReporter;

   public FindHEmismatch(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
   }

   public void report() {
	if (!hasEqualsObject &&  hasEqualsSelf) {
		if (extendsObject) 
		  bugReporter.reportBug(new BugInstance("EQ_SELF_USE_OBJECT", NORMAL_PRIORITY).addClass(prevClassName));
		else
		  bugReporter.reportBug(new BugInstance("EQ_SELF_NO_OBJECT", NORMAL_PRIORITY).addClass(prevClassName));
		}


	if (!hasCompareToObject &&  hasCompareToSelf) {
		if (!extendsObject)
		  bugReporter.reportBug(new BugInstance("CO_SELF_NO_OBJECT", NORMAL_PRIORITY).addClass(prevClassName));
		}

	if (!hasFields) return;
	if (hasHashCode && !(hasEqualsObject ||  hasEqualsSelf))  {
		if (extendsObject) 
		  bugReporter.reportBug(new BugInstance("HE_HASHCODE_USE_OBJECT_EQUALS", NORMAL_PRIORITY).addClass(prevClassName));
		else
		  bugReporter.reportBug(new BugInstance("HE_HASHCODE_NO_EQUALS", NORMAL_PRIORITY). addClass(prevClassName));
		}
	if (!hasHashCode && (hasEqualsObject ||  hasEqualsSelf))  {
		if (extendsObject) 
		  bugReporter.reportBug(new BugInstance("HE_EQUALS_USE_HASHCODE", NORMAL_PRIORITY).addClass(prevClassName));
		else
		  bugReporter.reportBug(new BugInstance("HE_EQUALS_NO_HASHCODE", NORMAL_PRIORITY).addClass(prevClassName));
		}
	}
   public void visit(JavaClass obj) {
	if (!isInterface && !firstTime) report(System.out);
	extendsObject = betterSuperclassName.equals("java.lang.Object");
	firstTime = false;
	prevClassName = betterClassName;
	hasFields = false;
	hasHashCode = false;
	hasCompareToObject = false;
	hasCompareToSelf = false;
	hasEqualsObject = false;
	hasEqualsSelf = false;
	int accessFlags = obj.getAccessFlags();
	isInterface = ((accessFlags & ACC_INTERFACE) != 0);
	}

    public void visit(Field obj) {
	int accessFlags = obj.getAccessFlags();
	if ((accessFlags & ACC_STATIC) != 0) return;
	hasFields = true;
	}
    public void visit(Method obj) {
	int accessFlags = obj.getAccessFlags();
	if ((accessFlags & ACC_STATIC) != 0) return;
	String name = obj.getName();
	String sig = obj.getSignature();
	if ((accessFlags & ACC_ABSTRACT) != 0) {
		if (name.equals("equals")
			&& sig.equals("(L"+className+";)Z"))
		  bugReporter.reportBug(new BugInstance("EQ_ABSTRACT_SELF", NORMAL_PRIORITY).addClass(betterClassName));
		else if (name.equals("compareTo")
			&& sig.equals("(L"+className+";)I"))
		  bugReporter.reportBug(new BugInstance("CO_ABSTRACT_SELF", NORMAL_PRIORITY).addClass(betterClassName));
		return;
		
		}
	boolean sigIsObject = sig.equals("(Ljava/lang/Object;)Z");
	if (name.equals("hashCode")) hasHashCode = true;
	else if (name.equals("equals")) {
		if (sigIsObject) hasEqualsObject = true;
		else if (sig.equals("(L"+className+";)Z"))
				hasEqualsSelf = true;
		}
	else if (name.equals("compareTo")) {
		if (sig.equals("(Ljava/lang/Object;)I")) 
			hasCompareToObject = true;
		else if (sig.equals("(L"+className+";)I"))
				hasCompareToSelf = true;
		}
	}
}

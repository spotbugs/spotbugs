package edu.umd.cs.findbugs;
import java.util.*;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class UnreadFields extends BytecodeScanningDetector implements   Constants2 {
    private static final boolean DEBUG = Boolean.getBoolean("unreadfields.debug");

    Set<FieldAnnotation> declaredFields = new TreeSet<FieldAnnotation>();
    Set<FieldAnnotation> myFields = new TreeSet<FieldAnnotation>();
    HashSet<FieldAnnotation> writtenFields = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> readFields = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> constantFields = new HashSet<FieldAnnotation>();
    // HashSet finalFields = new HashSet();
    HashSet<FieldAnnotation> superReadFields = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> superWrittenFields = new HashSet<FieldAnnotation>();
    HashSet<String> innerClassCannotBeStatic = new HashSet<String>();
    boolean hasNativeMethods;
    private BugReporter bugReporter;

    static final int doNotConsider = ACC_PUBLIC | ACC_PROTECTED
			| ACC_STATIC;

  public UnreadFields(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}


  public void visit(JavaClass obj)     {
	hasNativeMethods = false;
	if (superclassName.indexOf("$") >= 0) {
		// System.out.println("hicfsc: " + betterClassName);
		innerClassCannotBeStatic.add(betterClassName);
		// System.out.println("hicfsc: " + betterSuperclassName);
		innerClassCannotBeStatic.add(betterSuperclassName);
		}
	super.visit(obj);
	}
  public void visitAfter(JavaClass obj)     {
	if (!hasNativeMethods)
		declaredFields.addAll(myFields);
	myFields.clear();
	}


    public void visit(Field obj) {
        super.visit(obj);
	int flags = obj.getAccessFlags();
	if ((flags & doNotConsider) == 0 
			&& !fieldName.equals("serialVersionUID"))  {

		FieldAnnotation f = FieldAnnotation.fromVisitedField(this);
		myFields.add(f);
		}
	}

 public void visit(ConstantValue obj) {
	FieldAnnotation f = FieldAnnotation.fromVisitedField(this);
	constantFields.add(f);
        }



    public void visit(Method obj) {
        super.visit(obj);
	int flags = obj.getAccessFlags();
	if ((flags & ACC_NATIVE) != 0)
		hasNativeMethods = true;
	}


    public void sawOpcode(int seen) {

	if (seen == GETFIELD) {
		FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
		if (DEBUG) System.out.println("get: " + f);
		readFields.add(f);
		if (classConstant.equals(className) && 
			!myFields.contains(f)) {
			superReadFields.add(f);
			}
		}
	else if (seen == PUTFIELD) {
		FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
		if (DEBUG) System.out.println("put: " + f);
		writtenFields.add(f);
		if (classConstant.equals(className) && 
			!myFields.contains(f)) {
			superWrittenFields.add(f);
			}
		}
	}

public void report() {

	declaredFields.removeAll(readFields);

	for(Iterator<FieldAnnotation> i = declaredFields.iterator(); i.hasNext(); )  {
		FieldAnnotation f = i.next();
		String fieldName = f.getFieldName();
		String className = f.getClassName();
/*
		int lastDollar = className.lastIndexOf('$');
		boolean isAnonymousInnerClass =
			   (lastDollar > 0)
			&& (lastDollar < className.length() - 1)
			&& Character.isDigit(className.charAt(className.length() - 1));
*/
		boolean allUpperCase = 
				fieldName.equals(fieldName.toUpperCase());
		if (superReadFields.contains(f))  continue;
		if (!fieldName.startsWith("this$"))  {
		  if (allUpperCase || constantFields.contains(f) )
		    bugReporter.reportBug(new BugInstance("SS_SHOULD_BE_STATIC", NORMAL_PRIORITY)
			.addClass(className)
			.addField(f));
		  else if (!writtenFields.contains(f) && !superWrittenFields.contains(f))
		    bugReporter.reportBug(new BugInstance("UUF_UNUSED_FIELD", NORMAL_PRIORITY)
			.addClass(className)
			.addField(f));
		  else
		    bugReporter.reportBug(new BugInstance("URF_UNREAD_FIELD", NORMAL_PRIORITY)
			.addClass(className)
			.addField(f));
		   }
		  else if ( !innerClassCannotBeStatic.contains(className)
			  // && !isAnonymousInnerClass 
			)
			bugReporter.reportBug(new BugInstance("SIC_INNER_SHOULD_BE_STATIC", NORMAL_PRIORITY)
				.addClass(className));
		}

	}	
}

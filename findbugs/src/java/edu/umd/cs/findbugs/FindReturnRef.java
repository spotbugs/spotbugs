package edu.umd.cs.findbugs;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class FindReturnRef extends BytecodeScanningDetector implements   Constants2 {
    boolean check = false;
    boolean thisOnTOS = false;
    boolean fieldOnTOS = false;
    boolean staticMethod = false;
    String nameOnStack;
    String classNameOnStack;
    String sigOnStack;
    boolean fieldIsStatic;
    private BugReporter bugReporter;

    public FindReturnRef(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

  public void visit(JavaClass obj)     {
	super.visit(obj);
	}

    public void visit(Method obj) {
        check =  (obj.getAccessFlags() & (ACC_PUBLIC )) != 0;
        staticMethod =  (obj.getAccessFlags() & (ACC_STATIC)) != 0;
        if (check) 
		super.visit(obj);
        thisOnTOS = false;
        fieldOnTOS = false;
	}


    public void sawOpcode(int seen) {
	/*
	System.out.println("Saw " + OPCODE_NAMES[seen] + "	" 
			+ thisOnTOS
			+ "	"
			+ fieldOnTOS
			);
	*/
	if (seen == ALOAD_0)  {
		thisOnTOS = true;
		fieldOnTOS = false;
		return;
		}

	if (thisOnTOS && seen == GETFIELD && classConstant == className)  {
		fieldOnTOS = true;
		thisOnTOS = false;
		nameOnStack = nameConstant;
		classNameOnStack = betterClassConstant;
		sigOnStack = sigConstant;
		fieldIsStatic = false;
		// System.out.println("Saw getfield");
		return;
		}
	if (seen == GETSTATIC && classConstant == className)  {
		fieldOnTOS = true;
		thisOnTOS = false;
		nameOnStack = nameConstant;
		classNameOnStack = betterClassConstant;
		sigOnStack = sigConstant;
		fieldIsStatic = true;
		return;
		}
	thisOnTOS = false;
	if (check && fieldOnTOS && seen == ARETURN 
		&& !sigOnStack.equals("Ljava/lang/String;")
		&& sigOnStack.indexOf("Exception") == -1
		&& sigOnStack.indexOf("[") >= 0
		) {
			bugReporter.reportBug(new BugInstance(staticMethod ? "MS_EXPOSE_REP" : "EI_EXPOSE_REP", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addField(classNameOnStack, nameOnStack, sigOnStack, fieldIsStatic)
				.addSourceLine(this));
		}

	fieldOnTOS = false;
	thisOnTOS = false;
	}


}	

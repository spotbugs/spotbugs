package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.*;

import static edu.umd.cs.findbugs.detect.PublicIdentifiers.PUBLIC_IDENTIFIERS;


public class DontReusePublicIdentifiers extends OpcodeStackDetector {
    // TODO: Implement this method
    //  check method name clashes
    //  check variable name clashes(obscuring)
    //  add a constant set of public identifiers from the JSL
    // DONE:
    //  check class name clashes - ony for public classes

    private final BugReporter bugReporter;
    private String topLevelClassName = "";
    private String sourceFileName = "";

    public DontReusePublicIdentifiers(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass obj = classContext.getJavaClass();

        if (obj.isPublic()) {
            // save the source file name and class name for inner classes
            sourceFileName = obj.getFileName();
            topLevelClassName = obj.getClassName();
            classContext.getJavaClass().accept(this);
        }
    }

    @Override
    public void visit(JavaClass obj) {
        String[] fullName = obj.getClassName().split("\\.");
        String simpleName = fullName[fullName.length - 1];

        if (PUBLIC_IDENTIFIERS.contains(simpleName)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClass(this).addString(obj.getClassName()).addString(sourceFileName + " shadows").addString(simpleName));
        }
    }

    @Override
    public void visit(Field obj) {
        String name = obj.getName();
        if (PUBLIC_IDENTIFIERS.contains(name)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClass(this).addString(topLevelClassName + "." + name).addString("field " + name + " shadows").addString(name));
        }

    }

    @Override
    public void visit(LocalVariableTable obj) {
        LocalVariable[] vars = obj.getLocalVariableTable();
        System.out.println("LocalVariableTable: " + vars.length);

        for (LocalVariable var : vars) {
            String varName = var.getName();

            if ("this".equals(varName)) {
                continue;
            }

            if (PUBLIC_IDENTIFIERS.contains(varName)) {
                bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClassAndMethod(this).addString(topLevelClassName + "." + varName).addString("variable " + varName + " shadows").addString(varName));
            }
        }
    }

    @Override
    public void visitInnerClasses(InnerClasses obj) {
        super.visitInnerClasses(obj);

        ConstantPool constantPool = obj.getConstantPool();
        for (InnerClass cls : obj.getInnerClasses()) {
            // analyze the inner class itself

            // check if the inner class is public
            int accessFlags = cls.getInnerAccessFlags();
            if ((accessFlags & Const.ACC_PUBLIC) == 0) {
                continue;
            }

            // get the name of the cls through its name index
            int nameIndex = cls.getInnerNameIndex();
            String innerClassName = constantPool.getConstantUtf8(nameIndex).getBytes();
            if (PUBLIC_IDENTIFIERS.contains(innerClassName)) {
                bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClass(this).addString(topLevelClassName + "$" + innerClassName).addString(sourceFileName + " shadows").addString(innerClassName));
            }
        }
    }

    @Override
    public void sawOpcode(int seen) {
        // case #1: ASTORE is called when creating a new variable then the variable name should be checked
        // variable name when instantiating an object
        //        if (seen == Const.ASTORE) {
        //            XField f = getXFieldOperand();
        //            if (f != null) {
        //                if (PUBLIC_IDENTIFIERS.contains(f.getName())) {
        //                    bugAccumulator.accumulateBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClassAndMethod(this),
        //                            this);
        //                }
        //            }
        //        }

        // case #2: PUTFIELD/PUTFIELD is called inside a class to declare a variable
        // TODO: NOTE: It is for giving a value to a field
        // variable declared inside a class/interface
        //        if ((seen == Const.PUTFIELD || seen == Const.PUTSTATIC)) {
        //            XField f = getXFieldOperand();
        //            if (f != null) {
        //                bugAccumulator.accumulateBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClassAndMethod(this),
        //                        this);
        //            }
        //
        //        }
    }

}

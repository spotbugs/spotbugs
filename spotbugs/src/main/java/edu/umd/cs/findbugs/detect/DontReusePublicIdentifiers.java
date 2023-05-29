package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.*;

import static edu.umd.cs.findbugs.detect.publicidentifiers.PublicIdentifiers.PUBLIC_IDENTIFIERS;

public class DontReusePublicIdentifiers extends BytecodeScanningDetector {

    private final BugReporter bugReporter;
    private String topLevelClassName = "";
    private String sourceFileName = "";

    public DontReusePublicIdentifiers(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass obj = classContext.getJavaClass();

        // save the source file name and class name for inner classes
        sourceFileName = obj.getFileName();
        topLevelClassName = obj.getClassName();
        classContext.getJavaClass().accept(this);
    }

    @Override
    public void visit(JavaClass obj) {
        String[] fullName = obj.getClassName().split("\\.");
        String simpleName = fullName[fullName.length - 1];

        if (PUBLIC_IDENTIFIERS.contains(simpleName)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClass(this).addString(obj
                    .getClassName()).addString(sourceFileName + " shadows").addString(simpleName));
        }
    }

    @Override
    public void visit(Field obj) {
        String name = obj.getName();
        if (PUBLIC_IDENTIFIERS.contains(name)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClass(this).addString(
                    topLevelClassName + "." + name).addString("field " + name + " shadows").addString(name));
        }

    }

    @Override
    public void visit(Method obj) {
        String name = obj.getName();
        if (PUBLIC_IDENTIFIERS.contains(name)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClassAndMethod(this).addString(
                    topLevelClassName + "." + name).addString("method " + name + " shadows").addString(name));
        }
    }

    @Override
    public void visit(LocalVariableTable obj) {
        LocalVariable[] vars = obj.getLocalVariableTable();

        for (LocalVariable var : vars) {
            String varName = var.getName();

            if ("this".equals(varName)) {
                continue;
            }

            if (PUBLIC_IDENTIFIERS.contains(varName)) {
                bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClassAndMethod(this).addString(
                        topLevelClassName + "." + varName).addString("variable " + varName + " shadows").addString(varName));
            }
        }
    }

    @Override
    public void visitInnerClasses(InnerClasses obj) {
        super.visitInnerClasses(obj);

        ConstantPool constantPool = obj.getConstantPool();
        for (InnerClass cls : obj.getInnerClasses()) {

            // check if the inner class is public
            int accessFlags = cls.getInnerAccessFlags();
            if ((accessFlags & Const.ACC_PUBLIC) == 0) {
                continue;
            }

            // get the name of the cls through its name index
            int nameIndex = cls.getInnerNameIndex();
            String innerClassName = constantPool.getConstantUtf8(nameIndex).getBytes();
            if (PUBLIC_IDENTIFIERS.contains(innerClassName)) {
                bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY).addClass(this).addString(
                        topLevelClassName + "$" + innerClassName).addString(sourceFileName + " shadows").addString(innerClassName));
            }
        }
    }
}

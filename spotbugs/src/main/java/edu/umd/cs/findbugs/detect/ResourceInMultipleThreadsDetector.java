package edu.umd.cs.findbugs.detect;

import java.util.Optional;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.Method;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.BootstrapMethodsUtil;

public class ResourceInMultipleThreadsDetector extends OpcodeStackDetector {

    private final BugReporter bugReporter;

    public ResourceInMultipleThreadsDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKEDYNAMIC && getStack().getStackDepth() > 1 && getStack().getStackItem(1).getSignature().equals("Ljava/lang/Thread;")) {
            Optional<Method> method = getMethodFromBootstrap();
            if (method.isPresent()) {
                System.out.println("Found invoke: " + method.get().getName());
            }
        } else if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE) {
            XField xField = getXField();
            System.out.println("Found invoke: " + getMethodName());
        }
    }

    private Optional<Method> getMethodFromBootstrap() {
        ConstantInvokeDynamic constDyn = (ConstantInvokeDynamic) getConstantRefOperand();
        for (Attribute attr : getThisClass().getAttributes()) {
            if (attr instanceof BootstrapMethods) {
                return BootstrapMethodsUtil.getMethodFromBootstrap((BootstrapMethods) attr,
                        constDyn.getBootstrapMethodAttrIndex(), getConstantPool(), getThisClass());
            }
        }
        return Optional.empty();
    }
}

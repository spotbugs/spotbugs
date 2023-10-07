package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.classfile.AccessFlags;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import java.util.Optional;
import java.util.stream.Stream;

public class FindSynchronizationLock extends OpcodeStackDetector {

    /**
     * @todo: Change the place where the bug should be detected. It should be placed where the lock object is exposed to the outer world.
     */

    private final BugReporter bugReporter;

    public FindSynchronizationLock(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }


    @Override
    public void sawOpcode(int seen) {

    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        String javaClassName = javaClass.getClassName();

        Method[] methods = javaClass.getMethods();
        for (Method method : methods) {
            Type returnType = method.getReturnType();

            if (javaClassName.equals(returnType.toString())) {
                bugReporter.reportBug(new BugInstance(this, "PFL_SYNCHRONIZE_WITH_PRIVATE_FINAL_LOCK_OBJECTS", NORMAL_PRIORITY)
                        .addClass(this)
                        .addMethod(javaClass, method));
            } else {
                System.out.println("Bug not found");
            }
        }
    }

//    @Override
//    public void visit(JavaClass obj) {
//        boolean isThereSycnhronizedMethod = Stream.of(obj.getMethods()).anyMatch(AccessFlags::isSynchronized);
//        if (isThereSycnhronizedMethod) {
//            super.visit(obj);
//        }
//    }
}

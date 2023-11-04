package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericSignatureParser;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import edu.umd.cs.findbugs.util.ClassName;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.Type;

import java.util.Optional;

public class FindSynchronizationLock extends OpcodeStackDetector {

    /**
     * @todo: Change the place where the bug should be detected. It should be placed where the lock object is exposed to the outer world.
     */
    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private final BugReporter bugReporter;
    private String currentClassName;
    private boolean hasPublicSynchronizedMethod;

    public FindSynchronizationLock(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.hasPublicSynchronizedMethod = false;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            OpcodeStack.Item lock = stack.getStackItem(0);
            Optional<XField> maybeLock = Optional.ofNullable(lock.getXField());

            // To recognize a bad public and non-final lock, we need to check:
            //  - is it used as a lock in the synchronization
            //  - is this declared inside the current class
            //  - is this field public
            //  - is this field non-final
            if (maybeLock.isPresent()) {
                XField lockObject = maybeLock.get();

                // only interested in if it is inside the current class
                // what happens when the lock object is coming from its parent class? @todo create a separate test for this
                String declaringClass = lockObject.getClassName();
                if (currentClassName.equals(declaringClass) &&
                        lockObject.isPublic() && !lockObject.isFinal()) {
                    XMethod xMethod = getXMethod();
                    bugReporter.reportBug(new BugInstance(this, "PFL_SYNCHRONIZE_WITH_PRIVATE_FINAL_LOCK_OBJECTS", NORMAL_PRIORITY)
                            .addClassAndMethod(xMethod)
                            .addField(lockObject));
                }
            }

        }
    }

    // To recognize a bad method synchronization lock, we need to check:
    //      - the class has a synchronized method that is accessible from outside: static or public
    //      - there is a method that returns the class type
    //          - the method is accessible from outside
    @Override
    public void visit(Method obj) {
        // don't visit constructors
        if (Const.CONSTRUCTOR_NAME.equals(obj.getName())) {
            return;
        }

        // don't visit methods if class does not have a public synchronized method
        if (!hasPublicSynchronizedMethod) {
            return;
        }

        XMethod xMethod = getXMethod();

        // @note: to recognize a bad method synchronization, we need to check:
        //  - the method is public
        //  - the method is static
        //  - the method is synchronized
        if (xMethod.isPublic() && xMethod.isStatic() && xMethod.isSynchronized()) {
            bugReporter.reportBug(new BugInstance(this, METHOD_BUG, NORMAL_PRIORITY)
                    .addClassAndMethod(this));
        }

        // @note: to recognize a bad method synchronization lock, we need to check:
        //  - the class has a synchronized method that is public and accessible from outside
        //  - there is a method that returns the class type
        //      - this method is accessible from outside
        if (!(xMethod.isStatic() || xMethod.isPublic())) {
            return;
        }

        String sourceSig = xMethod.getSourceSignature();
        if (sourceSig != null) {
            GenericSignatureParser signature = new GenericSignatureParser(sourceSig);
            String genericReturnValue = signature.getReturnTypeSignature();
            if (genericReturnValue.contains(ClassName.toSlashedClassName(currentClassName))) {
                bugReporter.reportBug(new BugInstance(this, METHOD_BUG, NORMAL_PRIORITY)
                        .addClassAndMethod(xMethod));
            }
        } else {
            SignatureParser signature = new SignatureParser(obj.getSignature());
            String returnType = signature.getReturnTypeSignature();
            if (returnType.contains(ClassName.toSlashedClassName(currentClassName))) {
                bugReporter.reportBug(new BugInstance(this, METHOD_BUG, NORMAL_PRIORITY)
                        .addClassAndMethod(this));
            }
        }
        super.visit(obj);
    }

    @Override
    public void visit(JavaClass obj) {
        currentClassName = obj.getClassName();

        Method[] methods = obj.getMethods();
        for (Method method : methods) {

            if (method.isSynchronized() && method.isPublic()) { // there is a synchronized public method
                hasPublicSynchronizedMethod = true;
                break;
            }
        }
    }

}

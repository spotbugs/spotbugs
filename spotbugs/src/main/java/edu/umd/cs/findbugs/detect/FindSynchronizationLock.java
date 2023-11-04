package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.generic.GenericSignatureParser;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.HashSet;
import java.util.Optional;

public class FindSynchronizationLock extends OpcodeStackDetector {

    /**
     * @note: - Change the place where the bug should be detected.
     * - Think through what happens when multiple synchronized methods are in the same class
     * - Method synchronizations reports the function that exposes the class instead of the synchronized methods
     */
    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private final BugReporter bugReporter;
    private String currentClassName;
    private final HashSet<XMethod> synchronizedMethods;
    private boolean hasExposingMethod;

    public FindSynchronizationLock(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.synchronizedMethods = new HashSet<>();
        this.hasExposingMethod = false;
    }

    // To recognize a bad synchronization lock, we need to check that:
    //  - there is a publicly available synchronized method
    //      - that is declared inside the current class
    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            OpcodeStack.Item lock = stack.getStackItem(0);
            Optional<XField> maybeLock = Optional.ofNullable(lock.getXField());

            if (maybeLock.isPresent()) {
                XField lockObject = maybeLock.get();

                // only interested in if it is inside the current class
                // what happens when the lock object is coming from its parent class? @todo create a separate test for this
                String declaringClass = lockObject.getClassName();
                if (currentClassName.equals(declaringClass)) {
                    XMethod xMethod = getXMethod();

                    // To recognize a bad public non-final lock, we need to check that:
                    //  - is this field public
                    //  - is this field non-final
                    if (lockObject.isPublic() && !lockObject.isFinal()) {
                        bugReporter.reportBug(new BugInstance(this, OBJECT_BUG, NORMAL_PRIORITY)
                                .addClassAndMethod(xMethod)
                                .addField(lockObject));
                    }

                    // To recognize a bad public final lock, we need to check that:
                    //  - is this field public
                    //  - is this field final
                    if (lockObject.isPublic() && lockObject.isFinal()) {
                        bugReporter.reportBug(new BugInstance(this, OBJECT_BUG, NORMAL_PRIORITY)
                                .addClassAndMethod(xMethod)
                                .addField(lockObject));
                    }

                    // To recognize a bad publicly accessible and non-final lock, we need to check that:
                    //  - is this field publicly accessible(volatile)
                    //  - is this field non-final
                    //  - @todo do we have to check if there is an accessor(such as setLock)?
                    if (lockObject.isVolatile() && !lockObject.isFinal()) {
                        bugReporter.reportBug(new BugInstance(this, OBJECT_BUG, NORMAL_PRIORITY)
                                .addClassAndMethod(xMethod)
                                .addField(lockObject));
                    }
                }
            }

        }
    }

    @Override
    public void visit(Method obj) {
        // don't visit constructors
        if (Const.CONSTRUCTOR_NAME.equals(obj.getName())) {
            return;
        }

        XMethod xMethod = getXMethod();
        if (xMethod.isPublic() && xMethod.isSynchronized()) {
            // @note: to recognize a bad method synchronization, we need to check:
            //  - the class has a synchronized method that is public and accessible from outside
            //  - the method is public
            //  - the method is static
            //  - the method is synchronized
            if (xMethod.isStatic()) {
                bugReporter.reportBug(new BugInstance(this, METHOD_BUG, NORMAL_PRIORITY)
                        .addClassAndMethod(this));
            } else {
                // collect xMethods that possible can be reported later
                synchronizedMethods.add(xMethod);
            }
        }

        // method is not accessible from outside
        if (!(xMethod.isStatic() || xMethod.isPublic())) {
            return;
        }

        // @note: to recognize a bad method synchronization lock, we need to check:
        //  - the class has a synchronized method that is public and accessible from outside
        //  - there is a method that returns the class type
        //      - this method is accessible from outside
        String sourceSig = xMethod.getSourceSignature();
        if (sourceSig != null) {
            GenericSignatureParser signature = new GenericSignatureParser(sourceSig);
            String genericReturnValue = signature.getReturnTypeSignature();
            if (genericReturnValue.contains(ClassName.toSlashedClassName(currentClassName))) {
                hasExposingMethod = true;
            }
        } else {
            SignatureParser signature = new SignatureParser(obj.getSignature());
            String returnType = signature.getReturnTypeSignature();
            if (returnType.contains(ClassName.toSlashedClassName(currentClassName))) {
                hasExposingMethod = true;
            }
        }
        super.visit(obj);
    }

    @Override
    public void visit(JavaClass obj) {
        currentClassName = obj.getClassName();
    }

    @Override
    public void visitAfter(JavaClass obj) {
        if (hasExposingMethod) {
            synchronizedMethods.forEach(method ->
                    bugReporter.reportBug(new BugInstance(this, METHOD_BUG, NORMAL_PRIORITY)
                            .addClassAndMethod(method)));

        }
    }
}

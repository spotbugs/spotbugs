package tutorial;

import java.math.BigDecimal;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class DetectorTutorial extends OpcodeStackDetector {

    private BugReporter bugReporter;

    public DetectorTutorial(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Code code) {
        boolean interesting = true;
        if (interesting) {
            // initialize any variables we want to initialize for the method
            super.visit(code); // make callbacks to sawOpcode for all opcodes
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKESPECIAL && getClassConstantOperand().equals("java/math/BigDecimal")
                && getNameConstantOperand().equals("<init>") && getSigConstantOperand().equals("(D)V")) {
            OpcodeStack.Item top = stack.getStackItem(0);
            Object value = top.getConstant();
            if (value instanceof Double) {
                double arg = ((Double) value).doubleValue();
                String dblString = Double.toString(arg);
                String bigDecimalString = new BigDecimal(arg).toString();
                boolean ok = dblString.equals(bigDecimalString) || dblString.equals(bigDecimalString + ".0");

                if (!ok) {
                    boolean scary = dblString.length() <= 8 && dblString.toUpperCase().indexOf("E") == -1;
                    bugReporter.reportBug(new BugInstance(this, "TESTING", scary ? NORMAL_PRIORITY : LOW_PRIORITY)
                            .addClassAndMethod(this).addString(dblString).addSourceLine(this));
                }
            }

        }

    }

}

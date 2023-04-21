package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;

import static edu.umd.cs.findbugs.detect.PublicIdentifiers.PUBLIC_IDENTIFIERS;


public class DontReusePublicIdentifiers extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;


    public DontReusePublicIdentifiers(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    public DontReusePublicIdentifiers(BugAccumulator bugAccumulator) {
        this.bugAccumulator = bugAccumulator;
    }

    @Override
    public void sawOpcode(int seen) {
        // TODO: Implement this method
        //  check the case when a class/interface is declared/defined and check it's name
        // DONE:
        //  create a resource file with the list of public identifiers from JSL -
        //  try resolving the currently instantiated class name
        //  compare the currently instantiated class name with the list of public identifiers
        //  crete a bug instance and add to the bug accumulator

        // case #1: ASTORE is called when creating a new variable then the variable name should be checked
        // variable name when instantiating an object
        if (seen == Const.ASTORE) {
            XField f = getXFieldOperand();
            if (f != null) {
                if (PUBLIC_IDENTIFIERS.contains(f.getName())) {
                    bugAccumulator.accumulateBug(new BugInstance(this, "DONT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY)
                            .addClassAndMethod(this), this);
                }
            }
        }

        // case #2: PUTFIELD/PUTFIELD is called inside a class to declare a variable
        // variable declared inside a class/interface
        if ((seen == Const.PUTFIELD || seen == Const.PUTSTATIC)) {
            XField f = getXFieldOperand();
            if (f != null) {
                bugAccumulator.accumulateBug(new BugInstance(this, "DONT_REUSE_PUBLIC_IDENTIFIERS", NORMAL_PRIORITY)
                        .addClassAndMethod(this), this);
            }

        }

        // case #3: class/interface/enum declaration
        // check the opcode when the class/interface/enum is declared


        // case #4: meth
    }
}

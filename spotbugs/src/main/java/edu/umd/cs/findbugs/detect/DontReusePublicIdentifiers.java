package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

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
        return;
        // TODO: Implement this method
        //  create a resource file with the list of public identifiers from JSL
        //  try resolving the currently instantiated class name
        //  compare the currently instantiated class name with the list of public identifiers
        //  crete a bug instance and add to the bug accumulator
    }
}

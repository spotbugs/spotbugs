/*
 * Contributions to SpotBugs
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.umd.cs.findbugs.detect;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Find occurrences of replaceAll(String regex, String replacement) without any
 * special regex characters.
 *
 * @author Lasse Lindqvist
 */
public class InefficientReplaceAll extends OpcodeStackDetector implements StatelessDetector {

    private static final Logger LOGGER = Logger.getLogger(InefficientReplaceAll.class.getName());

    private static final List<MethodDescriptor> methods = Collections.singletonList(
            new MethodDescriptor("", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));

    private final BugAccumulator bugAccumulator;

    private static final Pattern REGEX_CHARS_PATTERN = Pattern.compile("[" + Pattern.quote("-.\\[]{}()<>*+=?^$|") + "]");

    public InefficientReplaceAll(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (hasInterestingMethod(classContext.getJavaClass().getConstantPool(), methods)) {
            classContext.getJavaClass().accept(this);
        }
    }

    @Override
    public void visit(Method obj) {
        LOGGER.log(Level.FINE, "------------------- Analyzing {0} ----------------", obj.getName());
        super.visit(obj);
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();

    }

    @Override
    public void sawOpcode(int seen) {
        LOGGER.log(Level.FINE, "Opcode: {0}", Const.getOpcodeName(seen));
        if (((seen == Const.INVOKEVIRTUAL) || (seen == Const.INVOKEINTERFACE))
                && ("replaceAll".equals(getNameConstantOperand()))
                && ("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;".equals(getSigConstantOperand()))
                && hasConstantArguments()) {
            Optional<String> firstArgument = getFirstArgument();
            if (firstArgument.isPresent() && !REGEX_CHARS_PATTERN.matcher(firstArgument.get()).find()) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "IRA_INEFFICIENT_REPLACEALL", LOW_PRIORITY).addClassAndMethod(this),
                        this);
            }
        }
    }

    /**
     * @return first argument of the called method if it's a constant
     */
    private Optional<String> getFirstArgument() {
        Object value = getStack().getStackItem(getNumberArguments(getMethodDescriptorOperand().getSignature()) - 1)
                .getConstant();
        return Optional.ofNullable(Objects.toString(value, null));
    }

    /**
     * @return true if only constants are passed to the called method
     */
    private boolean hasConstantArguments() {
        int nArgs = getNumberArguments(getMethodDescriptorOperand().getSignature());
        for (int i = 0; i < nArgs; i++) {
            if (getStack().getStackItem(i).getConstant() == null) {
                return false;
            }
        }
        return true;
    }
}

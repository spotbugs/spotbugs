/*
 * SpotBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;

import java.util.Optional;
import java.util.stream.Stream;

public class FindInstanceLockOnSharedStaticData extends OpcodeStackDetector {

    private final BugAccumulator bugAccumulator;
    private boolean isInsideSynchronizedBlock;
    private Optional<XField> maybeLockObject;

    public FindInstanceLockOnSharedStaticData(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
        isInsideSynchronizedBlock = false;
        maybeLockObject = Optional.empty();
    }

    @Override
    public void sawOpcode(int seen) {

        if (seen == Const.MONITORENTER) {
            isInsideSynchronizedBlock = true;
            OpcodeStack.Item lockObject = stack.getStackItem(0);
            maybeLockObject = Optional.ofNullable(lockObject.getXField());
            return;
        } else if (seen == Const.MONITOREXIT) {
            isInsideSynchronizedBlock = false;
            maybeLockObject = Optional.empty();
            return;
        }

        if (seen == Const.PUTSTATIC) {
            XMethod modificationMethod = getXMethod();
            Optional<XField> fieldToModify = Optional.ofNullable(getXFieldOperand());
            if (fieldToModify.isPresent() && modificationMethod.isSynchronized() && !modificationMethod.isStatic()) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA", NORMAL_PRIORITY)
                                .addClassAndMethod(this)
                                .addMethod(modificationMethod)
                                .addField(fieldToModify.get())
                                .addString(fieldToModify.get().getName())
                                .addString("synchronized method"),
                        this);
                return;
            }

            boolean isLockObjectAppropriate = maybeLockObject.map(XField::isStatic).orElse(false);
            if (fieldToModify.isPresent() && isInsideSynchronizedBlock && !isLockObjectAppropriate) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA", NORMAL_PRIORITY)
                                .addClassAndMethod(this)
                                .addMethod(modificationMethod)
                                .addField(fieldToModify.get())
                                .addString(fieldToModify.get().getName())
                                .addString("synchronization lock"),
                        this);
            }
        }

    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void visit(JavaClass obj) {
        boolean classIsInteresting = Stream.of(obj.getFields()).anyMatch(field -> field.isStatic() && !field.isFinal());
        if (classIsInteresting) {
            isInsideSynchronizedBlock = false;
            maybeLockObject = Optional.empty();
            super.visit(obj);
        }
    }

}

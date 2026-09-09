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

import java.util.Map;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class UnnecessaryEnvUsage extends OpcodeStackDetector {
    private static final Map<String, String> envvarPropertyMap = Map.ofEntries(
            Map.entry("JAVA_HOME", "java.home"),
            Map.entry("JAVA_VERSION", "java.version"),
            Map.entry("TEMP", "java.io.tmpdir"),
            Map.entry("TMP", "java.io.tmpdir"),
            Map.entry("PROCESSOR_ARCHITECTURE", "os.arch"),
            Map.entry("OS", "os.name"),
            Map.entry("USER", "user.name"),
            Map.entry("USERNAME", "user.name"),
            Map.entry("HOME", "user.home"),
            Map.entry("HOMEPATH", "user.home"),
            Map.entry("CD", "user.dir"),
            Map.entry("PWD", "user.dir"));

    private final BugAccumulator bugAccumulator;

    public UnnecessaryEnvUsage(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
    }

    private void reportBugIfParamIsProblematic() {
        OpcodeStack.Item top = stack.getStackItem(0);
        if (top.getConstant() instanceof String) {
            String constant = (String) top.getConstant();
            if (envvarPropertyMap.containsKey(constant)) {
                BugInstance pendingBug = new BugInstance(this, "ENV_USE_PROPERTY_INSTEAD_OF_ENV", NORMAL_PRIORITY)
                        .addClassAndMethod(this)
                        .addString(constant)
                        .addString(envvarPropertyMap.get(constant));
                bugAccumulator.accumulateBug(pendingBug, SourceLineAnnotation.fromVisitedInstruction(this, this.getPC()));
            }
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKESTATIC || seen == Const.INVOKEINTERFACE) {
            XMethod xMethod = this.getXMethodOperand();
            if (xMethod == null) {
                return;
            }

            // Directly call System.lang.getenv(String) with problematic parameter
            if ("java.lang.System".equals(xMethod.getClassName()) && "getenv".equals(xMethod.getName())
                    && "(Ljava/lang/String;)Ljava/lang/String;".equals(xMethod.getSignature())) {
                reportBugIfParamIsProblematic();

                // Get the map of environment variables with System.lang.getenv(), and then call java.util.Map.get() on it with problematic parameter
            } else if ("java.util.Map".equals(xMethod.getClassName()) && "get".equals(xMethod.getName())
                    && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(xMethod.getSignature()) && stack.getStackDepth() >= 2) {

                XMethod rvo = stack.getStackItem(1).getReturnValueOf();
                if (rvo != null && "java.lang.System".equals(rvo.getClassName()) && "getenv".equals(rvo.getName())
                        && "()Ljava/util/Map;".equals(rvo.getSignature())) {
                    reportBugIfParamIsProblematic();
                }
            }
        }
    }
}

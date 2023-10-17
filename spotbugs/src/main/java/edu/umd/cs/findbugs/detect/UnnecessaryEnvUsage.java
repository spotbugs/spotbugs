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

import java.util.Collections;
import java.util.HashMap;
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
    private static final Map<String, String> envvarPropertyMap = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put("OS", "os.name");
            put("PROCESSOR_ARCHITECTURE", "os.arch");
            put("USERNAME", "user.name");
            put("USER", "user.name");
            put("HOMEPATH", "user.home");
            put("HOME", "user.home");
            put("PWD", "user.dir");
            put("JAVA_HOME", "java.home");
            put("JAVA_VERSION", "java.version");
            put("TEMP", "java.io.tmpdir");
            put("TMP", "java.io.tmpdir");
        }
    });

    private final BugAccumulator bugAccumulator;

    public UnnecessaryEnvUsage(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKESTATIC) {
            XMethod xMethod = this.getXMethodOperand();
            if (xMethod == null) {
                return;
            }

            if ("java.lang.System".equals(xMethod.getClassName()) && "getenv".equals(xMethod.getName())) {
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
        }
    }
}

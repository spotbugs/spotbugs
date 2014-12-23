/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 University of Maryland
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.AALOAD;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.constant.Constant;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.ba.constant.ConstantFrame;
import edu.umd.cs.findbugs.ba.type.TopType;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.detect.BuildStringPassthruGraph.MethodParameter;
import edu.umd.cs.findbugs.detect.BuildStringPassthruGraph.StringPassthruDatabase;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Find potential SQL injection vulnerabilities.
 *
 * @author David Hovemeyer
 * @author Bill Pugh
 * @author Matt Hargett
 */
public class FindSqlInjection implements Detector {
    private static final String[] PREPARE_STATEMENT_SIGNATURES = new String[] {
        "(Ljava/lang/String;)Ljava/sql/PreparedStatement;",
        "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;",
        "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
        "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;",
        "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;",
        "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;",
    };

    private static final MethodDescriptor[] EXECUTE_METHODS = new MethodDescriptor[] {
        new MethodDescriptor("java/sql/Statement", "executeQuery", "(Ljava/lang/String;)Ljava/sql/ResultSet;"),
        new MethodDescriptor("java/sql/Statement", "executeUpdate", "(Ljava/lang/String;)I"),
        new MethodDescriptor("java/sql/Statement", "executeUpdate", "(Ljava/lang/String;I)I"),
        new MethodDescriptor("java/sql/Statement", "executeUpdate", "(Ljava/lang/String;[I)I"),
        new MethodDescriptor("java/sql/Statement", "executeUpdate", "(Ljava/lang/String;[Ljava/lang/String;)I"),
        new MethodDescriptor("java/sql/Statement", "executeLargeUpdate", "(Ljava/lang/String;)J"),
        new MethodDescriptor("java/sql/Statement", "executeLargeUpdate", "(Ljava/lang/String;I)J"),
        new MethodDescriptor("java/sql/Statement", "executeLargeUpdate", "(Ljava/lang/String;[I)J"),
        new MethodDescriptor("java/sql/Statement", "executeLargeUpdate", "(Ljava/lang/String;[Ljava/lang/String;)J"),
        new MethodDescriptor("java/sql/Statement", "execute", "(Ljava/lang/String;)Z"),
        new MethodDescriptor("java/sql/Statement", "execute", "(Ljava/lang/String;I)Z"),
        new MethodDescriptor("java/sql/Statement", "execute", "(Ljava/lang/String;[I)Z"),
        new MethodDescriptor("java/sql/Statement", "execute", "(Ljava/lang/String;[Ljava/lang/String;)Z"),
        new MethodDescriptor("java/sql/Statement", "addBatch", "(Ljava/lang/String;)V"),
    };

    private static class StringAppendState {
        // remember the smallest position at which we saw something that
        // concerns us
        int sawOpenQuote = Integer.MAX_VALUE;

        int sawCloseQuote = Integer.MAX_VALUE;

        int sawComma = Integer.MAX_VALUE;

        int sawAppend = Integer.MAX_VALUE;

        int sawUnsafeAppend = Integer.MAX_VALUE;

        int sawTaint = Integer.MAX_VALUE;

        int sawSeriousTaint = Integer.MAX_VALUE;

        public boolean getSawOpenQuote(InstructionHandle handle) {
            return sawOpenQuote <= handle.getPosition();
        }

        public boolean getSawCloseQuote(InstructionHandle handle) {
            return sawCloseQuote <= handle.getPosition();
        }

        public boolean getSawComma(InstructionHandle handle) {
            return sawComma <= handle.getPosition();
        }

        public boolean getSawAppend(InstructionHandle handle) {
            return sawAppend <= handle.getPosition();
        }

        public boolean getSawUnsafeAppend(InstructionHandle handle) {
            return sawUnsafeAppend <= handle.getPosition();
        }

        public boolean getSawTaint(InstructionHandle handle) {
            return sawTaint <= handle.getPosition();
        }

        public boolean getSawSeriousTaint(InstructionHandle handle) {
            return sawSeriousTaint <= handle.getPosition();
        }

        public void setSawOpenQuote(InstructionHandle handle) {
            sawOpenQuote = Math.min(sawOpenQuote, handle.getPosition());
        }

        public void setSawCloseQuote(InstructionHandle handle) {
            sawCloseQuote = Math.min(sawCloseQuote, handle.getPosition());
        }

        public void setSawComma(InstructionHandle handle) {
            sawComma = Math.min(sawComma, handle.getPosition());
        }

        public void setSawAppend(InstructionHandle handle) {
            sawAppend = Math.min(sawAppend, handle.getPosition());
        }

        public void setSawUnsafeAppend(InstructionHandle handle) {
            sawUnsafeAppend = Math.min(sawUnsafeAppend, handle.getPosition());
        }

        public void setSawSeriousTaint(InstructionHandle handle) {
            sawSeriousTaint = Math.min(sawSeriousTaint, handle.getPosition());
        }

        public void setSawTaint(InstructionHandle handle) {
            sawTaint = Math.min(sawTaint, handle.getPosition());
        }

        public void setSawInitialTaint() {
            sawTaint = 0;
        }

    }

    BugReporter bugReporter;

    BugAccumulator bugAccumulator;

    final Map<MethodDescriptor, int[]> preparedStatementMethods;
    final Map<MethodDescriptor, int[]> executeMethods;
    final Set<MethodDescriptor> allMethods = new HashSet<>();

    private final boolean testingEnabled;

    public FindSqlInjection(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
        testingEnabled = SystemProperties.getBoolean("report_TESTING_pattern_in_standard_detectors");
        Set<MethodParameter> baseExecuteMethods = new HashSet<>();
        for(MethodDescriptor executeMethod : EXECUTE_METHODS) {
            baseExecuteMethods.add(new MethodParameter(executeMethod, 0));
        }
        executeMethods = Global.getAnalysisCache().getDatabase(StringPassthruDatabase.class).findLinkedMethods(baseExecuteMethods);
        Set<MethodParameter> basePrepareMethods = new HashSet<>();
        for(String signature : PREPARE_STATEMENT_SIGNATURES) {
            basePrepareMethods.add(new MethodParameter(new MethodDescriptor("java/sql/Connection", "prepareStatement", signature), 0));
        }
        preparedStatementMethods = Global.getAnalysisCache().getDatabase(StringPassthruDatabase.class).findLinkedMethods(basePrepareMethods);
        allMethods.addAll(executeMethods.keySet());
        allMethods.addAll(preparedStatementMethods.keySet());
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        if(!PreorderVisitor.hasInterestingMethod(javaClass.getConstantPool(), allMethods)) {
            return;
        }
        Method[] methodList = javaClass.getMethods();

        for (Method method : methodList) {
            MethodGen methodGen = classContext.getMethodGen(method);
            if (methodGen == null) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (DataflowAnalysisException e) {
                bugReporter.logError(
                        "FindSqlInjection caught exception while analyzing " + classContext.getFullyQualifiedMethodName(method),
                        e);
            } catch (CFGBuilderException e) {
                bugReporter.logError(
                        "FindSqlInjection caught exception while analyzing " + classContext.getFullyQualifiedMethodName(method),
                        e);
            } catch (RuntimeException e) {
                bugReporter.logError(
                        "FindSqlInjection caught exception while analyzing " + classContext.getFullyQualifiedMethodName(method),
                        e);
            }
        }
    }

    private boolean isStringAppend(Instruction ins, ConstantPoolGen cpg) {
        if (ins instanceof INVOKEVIRTUAL) {
            INVOKEVIRTUAL invoke = (INVOKEVIRTUAL) ins;

            if ("append".equals(invoke.getMethodName(cpg)) && invoke.getClassName(cpg).startsWith("java.lang.StringB")) {
                String sig = invoke.getSignature(cpg);
                char firstChar = sig.charAt(1);
                return firstChar == '[' || firstChar == 'L';
            }
        }

        return false;
    }

    private boolean isConstantStringLoad(Location location, ConstantPoolGen cpg)  {
        Instruction ins = location.getHandle().getInstruction();
        if (ins instanceof LDC) {
            LDC load = (LDC) ins;
            Object value = load.getValue(cpg);
            if (value instanceof String) {
                return true;
            }
        }

        return false;
    }

    static final Pattern openQuotePattern = Pattern.compile("((^')|[^\\p{Alnum}]')$");

    public static boolean isOpenQuote(String s) {
        return openQuotePattern.matcher(s).find();
    }

    static final Pattern closeQuotePattern = Pattern.compile("^'($|[^\\p{Alnum}])");

    public static boolean isCloseQuote(String s) {
        return closeQuotePattern.matcher(s).find();
    }

    private StringAppendState updateStringAppendState(Location location, ConstantPoolGen cpg, StringAppendState stringAppendState)
    {
        InstructionHandle handle = location.getHandle();
        Instruction ins = handle.getInstruction();
        if (!isConstantStringLoad(location, cpg)) {
            throw new IllegalArgumentException("instruction must be LDC");
        }

        LDC load = (LDC) ins;
        Object value = load.getValue(cpg);
        String stringValue = ((String) value).trim();
        if (stringValue.startsWith(",") || stringValue.endsWith(",")) {
            stringAppendState.setSawComma(handle);
        }
        if (isCloseQuote(stringValue) && stringAppendState.getSawOpenQuote(handle)) {
            stringAppendState.setSawCloseQuote(handle);
        }
        if (isOpenQuote(stringValue)) {
            stringAppendState.setSawOpenQuote(handle);
        }

        return stringAppendState;
    }

    private StringAppendState getStringAppendState(CFG cfg, ConstantPoolGen cpg) throws CFGBuilderException {
        StringAppendState stringAppendState = new StringAppendState();
        String sig = method.getSignature();
        sig = sig.substring(0, sig.indexOf(')'));

        if (sig.indexOf("java/lang/String") >= 0) {
            stringAppendState.setSawInitialTaint();
        }
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();
            if (isConstantStringLoad(location, cpg)) {
                stringAppendState = updateStringAppendState(location, cpg, stringAppendState);
            } else if (isStringAppend(ins, cpg)) {
                stringAppendState.setSawAppend(handle);

                Location prevLocation = getPreviousLocation(cfg, location, true);
                if (prevLocation != null && !isSafeValue(prevLocation, cpg)) {
                    stringAppendState.setSawUnsafeAppend(handle);
                }

            } else if (ins instanceof InvokeInstruction) {
                InvokeInstruction inv = (InvokeInstruction) ins;
                String sig1 = inv.getSignature(cpg);
                String sig2 = sig1.substring(sig1.indexOf(')'));

                if (sig2.indexOf("java/lang/String") >= 0) {
                    String methodName = inv.getMethodName(cpg);
                    String className = inv.getClassName(cpg);
                    if ("valueOf".equals(methodName) && "java.lang.String".equals(className)
                            && "(Ljava/lang/Object;)Ljava/lang/String;".equals(sig1)) {
                        try {
                            TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
                            TypeFrame frame = typeDataflow.getFactAtLocation(location);
                            if (!frame.isValid()) {
                                // This basic block is probably dead
                                continue;
                            }
                            Type operandType = frame.getTopValue();
                            if (operandType.equals(TopType.instance())) {
                                // unreachable
                                continue;
                            }
                            String sig3 = operandType.getSignature();
                            if (!"Ljava/lang/String;".equals(sig3)) {
                                stringAppendState.setSawTaint(handle);
                            }
                        } catch (CheckedAnalysisException e) {
                            stringAppendState.setSawTaint(handle);
                        }
                    } else if (className.startsWith("java.lang.String") || "java.lang.Long".equals(className)
                            || "java.lang.Integer".equals(className) || "java.lang.Float".equals(className)
                            || "java.lang.Double".equals(className) || "java.lang.Short".equals(className)
                            || "java.lang.Byte".equals(className) || "java.lang.Character".equals(className)) {
                        // ignore it
                        assert true;
                    } else if (methodName.startsWith("to") && methodName.endsWith("String") && methodName.length() > 8) {
                        // ignore it
                        assert true;
                    } else if (className.startsWith("javax.servlet") && methodName.startsWith("get")) {
                        stringAppendState.setSawTaint(handle);
                        stringAppendState.setSawSeriousTaint(handle);
                    } else {
                        stringAppendState.setSawTaint(handle);
                    }

                }
            } else if (ins instanceof GETFIELD) {
                GETFIELD getfield = (GETFIELD) ins;
                String sig2 = getfield.getSignature(cpg);
                if (sig2.indexOf("java/lang/String") >= 0) {
                    stringAppendState.setSawTaint(handle);
                }
            }
        }

        return stringAppendState;
    }

    private boolean isSafeValue(Location location, ConstantPoolGen cpg) throws CFGBuilderException {
        Instruction prevIns = location.getHandle().getInstruction();
        if (prevIns instanceof LDC || prevIns instanceof GETSTATIC) {
            return true;
        }
        if (prevIns instanceof InvokeInstruction) {
            String methodName = ((InvokeInstruction) prevIns).getMethodName(cpg);
            if (methodName.startsWith("to") && methodName.endsWith("String") && methodName.length() > 8) {
                return true;
            }
        }
        if (prevIns instanceof AALOAD) {
            CFG cfg = classContext.getCFG(method);

            Location prev = getPreviousLocation(cfg, location, true);
            if (prev != null) {
                Location prev2 = getPreviousLocation(cfg, prev, true);
                if (prev2 != null && prev2.getHandle().getInstruction() instanceof GETSTATIC) {
                    GETSTATIC getStatic = (GETSTATIC) prev2.getHandle().getInstruction();
                    if ("[Ljava/lang/String;".equals(getStatic.getSignature(cpg))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private @CheckForNull
    InstructionHandle getPreviousInstruction(InstructionHandle handle, boolean skipNops) {
        while (handle.getPrev() != null) {
            handle = handle.getPrev();
            Instruction prevIns = handle.getInstruction();
            if (!(prevIns instanceof NOP && skipNops)) {
                return handle;
            }
        }
        return null;
    }

    private @CheckForNull
    Location getPreviousLocation(CFG cfg, Location startLocation, boolean skipNops) {
        Location loc = startLocation;
        InstructionHandle prev = getPreviousInstruction(loc.getHandle(), skipNops);
        if (prev != null) {
            return new Location(prev, loc.getBasicBlock());
        }
        BasicBlock block = loc.getBasicBlock();
        while (true) {
            block = cfg.getPredecessorWithEdgeType(block, EdgeTypes.FALL_THROUGH_EDGE);
            if (block == null) {
                return null;
            }
            InstructionHandle lastInstruction = block.getLastInstruction();
            if (lastInstruction != null) {
                return new Location(lastInstruction, block);
            }
        }
    }

    private BugInstance generateBugInstance(JavaClass javaClass, MethodGen methodGen, InstructionHandle handle,
            StringAppendState stringAppendState, boolean isExecute) {
        int priority = LOW_PRIORITY;
        boolean sawSeriousTaint = false;
        if (stringAppendState.getSawAppend(handle)) {
            if (stringAppendState.getSawOpenQuote(handle) && stringAppendState.getSawCloseQuote(handle)) {
                priority = HIGH_PRIORITY;
            } else if (stringAppendState.getSawComma(handle)) {
                priority = NORMAL_PRIORITY;
            }

            if (!stringAppendState.getSawUnsafeAppend(handle)) {
                priority += 2;
            } else if (stringAppendState.getSawSeriousTaint(handle)) {
                priority--;
                sawSeriousTaint = true;
            } else if (!stringAppendState.getSawTaint(handle)) {
                priority++;
            }
        }

        String description = "TESTING";
        if (isExecute) {
            description = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE";
        } else {
            description = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING";
        }

        BugInstance bug = new BugInstance(this, description, priority);
        bug.addClassAndMethod(methodGen, javaClass.getSourceFileName());
        if ("TESTING".equals(description)) {
            bug.addString("Incomplete report invoking non-constant SQL string");
        }
        if (sawSeriousTaint) {
            bug.addString("non-constant SQL string involving HTTP taint");
        }

        return bug;
    }

    Method method;

    ClassContext classContext;

    private void analyzeMethod(ClassContext classContext, Method method) throws DataflowAnalysisException, CFGBuilderException {
        JavaClass javaClass = classContext.getJavaClass();
        this.method = method;
        this.classContext = classContext;
        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen == null) {
            return;
        }

        ConstantPoolGen cpg = methodGen.getConstantPool();
        CFG cfg = classContext.getCFG(method);

        StringAppendState stringAppendState = getStringAppendState(cfg, cpg);

        ConstantDataflow dataflow = classContext.getConstantDataflow(method);
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            Instruction ins = location.getHandle().getInstruction();
            if (!(ins instanceof InvokeInstruction)) {
                continue;
            }
            InvokeInstruction invoke = (InvokeInstruction) ins;
            MethodDescriptor md = new MethodDescriptor(invoke, cpg);
            boolean executeMethod;
            int[] params = preparedStatementMethods.get(md);
            int paramNumber;
            // Currently only one method parameter is checked, though it's the most common case
            // TODO: support methods which take several SQL statements
            if(params != null) {
                executeMethod = false;
                paramNumber = params[0];
            } else {
                params = executeMethods.get(md);
                if(params != null) {
                    executeMethod = true;
                    paramNumber = params[0];
                } else {
                    continue;
                }
            }
            ConstantFrame frame = dataflow.getFactAtLocation(location);
            int numArguments = frame.getNumArguments(invoke, cpg);
            Constant value = frame.getStackValue(numArguments - 1 - paramNumber);

            if (!value.isConstantString()) {
                // TODO: verify it's the same string represented by
                // stringAppendState
                // FIXME: will false positive on const/static strings
                // returns by methods
                Location prev = getPreviousLocation(cfg, location, true);
                if (prev == null || !isSafeValue(prev, cpg)) {
                    BugInstance bug = generateBugInstance(javaClass, methodGen, location.getHandle(), stringAppendState, executeMethod);
                    if(!testingEnabled && "TESTING".equals(bug.getType())){
                        continue;
                    }
                    bugAccumulator.accumulateBug(
                            bug,
                            SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                                    javaClass.getSourceFileName(), location.getHandle()));
                }
            }
        }
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void report() {
    }
}


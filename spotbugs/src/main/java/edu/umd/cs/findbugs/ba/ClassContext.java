/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.AnalysisLocal;
import edu.umd.cs.findbugs.OpcodeStack.JumpInfo;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.ba.ca.CallListDataflow;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefDataflow;
import edu.umd.cs.findbugs.ba.heap.LoadDataflow;
import edu.umd.cs.findbugs.ba.heap.StoreDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.ReturnPathTypeDataflow;
import edu.umd.cs.findbugs.ba.npe.UsagesRequiringNonNullValues;
import edu.umd.cs.findbugs.ba.type.ExceptionSetFactory;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.vna.LoadedFieldSet;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.engine.bcel.NonExceptionPostdominatorsAnalysis;
import edu.umd.cs.findbugs.classfile.engine.bcel.NonImplicitExceptionPostDominatorsAnalysis;
import edu.umd.cs.findbugs.classfile.engine.bcel.UnpackedBytecodeCallback;
import edu.umd.cs.findbugs.classfile.engine.bcel.UnpackedCode;
import edu.umd.cs.findbugs.util.MapCache;

/**
 * A ClassContext caches all of the auxiliary objects used to analyze the
 * methods of a class. That way, these objects don't need to be created over and
 * over again.
 *
 * @author David Hovemeyer
 */
public class ClassContext {
    public static final boolean DEBUG = SystemProperties.getBoolean("classContext.debug");

    public static final boolean TIME_ANALYSES = SystemProperties.getBoolean("classContext.timeAnalyses");

    public static final boolean DUMP_DATAFLOW_ANALYSIS = SystemProperties.getBoolean("dataflow.dump");

    private final JavaClass jclass;

    private final ClassInfo classInfo;

    private final AnalysisContext analysisContext;

    private final Map<Class<?>, Map<MethodDescriptor, Object>> methodAnalysisObjectMap;

    /*
     * ----------------------------------------------------------------------
     * Public methods
     * ----------------------------------------------------------------------
     */

    @Override
    public String toString() {
        return jclass.getClassName();
    }

    /**
     * Constructor.
     *
     * @param jclass
     *            the JavaClass
     */
    public ClassContext(JavaClass jclass, AnalysisContext analysisContext) {
        this.jclass = jclass;
        this.analysisContext = analysisContext;
        this.methodAnalysisObjectMap = new HashMap<Class<?>, Map<MethodDescriptor, Object>>();
        try {
            classInfo = (ClassInfo) Global.getAnalysisCache().getClassAnalysis(XClass.class,
                    DescriptorFactory.createClassDescriptor(jclass));
        } catch (CheckedAnalysisException e) {
            throw new AssertionError("No ClassInfo for " + jclass);
        }
    }

    public Map<MethodDescriptor, Object> getObjectMap(Class<?> analysisClass) {
        Map<MethodDescriptor, Object> objectMap = methodAnalysisObjectMap.get(analysisClass);
        if (objectMap == null) {
            if (analysisClass == ValueNumberDataflow.class) {
                objectMap = new MapCache<MethodDescriptor, Object>(300);
            } else if (Dataflow.class.isAssignableFrom(analysisClass)) {
                objectMap = new MapCache<MethodDescriptor, Object>(500);
            } else {
                objectMap = new HashMap<MethodDescriptor, Object>();
            }
            methodAnalysisObjectMap.put(analysisClass, objectMap);
        }
        return objectMap;
    }

    /**
     * Store a method analysis object. Note that the cached analysis object
     * could be a special value (indicating null or an exception).
     *
     * @param analysisClass
     *            class the method analysis object belongs to
     * @param methodDescriptor
     *            method descriptor identifying the analyzed method
     * @param object
     *            the analysis object to cache
     */
    public void putMethodAnalysis(Class<?> analysisClass, MethodDescriptor methodDescriptor, Object object) {
        if (object == null) {
            throw new IllegalArgumentException();
        }
        Map<MethodDescriptor, Object> objectMap = getObjectMap(analysisClass);
        objectMap.put(methodDescriptor, object);
    }

    /**
     * Retrieve a method analysis object.
     *
     * @param analysisClass
     *            class the method analysis object should belong to
     * @param methodDescriptor
     *            method descriptor identifying the analyzed method
     * @return the analysis object
     */
    public Object getMethodAnalysis(Class<?> analysisClass, MethodDescriptor methodDescriptor) {
        Map<MethodDescriptor, Object> objectMap = getObjectMap(analysisClass);
        return objectMap.get(methodDescriptor);
    }

    public void purgeAllMethodAnalyses() {
        methodAnalysisObjectMap.clear();
    }

    /**
     * Purge all CFG-based method analyses for given method.
     *
     * @param methodDescriptor
     *            method descriptor identifying method to purge
     */
    public void purgeMethodAnalyses(MethodDescriptor methodDescriptor) {
        Set<Map.Entry<Class<?>, Map<MethodDescriptor, Object>>> entrySet = methodAnalysisObjectMap.entrySet();
        for (Iterator<Map.Entry<Class<?>, Map<MethodDescriptor, Object>>> i = entrySet.iterator(); i.hasNext();) {
            Map.Entry<Class<?>, Map<MethodDescriptor, Object>> entry = i.next();

            Class<?> cls = entry.getKey();

            // FIXME: hack
            if (!DataflowAnalysis.class.isAssignableFrom(cls) && !Dataflow.class.isAssignableFrom(cls)) {
                // There is really no need to purge analysis results
                // that aren't CFG-based.
                // Currently, only dataflow analyses need
                // to be purged.
                continue;
            }

            entry.getValue().remove(methodDescriptor);
        }
    }

    /**
     * Get the JavaClass.
     */
    public JavaClass getJavaClass() {
        return jclass;
    }

    public XClass getXClass() {
        return classInfo;
    }

    public ClassDescriptor getClassDescriptor() {
        return classInfo;
    }

    /**
     * Look up the Method represented by given MethodGen.
     *
     * @param methodGen
     *            a MethodGen
     * @return the Method represented by the MethodGen
     */
    public Method getMethod(MethodGen methodGen) {
        Method[] methodList = jclass.getMethods();
        for (Method method : methodList) {
            if (method.getName().equals(methodGen.getName()) && method.getSignature().equals(methodGen.getSignature())
                    && method.getAccessFlags() == methodGen.getAccessFlags()) {
                return method;
            }
        }
        return null;
    }

    public String getFullyQualifiedMethodName(Method method) {
        return getClassDescriptor().getDottedClassName() + "." + method.getName() + method.getSignature();
    }

    public @Nonnull
    List<Method> getMethodsInCallOrder() {
        Map<XMethod, Method> map = new HashMap<XMethod, Method>();
        for (Method m : getJavaClass().getMethods()) {
            XMethod xMethod = classInfo.findMethod(m.getName(), m.getSignature(), m.isStatic());
            map.put(xMethod, m);
        }
        List<? extends XMethod> xmethodsInCallOrder = classInfo.getXMethodsInCallOrder();
        List<Method> methodsInCallOrder = new ArrayList<Method>(xmethodsInCallOrder.size());
        for (XMethod x : xmethodsInCallOrder) {
            Method m = map.get(x);
            if (m != null) {
                methodsInCallOrder.add(m);
            }
        }
        return methodsInCallOrder;
    }

    /**
     * Get the AnalysisContext.
     */
    public AnalysisContext getAnalysisContext() {
        return analysisContext;
    }

    /**
     * Get the RepositoryLookupFailureCallback.
     *
     * @return the RepositoryLookupFailureCallback
     */
    public RepositoryLookupFailureCallback getLookupFailureCallback() {
        return analysisContext.getLookupFailureCallback();
    }

    /**
     * Get a MethodGen object for given method.
     *
     * @param method
     *            the method
     * @return the MethodGen object for the method, or null if the method has no
     *         Code attribute (and thus cannot be analyzed) or if the method
     *         seems unprofitable to analyze
     */
    @CheckForNull
    public MethodGen getMethodGen(Method method) {
        return getMethodAnalysisNoException(MethodGen.class, method);
    }

    /**
     * Get a CFG for given method. If pruning options are in effect, pruning
     * will be done. Because the CFG pruning can involve interprocedural
     * analysis, it is done on a best-effort basis, so the CFG returned might
     * not actually be pruned.
     *
     * @param method
     *            the method
     * @return the CFG
     * @throws CFGBuilderException
     *             if a CFG cannot be constructed for the method
     */
    public CFG getCFG(Method method) throws CFGBuilderException {
        return getMethodAnalysisNoDataflowAnalysisException(CFG.class, method);
    }

    /**
     * Get the ConstantPoolGen used to create the MethodGens for this class.
     *
     * @return the ConstantPoolGen
     */
    public @Nonnull
    ConstantPoolGen getConstantPoolGen() {
        return getClassAnalysisNoException(ConstantPoolGen.class);
    }

    /**
     * Get a UsagesRequiringNonNullValues for given method.
     *
     * @param method
     *            the method
     * @return the UsagesRequiringNonNullValues
     */
    public UsagesRequiringNonNullValues getUsagesRequiringNonNullValues(Method method) throws DataflowAnalysisException,
    CFGBuilderException {
        return getMethodAnalysis(UsagesRequiringNonNullValues.class, method);
    }

    /**
     * Get a ValueNumberDataflow for given method.
     *
     * @param method
     *            the method
     * @return the ValueNumberDataflow
     */
    public ValueNumberDataflow getValueNumberDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
        return getMethodAnalysis(ValueNumberDataflow.class, method);
    }

    /**
     * Get an IsNullValueDataflow for given method.
     *
     * @param method
     *            the method
     * @return the IsNullValueDataflow
     */
    public IsNullValueDataflow getIsNullValueDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
        return getMethodAnalysis(IsNullValueDataflow.class, method);
    }

    /**
     * Get a TypeDataflow for given method.
     *
     * @param method
     *            the method
     * @return the TypeDataflow
     */
    public TypeDataflow getTypeDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
        return getMethodAnalysis(TypeDataflow.class, method);
    }

    /**
     * Get a DepthFirstSearch for given method.
     *
     * @param method
     *            the method
     * @return the DepthFirstSearch
     */
    public DepthFirstSearch getDepthFirstSearch(Method method) throws CFGBuilderException {
        return getMethodAnalysisNoDataflowAnalysisException(DepthFirstSearch.class, method);
    }

    /**
     * Get a ReverseDepthFirstSearch for given method.
     *
     * @param method
     *            the method
     * @return the ReverseDepthFirstSearch
     */
    public ReverseDepthFirstSearch getReverseDepthFirstSearch(Method method) throws CFGBuilderException {
        return getMethodAnalysisNoDataflowAnalysisException(ReverseDepthFirstSearch.class, method);
    }

    static final AnalysisLocal<MapCache<XMethod, BitSet>> cachedBitsets_AL =
            new AnalysisLocal<MapCache<XMethod, BitSet>>() {
        @Override
        protected MapCache<XMethod, BitSet> initialValue() {
            return  new MapCache<XMethod, BitSet>(64);
        }
    };

    static final AnalysisLocal<MapCache<XMethod, Set<Integer>>> cachedLoopExits_AL =
            new AnalysisLocal<MapCache<XMethod, Set<Integer>>>() {
        @Override
        protected MapCache<XMethod, Set<Integer>> initialValue() {
            return  new MapCache<XMethod, Set<Integer>>(13);
        }
    };


    /**
     * Get a BitSet representing the bytecodes that are used in the given
     * method. This is useful for prescreening a method for the existence of
     * particular instructions. Because this step doesn't require building a
     * MethodGen, it is very fast and memory-efficient. It may allow a Detector
     * to avoid some very expensive analysis, which is a Big Win for the user.
     *
     * @param method
     *            the method
     * @return the BitSet containing the opcodes which appear in the method, or
     *         null if the method has no code
     */
    @CheckForNull
    public BitSet getBytecodeSet(Method method) {
        return getBytecodeSet(jclass, method);
    }

    /**
     * Get a BitSet representing the bytecodes that are used in the given
     * method. This is useful for prescreening a method for the existence of
     * particular instructions. Because this step doesn't require building a
     * MethodGen, it is very fast and memory-efficient. It may allow a Detector
     * to avoid some very expensive analysis, which is a Big Win for the user.
     *
     * @param method
     *            the method
     * @return the BitSet containing the opcodes which appear in the method, or
     *         null if the method has no code
     */
    @CheckForNull
    static public BitSet getBytecodeSet(JavaClass clazz, Method method) {

        XMethod xmethod = XFactory.createXMethod(clazz, method);
        if (cachedBitsets().containsKey(xmethod)) {
            return cachedBitsets().get(xmethod);
        }
        Code code = method.getCode();
        if (code == null) {
            return null;
        }

        byte[] instructionList = code.getCode();

        // Create callback
        UnpackedBytecodeCallback callback = new UnpackedBytecodeCallback(instructionList.length);

        // Scan the method.
        BytecodeScanner scanner = new BytecodeScanner();
        scanner.scan(instructionList, callback);

        UnpackedCode unpackedCode = callback.getUnpackedCode();
        BitSet result = null;
        if (unpackedCode != null) {
            result = unpackedCode.getBytecodeSet();
        }
        cachedBitsets().put(xmethod, result);
        return result;
    }

    private static MapCache<XMethod, BitSet> cachedBitsets() {
        return cachedBitsets_AL.get();
    }

    @Nonnull
    static public Set<Integer> getLoopExitBranches(Method method, MethodGen methodGen) {

        XMethod xmethod = XFactory.createXMethod(methodGen);
        if (cachedLoopExits().containsKey(xmethod)) {
            Set<Integer> result = cachedLoopExits().get(xmethod);
            if (result == null) {
                AnalysisContext.logError("Null cachedLoopExits for " + xmethod, new NullPointerException());
                assert false;
                return Collections.<Integer> emptySet();
            }
            return result;
        }
        Code code = method.getCode();
        if (code == null) {
            assert false;
            return Collections.<Integer> emptySet();
        }

        byte[] instructionList = code.getCode();

        Set<Integer> result = new HashSet<Integer>();
        for (int i = 0; i < instructionList.length; i++) {
            if (checkForBranchExit(instructionList, i)) {
                result.add(i);
            }
        }
        if (result.size() == 0) {
            result = Collections.<Integer> emptySet();
        }

        cachedLoopExits().put(xmethod, result);
        return result;
    }

    private static MapCache<XMethod, Set<Integer>> cachedLoopExits() {
        return cachedLoopExits_AL.get();
    }

    static short getBranchOffset(byte[] codeBytes, int pos) {
        int branchByte1 = 0xff & codeBytes[pos];
        int branchByte2 = 0xff & codeBytes[pos + 1];
        int branchOffset = (short) (branchByte1 << 8 | branchByte2);
        return (short) branchOffset;

    }

    static boolean checkForBranchExit(byte[] codeBytes, int pos) {
        if (pos < 0 || pos + 2 >= codeBytes.length) {
            return false;
        }
        switch (0xff & codeBytes[pos]) {
        case Const.IF_ACMPEQ:
        case Const.IF_ACMPNE:
        case Const.IF_ICMPEQ:
        case Const.IF_ICMPGE:
        case Const.IF_ICMPGT:
        case Const.IF_ICMPLE:
        case Const.IF_ICMPLT:
        case Const.IF_ICMPNE:
            break;
        default:
            return false;
        }
        int branchTarget = pos + getBranchOffset(codeBytes, pos + 1);
        if (branchTarget - 3 < pos || branchTarget >= codeBytes.length) {
            return false;
        }
        if ((codeBytes[branchTarget - 3] & 0xff) != Const.GOTO) {
            return false;
        }
        int backBranchTarget = branchTarget + getBranchOffset(codeBytes, branchTarget - 2);
        if (backBranchTarget <= pos && backBranchTarget + 12 >= pos) {
            return true;
        }
        return false;
    }

    /**
     * Get array mapping bytecode offsets to opcodes for given method. Array
     * elements containing zero are either not valid instruction offsets, or
     * contain a NOP instruction. (It is convenient not to distinguish these
     * cases.)
     *
     * @param method
     *            the method
     * @return map of bytecode offsets to opcodes, or null if the method has no
     *         code
     */
    @CheckForNull
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    public short[] getOffsetToOpcodeMap(Method method) {
        UnpackedCode unpackedCode = getMethodAnalysisNoException(UnpackedCode.class, method);
        return unpackedCode != null ? unpackedCode.getOffsetToBytecodeMap() : null;
    }

    /**
     * Get dataflow for LockAnalysis for given method.
     *
     * @param method
     *            the method
     * @return the LockDataflow
     */
    public LockDataflow getLockDataflow(Method method) throws CFGBuilderException, DataflowAnalysisException {
        return getMethodAnalysis(LockDataflow.class, method);
    }

    /**
     * Get LockChecker for method. This is like LockDataflow, but may be able to
     * avoid performing the actual dataflow analyses if the method doesn't
     * contain explicit monitorenter/monitorexit instructions.
     *
     * @param method
     *            the method
     * @return the LockChecker
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    public LockChecker getLockChecker(Method method) throws CFGBuilderException, DataflowAnalysisException {
        return getMethodAnalysis(LockChecker.class, method);
    }

    /**
     * Get ReturnPathDataflow for method.
     *
     * @param method
     *            the method
     * @return the ReturnPathDataflow
     */
    public ReturnPathDataflow getReturnPathDataflow(Method method) throws CFGBuilderException, DataflowAnalysisException {
        return getMethodAnalysis(ReturnPathDataflow.class, method);
    }

    /**
     * Get DominatorsAnalysis for given method, where exception edges are
     * ignored.
     *
     * @param method
     *            the method
     * @return the DominatorsAnalysis
     */
    public DominatorsAnalysis getNonExceptionDominatorsAnalysis(Method method) throws CFGBuilderException,
    DataflowAnalysisException {
        return getMethodAnalysis(DominatorsAnalysis.class, method);
    }

    /**
     * Get DominatorsAnalysis for given method, where implicit exception edges
     * are ignored.
     *
     * @param method
     *            the method
     * @return the DominatorsAnalysis
     */
    public PostDominatorsAnalysis getNonImplicitExceptionDominatorsAnalysis(Method method) throws CFGBuilderException,
    DataflowAnalysisException {
        return getMethodAnalysis(NonImplicitExceptionPostDominatorsAnalysis.class, method);
    }

    /**
     * Get PostDominatorsAnalysis for given method, where exception edges are
     * ignored.
     *
     * @param method
     *            the method
     * @return the PostDominatorsAnalysis
     */
    public PostDominatorsAnalysis getNonExceptionPostDominatorsAnalysis(Method method) throws CFGBuilderException,
    DataflowAnalysisException {
        return getMethodAnalysis(NonExceptionPostdominatorsAnalysis.class, method);
    }

    /**
     * Get ExceptionSetFactory for given method.
     *
     * @param method
     *            the method
     * @return the ExceptionSetFactory
     */
    public ExceptionSetFactory getExceptionSetFactory(Method method) {
        return getMethodAnalysisNoException(ExceptionSetFactory.class, method);
    }

    /**
     * Get array of type signatures of parameters for given method.
     *
     * @param method
     *            the method
     * @return an array of type signatures indicating the types of the method's
     *         parameters
     */
    public String[] getParameterSignatureList(Method method) {
        return getMethodAnalysisNoException(String[].class, method);
    }

    /**
     * Get the set of fields loaded by given method.
     *
     * @param method
     *            the method
     * @return the set of fields loaded by the method
     */
    public LoadedFieldSet getLoadedFieldSet(Method method) {
        return getMethodAnalysisNoException(LoadedFieldSet.class, method);
    }

    /**
     * Get LiveLocalStoreAnalysis dataflow for given method.
     *
     * @param method
     *            the method
     * @return the Dataflow object for LiveLocalStoreAnalysis on the method
     */
    public LiveLocalStoreDataflow getLiveLocalStoreDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
        return getMethodAnalysis(LiveLocalStoreDataflow.class, method);
    }

    /**
     * Get BlockType dataflow for given method.
     *
     * @param method
     *            the method
     * @return the Dataflow object for BlockTypeAnalysis on the method
     */
    public BlockTypeDataflow getBlockTypeDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
        return getMethodAnalysis(BlockTypeDataflow.class, method);
    }

    // /**
    // * Get the assigned field map for the class.
    // *
    // * @return the AssignedFieldMap
    // * @throws ClassNotFoundException if a class lookup prevents
    // * the class's superclasses from being searched for
    // * assignable fields
    // */
    // public AssignedFieldMap getAssignedFieldMap() throws
    // ClassNotFoundException {
    // return
    // getClassAnalysisPossibleClassNotFoundException(AssignedFieldMap.class);
    // }

    /**
     * Get AssertionMethods for class.
     *
     * @return the AssertionMethods
     */
    public AssertionMethods getAssertionMethods() {
        return getClassAnalysisNoException(AssertionMethods.class);
    }

    /**
     * Get ConstantDataflow for method.
     *
     * @param method
     *            the method
     * @return the ConstantDataflow
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    public ConstantDataflow getConstantDataflow(Method method) throws CFGBuilderException, DataflowAnalysisException {
        return getMethodAnalysis(ConstantDataflow.class, method);
    }

    /**
     * Get load dataflow.
     *
     * @param method
     *            the method
     * @return the LoadDataflow
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    public LoadDataflow getLoadDataflow(Method method) throws CFGBuilderException, DataflowAnalysisException {
        return getMethodAnalysis(LoadDataflow.class, method);
    }

    /**
     * Get store dataflow.
     *
     * @param method
     *            the method
     * @return the StoreDataflow
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    public StoreDataflow getStoreDataflow(Method method) throws CFGBuilderException, DataflowAnalysisException {
        return getMethodAnalysis(StoreDataflow.class, method);
    }

    /**
     * Get CallListDataflow for method.
     *
     * @param method
     *            the method
     * @return the CallListDataflow
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    public CallListDataflow getCallListDataflow(Method method) throws CFGBuilderException, DataflowAnalysisException {
        return getMethodAnalysis(CallListDataflow.class, method);
    }

    public BitSet linesMentionedMultipleTimes(Method method) {
        IAnalysisCache analysisCache = Global.getAnalysisCache();
        XMethod xMethod = XFactory.createXMethod(jclass, method);
        JumpInfo jumpInfo = null;
        try {
            jumpInfo = analysisCache.getMethodAnalysis(JumpInfo.class, xMethod.getMethodDescriptor());
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error getting jump information", e);
        }
        BitSet lineMentionedMultipleTimes = new BitSet();
        BitSet pcInFinallyBlock = new BitSet();

        Code code = method.getCode();
        if (code == null) {
            return lineMentionedMultipleTimes;
        }
        CodeException[] exceptionTable = code.getExceptionTable();
        if (exceptionTable == null || exceptionTable.length == 0) {
            return lineMentionedMultipleTimes;
        }
        int firstHandler = Integer.MAX_VALUE;
        for (CodeException e : exceptionTable) {
            if (e.getCatchType() == 0) {
                int pc = e.getHandlerPC();
                firstHandler = Math.min(firstHandler, pc);
                if (jumpInfo != null) {
                    int end = jumpInfo.getNextJump(pc + 1);
                    if (end >= pc) {
                        pcInFinallyBlock.set(pc, end);
                    }
                }
            }
        }
        BitSet foundOnce = new BitSet();
        BitSet afterHandler = new BitSet();
        LineNumberTable lineNumberTable = method.getLineNumberTable();
        int lineNum = -1;
        int prevStartPc = -1;
        if (lineNumberTable != null) {
            for (LineNumber line : lineNumberTable.getLineNumberTable()) {
                int newLine = line.getLineNumber();
                if (newLine == lineNum || newLine == -1) {
                    continue;
                }

                if (prevStartPc >= 0) {
                    int nextPcInFinallyBlock = pcInFinallyBlock.nextSetBit(prevStartPc);
                    if ( prevStartPc < nextPcInFinallyBlock && nextPcInFinallyBlock < line.getStartPC()) {
                        lineMentionedMultipleTimes.set(lineNum);
                    }
                }

                if (line.getStartPC() >= firstHandler) {
                    afterHandler.set(lineNum);
                    afterHandler.set(newLine);
                }
                lineNum = newLine;

                if (foundOnce.get(lineNum)) {
                    lineMentionedMultipleTimes.set(lineNum);
                } else {
                    foundOnce.set(lineNum);
                }
                prevStartPc = line.getStartPC();
            }
        }
        lineMentionedMultipleTimes.and(afterHandler);
        return lineMentionedMultipleTimes;
    }

    /**
     * Get the UnconditionalValueDerefDataflow for a method.
     *
     * @param method
     *            the method
     * @return the UnconditionalValueDerefDataflow
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    public UnconditionalValueDerefDataflow getUnconditionalValueDerefDataflow(Method method) throws CFGBuilderException,
    DataflowAnalysisException {
        return getMethodAnalysis(UnconditionalValueDerefDataflow.class, method);
    }

    /**
     * Get a CompactLocationNumbering for a method.
     *
     * @param method
     *            a method
     * @return the CompactLocationNumbering for the method
     * @throws CFGBuilderException
     */
    public CompactLocationNumbering getCompactLocationNumbering(Method method) throws CFGBuilderException {
        return getMethodAnalysisNoDataflowAnalysisException(CompactLocationNumbering.class, method);
    }


    /**
     * Get ReturnPathTypeDataflow for a method.
     *
     * @param method
     *            the method
     * @return the ReturnPathTypeDataflow for the method
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    public ReturnPathTypeDataflow getReturnPathTypeDataflow(Method method) throws CFGBuilderException, DataflowAnalysisException {
        return getMethodAnalysis(ReturnPathTypeDataflow.class, method);
    }

    public void dumpSimpleDataflowInformation(Method method) {
        try {
            dumpDataflowInformation(method, getCFG(method), getValueNumberDataflow(method), getIsNullValueDataflow(method), null,
                    null);
        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError(
                    "Could not dump data information for " + getJavaClass().getClassName() + "." + method.getName(), e);
        } catch (CFGBuilderException e) {
            AnalysisContext.logError(
                    "Could not dump data information for " + getJavaClass().getClassName() + "." + method.getName(), e);

        }
    }

    public void dumpDataflowInformation(Method method) {
        try {
            dumpDataflowInformation(method, getCFG(method), getValueNumberDataflow(method), getIsNullValueDataflow(method),
                    getUnconditionalValueDerefDataflow(method), getTypeDataflow(method));
        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError(
                    "Could not dump data information for " + getJavaClass().getClassName() + "." + method.getName(), e);
        } catch (CFGBuilderException e) {
            AnalysisContext.logError(
                    "Could not dump data information for " + getJavaClass().getClassName() + "." + method.getName(), e);

        }
    }

    public static void dumpDataflowInformation(Method method, CFG cfg, ValueNumberDataflow vnd, IsNullValueDataflow inv,
            @CheckForNull UnconditionalValueDerefDataflow dataflow, @CheckForNull TypeDataflow typeDataflow)
                    throws DataflowAnalysisException {
        System.out.println("\n\n{ UnconditionalValueDerefAnalysis analysis for " + method.getName());
        TreeSet<Location> tree = new TreeSet<Location>();

        for (Iterator<Location> locs = cfg.locationIterator(); locs.hasNext();) {
            Location loc = locs.next();
            tree.add(loc);
        }
        for (Location loc : tree) {
            System.out.println();
            if (dataflow != null) {
                System.out.println("\n Pre: " + dataflow.getFactAfterLocation(loc));
            }
            System.out.println("Vna: " + vnd.getFactAtLocation(loc));
            System.out.println("inv: " + inv.getFactAtLocation(loc));
            if (typeDataflow != null) {
                System.out.println("type: " + typeDataflow.getFactAtLocation(loc));
            }
            System.out.println("Location: " + loc);
            if (dataflow != null) {
                System.out.println("Post: " + dataflow.getFactAtLocation(loc));
            }
            System.out.println("Vna: " + vnd.getFactAfterLocation(loc));
            System.out.println("inv: " + inv.getFactAfterLocation(loc));
            if (typeDataflow != null) {
                System.out.println("type: " + typeDataflow.getFactAfterLocation(loc));
            }
        }
        System.out.println("}\n\n");
    }

    public static void dumpTypeDataflow(Method method, CFG cfg, TypeDataflow typeDataflow) throws DataflowAnalysisException {
        System.out.println("\n\n{ Type analysis for " + cfg.getMethodGen().getClassName() + "." + method.getName()
                + method.getSignature());
        TreeSet<Location> tree = new TreeSet<Location>();

        for (Iterator<Location> locs = cfg.locationIterator(); locs.hasNext();) {
            Location loc = locs.next();
            tree.add(loc);
        }
        for (Location loc : tree) {
            System.out.println("\n Pre: " + typeDataflow.getFactAtLocation(loc));
            System.out.println("Location: " + loc);
            System.out.println("Post: " + typeDataflow.getFactAfterLocation(loc));
        }
        System.out.println("}\n\n");
    }

    public static void dumpLiveLocalStoreDataflow(MethodDescriptor method, CFG cfg, LiveLocalStoreDataflow dataflow)
            throws DataflowAnalysisException {
        System.out.println("\n\n{ LiveLocalStore analysis for " + method);
        TreeSet<Location> tree = new TreeSet<Location>();

        for (Iterator<Location> locs = cfg.locationIterator(); locs.hasNext();) {
            Location loc = locs.next();
            tree.add(loc);
        }
        for (Location loc : tree) {
            System.out.println("\n Pre: " + dataflow.getFactAfterLocation(loc));
            System.out.println("Location: " + loc);
            System.out.println("Post: " + dataflow.getFactAtLocation(loc));
        }
        System.out.println("}\n\n");
    }

    /*
     * ----------------------------------------------------------------------
     * Helper methods for getting an analysis object from the analysis cache.
     * ----------------------------------------------------------------------
     */

    private <Analysis> Analysis getMethodAnalysisNoException(Class<Analysis> analysisClass, Method method) {
        try {
            return getMethodAnalysis(analysisClass, method);
        } catch (CheckedAnalysisException e) {
            IllegalStateException ise = new IllegalStateException("should not happen");
            ise.initCause(e);
            throw ise;
        }
    }

    private <Analysis> Analysis getMethodAnalysisNoDataflowAnalysisException(Class<Analysis> analysisClass, Method method)
            throws CFGBuilderException {
        try {
            return getMethodAnalysis(analysisClass, method);
        } catch (CFGBuilderException e) {
            throw e;
        } catch (CheckedAnalysisException e) {
            IllegalStateException ise = new IllegalStateException("should not happen");
            ise.initCause(e);
            throw ise;
        }

    }

    private <Analysis> Analysis getMethodAnalysis(Class<Analysis> analysisClass, Method method) throws DataflowAnalysisException,
    CFGBuilderException {
        try {
            MethodDescriptor methodDescriptor = BCELUtil.getMethodDescriptor(jclass, method);
            return Global.getAnalysisCache().getMethodAnalysis(analysisClass, methodDescriptor);
        } catch (DataflowAnalysisException e) {
            throw e;
        } catch (CFGBuilderException e) {
            throw e;
        } catch (CheckedAnalysisException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CFGBuilderException) {
                throw (CFGBuilderException) cause;
            }
            String message = "Should not happen: bad CAE: " + e.getClass().getName() + " for " + analysisClass.getName() + " of " + method;
            IllegalStateException ise = new IllegalStateException(message);
            ise.initCause(e);
            throw ise;
        }
    }

    private <Analysis> Analysis getClassAnalysis(Class<Analysis> analysisClass) throws CheckedAnalysisException {
        ClassDescriptor classDescriptor = BCELUtil.getClassDescriptor(jclass);
        return Global.getAnalysisCache().getClassAnalysis(analysisClass, classDescriptor);
    }

    private <Analysis> Analysis getClassAnalysisNoException(Class<Analysis> analysisClass) {
        try {
            return getClassAnalysis(analysisClass);
        } catch (CheckedAnalysisException e) {
            IllegalStateException ise = new IllegalStateException("should not happen");
            ise.initCause(e);
            throw ise;
        }
    }

    // private<Analysis> Analysis
    // getClassAnalysisPossibleClassNotFoundException(Class<Analysis>
    // analysisClass)
    // throws ClassNotFoundException {
    // try {
    // return Global.getAnalysisCache().getClassAnalysis(analysisClass,
    // BCELUtil.getClassDescriptor(jclass));
    // } catch (ResourceNotFoundException e) {
    // throw e.toClassNotFoundException();
    // } catch (CheckedAnalysisException e) {
    // throw new AnalysisException("Unexpected exception", e);
    // }
    // }
}

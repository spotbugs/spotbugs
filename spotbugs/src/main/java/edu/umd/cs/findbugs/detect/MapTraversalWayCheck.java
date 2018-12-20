/*
 * Contributions to SpotBugs
 * Copyright (C) 2018, Administrator
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

import java.util.Iterator;

import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.IFNE;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;

/**
 * @since ?
 *
 */
public class MapTraversalWayCheck implements Detector {

    private static final String CLASS_MAP = "java/util/Map";

    private static final String CLASS_ITERATOR = "java/util/Iterator";

    private static final String MAP_KEYSET = "keySet";

    private static final String MAP_ENTRYSET = "entrySet";

    private static final String MAP_HASNEXT = "hasNext";

    private static final int NUM_KEYSET = 0;

    private static final int NUM_ENTRYSET = 1;

    private static final int NUM_NOT_TRAVERSAL = -1;

    private final BugAccumulator bugAccumulator;

    /**
     * @param bugReporter
     */
    public MapTraversalWayCheck(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Method[] methodList = classContext.getJavaClass().getMethods();
        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            // Init method,skip
            String methodName = method.getName();
            if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (CFGBuilderException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (DataflowAnalysisException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        bugAccumulator.reportAccumulatedBugs();

    }

    private void analyzeMethod(ClassContext classContext, Method method)
            throws CFGBuilderException, DataflowAnalysisException {
        CFG cfg = classContext.getCFG(method);
        if (null == cfg) {
            return;
        }

        MethodGen methodGen = classContext.getMethodGen(method);
        String sourceFile = classContext.getJavaClass().getSourceFileName();
        ConstantPoolGen constPool = classContext.getConstantPoolGen();
        ValueNumberDataflow valueNumDataFlow = classContext.getValueNumberDataflow(method);

        Iterator<BasicBlock> bbIter = cfg.blockIterator();

        while (bbIter.hasNext()) {
            BasicBlock basicBlockTmp = bbIter.next();
            InstructionHandle insHandle = basicBlockTmp.getExceptionThrower();
            if (null == insHandle) {
                continue;
            }

            Instruction ins = insHandle.getInstruction();

            if (ins instanceof INVOKEINTERFACE) {
                int constIndex = ((INVOKEINTERFACE) ins).getIndex();
                int way = checkIsMapTravsal(constIndex, constPool);

                if (NUM_NOT_TRAVERSAL == way) {
                    continue;
                }
                // findMapTraversal(bbIter, constPool);
                // int startTravalPc = insHandle.getPosition();

                boolean res = checkValid(way, bbIter, constPool);
                if (res) {
                    continue;
                }

                Location location = new Location(insHandle, basicBlockTmp);
                ValueNumberFrame vnaFrame = valueNumDataFlow.getFactAtLocation(location);
                ValueNumberFrame vnaFrame1 = valueNumDataFlow.getStartFact(basicBlockTmp);
                ValueNumber valueNumber = vnaFrame1.getTopValue();

                BugAnnotation variableAnnotation = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location,
                        valueNumber, vnaFrame, "VALUE_OF");

                SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext,
                        methodGen, sourceFile, insHandle);

                bugAccumulator.accumulateBug(
                        new BugInstance(this, "DM_TRAVERSAL_MAP_INEFFICIENT_CHECK", NORMAL_PRIORITY)
                                .addClassAndMethod(methodGen, sourceFile).addOptionalAnnotation(variableAnnotation),
                        sourceLineAnnotation);
            }
        }
    }


    private int checkIsMapTravsal(int constIndex, ConstantPoolGen constPool) {

        ConstantCP constTmp = (ConstantCP) constPool.getConstant(constIndex);

        ConstantClass classInfo = (ConstantClass) constPool.getConstant(constTmp.getClassIndex());
        String className = ((ConstantUtf8) constPool.getConstant(classInfo.getNameIndex())).getBytes();

        ConstantNameAndType cnat = (ConstantNameAndType) constPool.getConstant(constTmp.getNameAndTypeIndex());
        String methodName = ((ConstantUtf8) constPool.getConstant(cnat.getNameIndex())).getBytes();

        if (!CLASS_MAP.equals(className)) {
            return NUM_NOT_TRAVERSAL;
        }

        if (MAP_KEYSET.equals(methodName)) {
            return NUM_KEYSET;
        } else if (MAP_ENTRYSET.equals(methodName)) {
            return NUM_ENTRYSET;
        } else {
            return NUM_NOT_TRAVERSAL;
        }
    }

    private boolean checkValid(int way, Iterator<BasicBlock> bbIter, ConstantPoolGen constPool) {
        boolean valid = true;
        int keyCount = 0;
        int valueCount = 0;

        while (bbIter.hasNext()) {
            BasicBlock basicBlock = bbIter.next();
            InstructionHandle insHandle = basicBlock.getExceptionThrower();
            if (null == insHandle) {
                continue;
            }
            Instruction ins = insHandle.getInstruction();

            if (ins instanceof INVOKEINTERFACE) {
                int constIndex = ((INVOKEINTERFACE) ins).getIndex();
                ConstantCP constTmp = (ConstantCP) constPool.getConstant(constIndex);

                ConstantClass classInfo = (ConstantClass) constPool.getConstant(constTmp.getClassIndex());
                String className = ((ConstantUtf8) constPool.getConstant(classInfo.getNameIndex())).getBytes();

                ConstantNameAndType cnat = (ConstantNameAndType) constPool.getConstant(constTmp.getNameAndTypeIndex());
                String methodName = ((ConstantUtf8) constPool.getConstant(cnat.getNameIndex())).getBytes();

                if (!CLASS_MAP.equals(className) && !CLASS_ITERATOR.equals(className)) {
                    continue;
                }

                if (CLASS_ITERATOR.equals(className) && MAP_HASNEXT.equals(methodName)) {
                    BasicBlock bb = bbIter.next();
                    InstructionHandle nextInsHandle = bb.getLastInstruction();
                    if (null == nextInsHandle) {
                        break;
                    }
                    Instruction nextIns = nextInsHandle.getInstruction();
                    if (nextIns instanceof IFNE) {
                        InstructionHandle targetHandle = ((IFNE) nextIns).getTarget();
                        BasicBlock tt = bbIter.next();
                        if (targetHandle.equals(tt.getLastInstruction())) {
                            continue;
                        }
                    }

                    break;
                }

                if (NUM_KEYSET == way) {
                    if ("get".equals(methodName)) {
                        return false;
                    }

                } else if (NUM_ENTRYSET == way) {
                    if ("getKey".equals(methodName)) {
                        keyCount++;
                    } else if ("getValue".equals(methodName)) {
                        valueCount++;
                    }
                }
            }
        }

        if (NUM_ENTRYSET == way) {
            if (0 == keyCount || 0 == valueCount) {
                valid = false;
            }
        }

        return valid;
    }

    @Override
    public void report() {
        // TODO Auto-generated method stub

    }

}

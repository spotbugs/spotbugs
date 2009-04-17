/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import java.util.BitSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.POP;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LiveLocalStoreAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

public class DontIgnoreResultOfPutIfAbsent implements Detector {

	final static boolean countOtherCalls = false;
	final BugReporter bugReporter;
	final BugAccumulator accumulator;
	final ClassDescriptor concurrentMapDescriptor = DescriptorFactory.createClassDescriptor(ConcurrentMap.class);
	public DontIgnoreResultOfPutIfAbsent(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.accumulator = new BugAccumulator(bugReporter);
	}

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.Detector#report()
     */
    public void report() {
	    // TODO Auto-generated method stub
	    
    }

    public void visitClassContext(ClassContext classContext) {
    	
    	
		JavaClass javaClass = classContext.getJavaClass();
		ConstantPool pool = javaClass.getConstantPool();
		boolean found = false;
		for(Constant constantEntry : pool.getConstantPool())
			if (constantEntry instanceof ConstantNameAndType) {
				ConstantNameAndType nt = (ConstantNameAndType) constantEntry;
				if (nt.getName(pool).equals("putIfAbsent")) {
					found = true;
					break;
				}
			}
		if (!found) return;

		Method[] methodList = javaClass.getMethods();
			

		for (Method method : methodList) {
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;


			try {
				analyzeMethod(classContext, method);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Error analyzing " + method.toString(), e);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Error analyzing " + method.toString(), e);
			}
		}
	}

    final static boolean DEBUG = false;

    private void analyzeMethod(ClassContext classContext, Method method) throws DataflowAnalysisException, CFGBuilderException {
    		if (method.isSynthetic() || (method.getAccessFlags() & Constants.ACC_BRIDGE) == Constants.ACC_BRIDGE) return;
    		
    		if (DEBUG) {
    			System.out.println("    Analyzing method " + classContext.getJavaClass().getClassName() + "." + method.getName());
    		}

    		JavaClass javaClass = classContext.getJavaClass();
    		ConstantPoolGen cpg = classContext.getConstantPoolGen();
    		Dataflow<BitSet, LiveLocalStoreAnalysis> llsaDataflow = classContext.getLiveLocalStoreDataflow(method);

    		MethodGen methodGen = classContext.getMethodGen(method);
    		CFG cfg = classContext.getCFG(method);
    		ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
    		String sourceFileName = javaClass.getSourceFileName();
			
    		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
    			Location location = i.next();
    			
    			InstructionHandle handle = location.getHandle();
				Instruction ins = handle.getInstruction();
    			
    			if (ins instanceof InvokeInstruction) {
    				InvokeInstruction invoke = (InvokeInstruction)ins;
    				String className = invoke.getClassName(cpg);
					if (invoke.getMethodName(cpg).equals("putIfAbsent") && extendsConcurrentMap(className)) {
    					InstructionHandle next = handle.getNext();
    					boolean isIgnored = next != null && next.getInstruction() instanceof POP;
						if (countOtherCalls || isIgnored) {
    						BitSet live = llsaDataflow.getAnalysis().getFactAtLocation(location);
    						ValueNumberFrame vna = vnaDataflow.getAnalysis().getFactAtLocation(location);
    						ValueNumber vn = vna.getTopValue();
    						int locals = vna.getNumLocals();
    						boolean isRetained = false;
    						for(int pos = 0; pos < locals; pos++) 
    							if (vna.getValue(pos).equals(vn) && live.get(pos)) {
    								BugAnnotation ba = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location, vn, 
    										vnaDataflow.getFactAtLocation(location), "VALUE_OF");
    								
    								String pattern = "RV_RETURN_VALUE_OF_PUTIFABSENT_IGNORED";
    								if (!isIgnored)
    									pattern = "UNKNOWN";
    								
									BugInstance bugInstance = new BugInstance(this,  pattern, 
    										Priorities.NORMAL_PRIORITY)
    											.addClassAndMethod(methodGen,sourceFileName)
    											.addCalledMethod(methodGen, invoke).addOptionalAnnotation(ba);
    								SourceLineAnnotation where = SourceLineAnnotation.fromVisitedInstruction(
    										classContext, method, location);
    								accumulator.accumulateBug(bugInstance, where);
    								isRetained = true;
    								break;
    						}
    						if (countOtherCalls && !isRetained) {
    							BugInstance bugInstance = new BugInstance(this,  "UNKNOWN", 
										isIgnored ? Priorities.LOW_PRIORITY : Priorities.HIGH_PRIORITY)
											.addClassAndMethod(methodGen,sourceFileName)
											.addCalledMethod(methodGen, invoke);
								SourceLineAnnotation where = SourceLineAnnotation.fromVisitedInstruction(
										classContext, method, location);
								accumulator.accumulateBug(bugInstance, where);
    						}
    						
    					}
    				}
    				
    			}
    		}
    		accumulator.reportAccumulatedBugs();
    }

    
    private  boolean extendsConcurrentMap(@DottedClassName String className) {
    	if (className.equals("java.util.concurrent.ConcurrentHashMap") 
    			|| className.equals(concurrentMapDescriptor.getDottedClassName()))
    		return true;
    	ClassDescriptor c = DescriptorFactory.createClassDescriptorFromDottedClassName(className);
    	Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();

    	try {
    		if (subtypes2.isSubtype(c, concurrentMapDescriptor))  
    			return true;
    	} catch (ClassNotFoundException e) {
    		AnalysisContext.reportMissingClass(e);
    	}

    	return false;

    }


}

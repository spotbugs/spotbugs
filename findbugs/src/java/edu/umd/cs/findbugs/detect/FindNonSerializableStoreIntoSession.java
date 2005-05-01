package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.NullType;
import edu.umd.cs.findbugs.ba.TopType;
import edu.umd.cs.findbugs.ba.TypeDataflow;
import edu.umd.cs.findbugs.ba.TypeFrame;
import edu.umd.cs.findbugs.ba.ValueNumber;
import edu.umd.cs.findbugs.ba.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.ValueNumberFrame;

public class FindNonSerializableStoreIntoSession implements Detector {
	
	private BugReporter bugReporter;
	private static final boolean DEBUG = false;
	private JavaClass serializable;
	private JavaClass collection;
	private JavaClass map;

	
	public FindNonSerializableStoreIntoSession(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		try {
		serializable = Repository.lookupClass("java.io.Serializable");
		collection = Repository.lookupClass("java.util.Collection");
		map = Repository.lookupClass("java.util.Map");
		} catch (ClassNotFoundException e) {
			// can't do anything
			}
	}

	public void visitClassContext(ClassContext classContext) {
		if (serializable == null) return;
		Method[] methodList = classContext.getJavaClass().getMethods();
		
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			
			if (method.getCode() == null)
				continue;
			
			try {
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
			}
		}
	}

        public boolean prescreen(ClassContext classContext, Method method) {
                BitSet bytecodeSet = classContext.getBytecodeSet(method);
                return bytecodeSet.get(Constants.CHECKCAST) || bytecodeSet.get(Constants.INSTANCEOF);
        }

	private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
		CFG cfg = classContext.getCFG(method);
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
		ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
		 // get the ValueNumberFrame at entry to a method:
		 ValueNumberFrame vnaFrameAtEntry 
			= vnaDataflow.getStartFact(cfg.getEntry());

		// get Set of parameter values
		 Set<ValueNumber> paramValueNumberSet = new HashSet<ValueNumber>();
		 int firstParam = method.isStatic() ? 0 : 1;
		 for (int i = firstParam; i < vnaFrameAtEntry.getNumLocals(); ++i) {
		   paramValueNumberSet.add(vnaFrameAtEntry.getValue(i));
		 }

		 
			

		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);
                String sourceFile = classContext.getJavaClass().getSourceFileName();
		 if (DEBUG) {
			String methodName = methodGen.getClassName() + "." + methodGen.getName();
			System.out.println("Checking " + methodName);
		}
	
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			InstructionHandle handle = location.getHandle();
			int pc = handle.getPosition();
			Instruction ins = handle.getInstruction();

			if (!(ins instanceof INVOKEINTERFACE))
				continue;

			INVOKEINTERFACE invoke = (INVOKEINTERFACE) ins;
			String mName = invoke.getMethodName(cpg);
			if (!mName.equals("setAttribute")) continue;
			String cName = invoke.getClassName(cpg);
			if (!cName.equals("javax.servlet.http.HttpSession")) continue;

		      TypeFrame frame = typeDataflow.getFactAtLocation(location);
                        if (!frame.isValid()) {
                                // This basic block is probably dead
                                continue;
                        }
			Type operandType = frame.getTopValue();

			if (operandType.equals(TopType.instance()))  {
				// unreachable
				continue;
			}
			if (!(operandType instanceof ReferenceType)) {
				// Shouldn't happen - illegal bytecode
				continue;
			}
			ReferenceType refType = (ReferenceType) operandType;

			if (refType.equals(NullType.instance())) {
				continue;
				}
			String refSig = refType.getSignature();
			
			SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(methodGen, sourceFile, handle);

			ValueNumberFrame vFrame = vnaDataflow.getFactAtLocation(location);
			boolean isParameter = paramValueNumberSet.contains(vFrame.getTopValue());
			try {

			double isSerializable
				= Analyze.isDeepSerializable(refSig);

			if (isSerializable < 0.9) 
			     bugReporter.reportBug(new BugInstance(this,
                                "J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION",
				isSerializable < 0.15 ? HIGH_PRIORITY :
				isSerializable > 0.5 ? LOW_PRIORITY :
				NORMAL_PRIORITY)
                                  .addClassAndMethod(methodGen, sourceFile)
                                  .addSourceLine(sourceLineAnnotation)
				  .addClass(Analyze.getComponentClass(refSig))
				  );
      
			} catch (ClassNotFoundException e) {
				// ignore
				}
			}
		}

	public void report() {
	}

}

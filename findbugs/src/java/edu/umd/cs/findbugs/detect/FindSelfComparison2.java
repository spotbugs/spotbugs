package edu.umd.cs.findbugs.detect;

import static org.apache.bcel.Constants.DCMPG;
import static org.apache.bcel.Constants.DCMPL;
import static org.apache.bcel.Constants.FCMPG;
import static org.apache.bcel.Constants.FCMPL;
import static org.apache.bcel.Constants.IAND;
import static org.apache.bcel.Constants.IF_ACMPEQ;
import static org.apache.bcel.Constants.IF_ACMPNE;
import static org.apache.bcel.Constants.IF_ICMPEQ;
import static org.apache.bcel.Constants.IF_ICMPGE;
import static org.apache.bcel.Constants.IF_ICMPGT;
import static org.apache.bcel.Constants.IF_ICMPLE;
import static org.apache.bcel.Constants.IF_ICMPLT;
import static org.apache.bcel.Constants.IF_ICMPNE;
import static org.apache.bcel.Constants.INVOKEINTERFACE;
import static org.apache.bcel.Constants.INVOKEVIRTUAL;
import static org.apache.bcel.Constants.IOR;
import static org.apache.bcel.Constants.ISUB;
import static org.apache.bcel.Constants.IXOR;
import static org.apache.bcel.Constants.LCMP;

import java.util.Iterator;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

public class FindSelfComparison2 implements Detector {

	private BugReporter bugReporter;

	public FindSelfComparison2(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		Method[] methodList = classContext.getJavaClass().getMethods();

		for (Method method : methodList) {
			if (method.getCode() == null)
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (MethodUnprofitableException mue) {
				if (SystemProperties.getBoolean("unprofitable.debug")) // otherwise don't report
					bugReporter.logError("skipping unprofitable method in " + getClass().getName());
			} catch (CFGBuilderException e) {
				bugReporter.logError("Detector " + this.getClass().getName()
						+ " caught exception", e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Detector " + this.getClass().getName()
						+ " caught exception", e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		CFG cfg = classContext.getCFG(method);
		ValueNumberDataflow valueNumberDataflow = classContext
				.getValueNumberDataflow(method);
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);
		String sourceFile = classContext.getJavaClass().getSourceFileName();

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			Instruction ins = location.getHandle().getInstruction();
            switch(ins.getOpcode()) {
            case INVOKEVIRTUAL:
            case INVOKEINTERFACE:
                InvokeInstruction iins = (InvokeInstruction) ins;
                String invoking = iins.getName(cpg);
                if (invoking.equals("equals") || invoking.equals("compareTo")) {
                if (methodGen.getName().toLowerCase().indexOf("test") >= 0) break;
                if (methodGen.getClassName().toLowerCase().indexOf("test") >= 0) break;
                if (classContext.getJavaClass().getSuperclassName().toLowerCase().indexOf("test") >= 0) break;
                
               String sig = iins.getSignature(cpg);
               
               SignatureParser parser = new SignatureParser(sig);
               if (parser.getNumParameters() == 1 && 
                       (invoking.equals("equals") && sig.endsWith(";)Z")
                       || invoking.equals("compareTo")  && sig.endsWith(";)I")))
                   checkForSelfOperation(classContext, location, valueNumberDataflow, "COMPARISON", methodGen, sourceFile);

       
                }
                break;
                
            case IOR:
            case IAND:
            case IXOR:
            case ISUB:
                checkForSelfOperation(classContext, location, valueNumberDataflow, "COMPUTATION", methodGen, sourceFile);
                break;
            case FCMPG:
            case DCMPG:
            case DCMPL:
            case FCMPL:
                break;
            case LCMP:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case IF_ICMPNE:
            case IF_ICMPEQ:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ICMPLT:
            case IF_ICMPGE: 
                checkForSelfOperation(classContext, location, valueNumberDataflow, "COMPARISON", methodGen, sourceFile);

            }


		}
	}

	/**
     * @param classContext TODO
	 * @param location
	 * @param methodGen TODO
	 * @param sourceFile TODO
	 * @param string
	 * @throws DataflowAnalysisException 
     */
    private void checkForSelfOperation(ClassContext classContext, Location location, ValueNumberDataflow valueNumberDataflow, String op, MethodGen methodGen, String sourceFile) throws DataflowAnalysisException {
        ValueNumberFrame frame = valueNumberDataflow.getFactAtLocation(location);
        if (!frame.isValid())  return;
        ValueNumber v0 = frame.getStackValue(0);
        ValueNumber v1 = frame.getStackValue(1);
        if (!v1.equals(v0)) return;

        Instruction ins = location.getHandle().getInstruction();
        int priority = HIGH_PRIORITY;
        int opcode = ins.getOpcode();
        if (opcode == ISUB || opcode == INVOKEINTERFACE || opcode == INVOKEVIRTUAL)
            priority = NORMAL_PRIORITY;
        BugAnnotation annotation = FindNullDeref.findAnnotationFromValueNumber(methodGen.getMethod(), location, v0, frame);
        String prefix = "SA_LOCAL_SELF_" ;
        if (annotation instanceof FieldAnnotation)
            prefix = "SA_FIELD_SELF_";
        BugInstance bug = new BugInstance(this, "SA_LOCAL_SELF_" + op, priority).addClassAndMethod(methodGen, sourceFile)
        .add(annotation).addSourceLine(classContext, methodGen, sourceFile, location.getHandle());
        
        

        
    }

    public void report() {
	}

}

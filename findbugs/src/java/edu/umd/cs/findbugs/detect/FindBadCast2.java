package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INSTANCEOF;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;

import edu.umd.cs.findbugs.DeepSubtypeAnalysis;
import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.type.NullType;
import edu.umd.cs.findbugs.ba.type.TopType;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

public class FindBadCast2 implements Detector {

	private BugReporter bugReporter;

	private Set<String> concreteCollectionClasses = new HashSet<String>();

	private Set<String> abstractCollectionClasses = new HashSet<String>();
	private Set<String> veryAbstractCollectionClasses = new HashSet<String>();

	private static final boolean DEBUG = SystemProperties.getBoolean("bc.debug");

	public FindBadCast2(BugReporter bugReporter) {		
		this.bugReporter = bugReporter;
		veryAbstractCollectionClasses.add("java.util.Collection");
		veryAbstractCollectionClasses.add("java.util.Iterable");
		abstractCollectionClasses.add("java.util.Collection");
		abstractCollectionClasses.add("java.util.List");
		abstractCollectionClasses.add("java.util.Set");
		abstractCollectionClasses.add("java.util.SortedSet");
		abstractCollectionClasses.add("java.util.SortedMap");
		abstractCollectionClasses.add("java.util.Map");
		concreteCollectionClasses.add("java.util.LinkedHashMap");
		concreteCollectionClasses.add("java.util.LinkedHashSet");
		concreteCollectionClasses.add("java.util.HashMap");
		concreteCollectionClasses.add("java.util.HashSet");
		concreteCollectionClasses.add("java.util.TreeMap");
		concreteCollectionClasses.add("java.util.TreeSet");
		concreteCollectionClasses.add("java.util.ArrayList");
		concreteCollectionClasses.add("java.util.LinkedList");
		concreteCollectionClasses.add("java.util.Hashtable");
		concreteCollectionClasses.add("java.util.Vector");
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();

		for (Method method : methodList) {
			if (method.getCode() == null)
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (MethodUnprofitableException e) {
				assert true; // move along; nothing to see
			} catch (CFGBuilderException e) {
				String msg = "Detector " + this.getClass().getName()
										+ " caught exception while analyzing " + javaClass.getClassName() + "." + method.getName() + " : " + method.getSignature();
				bugReporter.logError(msg , e);
			} catch (DataflowAnalysisException e) {
				String msg = "Detector " + this.getClass().getName()
										+ " caught exception while analyzing " + javaClass.getClassName() + "." + method.getName() + " : " + method.getSignature();
				bugReporter.logError(msg, e);
			}
		}
	}

	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet != null && (bytecodeSet.get(Constants.CHECKCAST)
				|| bytecodeSet.get(Constants.INSTANCEOF));
	}

	private boolean isSynthetic(Method m) {
		Attribute[] attrs = m.getAttributes();
		for (Attribute attr : attrs) {
			if (attr instanceof Synthetic)
				return true;
		}
		return false;
	}
	private Set<ValueNumber> getParameterValueNumbers(ClassContext classContext, Method method,  CFG cfg ) throws DataflowAnalysisException, CFGBuilderException {
		ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
		ValueNumberFrame vnaFrameAtEntry = vnaDataflow.getStartFact(cfg
				.getEntry());
		Set<ValueNumber> paramValueNumberSet = new HashSet<ValueNumber>();
		int firstParam = method.isStatic() ? 0 : 1;
		for (int i = firstParam; i < vnaFrameAtEntry.getNumLocals(); ++i) {
			paramValueNumberSet.add(vnaFrameAtEntry.getValue(i));
		}
		return paramValueNumberSet;
	}
	private void analyzeMethod(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		if (isSynthetic(method) || !prescreen(classContext, method))
			return;
		BugAccumulator accumulator = new BugAccumulator(bugReporter);
		
		
		CFG cfg = classContext.getCFG(method);
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
		IsNullValueDataflow isNullDataflow = classContext.getIsNullValueDataflow(method);
		Set<ValueNumber> paramValueNumberSet = null;
		
		ValueNumberDataflow vnaDataflow = null;

		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return;
		String methodName = methodGen.getClassName() + "."
				+ methodGen.getName();
		String sourceFile = classContext.getJavaClass().getSourceFileName();
		if (DEBUG) {
			System.out.println("Checking " + methodName);
		}

		Set<SourceLineAnnotation> haveInstanceOf = new HashSet<SourceLineAnnotation>();
		Set<SourceLineAnnotation> haveCast = new HashSet<SourceLineAnnotation>();
		Set<SourceLineAnnotation> haveMultipleInstanceOf = new HashSet<SourceLineAnnotation>();
		Set<SourceLineAnnotation> haveMultipleCast = new HashSet<SourceLineAnnotation>();
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			InstructionHandle handle = location.getHandle();
			Instruction ins = handle.getInstruction();

			if (!(ins instanceof CHECKCAST) && !(ins instanceof INSTANCEOF))
				continue;

			SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation
					.fromVisitedInstruction(classContext, methodGen, sourceFile, handle);
			if (ins instanceof CHECKCAST) {
				if (!haveCast.add(sourceLineAnnotation))
					haveMultipleCast.add(sourceLineAnnotation);
			} else {
				if (!haveInstanceOf.add(sourceLineAnnotation))
					haveMultipleInstanceOf.add(sourceLineAnnotation);
			}
		}
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
	
			InstructionHandle handle = location.getHandle();
			int pc = handle.getPosition();
			Instruction ins = handle.getInstruction();
	
			if (!(ins instanceof CHECKCAST) && !(ins instanceof INSTANCEOF))
				continue;
			if (handle.getNext() == null) continue;
			Instruction nextIns = handle.getNext().getInstruction();

			boolean isCast = ins instanceof CHECKCAST;
			String kind = isCast ? "checkedCast" : "instanceof";
			int occurrences = cfg.getLocationsContainingInstructionWithOffset(
					pc).size();
			boolean split = occurrences > 1;
			IsNullValueFrame nullFrame = isNullDataflow.getFactAtLocation(location);
			IsNullValue operandNullness = nullFrame.getTopValue();
			if (DEBUG) {
				System.out
						.println(kind + " at pc: " + pc + " in " + methodName);
				System.out.println(" occurrences: " + occurrences);
				System.out.println("XXX: " + operandNullness);
				
			}

			if (split && !isCast) {
				// don't report this case; it might be infeasible due to inlining
				continue;
			}

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
			boolean operandTypeIsExact = frame.isExact(frame.getStackLocation(0));
			Type castType = ((TypedInstruction) ins).getType(cpg);

			if (!(castType instanceof ReferenceType)) {
				// This shouldn't happen either
				continue;
			}
			String castSig = castType.getSignature();
			
			if (operandType.equals(NullType.instance()) || operandNullness.isDefinitelyNull()) {
				SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation
				.fromVisitedInstruction(classContext, methodGen, sourceFile, handle);
				String castName = castSig.substring(1, castSig.length() - 1)
				.replace('/', '.');
				if (!isCast) accumulator.accumulateBug(new BugInstance(this,
						"NP_NULL_INSTANCEOF", NORMAL_PRIORITY)
						.addClassAndMethod(methodGen, sourceFile)
						.addClass(castName), sourceLineAnnotation);
				continue;

			}
			if (!(operandType instanceof ReferenceType)) {
				// Shouldn't happen - illegal bytecode
				continue;
			}
			ReferenceType refType = (ReferenceType) operandType;

		
			if (refType.equals(castType)) {
				// System.out.println("self-cast to " + castType.getSignature());
				continue;
			}
			
			String refSig = refType.getSignature();
			String castSig2 = castSig;
			String refSig2 = refSig;
			while (castSig2.charAt(0) == '[' && refSig2.charAt(0) == '[') {
				castSig2 = castSig2.substring(1);
				refSig2 = refSig2.substring(1);
			}

			
			SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation
			.fromVisitedInstruction(classContext, methodGen, sourceFile, handle);

			if (refSig2.charAt(0) != 'L' || castSig2.charAt(0) != 'L') {
				if ( castSig2.charAt(0) == '[' && (refSig2.equals("Ljava/io/Serializable;") 
						|| refSig2.equals("Ljava/lang/Object;")
						|| refSig2.equals("Ljava/lang/Cloneable;"))) continue;
				if ( refSig2.charAt(0) == '[' && (castSig2.equals("Ljava/io/Serializable;") 
						|| castSig2.equals("Ljava/lang/Object;")
						|| castSig2.equals("Ljava/lang/Cloneable;"))) continue;
				bugReporter.reportBug(
						new BugInstance(this,
						"BC_IMPOSSIBLE_CAST", HIGH_PRIORITY )
						.addClassAndMethod(methodGen, sourceFile)
						.addType(refSig)
						.addType(castSig)
						.addSourceLine(sourceLineAnnotation));
				continue;
			}

			if (refSig2.equals("Ljava/lang/Object;")  &!operandTypeIsExact) {
				continue;
			}
			if (false && isCast && haveMultipleCast.contains(sourceLineAnnotation)
					|| !isCast
					&& haveMultipleInstanceOf.contains(sourceLineAnnotation)) {
				// skip; might be due to JSR inlining
				continue;
			}
			String castName = castSig2.substring(1, castSig2.length() - 1)
					.replace('/', '.');
			String refName = refSig2.substring(1, refSig2.length() - 1)
					.replace('/', '.');

			if (vnaDataflow == null)
				vnaDataflow = classContext
				.getValueNumberDataflow(method);
			ValueNumberFrame vFrame = vnaDataflow.getFactAtLocation(location);
			if (paramValueNumberSet == null) 
				paramValueNumberSet = getParameterValueNumbers(classContext, method, cfg);
			boolean isParameter = paramValueNumberSet.contains(vFrame
					.getTopValue());
			try {
				JavaClass castJavaClass = Repository.lookupClass(castName);
				JavaClass refJavaClass = Repository.lookupClass(refName);
				boolean upcast = Repository.instanceOf(refJavaClass,
						castJavaClass);
				if (upcast) {
					if (!isCast)
						accumulator.accumulateBug(new BugInstance(this,
								"BC_VACUOUS_INSTANCEOF", NORMAL_PRIORITY)
								.addClassAndMethod(methodGen, sourceFile)
							
								.addType(refSig)
								.addType(castSig)
								,sourceLineAnnotation);
				} else {
					boolean downcast = Repository.instanceOf(castJavaClass,
							refJavaClass);
					
					if (refName.equals("java.lang.Object")  &!operandTypeIsExact) continue;
					double rank = 0.0;
					boolean castToConcreteCollection = concreteCollectionClasses.contains(castName)
							&& abstractCollectionClasses.contains(refName);
					boolean castToAbstractCollection = 
							abstractCollectionClasses.contains(castName)
							&& veryAbstractCollectionClasses.contains(refName);
	
					if (!operandTypeIsExact) {
						rank = DeepSubtypeAnalysis.deepInstanceOf(refJavaClass,
								castJavaClass);
							if (castToConcreteCollection
							&& rank > 0.6)
						  rank = (rank + 0.6) /2;
						else if (castToAbstractCollection
							&& rank > 0.3)
						  rank = (rank + 0.3) /2;
					}
					
						
					if (false)
						System.out.println("Rank:\t" + rank + "\t" + refName
								+ "\t" + castName);
					boolean completeInformation =  (!castJavaClass.isInterface() && !refJavaClass
							.isInterface())
							|| refJavaClass.isFinal()
							|| castJavaClass.isFinal();
					if (DEBUG) {
						System.out.println("cast from " + refName + " to "
								+ castName);
						System.out.println("  is downcast: " + downcast);
						System.out.println("  operand type is exact: " + operandTypeIsExact);
						
						System.out.println("  complete information: "
								+ completeInformation);
						System.out.println("  isParameter: "
								+ vFrame.getTopValue());
						System.out.println("  score: " + rank);
					}
					if (!downcast && completeInformation || operandTypeIsExact)
						bugReporter.reportBug(new BugInstance(this,
								isCast ? "BC_IMPOSSIBLE_CAST"
										: "BC_IMPOSSIBLE_INSTANCEOF",
								isCast ? HIGH_PRIORITY : NORMAL_PRIORITY)
								.addClassAndMethod(methodGen, sourceFile)
								
								.addType(refSig)
								.addType(castSig)
								.addSourceLine(sourceLineAnnotation));
					else if (isCast && rank < 0.9) {

						int priority = NORMAL_PRIORITY;

						if (rank > 0.75)
							priority += 2;
						else if (rank > 0.5)
							priority += 1;
						else if (rank > 0.25)
							priority += 0;
						else
							priority--;

						if (DEBUG)
							System.out.println(" priority a: " + priority);
						if (methodGen.getClassName().startsWith(refName)
								|| methodGen.getClassName().startsWith(castName))
							priority += 1;
						if (DEBUG)
							System.out.println(" priority b: " + priority);
						if (castJavaClass.isInterface() && !castToAbstractCollection)
							priority++;
						if (DEBUG)
							System.out.println(" priority c: " + priority);
						if (castToConcreteCollection
							&& veryAbstractCollectionClasses.contains(refName))
							priority--;
						if (DEBUG)
							System.out.println(" priority d: " + priority);
						if (priority <= LOW_PRIORITY 
								&& !castToAbstractCollection
								&& !castToConcreteCollection
								&& (refJavaClass.isInterface() || refJavaClass
										.isAbstract()))
							priority++;
						if (DEBUG)
							System.out.println(" priority e: " + priority);
						if (DEBUG)
							System.out.println(" ref name: " + refName);
						if (methodGen.getName().equals("compareTo"))
							priority++;
						else if (methodGen.isPublic() && isParameter)
							priority--;
						if (DEBUG)
							System.out.println(" priority h: " + priority);
						if (priority < HIGH_PRIORITY)
							priority = HIGH_PRIORITY;
						if (priority <= LOW_PRIORITY) {
							String bug = "BC_UNCONFIRMED_CAST";
							if (castToConcreteCollection)
								bug = "BC_BAD_CAST_TO_CONCRETE_COLLECTION";
							else if (castToAbstractCollection)
								bug = "BC_BAD_CAST_TO_ABSTRACT_COLLECTION";

							accumulator.accumulateBug(new BugInstance(this, bug, priority)
									.addClassAndMethod(methodGen, sourceFile)
									.addType(refSig)
									.addType(castSig),
									sourceLineAnnotation
									);
						}

					}

				}
			} catch (ClassNotFoundException e) {
			}
		}
		accumulator.reportAccumulatedBugs();
	}

	public void report() {
	}

}

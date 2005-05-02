package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INSTANCEOF;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;

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

public class FindBadCast2 implements Detector {

	private BugReporter bugReporter;

	private Set<String> concreteCollectionClasses = new HashSet<String>();

	private Set<String> abstractCollectionClasses = new HashSet<String>();

	private static final boolean DEBUG = false;

	public FindBadCast2(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		abstractCollectionClasses.add("java.util.Collection");
		abstractCollectionClasses.add("java.util.List");
		abstractCollectionClasses.add("java.util.Set");
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
		Method[] methodList = classContext.getJavaClass().getMethods();

		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];

			if (method.getCode() == null)
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Detector " + this.getClass().getName()
						+ " caught exception", e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Detector " + this.getClass().getName()
						+ " caught exception", e);
			}
		}
	}

	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet.get(Constants.CHECKCAST)
				|| bytecodeSet.get(Constants.INSTANCEOF);
	}

	private void analyzeMethod(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		CFG cfg = classContext.getCFG(method);
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
		ValueNumberDataflow vnaDataflow = classContext
				.getValueNumberDataflow(method);
		// get the ValueNumberFrame at entry to a method:
		ValueNumberFrame vnaFrameAtEntry = vnaDataflow.getStartFact(cfg
				.getEntry());

		// get Set of parameter values
		Set<ValueNumber> paramValueNumberSet = new HashSet<ValueNumber>();
		int firstParam = method.isStatic() ? 0 : 1;
		for (int i = firstParam; i < vnaFrameAtEntry.getNumLocals(); ++i) {
			paramValueNumberSet.add(vnaFrameAtEntry.getValue(i));
		}

		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);
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
					.fromVisitedInstruction(methodGen, sourceFile, handle);
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

			boolean isCast = ins instanceof CHECKCAST;
			String kind = isCast ? "checkedCast" : "instanceof";
			int occurrences = cfg.getLocationsContainingInstructionWithOffset(
					pc).size();
			boolean split = occurrences > 1;

			if (DEBUG) {
				System.out
						.println(kind + " at pc: " + pc + " in " + methodName);
				System.out.println(" occurrences: " + occurrences);
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
			if (!(operandType instanceof ReferenceType)) {
				// Shouldn't happen - illegal bytecode
				continue;
			}
			ReferenceType refType = (ReferenceType) operandType;

			Type castType = ((TypedInstruction) ins).getType(cpg);
			if (!(castType instanceof ReferenceType)) {
				// This shouldn't happen either
				continue;
			}
			if (refType.equals(castType)) {
				// System.out.println("self-cast to " + castType.getSignature());
				continue;
			}
			if (refType.equals(NullType.instance())) {
				// Value is a literal null
				System.out.println("cast of null value to "
						+ castType.getSignature() + " in " + methodName);
				continue;
			}
			String castSig = castType.getSignature();
			String refSig = refType.getSignature();
			String castSig2 = castSig;
			String refSig2 = refSig;
			while (castSig2.charAt(0) == '[' && refSig2.charAt(0) == '[') {
				castSig2 = castSig2.substring(1);
				refSig2 = refSig2.substring(1);
			}

			if (refSig2.equals("Ljava/lang/Object;")) {
				// System.out.println("cast of object value to " + castType.getSignature());
				continue;
			}

			if (refSig2.charAt(0) != 'L' || castSig2.charAt(0) != 'L') {
				// cast involving primative arrays
				continue;
			}

			SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation
					.fromVisitedInstruction(methodGen, sourceFile, handle);

			if (isCast && haveMultipleCast.contains(sourceLineAnnotation)
					|| !isCast
					&& haveMultipleInstanceOf.contains(sourceLineAnnotation)) {
				// skip; might be due to JSR inlining
				continue;
			}
			String castName = castSig2.substring(1, castSig2.length() - 1)
					.replace('/', '.');
			String refName = refSig2.substring(1, refSig2.length() - 1)
					.replace('/', '.');

			ValueNumberFrame vFrame = vnaDataflow.getFactAtLocation(location);
			boolean isParameter = paramValueNumberSet.contains(vFrame
					.getTopValue());
			try {
				JavaClass castJavaClass = Repository.lookupClass(castName);
				JavaClass refJavaClass = Repository.lookupClass(refName);
				boolean upcast = Repository.instanceOf(refJavaClass,
						castJavaClass);
				if (upcast) {
					if (!isCast)
						bugReporter.reportBug(new BugInstance(this,
								"BC_VACUOUS_INSTANCEOF", NORMAL_PRIORITY)
								.addClassAndMethod(methodGen, sourceFile)
								.addSourceLine(sourceLineAnnotation).addClass(
										refName.replace('/', '.')).addClass(
										castName.replace('/', '.')));
				} else {
					boolean downcast = Repository.instanceOf(castJavaClass,
							refJavaClass);
					double rank = Analyze.deepInstanceOf(refJavaClass,
							castJavaClass);
					if (false)
						System.out.println("Rank:\t" + rank + "\t" + refName
								+ "\t" + castName);
					boolean completeInformation = (!castJavaClass.isInterface() && !refJavaClass
							.isInterface())
							|| refJavaClass.isFinal()
							|| castJavaClass.isFinal();
					if (DEBUG) {
						System.out.println("cast from " + refName + " to "
								+ castName);
						System.out.println("  is downcast: " + downcast);
						System.out.println("  complete information: "
								+ completeInformation);
						System.out.println("  isParameter: "
								+ vFrame.getTopValue());
						System.out.println("  score: " + rank);
					}
					if (!downcast && completeInformation)
						bugReporter.reportBug(new BugInstance(this,
								isCast ? "BC_IMPOSSIBLE_CAST"
										: "BC_IMPOSSIBLE_INSTANCEOF",
								isCast ? HIGH_PRIORITY : NORMAL_PRIORITY)
								.addClassAndMethod(methodGen, sourceFile)
								.addSourceLine(sourceLineAnnotation).addClass(
										refName.replace('/', '.')).addClass(
										castName.replace('/', '.')));
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
						if (castJavaClass.isInterface())
							priority++;
						if (DEBUG)
							System.out.println(" priority c: " + priority);
						if (priority <= LOW_PRIORITY
								&& (refJavaClass.isInterface() || refJavaClass
										.isAbstract()))
							priority++;
						if (DEBUG)
							System.out.println(" priority d: " + priority);
						if (DEBUG)
							System.out.println(" ref name: " + refName);
						if (concreteCollectionClasses.contains(refName)
								|| abstractCollectionClasses.contains(refName)
							|| concreteCollectionClasses.contains(castName)
								|| abstractCollectionClasses.contains(castName))
							priority--;
						if (DEBUG)
							System.out.println(" priority f: " + priority);
						if (methodGen.getName().equals("compareTo"))
							priority++;
						if (DEBUG)
							System.out.println(" priority g: " + priority);
						if (methodGen.isPublic() && isParameter)
							priority--;
						if (DEBUG)
							System.out.println(" priority h: " + priority);
						if (priority < HIGH_PRIORITY)
							priority = HIGH_PRIORITY;
						if (priority <= LOW_PRIORITY) {
							String bug = "BC_UNCONFIRMED_CAST";
							if (concreteCollectionClasses.contains(castName))
								bug = "BC_BAD_CAST_TO_CONCRETE_COLLECTION";
							else if (abstractCollectionClasses
									.contains(castName)
									&& (refName.equals("java/util/Collection") || refName
											.equals("java/lang/Iterable")))
								bug = "BC_BAD_CAST_TO_ABSTRACT_COLLECTION";
							bugReporter.reportBug(new BugInstance(this, bug,
									priority).addClassAndMethod(methodGen,
									sourceFile).addSourceLine(
									sourceLineAnnotation).addClass(
									refName.replace('/', '.')).addClass(
									castName.replace('/', '.')));
						}

					}

				}
			} catch (ClassNotFoundException e) {
			}
		}
	}

	public void report() {
	}

}

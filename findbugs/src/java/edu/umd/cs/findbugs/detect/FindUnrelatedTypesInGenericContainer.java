/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ClassSummary;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.IncompatibleTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.TestCaseDetector;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities.TypeCategory;
import edu.umd.cs.findbugs.ba.type.TopType;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.MultiMap;

/**
 * @author Nat Ayewah
 */
public class FindUnrelatedTypesInGenericContainer implements Detector {

	private BugReporter bugReporter;

	private static final boolean DEBUG = SystemProperties.getBoolean("gc.debug");

	/**
	 * Map classname, methodname and signature to an int []. Each position in
	 * the int [] corresponds to an argument in the methodSignature. For each
	 * argument i, the value at position i corresponds to the index of the
	 * corresponding type in the class type parameters. If the argument has no
	 * correspondence, then the value is -1.
	 * <p>
	 * 
	 * Get the String key by calling getCollectionsMapKey()
	 */

	private MultiMap<String, ClassDescriptor> nameToInterfaceMap = new MultiMap<String, ClassDescriptor>(LinkedList.class);

	private Map<String, Integer> nameToTypeArgumentIndex = new HashMap<String, Integer>();

	private void addToCollectionsMap(@DottedClassName String className, String methodName, int argumentParameterIndex) {
		ClassDescriptor c = DescriptorFactory.instance().getClassDescriptorForDottedClassName(className);
		nameToInterfaceMap.add(methodName, c);
		nameToTypeArgumentIndex.put(methodName, argumentParameterIndex);
	}

	public FindUnrelatedTypesInGenericContainer(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		
		// Collection<E>
		addToCollectionsMap(Collection.class.getName(), "contains", 0);
		addToCollectionsMap(Collection.class.getName(), "remove", 0);
		addToCollectionsMap(Collection.class.getName(), "removeFirstOccurrence", 0);
		addToCollectionsMap(Collection.class.getName(), "removeLastOccurrence", 0);
		addToCollectionsMap(Collection.class.getName(), "containsAll", -1);
		addToCollectionsMap(Collection.class.getName(), "removeAll", -1);
		addToCollectionsMap(Collection.class.getName(), "retainAll", -1);
		
		// List<E>
		addToCollectionsMap(List.class.getName(), "indexOf", 0);
		addToCollectionsMap(List.class.getName(), "lastIndexOf", 0);

		// Map<K,V>
		addToCollectionsMap(Map.class.getName(), "containsKey", 0);
		addToCollectionsMap(Map.class.getName(), "containsValue", 1);
		addToCollectionsMap(Map.class.getName(), "get", 0);
		addToCollectionsMap(Map.class.getName(), "remove", 0);
	}

	/**
	 * Visit the class context
	 * 
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
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
				String msg = "Detector " + this.getClass().getName() + " caught exception while analyzing "
				        + javaClass.getClassName() + "." + method.getName() + " : " + method.getSignature();
				bugReporter.logError(msg, e);
			} catch (DataflowAnalysisException e) {
				String msg = "Detector " + this.getClass().getName() + " caught exception while analyzing "
				        + javaClass.getClassName() + "." + method.getName() + " : " + method.getSignature();
				bugReporter.logError(msg, e);
			}
		}
	}

	/**
	 * Use this to screen out methods that do not contain invocations.
	 */
	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet != null
		        && (bytecodeSet.get(Constants.INVOKEINTERFACE) || bytecodeSet.get(Constants.INVOKEVIRTUAL)
		                || bytecodeSet.get(Constants.INVOKESPECIAL) || bytecodeSet.get(Constants.INVOKESTATIC) || bytecodeSet
		                .get(Constants.INVOKENONVIRTUAL));
	}

	/**
	 * Methods marked with the "Synthetic" attribute do not appear in the source
	 * code
	 */
	private boolean isSynthetic(Method m) {
		Attribute[] attrs = m.getAttributes();
		for (Attribute attr : attrs) {
			if (attr instanceof Synthetic)
				return true;
		}
		return false;
	}

	private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
		if (isSynthetic(method) || !prescreen(classContext, method))
			return;

		BugAccumulator accumulator = new BugAccumulator(bugReporter);

		CFG cfg = classContext.getCFG(method);
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
		ValueNumberDataflow vnDataflow = classContext.getValueNumberDataflow(method);

		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null)
			return;
		String fullMethodName = methodGen.getClassName() + "." + methodGen.getName();

		String sourceFile = classContext.getJavaClass().getSourceFileName();
		if (DEBUG) {
			System.out.println("Checking " + fullMethodName);
		}

		// Process each instruction
		for (Iterator<Location> iter = cfg.locationIterator(); iter.hasNext();) {
			Location location = iter.next();
			InstructionHandle handle = location.getHandle();
			Instruction ins = handle.getInstruction();

			// Only consider invoke instructions
			if (!(ins instanceof InvokeInstruction))
				continue;

			InvokeInstruction inv = (InvokeInstruction) ins;

			XMethod m = XFactory.createXMethod(inv, cpg);

			for (ClassDescriptor interfaceOfInterest : nameToInterfaceMap.get(m.getName())) {

				if (DEBUG) System.out.println("Checking call to " + interfaceOfInterest + " : " + m);
				String argSignature = m.getSignature();
				argSignature = argSignature.substring(0, argSignature.indexOf(')') + 1);
				int pos = 0;
				boolean allMethod = false;
				if (!argSignature.equals("(Ljava/lang/Object;)")) {
						if (m.getName().equals("removeAll") || m.getName().equals("containsAll")
								|| m.getName().equals("retainAll")) {
							if (!m.getSignature().equals("(Ljava/util/Collection;)Z")) continue;
							allMethod = true;
						} else if (m.getName().endsWith("ndexOf")
								&& m.getClassName().equals("java.util.Vector") && 
								argSignature.equals("(Ljava/lang/Object;I)"))
							pos = 1;
						else continue;
				}
				Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
				try {
					if (!subtypes2.isSubtype(m.getClassDescriptor(), interfaceOfInterest))
						continue;
				} catch (ClassNotFoundException e) {
					AnalysisContext.reportMissingClass(e);
					continue;
				}
				// OK, we've fold a method call of interest

				int typeArgument = nameToTypeArgumentIndex.get(m.getName());

				TypeFrame frame = typeDataflow.getFactAtLocation(location);
				if (!frame.isValid()) {
					// This basic block is probably dead
					continue;
				}

				Type operandType = frame.getStackValue(pos);
				if (operandType.equals(TopType.instance())) {
					// unreachable
					continue;
				}
				
				ValueNumberFrame vnFrame = vnDataflow.getFactAtLocation(location);
				int numArguments = frame.getNumArguments(inv, cpg);

				if (numArguments != 1+pos)
					continue;
				
				int expectedParameters = 1;
				if (interfaceOfInterest.getSimpleName().equals("Map"))
					expectedParameters = 2;
				
				// compare containers type parameters to corresponding arguments
				SignatureParser sigParser = new SignatureParser(inv.getSignature(cpg));

				ValueNumber objectVN = vnFrame.getInstance(ins, cpg);
				ValueNumber argVN = vnFrame.getArgument(inv, cpg, 0, sigParser);
				
				if (objectVN.equals(argVN)) {
					String bugPattern =  "DMI_COLLECTIONS_SHOULD_NOT_CONTAIN_THEMSELVES";
					int priority = HIGH_PRIORITY;
					if (m.getName().equals("removeAll")) {
						bugPattern = "DMI_USING_REMOVEALL_TO_CLEAR_COLLECTION";
						priority = NORMAL_PRIORITY;
					} else if (m.getName().endsWith("All")) {
						bugPattern = "DMI_VACUOUS_SELF_COLLECTION_CALL";
						priority = NORMAL_PRIORITY;
					}
						
					accumulator.accumulateBug(new BugInstance(this,bugPattern, priority)
					.addClassAndMethod(methodGen,
					        sourceFile).addCalledMethod(
					        methodGen, (InvokeInstruction) ins)
					        .addOptionalAnnotation(ValueNumberSourceInfo.findAnnotationFromValueNumber(method,
							location, objectVN, vnFrame, "INVOKED_ON")),
					        SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
							        sourceFile, handle));
				}
			
				// Only consider generic...
				Type objectType = frame.getInstance(inv, cpg);
				if (!(objectType instanceof GenericObjectType))
					continue;

				GenericObjectType operand = (GenericObjectType) objectType;

				// ... containers
				if (!operand.hasParameters())
					continue;
				ClassDescriptor operandClass = DescriptorFactory.getClassDescriptor(operand);
				if (!operandClass.getClassName().startsWith("java/util")) try {
	            	XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, operandClass);
	            	String sig = xclass.getSourceSignature();
	            	if (sig != null && sig.indexOf("<L") > 0)
	            		continue;
		           
			    } catch (CheckedAnalysisException e1) {
		          AnalysisContext.logError("Error checking for weird generic parameterization", e1);
	            }

				if (operand.getNumParameters() != expectedParameters)
					continue;

				Type parmType;
				if (typeArgument < 0) parmType = operand;
				else parmType = operand.getParameterAt(typeArgument);
				Type argType = frame.getArgument(inv, cpg, 0, sigParser);
				IncompatibleTypes matchResult = compareTypes(parmType, argType, allMethod);


				boolean parmIsObject = parmType.getSignature().equals("Ljava/lang/Object;");
				boolean selfOperation = !allMethod && operand.equals(argType) && !parmIsObject;
				if (!allMethod && !parmIsObject && argType instanceof GenericObjectType) {

					GenericObjectType p2 = (GenericObjectType) argType;
					List<? extends ReferenceType> parameters = p2.getParameters();
					if (parameters != null && parameters.equals(operand.getParameters()))
						selfOperation = true;
				}
				
				if (!selfOperation && matchResult == IncompatibleTypes.SEEMS_OK)
					continue;

				// Prepare bug report
				SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
				        sourceFile, handle);

				// Report a bug that mentions each of the failed arguments in
				// matches

				if (parmType instanceof GenericObjectType)
					parmType = ((GenericObjectType) parmType).getUpperBound();

				int priority = matchResult.getPriority();
				XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);

				if (TestCaseDetector.likelyTestCase(xmethod))
					priority = Math.max(priority,Priorities.NORMAL_PRIORITY);
				else if (selfOperation)
					priority = Priorities.HIGH_PRIORITY;
				ClassDescriptor expectedClassDescriptor = DescriptorFactory.createClassOrObjectDescriptorFromSignature(parmType
				        .getSignature());
				ClassDescriptor actualClassDescriptor = DescriptorFactory.createClassOrObjectDescriptorFromSignature(argType
				        .getSignature());
				ClassSummary classSummary = AnalysisContext.currentAnalysisContext().getClassSummary();
				Set<XMethod> targets = null;
				try {
					targets = Hierarchy2.resolveVirtualMethodCallTargets(actualClassDescriptor, "equals",
					        "(Ljava/lang/Object;)Z", false, false);
					boolean allOk = targets.size() > 0;
					for (XMethod m2 : targets)
						if (!classSummary.mightBeEqualTo(m2.getClassDescriptor(), expectedClassDescriptor))
							allOk = false;
					if (allOk)
						priority += 2;
				} catch (ClassNotFoundException e) {
					AnalysisContext.reportMissingClass(e);
				}
				accumulator.accumulateBug(new BugInstance(this, "GC_UNRELATED_TYPES", priority).addClassAndMethod(methodGen,
				        sourceFile).addFoundAndExpectedType(argType, parmType).addCalledMethod(
				        methodGen, (InvokeInstruction) ins)
				        .addOptionalAnnotation(ValueNumberSourceInfo.findAnnotationFromValueNumber(method,
								location, objectVN, vnFrame, "INVOKED_ON"))
								.addOptionalAnnotation(ValueNumberSourceInfo.findAnnotationFromValueNumber(method,
										location, argVN, vnFrame, "ARGUMENT"))
										.addEqualsMethodUsed(targets), sourceLineAnnotation);
			}
		}
		accumulator.reportAccumulatedBugs();
	}

	/**
	 * Compare to see if the argument <code>argType</code> passed to the method
	 * matches the type of the corresponding parameter. The simplest case is
	 * when both are equal.
	 * <p>
	 * This is a conservative comparison - returns true if it cannot decide. If
	 * the parameter type is a type variable (e.g. <code>T</code>) then we don't
	 * know enough (yet) to decide if they do not match so return true.
	 * @param ignoreBaseType TODO
	 */
	private IncompatibleTypes compareTypes(Type parmType, Type argType, boolean ignoreBaseType) {
		// XXX equality not implemented for GenericObjectType
		// if (parmType.equals(argType)) return true;
		if (parmType == argType) 
			return IncompatibleTypes.SEEMS_OK;
		// Compare type signatures instead
		String parmString = GenericUtilities.getString(parmType);
		String argString = GenericUtilities.getString(argType);
		if (parmString.equals(argString))
			return IncompatibleTypes.SEEMS_OK;

		// if either type is java.lang.Object, then automatically true!
		// again compare strings...
		String objString = GenericUtilities.getString(Type.OBJECT);
		if (parmString.equals(objString) || argString.equals(objString)) {
			return IncompatibleTypes.SEEMS_OK;
		}

		// get a category for each type
		TypeCategory parmCat = GenericUtilities.getTypeCategory(parmType);
		TypeCategory argCat = GenericUtilities.getTypeCategory(argType);

		if (ignoreBaseType) {
			if (parmCat == TypeCategory.PARAMETERIZED && argCat == TypeCategory.PARAMETERIZED) {
				GenericObjectType parmGeneric = (GenericObjectType) parmType;
				GenericObjectType argGeneric = (GenericObjectType) argType;
				return compareTypeParameters(parmGeneric, argGeneric);
			}
			return IncompatibleTypes.SEEMS_OK;
		}
		// -~- plain objects are easy
		if (parmCat == TypeCategory.PLAIN_OBJECT_TYPE && argCat == TypeCategory.PLAIN_OBJECT_TYPE)

			return IncompatibleTypes.getPriorityForAssumingCompatible(parmType, argType);

		// -~- parmType is: "? extends Another Type" OR "? super Another Type"
		if (parmCat == TypeCategory.WILDCARD_EXTENDS || parmCat == TypeCategory.WILDCARD_SUPER)
			return compareTypes(((GenericObjectType) parmType).getExtension(), argType, ignoreBaseType);

		// -~- Not handling type variables
		if (parmCat == TypeCategory.TYPE_VARIABLE || argCat == TypeCategory.TYPE_VARIABLE)
			return IncompatibleTypes.SEEMS_OK;

		// -~- Array Types: compare dimensions, then base type
		if (parmCat == TypeCategory.ARRAY_TYPE && argCat == TypeCategory.ARRAY_TYPE) {
			ArrayType parmArray = (ArrayType) parmType;
			ArrayType argArray = (ArrayType) argType;

			if (parmArray.getDimensions() != argArray.getDimensions())
				return IncompatibleTypes.ARRAY_AND_NON_ARRAY;

			return compareTypes(parmArray.getBasicType(), argArray.getBasicType(), ignoreBaseType);
		}
		// If one is an Array Type and the other is not, then they
		// are incompatible. (We already know neither is java.lang.Object)
		if (parmCat == TypeCategory.ARRAY_TYPE ^ argCat == TypeCategory.ARRAY_TYPE) {
			return IncompatibleTypes.ARRAY_AND_NON_ARRAY;
		}

		if (parmCat == TypeCategory.PARAMETERIZED && argCat == TypeCategory.PLAIN_OBJECT_TYPE) {
			return IncompatibleTypes.getPriorityForAssumingCompatible(((GenericObjectType) parmType).getObjectType(), argType);
		}
		if (parmCat == TypeCategory.PLAIN_OBJECT_TYPE && argCat == TypeCategory.PARAMETERIZED) {
			return IncompatibleTypes.getPriorityForAssumingCompatible(parmType, ((GenericObjectType) argType).getObjectType());
		}
		// -~- Parameter Types: compare base type then parameters
		if (parmCat == TypeCategory.PARAMETERIZED && argCat == TypeCategory.PARAMETERIZED) {
			GenericObjectType parmGeneric = (GenericObjectType) parmType;
			GenericObjectType argGeneric = (GenericObjectType) argType;

			// base types should be related
			{
			IncompatibleTypes result = compareTypes(parmGeneric.getObjectType(), argGeneric.getObjectType(), ignoreBaseType);
			if (!result.equals(IncompatibleTypes.SEEMS_OK)) return result;
			}
			return compareTypeParameters(parmGeneric, argGeneric);

			// XXX More to come
		}
		// If one is a Parameter Type and the other is not, then they
		// are incompatible. (We already know neither is java.lang.Object)
		if (false) {
			// not true. Consider class Foo extends ArrayList<String>
			if (parmCat == TypeCategory.PARAMETERIZED ^ argCat == TypeCategory.PARAMETERIZED) {
				return IncompatibleTypes.SEEMS_OK; // fix this when we know what
												   // we are doing here
			}
		}

		// -~- Wildcard e.g. List<*>.contains(...)
		if (parmCat == TypeCategory.WILDCARD) // No Way to know
			return IncompatibleTypes.SEEMS_OK;

		// -~- Non Reference types
		// if ( parmCat == TypeCategory.NON_REFERENCE_TYPE ||
		// argCat == TypeCategory.NON_REFERENCE_TYPE )
		if (parmType instanceof BasicType || argType instanceof BasicType) {
			// this should not be possible, compiler will complain (pre 1.5)
			// or autobox primitive types (1.5 +)
			throw new IllegalArgumentException("checking for compatibility of " + parmType + " with " + argType);
		}

		return IncompatibleTypes.SEEMS_OK;

	}

	private IncompatibleTypes compareTypeParameters(GenericObjectType parmGeneric, GenericObjectType argGeneric) {
	    int p = parmGeneric.getNumParameters();
	    if (p != argGeneric.getNumParameters()) {
	    	if (SystemProperties.ASSERTIONS_ENABLED) 
	    		AnalysisContext.logError("Wierd generic parameters: " + parmGeneric + " and " + argGeneric);
	    	return IncompatibleTypes.SEEMS_OK;
	    }
	    for(int x = 0; x< p; x++) {
	    	IncompatibleTypes result = compareTypes(parmGeneric.getParameterAt(x), argGeneric.getParameterAt(x), false);
	    	if (result != IncompatibleTypes.SEEMS_OK) return result;
	    }
	    return IncompatibleTypes.SEEMS_OK;
    }

	// old version of compare types
	private boolean compareTypesOld(Type parmType, Type argType) {
		// XXX equality not implemented for GenericObjectType
		// if (parmType.equals(argType)) return true;
		// Compare type signatures instead
		if (GenericUtilities.getString(parmType).equals(GenericUtilities.getString(argType)))
			return true;

		if (parmType instanceof GenericObjectType) {
			GenericObjectType o = (GenericObjectType) parmType;
			if (o.getTypeCategory() == GenericUtilities.TypeCategory.WILDCARD_EXTENDS) {
				return compareTypesOld(o.getExtension(), argType);
			}
		}
		// ignore type variables for now
		if (parmType instanceof GenericObjectType && !((GenericObjectType) parmType).hasParameters())
			return true;
		if (argType instanceof GenericObjectType && !((GenericObjectType) argType).hasParameters())
			return true;

		// Case: Both are generic containers
		if (parmType instanceof GenericObjectType && argType instanceof GenericObjectType) {
			return true;
		} else {
			// Don't consider non reference types (should not be possible)
			if (!(parmType instanceof ReferenceType && argType instanceof ReferenceType))
				return true;

			// Don't consider non object types (for now)
			if (!(parmType instanceof ObjectType && argType instanceof ObjectType))
				return true;

			// Otherwise, compare base types ignoring generic information
			try {
				return Repository.instanceOf(((ObjectType) argType).getClassName(), ((ObjectType) parmType).getClassName());
			} catch (ClassNotFoundException e) {
			}
		}

		return true;
	}

	/**
	 * Empty
	 * 
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
	}

}

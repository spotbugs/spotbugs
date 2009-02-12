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
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CastableTo;

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
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.IncompatibleTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities.TypeCategory;
import edu.umd.cs.findbugs.ba.type.TopType;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;

/**
 * @author Nat Ayewah
 */
public class TestingGround implements Detector {

	static final Pattern fieldTypeSpecifier = Pattern.compile("^((:+T\\w+)|(:+(\\[)*([BCDFIJZS])|(:+L[^;]+;)))+");
	static void dumpTypeVariables(String s) {
		// s = getTopLevel(s);
		System.out.println(s);
		while (s.length() > 0) {
			int i = s.indexOf(':');
			if (i == -1) break;
			System.out.println(s.substring(0,i));
			s = s.substring(i);
			Matcher m = fieldTypeSpecifier.matcher(s);
			if (!m.find()) throw new IllegalArgumentException("Bad signature" + s);
			
			System.out.println("  matched " + m.group() + " in " + s);
			s = s.substring(m.end());
			
			
			
		}
		
	}
	static String getTopLevel(String s) { 
		if (s.charAt(0) != '<') throw new IllegalArgumentException(s);
		StringBuffer result = new StringBuffer();
		int pos = 1;
		int count = 1;
		while (true) {
			char c = s.charAt(pos);
			switch(c) {
			case '<': count++;
			break;
			case '>': count--;
			if (count == 0) return result.toString();;
			break;
			default: 
				if (count == 1)
					result.append(c);
			}
			pos++;
		}
	}
	
	
	private BugReporter bugReporter;

	private static final boolean DEBUG = SystemProperties.getBoolean("gc.debug");

	private static final ClassDescriptor CASTABLE_TO = DescriptorFactory.createClassDescriptor(CastableTo.class);
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

	
	public TestingGround( BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		
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
			for(int i = 0; i < m.getNumParams(); i++) {
				AnnotationValue parameterAnnotation = m.getParameterAnnotation(i, CASTABLE_TO);
				if (parameterAnnotation == null) continue;
				Object value = parameterAnnotation.getValue("value");
				System.out.println("Castable to " + value);
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

				// Only consider generic...
				Type objectType = frame.getInstance(inv, cpg);
				if (!(objectType instanceof GenericObjectType))
					continue;
				XClass xclass;
				try {
	                xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, DescriptorFactory.createClassDescriptorFromSignature(objectType.getSignature()));
                } catch (CheckedAnalysisException e) {
	               continue;
                }
				
				GenericObjectType operand = (GenericObjectType) objectType;
				// ... containers
				if (!operand.hasParameters())
					continue;
				
				String topLevel = getTopLevel(xclass.getSourceSignature());
				
				dumpTypeVariables(topLevel);
				// compare containers type parameters to corresponding arguments
				SignatureParser sigParser = new SignatureParser(inv.getSignature(cpg));

				
				
				Type parmType = operand.getParameterAt(0);
				Type argType = frame.getArgument(inv, cpg, 0, sigParser);
				IncompatibleTypes matchResult = compareTypes(parmType, argType);

			}
			
			/*
			for (ClassDescriptor interfaceOfInterest : nameToInterfaceMap.get(m.getName())) {

				String argSignature = m.getSignature();
				argSignature = argSignature.substring(0, argSignature.indexOf(')') + 1);
				if (!argSignature.equals("(Ljava/lang/Object;)"))
					continue;

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

				Type operandType = frame.getTopValue();
				if (operandType.equals(TopType.instance())) {
					// unreachable
					continue;
				}

				// Only consider generic...
				Type objectType = frame.getInstance(inv, cpg);
				if (!(objectType instanceof GenericObjectType))
					continue;

				GenericObjectType operand = (GenericObjectType) objectType;

				// ... containers
				if (!operand.hasParameters())
					continue;

				int expectedParameters = 1;
				if (interfaceOfInterest.getSimpleName().equals("Map"))
					expectedParameters = 2;
				if (operand.getNumParameters() != expectedParameters)
					continue;

				int numArguments = frame.getNumArguments(inv, cpg);

				if (numArguments != 1)
					continue;

				// compare containers type parameters to corresponding arguments
				SignatureParser sigParser = new SignatureParser(inv.getSignature(cpg));

				Type parmType = operand.getParameterAt(typeArgument);
				Type argType = frame.getArgument(inv, cpg, 0, sigParser);
				IncompatibleTypes matchResult = compareTypes(parmType, argType);

				boolean selfOperation = operand.equals(argType);
				if (argType instanceof GenericObjectType) {
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
				        sourceFile).addFoundAndExpectedType(argType.getSignature(), parmType.getSignature()).addCalledMethod(
				        methodGen, (InvokeInstruction) ins).addEqualsMethodUsed(targets), sourceLineAnnotation);
			
			}
			*/
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
	 */
	private IncompatibleTypes compareTypes(Type parmType, Type argType) {
		// XXX equality not implemented for GenericObjectType
		// if (parmType.equals(argType)) return true;
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

		// -~- plain objects are easy
		if (parmCat == TypeCategory.PLAIN_OBJECT_TYPE && argCat == TypeCategory.PLAIN_OBJECT_TYPE)

			return IncompatibleTypes.getPriorityForAssumingCompatible(parmType, argType);

		// -~- parmType is: "? extends Another Type" OR "? super Another Type"
		if (parmCat == TypeCategory.WILDCARD_EXTENDS || parmCat == TypeCategory.WILDCARD_SUPER)
			return compareTypes(((GenericObjectType) parmType).getExtension(), argType);

		// -~- Not handling type variables
		if (parmCat == TypeCategory.TYPE_VARIABLE || argCat == TypeCategory.TYPE_VARIABLE)
			return IncompatibleTypes.SEEMS_OK;

		// -~- Array Types: compare dimensions, then base type
		if (parmCat == TypeCategory.ARRAY_TYPE && argCat == TypeCategory.ARRAY_TYPE) {
			ArrayType parmArray = (ArrayType) parmType;
			ArrayType argArray = (ArrayType) argType;

			if (parmArray.getDimensions() != argArray.getDimensions())
				return IncompatibleTypes.ARRAY_AND_NON_ARRAY;

			return compareTypes(parmArray.getBasicType(), argArray.getBasicType());
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
			return compareTypes(parmGeneric.getObjectType(), argGeneric.getObjectType());

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

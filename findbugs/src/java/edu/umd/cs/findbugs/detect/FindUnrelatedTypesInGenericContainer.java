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

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
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
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities.TypeCategory;
import edu.umd.cs.findbugs.ba.type.TopType;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;

/**
 * @author Nat Ayewah
 */
public class FindUnrelatedTypesInGenericContainer implements Detector {

	private BugReporter bugReporter;

	private static final boolean DEBUG = SystemProperties.getBoolean("gc.debug");
			
	/** 
	 * Map classname, methodname and signature to an int []. 
	 * Each position in the int [] corresponds to an argument in the methodSignature.
	 * For each argument i, the value at position i corresponds to the index of the 
	 * corresponding type in the class type parameters. If the argument
	 * has no correspondence, then the value is -1. <p>
	 * 
	 * Get the String key by calling getCollectionsMapKey()
	 */
	private Map<String, int []> collectionsMap = new HashMap<String, int[]>();
	
	/**
	 * @param triplet[0] = className. 
	 * 			The name of the collection e.g. <code>java.util.List</code>
	 * @param triplet[1] = methodName.
	 * 			The method's name e.g. <code>contains</code>
	 * @param triplet[2] = methodSignature.
	 * 			The method's signature e.g. <code>(Ljava/lang/Object;)Z</code>
	 * @return
	 */
	public static String getCollectionsMapKey(String...triplet) {
		return triplet[0] + "??" + triplet[1] + "???" + triplet[2];
	}
	
	private void addToCollectionsMap(String className, String methodName, 
			String methodSignature, int... argumentParameterIndex) {
		collectionsMap.put(
				getCollectionsMapKey(className, methodName, methodSignature), 
				argumentParameterIndex);
	}

	private void addToCollectionsMap(String [] classNames, String methodName, 
			String methodSignature, int... argumentParameterIndex) {
		for (String className : classNames)
			addToCollectionsMap(
					className, methodName, methodSignature, 
					argumentParameterIndex);
	}

	String [] collectionMembers = new String [] {
			"java.util.Collection",
			"java.util.AbstractCollection",
			"java.util.List",
			"java.util.AbstractList",
			"java.util.ArrayList",
			"java.util.LinkedList",
			"java.util.Set",
			"java.util.SortedSet",
			"java.util.LinkedHashSet",
			"java.util.HashSet",
			"java.util.TreeSet"	
	};
	
	String [] mapMembers = new String [] {
			"java.util.Map",
			"java.util.AbstractMap",
			"java.util.SortedMap",
			"java.util.TreeMap",
			"java.util.HashMap",
			"java.util.LinkedHashMap",
			"java.util.concurrent.ConcurrentHashMap",
			"java.util.EnumMap",
			"java.util.Hashtable",
			"java.util.IdentityHashMap",
			"java.util.WeakHashMap"
	};
	
	String [] listMembers = new String [] {
			"java.util.List",
			"java.util.AbstractList",
			"java.util.ArrayList",
			"java.util.LinkedList"
	};

	public FindUnrelatedTypesInGenericContainer(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		String basicSignature = "(Ljava/lang/Object;)Z";
		String collectionSignature = "(Ljava/util/Collection<*>;)Z";
		String indexSignature = "(Ljava/lang/Object;)I";
		
		// Collection<E>
		addToCollectionsMap(collectionMembers, "contains", basicSignature, 0);
		//addToCollectionsMap(collectionMembers, "equals",   basicSignature, 0);
		addToCollectionsMap(collectionMembers, "remove",   basicSignature, 0);

		//addToCollectionsMap(collectionMembers, "containsAll", collectionSignature, 0);
		//addToCollectionsMap(collectionMembers, "removeAll",   collectionSignature, 0);
		//addToCollectionsMap(collectionMembers, "retainAll",   collectionSignature, 0);
		
		// List<E>
		addToCollectionsMap(listMembers, "indexOf", indexSignature, 0);
		addToCollectionsMap(listMembers, "lastIndexOf", indexSignature, 0);
		
		// Map<K,V>
		addToCollectionsMap(mapMembers, "containsKey", basicSignature, 0);
		addToCollectionsMap(mapMembers, "containsValue", basicSignature, 1);
		
		// XXX these do not work, to support these need changeable return types
		addToCollectionsMap(mapMembers, "get", basicSignature, 0);
		addToCollectionsMap(mapMembers, "remove", basicSignature, 0);
	}
	
	/**
	 * Visit the class context
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

	/**
	 * Use this to screen out methods that do not contain invocations.
	 */
	public boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		return bytecodeSet != null && (
				  bytecodeSet.get(Constants.INVOKEINTERFACE) || 
				  bytecodeSet.get(Constants.INVOKEVIRTUAL)   ||
				  bytecodeSet.get(Constants.INVOKESPECIAL) 	 ||
				  bytecodeSet.get(Constants.INVOKESTATIC)    ||
				  bytecodeSet.get(Constants.INVOKENONVIRTUAL)				  
				);
	}

	/**
	 * Methods marked with the "Synthetic" attribute do not appear 
	 * in the source code
	 */
	private boolean isSynthetic(Method m) {
		Attribute[] attrs = m.getAttributes();
		for (Attribute attr : attrs) {
			if (attr instanceof Synthetic)
				return true;
		}
		return false;
	}

	private void analyzeMethod(ClassContext classContext, Method method)
	throws CFGBuilderException, DataflowAnalysisException {
		if (isSynthetic(method) || !prescreen(classContext, method))
			return;
		
		BugAccumulator accumulator = new BugAccumulator(bugReporter);
		
		CFG cfg = classContext.getCFG(method);
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return;
		String fullMethodName = 
			methodGen.getClassName() + "." + methodGen.getName(); 

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

			InvokeInstruction inv = (InvokeInstruction)ins;
			
			// check the relevance of this instruction
			String [] itriplet = getInstructionTriplet(inv, cpg);
			String [] triplet = getRelevantTriplet(itriplet);
			if (triplet == null)
				continue;
			
			// get the list of parameter indexes for each argument position
			int [] argumentParameterIndex = 
				collectionsMap.get( getCollectionsMapKey(triplet) );
			
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
			if (!operand.hasParameters()) continue;
			
			int numArguments = frame.getNumArguments(inv, cpg);
			
			if (numArguments <= 0 || argumentParameterIndex.length != numArguments)
				continue; 
			
			// compare containers type parameters to corresponding arguments
			boolean match = true;
			boolean [] matches = new boolean [numArguments];
			for (int i=0; i<numArguments; i++) matches[i] = true;
			
			Type parmType = null, argType = null;			
			for (int ii=0; ii < numArguments; ii++) {
				if (argumentParameterIndex[ii] < 0) continue; // not relevant argument
				if (argumentParameterIndex[ii] >= operand.getNumParameters()) 
					continue; // should never happen
				
				parmType = operand.getParameterAt(argumentParameterIndex[ii]);
				argType = frame.getArgument(inv, cpg, ii, numArguments);
				
				if (!compareTypes(parmType, argType)) {
					match = false;
					matches[ii] = false;
				}
			}
			
			if (match)
				continue; // no bug

			// Prepare bug report
			SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation
			.fromVisitedInstruction(classContext, methodGen, sourceFile, handle);

			// Report a bug that mentions each of the failed arguments in matches
			for (int i=0; i<numArguments; i++) {
				if (matches[i]) continue;

				parmType = operand.getParameterAt(argumentParameterIndex[i]);
				argType = frame.getArgument(inv, cpg, i, numArguments);
				
				accumulator.accumulateBug(new BugInstance(this,
						"GC_UNRELATED_TYPES", NORMAL_PRIORITY)
						.addClassAndMethod(methodGen, sourceFile)					
						.addString(GenericUtilities.getString(parmType))
						.addString(GenericUtilities.getString(argType))
						.addType(parmType.getSignature()) //XXX addType not handling 
						.addType(argType.getSignature())  //    generics properly
						,sourceLineAnnotation);
			}
			
		}
						
		accumulator.reportAccumulatedBugs();
	}
	
	/**
	 * Get a String triplet representing the information in this instruction:
	 * the className, methodName, and methodSignature
	 */
	private String [] getInstructionTriplet(InvokeInstruction inv, ConstantPoolGen cpg) {

		// get the class name
		ConstantCP ref = (ConstantCP) cpg.getConstant( inv.getIndex() );
		String className = ref.getClass(cpg.getConstantPool());
		
		// get the method name
		ConstantNameAndType refNT = 
			(ConstantNameAndType) cpg.getConstant( ref.getNameAndTypeIndex() );
		String methodName = refNT.getName(cpg.getConstantPool());

		// get the method signature
		String methodSignature = refNT.getSignature(cpg.getConstantPool());
		
		return new String[] { className, methodName, methodSignature };
	}
	
	/**
	 * Given a triplet representing the className, methodName, and methodSignature
	 * of an instruction, check to see if it is in our collectionsMap. <p>
	 * [Not Implemented] If not, search harder to see if one of the super classes 
	 * of className is in our collectionMap
	 */
	private @CheckForNull String [] getRelevantTriplet(String [] instructionTriplet) {
		if (collectionsMap.containsKey( getCollectionsMapKey(instructionTriplet) ))
			return instructionTriplet;
		
		// HARDCODES
		// Map "get" and "remove"
		if (Arrays.asList(mapMembers).contains(instructionTriplet[0])) {
			if ( "get"   .equals(instructionTriplet[1]) || 
				 "remove".equals(instructionTriplet[1]) ) {
				addToCollectionsMap(instructionTriplet[0], 
						instructionTriplet[1], instructionTriplet[2], 0);
				return instructionTriplet;
			}
		}
		
		// XXX The rest not implemented
		
		// Not found
		return null;
	}
	
	/**
	 * Compare to see if the argument <code>argType</code> passed to the method 
	 * matches the type of the corresponding parameter. The simplest case is when
	 * both are equal. <p>
	 * This is a conservative comparison - returns true if it cannot decide.
	 * If the parameter type is a type variable (e.g. <code>T</code>) then we don't 
	 * know enough (yet) to decide if they do not match so return true.
	 */
	private boolean compareTypes(Type parmType, Type argType) {
		// XXX equality not implemented for GenericObjectType
		// if (parmType.equals(argType)) return true;
		// Compare type signatures instead
		String parmString = GenericUtilities.getString(parmType);
		String argString = GenericUtilities.getString(argType);
		if (parmString.equals(argString)) return true;
		
		// if either type is java.lang.Object, then automatically true!
		// again compare strings...
		String objString = GenericUtilities.getString(Type.OBJECT);
		if ( parmString.equals(objString) || 
			 argString.equals(objString) ) {
			return true;
		}
		
		// get a category for each type
		TypeCategory parmCat = GenericUtilities.getTypeCategory(parmType);
		TypeCategory argCat  = GenericUtilities.getTypeCategory(argType);
		
		// -~- plain objects are easy
		if ( parmCat == TypeCategory.PLAIN_OBJECT_TYPE && 
			 argCat == TypeCategory.PLAIN_OBJECT_TYPE ) {

			try {		
				String parmName = ((ObjectType)parmType).getClassName();
				String argName  = ((ObjectType)argType) .getClassName();
				
				// e.g. List<String>.contains(CharSequence)
				if (Repository.instanceOf(parmName, argName)) return true;
				
				// e.g. List<CharSequence>.contains(String)
				return Repository.instanceOf(argName, parmName);
				
			} catch (ClassNotFoundException e) { return true; }			
		}
		
		// -~- parmType is: "? extends Another Type" OR "? super Another Type"
		if ( parmCat == TypeCategory.WILDCARD_EXTENDS || 
			 parmCat == TypeCategory.WILDCARD_SUPER ) {
			return compareTypes( 
					((GenericObjectType)parmType).getExtension(), argType);
		}
		
		// -~- Not handling type variables
		if ( parmCat == TypeCategory.TYPE_VARIABLE || 
			 argCat == TypeCategory.TYPE_VARIABLE ) {
			return true;
		}
		
		// -~- Array Types: compare dimensions, then base type
		if ( parmCat == TypeCategory.ARRAY_TYPE && 
			 argCat  == TypeCategory.ARRAY_TYPE ) {
			ArrayType parmArray = (ArrayType) parmType;
			ArrayType argArray  = (ArrayType) argType;
			
			if (parmArray.getDimensions() != argArray.getDimensions())
				return false;
			
			return compareTypes(parmArray.getBasicType(), argArray.getBasicType());
		}
		//     If one is an Array Type and the other is not, then they
		//     are incompatible. (We already know neither is java.lang.Object)
		if ( parmCat == TypeCategory.ARRAY_TYPE ^ 
			 argCat  == TypeCategory.ARRAY_TYPE ) {
			return false;
		}
		
		// -~- Parameter Types: compare base type then parameters
		if ( parmCat == TypeCategory.PARAMETERS && 
			 argCat  == TypeCategory.PARAMETERS ) {
			GenericObjectType parmGeneric = (GenericObjectType) parmType;
			GenericObjectType argGeneric  = (GenericObjectType) argType;
			
			// base types should be related
			if (!compareTypes( parmGeneric.getObjectType(), 
					argGeneric.getObjectType()) )
				return false;
			
			// XXX More to come
		}
		//     If one is a Parameter Type and the other is not, then they
		//     are incompatible. (We already know neither is java.lang.Object)
		if (false) {
			// not true. Consider class Foo extends ArrayList<String>
			if ( parmCat == TypeCategory.PARAMETERS ^ 
				 argCat  == TypeCategory.PARAMETERS ) {
				return false;
			}
		}
		
		// -~- Wildcard e.g. List<*>.contains(...)		
		if (parmCat == TypeCategory.WILDCARD) // No Way to know 
			return true;
		
		// -~- Non Reference types
		// if ( parmCat == TypeCategory.NON_REFERENCE_TYPE || 
		// 	    argCat == TypeCategory.NON_REFERENCE_TYPE )
		if (parmType instanceof BasicType || argType instanceof BasicType) {
			// this should not be possible, compiler will complain (pre 1.5) 
			// or autobox primitive types (1.5 +)
			assert false;
		}
		
		return true;
	}
	
	// old version of compare types
	private boolean compareTypesOld(Type parmType, Type argType) {
		// XXX equality not implemented for GenericObjectType
		// if (parmType.equals(argType)) return true;
		// Compare type signatures instead
		if (GenericUtilities.getString(parmType)
				.equals(GenericUtilities.getString(argType)))
			return true;
		
		if (parmType instanceof GenericObjectType) {
			GenericObjectType o = (GenericObjectType) parmType;
			if (o.getTypeCategory() == GenericUtilities.TypeCategory.WILDCARD_EXTENDS) {
				return compareTypes(o.getExtension(), argType);
			}
		}
		// ignore type variables for now
		if (parmType instanceof GenericObjectType && 
				!((GenericObjectType) parmType).hasParameters()) return true;
		if (argType instanceof GenericObjectType && 
				!((GenericObjectType) argType).hasParameters()) return true;
		
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
				return Repository.instanceOf(
						((ObjectType)argType).getClassName(), 
						((ObjectType)parmType).getClassName());
			} catch (ClassNotFoundException e) {}
		}
				 
		return true;
	}
	
	/**
	 * Empty
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
	}

}

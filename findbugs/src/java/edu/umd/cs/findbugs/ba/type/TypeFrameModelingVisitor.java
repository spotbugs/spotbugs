/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
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

package edu.umd.cs.findbugs.ba.type;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Signature;
import org.apache.bcel.generic.*;

import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.InvalidBytecodeException;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.util.Util;

/**
 * Visitor to model the effects of bytecode instructions on the types of the
 * values (local and operand stack) in Java stack frames. This visitor does not
 * verify that the types are sensible for the bytecodes executed. In other
 * words, this isn't a bytecode verifier, although it wouldn't be too hard to
 * turn it into something vaguely verifier-like.
 * 
 * @author David Hovemeyer
 * @see TypeFrame
 * @see TypeAnalysis
 */
public class TypeFrameModelingVisitor extends AbstractFrameModelingVisitor<Type, TypeFrame> implements Constants, Debug {

	static private final ObjectType COLLECTION_TYPE = ObjectTypeFactory.getInstance("java.util.Collection");

	private ValueNumberDataflow valueNumberDataflow;

	// Fields for precise modeling of instanceof instructions.
	private boolean instanceOfFollowedByBranch;

	private ReferenceType instanceOfType;

	private ValueNumber instanceOfValueNumber;

	private FieldSummary fieldSummary;

	private FieldStoreTypeDatabase database;
	  
	private Set<ReferenceType> typesComputedFromGenerics = Util.newSetFromMap(new IdentityHashMap<ReferenceType, Boolean>());
	

	/**
	 * Constructor.
	 * 
	 * @param cpg
	 *            the ConstantPoolGen of the method whose instructions we are
	 *            examining
	 * @param typesComputerFromGenerics TODO
	 */
	public TypeFrameModelingVisitor(ConstantPoolGen cpg) {
		super(cpg);
		fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();

	}

	/**
	 * Set ValueNumberDataflow for the method being analyzed. This is optional;
	 * if set, we will use the information to more accurately model the effects
	 * of instanceof instructions.
	 * 
	 * @param valueNumberDataflow
	 *            the ValueNumberDataflow
	 */
	public void setValueNumberDataflow(ValueNumberDataflow valueNumberDataflow) {
		this.valueNumberDataflow = valueNumberDataflow;
	}

	/**
	 * Return whether an instanceof instruction was followed by a branch. The
	 * TypeAnalysis may use this to get more precise types in the resulting
	 * frame.
	 * 
	 * @return true if an instanceof instruction was followed by a branch, false
	 *         if not
	 */
	public boolean isInstanceOfFollowedByBranch() {
		return instanceOfFollowedByBranch;
	}

	/**
	 * Get the type of the most recent instanceof instruction modeled. The
	 * TypeAnalysis may use this to get more precise types in the resulting
	 * frame.
	 * 
	 * @return the Type checked by the most recent instanceof instruction
	 */
	public Type getInstanceOfType() {
		return instanceOfType;
	}

	/**
	 * Get the value number of the most recent instanceof instruction modeled.
	 * The TypeAnalysis may use this to get more precise types in the resulting
	 * frame.
	 * 
	 * @return the ValueNumber checked by the most recent instanceof instruction
	 */
	public ValueNumber getInstanceOfValueNumber() {
		return instanceOfValueNumber;
	}

	/**
	 * Set the field store type database. We can use this to get more accurate
	 * types for values loaded from fields.
	 * 
	 * @param database
	 *            the FieldStoreTypeDatabase
	 */
	public void setFieldStoreTypeDatabase(FieldStoreTypeDatabase database) {
		this.database = database;
	}

	@Override
	public Type getDefaultValue() {
		return TypeFrame.getBottomType();
	}

	boolean sawEffectiveInstanceOf;

	boolean previousWasEffectiveInstanceOf;

	@Override
	public void analyzeInstruction(Instruction ins) throws DataflowAnalysisException {
		instanceOfFollowedByBranch = false;
		sawEffectiveInstanceOf = false;
		super.analyzeInstruction(ins);
		previousWasEffectiveInstanceOf = sawEffectiveInstanceOf;
	}

	/**
	 * This method must be called at the beginning of modeling a basic block in
	 * order to clear information cached for instanceof modeling.
	 */
	public void startBasicBlock() {
		instanceOfType = null;
		instanceOfValueNumber = null;
	}

	/**
	 * Consume stack. This is a convenience method for instructions where the
	 * types of popped operands can be ignored.
	 */
	protected void consumeStack(Instruction ins) {
		ConstantPoolGen cpg = getCPG();
		TypeFrame frame = getFrame();

		int numWordsConsumed = ins.consumeStack(cpg);
		if (numWordsConsumed == Constants.UNPREDICTABLE)
			throw new InvalidBytecodeException("Unpredictable stack consumption for " + ins);
		try {
			while (numWordsConsumed-- > 0) {
				frame.popValue();
			}
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException("Stack underflow for " + ins + ": " + e.getMessage());
		}
	}

	/**
	 * Work around some weirdness in BCEL (inherited from JVM Spec 1): BCEL
	 * considers long and double types to consume two slots on the stack. This
	 * method ensures that we push two types for each double or long value.
	 */
	protected void pushValue(Type type) {
		TypeFrame frame = getFrame();
		if (type.getType() == T_LONG) {
			frame.pushValue(Type.LONG);
			frame.pushValue(TypeFrame.getLongExtraType());
		} else if (type.getType() == T_DOUBLE) {
			frame.pushValue(Type.DOUBLE);
			frame.pushValue(TypeFrame.getDoubleExtraType());
		} else
			frame.pushValue(type);
	}

	/**
	 * Helper for pushing the return type of an invoke instruction.
	 */
	protected void pushReturnType(InvokeInstruction ins) {
		ConstantPoolGen cpg = getCPG();
		Type type = ins.getType(cpg);
		if (type.getType() != T_VOID)
			pushValue(type);
	}

	/**
	 * This is overridden only to ensure that we don't rely on the base class to
	 * handle instructions that produce stack operands.
	 */
	@Override
	public void modelNormalInstruction(Instruction ins, int numWordsConsumed, int numWordsProduced) {
		if (VERIFY_INTEGRITY) {
			if (numWordsProduced > 0)
				throw new InvalidBytecodeException("missing visitor method for " + ins);
		}
		super.modelNormalInstruction(ins, numWordsConsumed, numWordsProduced);
	}

	// ----------------------------------------------------------------------
	// Instruction visitor methods
	// ----------------------------------------------------------------------

	// NOTES:
	// - Instructions that only consume operands need not be overridden,
	// because the base class visit methods handle them correctly.
	// - Instructions that simply move values around in the frame,
	// such as DUP, xLOAD, etc., do not need to be overridden because
	// the base class handles them.
	// - Instructions that consume and produce should call
	// consumeStack(Instruction) and then explicitly push produced operands.

	@Override
	public void visitATHROW(ATHROW obj) {
		// do nothing. The same value remains on the stack (but we jump to a new
		// location)
	}

	@Override
	public void visitACONST_NULL(ACONST_NULL obj) {
		pushValue(TypeFrame.getNullType());
	}

	@Override
	public void visitDCONST(DCONST obj) {
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitFCONST(FCONST obj) {
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitICONST(ICONST obj) {
		pushValue(Type.INT);
	}

	@Override
	public void visitLCONST(LCONST obj) {
		pushValue(Type.LONG);
	}

	@Override
	public void visitLDC(LDC obj) {
		pushValue(obj.getType(getCPG()));
	}

	@Override
	public void visitLDC2_W(LDC2_W obj) {
		pushValue(obj.getType(getCPG()));
	}

	@Override
	public void visitBIPUSH(BIPUSH obj) {
		pushValue(Type.INT);
	}

	@Override
	public void visitSIPUSH(SIPUSH obj) {
		pushValue(Type.INT);
	}

	@Override
	public void visitGETSTATIC(GETSTATIC obj) {
		modelFieldLoad(obj);
	}

	@Override
	public void visitGETFIELD(GETFIELD obj) {
		modelFieldLoad(obj);
	}

	public void modelFieldLoad(FieldInstruction obj) {
		consumeStack(obj);

		Type loadType = obj.getType(getCPG());
		Type originalLoadType = loadType;

		try {
			// Check the field store type database to see if we can
			// get a more precise type for this load.
			XField xfield = Hierarchy.findXField(obj, getCPG());
			if (xfield != null) {
				if (database != null && (loadType instanceof ReferenceType)) {
					FieldStoreType property = database.getProperty(xfield.getFieldDescriptor());
					if (property != null) {
						loadType = property.getLoadType((ReferenceType) loadType);
					}
				}

				Item summary = fieldSummary.getSummary(xfield);
				if (xfield.isFinal() && summary.isNull()) {
					pushValue(TypeFrame.getNullType());
					return;
				}
				if (loadType == originalLoadType && summary != null && !summary.getSignature().equals("Ljava/lang/Object;")) {
					loadType = Type.getType(summary.getSignature());
				}

				// [Added: Support for Generics]
				// XXX If the loadType was not changed by the
				// FieldStoreTypeDatabase, then
				// we can assume, that the signature for obj is still relevant.
				// This should
				// be updated by inserting generic information in the
				// FieldStoreTypeDatabase

				// find the field and its signature
				Field field = Hierarchy.findField(xfield.getClassName(), xfield.getName());
				String signature = null;
				for (Attribute a : field.getAttributes()) {
					if (a instanceof Signature) {
						signature = ((Signature) a).getSignature();
						break;
					}
				}

				// replace loadType with information from field signature
				// (conservative)
				if (signature != null && (loadType instanceof ObjectType)) {
					loadType = GenericUtilities.merge(GenericUtilities.getType(signature), (ObjectType) loadType);
				}

			}
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		} catch (RuntimeException e) {
		} // degrade gracefully

		pushValue(loadType);
	}

	@Override
	public void visitINVOKESTATIC(INVOKESTATIC obj) {
		String methodName = obj.getMethodName(cpg);
		String signature = obj.getSignature(cpg);
		String className = obj.getClassName(cpg);
		if (methodName.equals("asList") && className.equals("java.util.Arrays")
		        && signature.equals("([Ljava/lang/Object;)Ljava/util/List;")) {
			consumeStack(obj);
			Type returnType = Type.getType("Ljava/util/Arrays$ArrayList;");
			pushValue(returnType);
			return;
		}
		consumeStack(obj);
		pushReturnType(obj);
	}

	@Override
	public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
		consumeStack(obj);
		pushReturnType(obj);
	}

	@Override
	public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
		visitInvokeInstructionCommon(obj);
	}

	@Override
	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
		visitInvokeInstructionCommon(obj);
	}

	private boolean getResultTypeFromGenericType(TypeFrame frame, int index, int expectedParameters) {
		try {
			Type mapType = frame.getStackValue(0);
			if (mapType instanceof GenericObjectType) {
				GenericObjectType genericMapType = (GenericObjectType) mapType;
				List<? extends ReferenceType> parameters = genericMapType.getParameters();
				if (parameters != null && parameters.size() == expectedParameters) {
					ReferenceType resultType = parameters.get(index);
					if (resultType instanceof GenericObjectType)
						resultType = ((GenericObjectType)resultType).produce();
					typesComputedFromGenerics.add(resultType);
					frame.popValue();
					frame.pushValue(resultType);
					return true;
				}
			}

		} catch (DataflowAnalysisException e) {
			AnalysisContext.logError("oops", e);
		}

		return false;
	}

	
	private boolean handleGetMapView(TypeFrame frame, String typeName, int index, int expectedNumberOfTypeParameters) {
		try {
			Type mapType = frame.getStackValue(0);
			if (mapType instanceof GenericObjectType) {
				GenericObjectType genericMapType = (GenericObjectType) mapType;
				List<? extends ReferenceType> parameters = genericMapType.getParameters();
				if (parameters == null)
					return false;
				if (parameters.size() == expectedNumberOfTypeParameters) {
					ReferenceType keyType = parameters.get(index);
					frame.popValue();
					typesComputedFromGenerics.add(keyType);
					GenericObjectType keySetType = GenericUtilities.getType(typeName,
							Collections.singletonList(keyType));
					typesComputedFromGenerics.add(keySetType);
					frame.pushValue(keySetType);
					return true;
				}
			}

		} catch (DataflowAnalysisException e) {
			AnalysisContext.logError("oops", e);
		}
		return false;

	}
	public void visitInvokeInstructionCommon(InvokeInstruction obj) {
		TypeFrame frame = getFrame();

		String methodName = obj.getMethodName(cpg);
		String signature = obj.getSignature(cpg);
		String className = obj.getClassName(cpg);

		if (methodName.equals("cast") && className.equals("java.lang.Class")) {
			try {
				Type resultType = frame.popValue();
				frame.popValue();
				frame.pushValue(resultType);
			} catch (DataflowAnalysisException e) {
				AnalysisContext.logError("oops", e);
			}

			return;
		}

		if (methodName.equals("get") && signature.equals("(Ljava/lang/Object;)Ljava/lang/Object;") && className.endsWith("Map")) {
			try {
				Type mapType = frame.getStackValue(1);
				if (mapType instanceof GenericObjectType) {
					GenericObjectType genericMapType = (GenericObjectType) mapType;
					List<? extends ReferenceType> parameters = genericMapType.getParameters();
					if (parameters != null && parameters.size() == 2) {
						ReferenceType valueType = parameters.get(1);
						consumeStack(obj);
						frame.pushValue(valueType);
						return;
					}
				}

			} catch (DataflowAnalysisException e) {
				AnalysisContext.logError("oops", e);
			}

		}

		if (className.equals("java.util.Map$Entry"))
			if (methodName.equals("getKey") && getResultTypeFromGenericType(frame, 0, 2) || methodName.equals("getValue")
			        && getResultTypeFromGenericType(frame, 1, 2))
				return;

		if (methodName.equals("entrySet") && signature.equals("()Ljava/util/Set;") 
				&& className.startsWith("java.util") && className.endsWith("Map")) {
			Type argType;
			try {
				argType = frame.popValue();
			} catch (DataflowAnalysisException e) {
				AnalysisContext.logError("oops", e);
				return;
			}
			ObjectType mapType = (ObjectType) Type.getType("Ljava/util/Map$Entry;");
			
			if (argType instanceof GenericObjectType) {
				GenericObjectType genericArgType = (GenericObjectType) argType;
				List<? extends ReferenceType> parameters = genericArgType.getParameters();
				if (parameters != null && parameters.size() == 2)
					mapType = GenericUtilities.getType("java.util.Map$Entry", parameters);
			}
			GenericObjectType entrySetType = GenericUtilities.getType("java.util.Set", Collections.singletonList(mapType));
			frame.pushValue(entrySetType);
			return;

		}
		if (className.startsWith("java.util") && className.endsWith("Map")) 
			if (methodName.equals("keySet") && signature.equals("()Ljava/util/Set;") && handleGetMapView(frame, "java.util.Set", 0,2 )
					|| methodName.equals("values") && signature.equals("()Ljava/util/Collection;") 
					&& handleGetMapView(frame, "java.util.Collection", 1,2 ))
					return;

		if (methodName.equals("iterator") && signature.equals("()Ljava/util/Iterator;") && className.startsWith("java.util")
				&& handleGetMapView(frame, "java.util.Iterator", 0,1 ))
					return;
		if (className.equals("java.util.Iterator") &&methodName.equals("next") 
				&& signature.equals("()Ljava/lang/Object;") && getResultTypeFromGenericType(frame, 0, 1))
			return;


		if (methodName.equals("isInstance")) {
			if (className.equals("java.lang.Class") && valueNumberDataflow != null) {
				// Record the value number of the value checked by this
				// instruction,
				// and the type the value was compared to.
				try {
					ValueNumberFrame vnaFrame = valueNumberDataflow.getFactAtLocation(getLocation());
					if (vnaFrame.isValid()) {
						ValueNumber stackValue = vnaFrame.getStackValue(1);
						if (stackValue.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT)) {
							String c = valueNumberDataflow.getClassName(stackValue);
							if (c != null) {
								if (c.charAt(0) != '[' && !c.endsWith(";"))
									c = "L" + c.replace('.', '/') + ";";
								Type type = Type.getType(c);
								if (type instanceof ReferenceType) {
									instanceOfValueNumber = vnaFrame.getTopValue();
									instanceOfType = (ReferenceType) type;
									sawEffectiveInstanceOf = true;
								}
							}
						}

					}
				} catch (DataflowAnalysisException e) {
					// Ignore
				}
			}
		}
		if (methodName.equals("initCause") && signature.equals("(Ljava/lang/Throwable;)Ljava/lang/Throwable;")
		        && className.endsWith("Exception")) {
			try {

				frame.popValue();
				return;
			} catch (DataflowAnalysisException e) {
				AnalysisContext.logError("Ooops", e);
			}
		}
		if (handleToArray(obj))
			return;
		consumeStack(obj);
		pushReturnType(obj);
	}

	private boolean handleToArray(InvokeInstruction obj) {
		try {
			TypeFrame frame = getFrame();
			Type topValue = frame.getTopValue();
			if (obj.getName(getCPG()).equals("toArray")) {
				ReferenceType target = obj.getReferenceType(getCPG());
				String signature = obj.getSignature(getCPG());
				if (signature.equals("([Ljava/lang/Object;)[Ljava/lang/Object;") && isCollection(target)) {

					boolean topIsExact = frame.isExact(frame.getStackLocation(0));
					Type resultType = frame.popValue();
					frame.popValue();
					frame.pushValue(resultType);
					frame.setExact(frame.getStackLocation(0), topIsExact);
					return true;
				} else if (signature.equals("()[Ljava/lang/Object;") && isCollection(target)
				        && !topValue.getSignature().equals("Ljava/util/Arrays$ArrayList;")) {
					consumeStack(obj);
					pushReturnType(obj);
					frame.setExact(frame.getStackLocation(0), true);
					return true;
				}
			}
			return false;
		} catch (DataflowAnalysisException e) {
			return false;
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
			return false;
		}
	}

	@Override
	public void handleStoreInstruction(StoreInstruction obj) {
		int numConsumed = obj.consumeStack(cpg);
		if (numConsumed == 1) {
			try {
				boolean isExact = isTopOfStackExact();
				TypeFrame frame = getFrame();
				int index = obj.getIndex();
				Type value = frame.popValue();
				frame.setValue(index, value);
				frame.setExact(index, isExact);
			} catch (DataflowAnalysisException e) {
				throw new InvalidBytecodeException(e.toString());
			}
		} else
			super.handleStoreInstruction(obj);

	}

	/**
	 * Handler for all instructions which load values from a local variable and
	 * push them on the stack. Note that two locals are loaded for long and
	 * double loads.
	 */
	@Override
    public void handleLoadInstruction(LoadInstruction obj) {
		int numProduced = obj.produceStack(cpg);
		if (numProduced == Constants.UNPREDICTABLE)
			throw new InvalidBytecodeException("Unpredictable stack production");

		if (numProduced != 1) {
			super.handleLoadInstruction(obj);
			return;
		}
		int index = obj.getIndex();
		TypeFrame frame = getFrame();
		Type value = frame.getValue(index);
		boolean isExact = frame.isExact(index);
		frame.pushValue(value);
		if (isExact)
			setTopOfStackIsExact();
	}

	private boolean isCollection(ReferenceType target) throws ClassNotFoundException {
		if (Subtypes2.ENABLE_SUBTYPES2) {
			Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
			return subtypes2.isSubtype(target, COLLECTION_TYPE);
		} else {
			return target.isAssignmentCompatibleWith(COLLECTION_TYPE);
		}
	}

	@Override
	public void visitCHECKCAST(CHECKCAST obj) {

		try {
			Type t = getFrame().popValue();
			if (t instanceof NullType)
				pushValue(t);
			else
				pushValue(obj.getType(getCPG()));
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException("Stack underflow for " + obj + ": " + e.getMessage());
		}
	}

	@Override
	public void visitINSTANCEOF(INSTANCEOF obj) {
		if (valueNumberDataflow != null) {
			// Record the value number of the value checked by this instruction,
			// and the type the value was compared to.
			try {
				ValueNumberFrame vnaFrame = valueNumberDataflow.getFactAtLocation(getLocation());
				if (vnaFrame.isValid()) {
					final Type type = obj.getType(getCPG());
					if (type instanceof ReferenceType) {
						instanceOfValueNumber = vnaFrame.getTopValue();
						instanceOfType = (ReferenceType) type;
						sawEffectiveInstanceOf = true;
					}
				}
			} catch (DataflowAnalysisException e) {
				// Ignore
			}
		}

		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitIFNULL(IFNULL obj) {

		if (valueNumberDataflow != null) {
			// Record the value number of the value checked by this instruction,
			// and the type the value was compared to.
			try {
				ValueNumberFrame vnaFrame = valueNumberDataflow.getFactAtLocation(getLocation());
				if (vnaFrame.isValid()) {
					instanceOfValueNumber = vnaFrame.getTopValue();

					instanceOfType = NullType.instance();
					instanceOfFollowedByBranch = true;
				}
			} catch (DataflowAnalysisException e) {
				// Ignore
			}
		}

		consumeStack(obj);
	}

	@Override
	public void visitIFNONNULL(IFNONNULL obj) {

		if (valueNumberDataflow != null) {
			// Record the value number of the value checked by this instruction,
			// and the type the value was compared to.
			try {
				ValueNumberFrame vnaFrame = valueNumberDataflow.getFactAtLocation(getLocation());
				if (vnaFrame.isValid()) {
					instanceOfValueNumber = vnaFrame.getTopValue();

					instanceOfType = NullType.instance();
					instanceOfFollowedByBranch = true;
				}
			} catch (DataflowAnalysisException e) {
				// Ignore
			}
		}

		consumeStack(obj);
	}

	@Override
	public void visitFCMPL(FCMPL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitFCMPG(FCMPG obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitDCMPL(DCMPL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitDCMPG(DCMPG obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLCMP(LCMP obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitD2F(D2F obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitD2I(D2I obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitD2L(D2L obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitF2D(F2D obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitF2I(F2I obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitF2L(F2L obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitI2B(I2B obj) {
		consumeStack(obj);
		pushValue(Type.BYTE);
	}

	@Override
	public void visitI2C(I2C obj) {
		consumeStack(obj);
		pushValue(Type.CHAR);
	}

	@Override
	public void visitI2D(I2D obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitI2F(I2F obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitI2L(I2L obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitI2S(I2S obj) {
	} // no change

	@Override
	public void visitL2D(L2D obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitL2F(L2F obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitL2I(L2I obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitIAND(IAND obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLAND(LAND obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitIOR(IOR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLOR(LOR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitIXOR(IXOR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLXOR(LXOR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitISHR(ISHR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitIUSHR(IUSHR obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLSHR(LSHR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitLUSHR(LUSHR obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitISHL(ISHL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLSHL(LSHL obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitDADD(DADD obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitFADD(FADD obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitIADD(IADD obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLADD(LADD obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitDSUB(DSUB obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitDUP(DUP obj) {
		try {
			TypeFrame frame = getFrame();
			boolean isExact = isTopOfStackExact();
			Type value = frame.popValue();
			frame.pushValue(value);
			if (isExact)
				setTopOfStackIsExact();
			frame.pushValue(value);
			if (isExact)
				setTopOfStackIsExact();
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException(e.toString());
		}
	}

	@Override
	public void visitFSUB(FSUB obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitISUB(ISUB obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLSUB(LSUB obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitDMUL(DMUL obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitFMUL(FMUL obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitIMUL(IMUL obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLMUL(LMUL obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitDDIV(DDIV obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitFDIV(FDIV obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitIDIV(IDIV obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLDIV(LDIV obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitDREM(DREM obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitFREM(FREM obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitIREM(IREM obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLREM(LREM obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitIINC(IINC obj) {
	} // no change to types of stack or locals

	@Override
	public void visitDNEG(DNEG obj) {
	} // no change

	@Override
	public void visitFNEG(FNEG obj) {
	} // no change

	@Override
	public void visitINEG(INEG obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLNEG(LNEG obj) {
	} // no change

	@Override
	public void visitARRAYLENGTH(ARRAYLENGTH obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitAALOAD(AALOAD obj) {
		// To determine the type pushed on the stack,
		// we look at the type of the array reference which was
		// popped off of the stack.
		TypeFrame frame = getFrame();
		try {
			frame.popValue(); // index
			Type arrayType = frame.popValue(); // arrayref
			if (arrayType instanceof ArrayType) {
				ArrayType arr = (ArrayType) arrayType;
				pushValue(arr.getElementType());
			} else {
				pushValue(TypeFrame.getBottomType());
			}
		} catch (DataflowAnalysisException e) {
			throw new InvalidBytecodeException("Stack underflow: " + e.getMessage());
		}
	}

	@Override
	public void visitBALOAD(BALOAD obj) {
		consumeStack(obj);
		pushValue(Type.BYTE);
	}

	@Override
	public void visitCALOAD(CALOAD obj) {
		consumeStack(obj);
		pushValue(Type.CHAR);
	}

	@Override
	public void visitDALOAD(DALOAD obj) {
		consumeStack(obj);
		pushValue(Type.DOUBLE);
	}

	@Override
	public void visitFALOAD(FALOAD obj) {
		consumeStack(obj);
		pushValue(Type.FLOAT);
	}

	@Override
	public void visitIALOAD(IALOAD obj) {
		consumeStack(obj);
		pushValue(Type.INT);
	}

	@Override
	public void visitLALOAD(LALOAD obj) {
		consumeStack(obj);
		pushValue(Type.LONG);
	}

	@Override
	public void visitSALOAD(SALOAD obj) {
		consumeStack(obj);
		pushValue(Type.SHORT);
	}

	// The various xASTORE instructions only consume stack.

	@Override
	public void visitNEW(NEW obj) {
		// FIXME: type is technically "uninitialized"
		// However, we don't model that yet.
		pushValue(obj.getType(getCPG()));

		// We now have an exact type for this value.
		setTopOfStackIsExact();
	}

	@Override
	public void visitNEWARRAY(NEWARRAY obj) {
		consumeStack(obj);
		Type elementType = obj.getType();
		pushValue(elementType);

		// We now have an exact type for this value.
		setTopOfStackIsExact();
	}

	@Override
	public void visitANEWARRAY(ANEWARRAY obj) {
		consumeStack(obj);
		Type elementType = obj.getType(getCPG());
		pushValue(new ArrayType(elementType, 1));

		// We now have an exact type for this value.
		setTopOfStackIsExact();
	}

	@Override
	public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) {
		consumeStack(obj);
		Type elementType = obj.getType(getCPG());
		pushValue(elementType);
		// We now have an exact type for this value.
		setTopOfStackIsExact();
	}

	private void setTopOfStackIsExact() {
		TypeFrame frame = getFrame();
		frame.setExact(frame.getNumSlots() - 1, true);
	}

	private boolean isTopOfStackExact() {
		TypeFrame frame = getFrame();
		return frame.isExact(frame.getNumSlots() - 1);
	}

	@Override
	public void visitJSR(JSR obj) {
		pushValue(ReturnaddressType.NO_TARGET);
	}

	@Override
	public void visitJSR_W(JSR_W obj) {
		pushValue(ReturnaddressType.NO_TARGET);
	}

	@Override
	public void visitRET(RET obj) {
	} // no change

	@Override
	public void visitIFEQ(IFEQ obj) {
		if (previousWasEffectiveInstanceOf)
			instanceOfFollowedByBranch = true;
		super.visitIFEQ(obj);
	}

	@Override
	public void visitIFGT(IFGT obj) {
		if (previousWasEffectiveInstanceOf)
			instanceOfFollowedByBranch = true;
		super.visitIFGT(obj);
	}

	@Override
	public void visitIFLE(IFLE obj) {
		if (previousWasEffectiveInstanceOf)
			instanceOfFollowedByBranch = true;
		super.visitIFLE(obj);
	}

	@Override
	public void visitIFNE(IFNE obj) {
		if (previousWasEffectiveInstanceOf)
			instanceOfFollowedByBranch = true;
		super.visitIFNE(obj);
	}

	public boolean isImpliedByGenericTypes(ReferenceType t) {
		return typesComputedFromGenerics.contains(t);
	}
}

// vim:ts=4

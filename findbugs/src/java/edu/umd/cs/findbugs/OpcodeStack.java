/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2003-2006 University of Maryland
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

package edu.umd.cs.findbugs;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.meta.TypeQualifier;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.classfile.engine.bcel.AnalysisFactory;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.LVTHelper;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * tracks the types and numbers of objects that are currently on the operand
 * stack throughout the execution of method. To use, a detector should
 * instantiate one for each method, and call
 * <p>
 * stack.sawOpcode(this,seen);
 * </p>
 * at the bottom of their sawOpcode method. at any point you can then inspect
 * the stack and see what the types of objects are on the stack, including
 * constant values if they were pushed. The types described are of course, only
 * the static types. There are some outstanding opcodes that have yet to be
 * implemented, I couldn't find any code that actually generated these, so i
 * didn't put them in because I couldn't test them:
 * <ul>
 * <li>dup2_x2</li>
 * <li>jsr_w</li>
 * <li>wide</li>
 * </ul>
 */
public class OpcodeStack implements Constants2 {
    /**
     *
     */
    private static final String JAVA_UTIL_ARRAYS_ARRAY_LIST = "Ljava/util/Arrays$ArrayList;";

    private static final boolean DEBUG = SystemProperties.getBoolean("ocstack.debug");

    private static final boolean DEBUG2 = DEBUG;

    private List<Item> stack;

    private List<Item> lvValues;

    private final List<Integer> lastUpdate;

    private boolean top;

    static class HttpParameterInjection {
        HttpParameterInjection(String parameterName, int pc) {
            this.parameterName = parameterName;
            this.pc = pc;
        }

        String parameterName;

        int pc;
    }

    private boolean seenTransferOfControl = false;

    private final boolean useIterativeAnalysis = AnalysisContext.currentAnalysisContext().getBoolProperty(
            AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS);

    public static class Item {

        @Documented
        @TypeQualifier(applicableTo = Integer.class)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface SpecialKind {
        }

        public static final @SpecialKind
        int NOT_SPECIAL = 0;

        public static final @SpecialKind
        int SIGNED_BYTE = 1;

        public static final @SpecialKind
        int RANDOM_INT = 2;

        public static final @SpecialKind
        int LOW_8_BITS_CLEAR = 3;

        public static final @SpecialKind
        int HASHCODE_INT = 4;

        public static final @SpecialKind
        int INTEGER_SUM = 5;

        public static final @SpecialKind
        int AVERAGE_COMPUTED_USING_DIVISION = 6;

        public static final @SpecialKind
        int FLOAT_MATH = 7;

        public static final @SpecialKind
        int RANDOM_INT_REMAINDER = 8;

        public static final @SpecialKind
        int HASHCODE_INT_REMAINDER = 9;

        public static final @SpecialKind
        int FILE_SEPARATOR_STRING = 10;

        public static final @SpecialKind
        int MATH_ABS = 11;

        public static final @SpecialKind
        int MATH_ABS_OF_RANDOM = 12;

        public static final @SpecialKind
        int MATH_ABS_OF_HASHCODE = 13;

        public static final @SpecialKind
        int NON_NEGATIVE = 14;

        public static final @SpecialKind
        int NASTY_FLOAT_MATH = 15;

        public static final @SpecialKind
        int FILE_OPENED_IN_APPEND_MODE = 16;

        public static final @SpecialKind
        int SERVLET_REQUEST_TAINTED = 17;

        public static final @SpecialKind
        int NEWLY_ALLOCATED = 18;

        public static final @SpecialKind
        int ZERO_MEANS_NULL = 19;

        public static final @SpecialKind
        int NONZERO_MEANS_NULL = 20;

        public static final @SpecialKind
        int RESULT_OF_I2L = 21;

        public static final @SpecialKind
        int RESULT_OF_L2I = 22;

        public static final @SpecialKind
        int SERVLET_OUTPUT = 23;

        public static HashMap<Integer, String> specialKindNames = new HashMap<Integer, String>();

        private static int nextSpecialKind = SERVLET_OUTPUT + 1;

        public static @SpecialKind
        int defineNewSpecialKind(String name) {
            specialKindNames.put(nextSpecialKind, name);
            return nextSpecialKind++;
        }

        private static final int IS_INITIAL_PARAMETER_FLAG = 1;

        private static final int COULD_BE_ZERO_FLAG = 2;

        private static final int IS_NULL_FLAG = 4;

        public static final Object UNKNOWN = null;

        private @SpecialKind
        int specialKind = NOT_SPECIAL;

        private String signature;

        private Object constValue = UNKNOWN;

        private @CheckForNull
        ClassMember source;

        private int pc = -1;

        private int flags;

        private int registerNumber = -1;

        private Object userValue = null;

        private HttpParameterInjection injection = null;

        private int fieldLoadedFromRegister = -1;

        public void makeCrossMethod() {
            pc = -1;
            registerNumber = -1;
            fieldLoadedFromRegister = -1;
        }

        public int getSize() {
            if (signature.equals("J") || signature.equals("D"))
                return 2;
            return 1;
        }

        public int getPC() {
            return pc;
        }

        public void setPC(int pc) {
            this.pc = pc;
        }

        public boolean isWide() {
            return getSize() == 2;
        }

        @Override
        public int hashCode() {
            int r = 42 + specialKind;
            if (signature != null)
                r += signature.hashCode();
            r *= 31;
            if (constValue != null)
                r += constValue.hashCode();
            r *= 31;
            if (source != null)
                r += source.hashCode();
            r *= 31;
            r += flags;
            r *= 31;
            r += registerNumber;
            return r;

        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Item))
                return false;
            Item that = (Item) o;

            return Util.nullSafeEquals(this.signature, that.signature) && Util.nullSafeEquals(this.constValue, that.constValue)
                    && Util.nullSafeEquals(this.source, that.source) && Util.nullSafeEquals(this.userValue, that.userValue)
                    && Util.nullSafeEquals(this.injection, that.injection) && this.specialKind == that.specialKind
                    && this.registerNumber == that.registerNumber && this.flags == that.flags
                    && this.fieldLoadedFromRegister == that.fieldLoadedFromRegister;

        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder("< ");
            buf.append(signature);
            switch (specialKind) {
            case SIGNED_BYTE:
                buf.append(", signed_byte");
                break;
            case RANDOM_INT:
                buf.append(", random_int");
                break;
            case LOW_8_BITS_CLEAR:
                buf.append(", low8clear");
                break;
            case HASHCODE_INT:
                buf.append(", hashcode_int");
                break;
            case INTEGER_SUM:
                buf.append(", int_sum");
                break;
            case AVERAGE_COMPUTED_USING_DIVISION:
                buf.append(", averageComputingUsingDivision");
                break;
            case FLOAT_MATH:
                buf.append(", floatMath");
                break;
            case NASTY_FLOAT_MATH:
                buf.append(", nastyFloatMath");
                break;
            case HASHCODE_INT_REMAINDER:
                buf.append(", hashcode_int_rem");
                break;
            case RANDOM_INT_REMAINDER:
                buf.append(", random_int_rem");
                break;
            case MATH_ABS_OF_RANDOM:
                buf.append(", abs_of_random");
                break;
            case MATH_ABS_OF_HASHCODE:
                buf.append(", abs_of_hashcode");
                break;
            case FILE_SEPARATOR_STRING:
                buf.append(", file_separator_string");
                break;
            case MATH_ABS:
                buf.append(", Math.abs");
                break;
            case NON_NEGATIVE:
                buf.append(", non_negative");
                break;
            case FILE_OPENED_IN_APPEND_MODE:
                buf.append(", file opened in append mode");
                break;
            case SERVLET_REQUEST_TAINTED:
                buf.append(", servlet request tainted");
                break;
            case NEWLY_ALLOCATED:
                buf.append(", new");
                break;
            case ZERO_MEANS_NULL:
                buf.append(", zero means null");
                break;
            case NONZERO_MEANS_NULL:
                buf.append(", nonzero means null");
                break;
            case SERVLET_OUTPUT:
                buf.append(", servlet_output");
                break;

            case NOT_SPECIAL:
                break;
            default:
                buf.append(", #" + specialKind);
                buf.append("(" + specialKindNames.get(specialKind) + ")");
                break;

            }
            if (constValue != UNKNOWN) {
                if (constValue instanceof String) {
                    buf.append(", \"");
                    buf.append(constValue);
                    buf.append("\"");
                } else {
                    buf.append(", ");
                    buf.append(constValue);
                }
            }
            if (source instanceof XField) {
                buf.append(", ");
                if (fieldLoadedFromRegister != -1 && fieldLoadedFromRegister != Integer.MAX_VALUE)
                    buf.append(fieldLoadedFromRegister).append(':');
                buf.append(source);
            }
            if (source instanceof XMethod) {
                buf.append(", return value from ");
                buf.append(source);
            }
            if (isInitialParameter()) {
                buf.append(", IP");
            }
            if (isNull()) {
                buf.append(", isNull");
            }

            if (registerNumber != -1) {
                buf.append(", r");
                buf.append(registerNumber);
            }
            if (isCouldBeZero())
                buf.append(", cbz");
            if (userValue != null) {
                buf.append(", uv: ");
                buf.append(userValue.toString());
            }
            
            buf.append(" >");
            return buf.toString();
        }

        public static Item merge(Item i1, Item i2) {
            if (i1 == null)
                return i2;
            if (i2 == null)
                return i1;
            if (i1.equals(i2))
                return i1;
            Item m = new Item();
            m.flags = i1.flags & i2.flags;
            m.setCouldBeZero(i1.isCouldBeZero() || i2.isCouldBeZero());
            if (i1.pc == i2.pc)
                m.pc = i1.pc;
            if (Util.nullSafeEquals(i1.signature, i2.signature))
                m.signature = i1.signature;
            else if (i1.isNull())
                m.signature = i2.signature;
            else if (i2.isNull())
                m.signature = i1.signature;
            if (Util.nullSafeEquals(i1.constValue, i2.constValue))
                m.constValue = i1.constValue;
            if (Util.nullSafeEquals(i1.source, i2.source)) {
                m.source = i1.source;
            } else if ("".equals(i1.constValue))
                m.source = i2.source;
            else if ("".equals(i2.constValue))
                m.source = i1.source;

            if (Util.nullSafeEquals(i1.userValue, i2.userValue))
                m.userValue = i1.userValue;

            if (i1.registerNumber == i2.registerNumber)
                m.registerNumber = i1.registerNumber;
            if (i1.fieldLoadedFromRegister == i2.fieldLoadedFromRegister)
                m.fieldLoadedFromRegister = i1.fieldLoadedFromRegister;

            if (i1.specialKind == SERVLET_REQUEST_TAINTED) {
                m.specialKind = SERVLET_REQUEST_TAINTED;
                m.injection = i1.injection;
            } else if (i2.specialKind == SERVLET_REQUEST_TAINTED) {
                m.specialKind = SERVLET_REQUEST_TAINTED;
                m.injection = i2.injection;
            } else if (i1.specialKind == i2.specialKind)
                m.specialKind = i1.specialKind;
            else if (i1.specialKind == NASTY_FLOAT_MATH || i2.specialKind == NASTY_FLOAT_MATH)
                m.specialKind = NASTY_FLOAT_MATH;
            else if (i1.specialKind == FLOAT_MATH || i2.specialKind == FLOAT_MATH)
                m.specialKind = FLOAT_MATH;
            if (DEBUG)
                System.out.println("Merge " + i1 + " and " + i2 + " gives " + m);
            return m;
        }

        public Item(String signature, int constValue) {
            this(signature, Integer.valueOf(constValue));
        }

        public Item(String signature) {
            this(signature, UNKNOWN);
        }

        public Item(Item it) {
            this.signature = it.signature;
            this.constValue = it.constValue;
            this.source = it.source;
            this.registerNumber = it.registerNumber;
            this.userValue = it.userValue;
            this.injection = it.injection;
            this.flags = it.flags;
            this.specialKind = it.specialKind;
            this.pc = it.pc;
        }

        public Item(Item it, String signature) {
            this(it);
            this.signature = DescriptorFactory.canonicalizeString(signature);
            if (constValue instanceof Number) {
                Number constantNumericValue = (Number) constValue;
                if (signature.equals("B"))
                    this.constValue = constantNumericValue.byteValue();
                else if (signature.equals("S"))
                    this.constValue = constantNumericValue.shortValue();
                else if (signature.equals("C"))
                    this.constValue = (char) constantNumericValue.intValue();
                else if (signature.equals("I"))
                    this.constValue = constantNumericValue.intValue();

            }
            setSpecialKindFromSignature();
        }

        public Item(Item it, int reg) {
            this(it);
            this.registerNumber = reg;
        }

        public Item(String signature, FieldAnnotation f) {
            this.signature = DescriptorFactory.canonicalizeString(signature);
            setSpecialKindFromSignature();
            if (f != null)
                source = XFactory.createXField(f);
            fieldLoadedFromRegister = -1;
        }

        public Item(String signature, FieldAnnotation f, int fieldLoadedFromRegister) {
            this.signature = DescriptorFactory.canonicalizeString(signature);
            if (f != null)
                source = XFactory.createXField(f);
            this.fieldLoadedFromRegister = fieldLoadedFromRegister;
        }

        /**
         * If this value was loaded from an instance field,
         * give the register number containing the object that the field was loaded from.
         * If Integer.MAX value, the value was loaded from a static field
         * If -1, we don't know or don't have the register containing the object that
         * the field was loaded from.
         * @return
         *
         */
        public int getFieldLoadedFromRegister() {
            return fieldLoadedFromRegister;
        }

        public void setLoadedFromField(XField f, int fieldLoadedFromRegister) {
            source = f;
            this.fieldLoadedFromRegister = fieldLoadedFromRegister;
            this.registerNumber = -1;
        }

        public @CheckForNull
        String getHttpParameterName() {
            if (!isServletParameterTainted())
                throw new IllegalStateException();
            if (injection == null)
                return null;
            return injection.parameterName;
        }

        public int getInjectionPC() {
            if (!isServletParameterTainted())
                throw new IllegalStateException();
            if (injection == null)
                return -1;
            return injection.pc;
        }

        public Item(String signature, Object constantValue) {
            this.signature = DescriptorFactory.canonicalizeString(signature);
            setSpecialKindFromSignature();
            constValue = constantValue;
            if (constantValue instanceof Integer) {
                int value = ((Integer) constantValue).intValue();
                if (value != 0 && (value & 0xff) == 0)
                    specialKind = LOW_8_BITS_CLEAR;
                if (value == 0)
                    setCouldBeZero(true);

            } else if (constantValue instanceof Long) {
                long value = ((Long) constantValue).longValue();
                if (value != 0 && (value & 0xff) == 0)
                    specialKind = LOW_8_BITS_CLEAR;
                if (value == 0)
                    setCouldBeZero(true);
            }

        }

        private void setSpecialKindFromSignature() {
            if (false && specialKind != NOT_SPECIAL)
                return;
            if (signature.equals("B"))
                specialKind = SIGNED_BYTE;
            else if (signature.equals("C"))
                specialKind = NON_NEGATIVE;
        }

        public Item() {
            signature = "Ljava/lang/Object;";
            constValue = null;
            setNull(true);
        }

        public static Item nullItem(String signature) {
            Item item = new Item(signature);
            item.constValue = null;
            item.setNull(true);
            return item;
        }

        /** Returns null for primitive and arrays */
        public @CheckForNull
        JavaClass getJavaClass() throws ClassNotFoundException {
            String baseSig;

            if (isPrimitive() || isArray())
                return null;

            baseSig = signature;

            if (baseSig.length() == 0)
                return null;
            baseSig = baseSig.substring(1, baseSig.length() - 1);
            baseSig = baseSig.replace('/', '.');
            return Repository.lookupClass(baseSig);
        }

        public boolean isArray() {
            return signature.startsWith("[");
        }

        @Deprecated
        public String getElementSignature() {
            if (!isArray())
                return signature;
            else {
                int pos = 0;
                int len = signature.length();
                while (pos < len) {
                    if (signature.charAt(pos) != '[')
                        break;
                    pos++;
                }
                return signature.substring(pos);
            }
        }

        public boolean isNonNegative() {
            if (specialKind == NON_NEGATIVE)
                return true;
            if (constValue instanceof Number) {
                double value = ((Number) constValue).doubleValue();
                return value >= 0;
            }
            return false;
        }

        public boolean isPrimitive() {
            return !signature.startsWith("L") && !signature.startsWith("[");
        }

        public int getRegisterNumber() {
            return registerNumber;
        }

        public String getSignature() {
            return signature;
        }

        /**
         * Returns a constant value for this Item, if known. NOTE: if the value
         * is a constant Class object, the constant value returned is the name
         * of the class.
         */
        public Object getConstant() {
            return constValue;
        }

        /** Use getXField instead */
        @Deprecated
        public FieldAnnotation getFieldAnnotation() {
            return FieldAnnotation.fromXField(getXField());
        }

        public XField getXField() {
            if (source instanceof XField)
                return (XField) source;
            return null;
        }

        /**
         * @param specialKind
         *            The specialKind to set.
         */
        public void setSpecialKind(@SpecialKind int specialKind) {
            this.specialKind = specialKind;
        }

        public Item cloneAndSetSpecialKind(@SpecialKind int specialKind) {
            Item that = new Item(this);
            that.specialKind = specialKind;
            return that;
        }

        /**
         * @return Returns the specialKind.
         */
        public @SpecialKind
        int getSpecialKind() {
            return specialKind;
        }

        /**
         * @return Returns the specialKind.
         */
        public boolean isBooleanNullnessValue() {
            return specialKind == ZERO_MEANS_NULL || specialKind == NONZERO_MEANS_NULL;
        }

        /**
         * attaches a detector specified value to this item
         *
         * @param value
         *            the custom value to set
         */
        public void setUserValue(Object value) {
            userValue = value;
        }

        /**
         *
         * @return if this value is the return value of a method, give the
         *         method invoked
         */
        public @CheckForNull
        XMethod getReturnValueOf() {
            if (source instanceof XMethod)
                return (XMethod) source;
            return null;
        }

        public boolean couldBeZero() {
            return isCouldBeZero();
        }

        public boolean mustBeZero() {
            Object value = getConstant();
            return value instanceof Number && ((Number) value).intValue() == 0;
        }

        /**
         * gets the detector specified value for this item
         *
         * @return the custom value
         */
        public Object getUserValue() {
            return userValue;
        }

        public boolean isServletParameterTainted() {
            return getSpecialKind() == Item.SERVLET_REQUEST_TAINTED;
        }

        public void setServletParameterTainted() {
            setSpecialKind(Item.SERVLET_REQUEST_TAINTED);
        }
        public void setIsServletWriter() {
            setSpecialKind(Item.SERVLET_OUTPUT);
        }


        public  boolean isServletWriter() {
            if (getSpecialKind() == Item.SERVLET_OUTPUT)
                return true;
            if (getSignature().equals("Ljavax/servlet/ServletOutputStream;"))
                return true;
            XMethod writingToSource = getReturnValueOf();


            return writingToSource != null && writingToSource.getClassName().equals("javax.servlet.http.HttpServletResponse")
                    && (writingToSource.getName().equals("getWriter") || writingToSource.getName().equals("getOutputStream"));
        }

        public boolean valueCouldBeNegative() {
            return !isNonNegative()
                    && (getSpecialKind() == Item.RANDOM_INT || getSpecialKind() == Item.SIGNED_BYTE
                            || getSpecialKind() == Item.HASHCODE_INT || getSpecialKind() == Item.RANDOM_INT_REMAINDER
                            || getSpecialKind() == Item.HASHCODE_INT_REMAINDER || getSpecialKind() == Item.MATH_ABS_OF_RANDOM || getSpecialKind() == Item.MATH_ABS_OF_HASHCODE);

        }

        public @SpecialKind int getSpecialKindForAbs() {
            switch (getSpecialKind()) {
            case Item.HASHCODE_INT:
                return Item.MATH_ABS_OF_HASHCODE;
            case Item.RANDOM_INT:
                return Item.MATH_ABS_OF_RANDOM;
            default:
                return Item.MATH_ABS;
            }
        }

        public @SpecialKind int getSpecialKindForRemainder() {
            switch (getSpecialKind()) {
            case Item.HASHCODE_INT:
                return Item.HASHCODE_INT_REMAINDER;
            case Item.RANDOM_INT:
                return Item.RANDOM_INT_REMAINDER;
            default:
                return Item.NOT_SPECIAL;
            }
        }

        /** Value could be Integer.MIN_VALUE */
        public boolean checkForIntegerMinValue() {
            return !isNonNegative() && (getSpecialKind() == Item.RANDOM_INT || getSpecialKind() == Item.HASHCODE_INT);
        }

        /** The result of applying Math.abs to a checkForIntegerMinValue() value */
        public boolean mightRarelyBeNegative() {
            return !isNonNegative()
                    && (getSpecialKind() == Item.MATH_ABS_OF_RANDOM || getSpecialKind() == Item.MATH_ABS_OF_HASHCODE);
        }

        /**
         * @param isInitialParameter
         *            The isInitialParameter to set.
         */
        private void setInitialParameter(boolean isInitialParameter) {
            setFlag(isInitialParameter, IS_INITIAL_PARAMETER_FLAG);
        }

        /**
         * @return Returns the isInitialParameter.
         */
        public boolean isInitialParameter() {
            return (flags & IS_INITIAL_PARAMETER_FLAG) != 0;
        }

        /**
         * @param couldBeZero
         *            The couldBeZero to set.
         */
        private void setCouldBeZero(boolean couldBeZero) {
            setFlag(couldBeZero, COULD_BE_ZERO_FLAG);
        }

        /**
         * @return Returns the couldBeZero.
         */
        private boolean isCouldBeZero() {
            return (flags & COULD_BE_ZERO_FLAG) != 0;
        }

        /**
         * @param isNull
         *            The isNull to set.
         */
        private void setNull(boolean isNull) {
            setFlag(isNull, IS_NULL_FLAG);
        }

        private void setFlag(boolean value, int flagBit) {
            if (value)
                flags |= flagBit;
            else
                flags &= ~flagBit;
        }

        /**
         * @return Returns the isNull.
         */
        public boolean isNull() {
            return (flags & IS_NULL_FLAG) != 0;
        }

        /**
         *
         */
        public void clearNewlyAllocated() {
            if (specialKind == NEWLY_ALLOCATED) {
                if (signature.startsWith("Ljava/lang/StringB"))
                    constValue = null;
                specialKind = NOT_SPECIAL;
            }
        }

        public boolean isNewlyAllocated() {
            return specialKind == NEWLY_ALLOCATED;
        }

        /**
         * @param i
         * @return
         */
        public boolean hasConstantValue(int value) {
            if (constValue instanceof Number)
                return ((Number) constValue).intValue() == value;
            return false;
        }

        public boolean hasConstantValue(long value) {
            if (constValue instanceof Number)
                return ((Number) constValue).longValue() == value;
            return false;
        }
    }

    @Override
    public String toString() {
        if (isTop())
            return "TOP";
        return stack.toString() + "::" + lvValues.toString();
    }

    public OpcodeStack() {
        stack = new ArrayList<Item>();
        lvValues = new ArrayList<Item>();
        lastUpdate = new ArrayList<Integer>();
    }

    public boolean hasIncomingBranches(int pc) {
        return jumpEntryLocations.get(pc) && jumpEntries.get(pc) != null;

    }

    boolean needToMerge = true;

    private boolean reachOnlyByBranch = false;

    public static String getExceptionSig(DismantleBytecode dbc, CodeException e) {
        if (e.getCatchType() == 0)
            return "Ljava/lang/Throwable;";
        Constant c = dbc.getConstantPool().getConstant(e.getCatchType());
        if (c instanceof ConstantClass)
            return "L" + ((ConstantClass) c).getBytes(dbc.getConstantPool()) + ";";
        return "Ljava/lang/Throwable;";
    }

    public void mergeJumps(DismantleBytecode dbc) {
        if (!needToMerge)
            return;
        needToMerge = false;
        if (dbc.getPC() == zeroOneComing) {
            pop();
            top = false;
            OpcodeStack.Item item = new Item("I");
            if (oneMeansNull)
                item.setSpecialKind(Item.NONZERO_MEANS_NULL);
            else
                item.setSpecialKind(Item.ZERO_MEANS_NULL);
            item.setPC(dbc.getPC() - 8);
            item.setCouldBeZero(true);

            push(item);

            zeroOneComing = -1;
            if (DEBUG)
                System.out.println("Updated to " + this);
            return;
        }

        boolean stackUpdated = false;
        if (!isTop() && (convertJumpToOneZeroState == 3 || convertJumpToZeroOneState == 3)) {
            pop();
            Item topItem = new Item("I");
            topItem.setCouldBeZero(true);
            push(topItem);
            convertJumpToOneZeroState = convertJumpToZeroOneState = 0;
            stackUpdated = true;
        }

        List<Item> jumpEntry = null;
        if (jumpEntryLocations.get(dbc.getPC()))
            jumpEntry = jumpEntries.get(Integer.valueOf(dbc.getPC()));
        if (jumpEntry != null) {
            setReachOnlyByBranch(false);
            List<Item> jumpStackEntry = jumpStackEntries.get(Integer.valueOf(dbc.getPC()));
            
            if (DEBUG2) {
                System.out.println("XXXXXXX " + isReachOnlyByBranch());
                System.out.println("merging lvValues at jump target " + dbc.getPC() + " -> " + jumpEntry);
                System.out.println(" current lvValues " + lvValues);
                System.out.println(" merging stack entry " + jumpStackEntry);
                System.out.println(" current stack values " + stack);

            }
            if (isTop()) {
                lvValues = new ArrayList<Item>(jumpEntry);
                if (jumpStackEntry != null)
                    stack = new ArrayList<Item>(jumpStackEntry);
                else
                    stack.clear();
                setTop(false);
                return;
            }
            if (isReachOnlyByBranch()) {
                setTop(false);
                lvValues = new ArrayList<Item>(jumpEntry);
                if (!stackUpdated) {
                    if (jumpStackEntry != null)
                        stack = new ArrayList<Item>(jumpStackEntry);
                    else
                        stack.clear();
                }

            } else {
                setTop(false);
                mergeLists(lvValues, jumpEntry, false);
                if (!stackUpdated && jumpStackEntry != null)
                    mergeLists(stack, jumpStackEntry, false);
            }
            if (DEBUG)
                System.out.println(" merged lvValues " + lvValues);
        } else if (isReachOnlyByBranch() && !stackUpdated) {
            stack.clear();

            for (CodeException e : dbc.getCode().getExceptionTable()) {
                if (e.getHandlerPC() == dbc.getPC()) {
                    push(new Item(getExceptionSig(dbc, e)));
                    setReachOnlyByBranch(false);
                    setTop(false);
                    return;

                }
            }
            setTop(true);
        }

    }

    int convertJumpToOneZeroState = 0;

    int convertJumpToZeroOneState = 0;

    int registerTestedFoundToBeNonnegative = -1;

    private void setLastUpdate(int reg, int pc) {
        while (lastUpdate.size() <= reg)
            lastUpdate.add(Integer.valueOf(0));
        lastUpdate.set(reg, Integer.valueOf(pc));
    }

    public int getLastUpdate(int reg) {
        if (lastUpdate.size() <= reg)
            return 0;
        return lastUpdate.get(reg).intValue();
    }

    public int getNumLastUpdates() {
        return lastUpdate.size();
    }

    int zeroOneComing = -1;

    boolean oneMeansNull;

    public void sawOpcode(DismantleBytecode dbc, int seen) {
        int register;
        String signature;
        Item it, it2;
        Constant cons;

        // System.out.printf("%3d %12s%s%n", dbc.getPC(), OPCODE_NAMES[seen],
        // this);
        if (dbc.isRegisterStore())
            setLastUpdate(dbc.getRegisterOperand(), dbc.getPC());

        precomputation(dbc);
        needToMerge = true;
        try {
            if (isTop()) {
                encountedTop = true;
                return;
            }

            if (seen == GOTO) {
                int nextPC = dbc.getPC() + 3;
                if (nextPC <= dbc.getMaxPC()) {

                    int prevOpcode1 = dbc.getPrevOpcode(1);
                    int prevOpcode2 = dbc.getPrevOpcode(2);
                    try {
                        int nextOpcode = dbc.getCodeByte(dbc.getPC() + 3);

                        if ((prevOpcode1 == ICONST_0 || prevOpcode1 == ICONST_1)
                                && (prevOpcode2 == IFNULL || prevOpcode2 == IFNONNULL)
                                && (nextOpcode == ICONST_0 || nextOpcode == ICONST_1) && prevOpcode1 != nextOpcode) {
                            oneMeansNull = prevOpcode1 == ICONST_0;
                            if (prevOpcode2 != IFNULL)
                                oneMeansNull = !oneMeansNull;
                            zeroOneComing = nextPC + 1;
                            convertJumpToOneZeroState = convertJumpToZeroOneState = 0;
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw e; // throw new
                                 // ArrayIndexOutOfBoundsException(nextPC + " "
                                 // + dbc.getMaxPC());
                    }
                }
            }

            switch (seen) {
            case ICONST_1:
                convertJumpToOneZeroState = 1;
                break;
            case GOTO:
                if (convertJumpToOneZeroState == 1 && dbc.getBranchOffset() == 4)
                    convertJumpToOneZeroState = 2;
                else
                    convertJumpToOneZeroState = 0;
                break;
            case ICONST_0:
                if (convertJumpToOneZeroState == 2)
                    convertJumpToOneZeroState = 3;
                else
                    convertJumpToOneZeroState = 0;
                break;
            default:
                convertJumpToOneZeroState = 0;

            }
            switch (seen) {
            case ICONST_0:
                convertJumpToZeroOneState = 1;
                break;
            case GOTO:
                if (convertJumpToZeroOneState == 1 && dbc.getBranchOffset() == 4)
                    convertJumpToZeroOneState = 2;
                else
                    convertJumpToZeroOneState = 0;
                break;
            case ICONST_1:
                if (convertJumpToZeroOneState == 2)
                    convertJumpToZeroOneState = 3;
                else
                    convertJumpToZeroOneState = 0;
                break;
            default:
                convertJumpToZeroOneState = 0;
            }

            switch (seen) {
            case ALOAD:
                pushByLocalObjectLoad(dbc, dbc.getRegisterOperand());
                break;

            case ALOAD_0:
            case ALOAD_1:
            case ALOAD_2:
            case ALOAD_3:
                pushByLocalObjectLoad(dbc, seen - ALOAD_0);
                break;

            case DLOAD:
                pushByLocalLoad("D", dbc.getRegisterOperand());
                break;

            case DLOAD_0:
            case DLOAD_1:
            case DLOAD_2:
            case DLOAD_3:
                pushByLocalLoad("D", seen - DLOAD_0);
                break;

            case FLOAD:
                pushByLocalLoad("F", dbc.getRegisterOperand());
                break;

            case FLOAD_0:
            case FLOAD_1:
            case FLOAD_2:
            case FLOAD_3:
                pushByLocalLoad("F", seen - FLOAD_0);
                break;

            case ILOAD:
                pushByLocalLoad("I", dbc.getRegisterOperand());
                break;

            case ILOAD_0:
            case ILOAD_1:
            case ILOAD_2:
            case ILOAD_3:
                pushByLocalLoad("I", seen - ILOAD_0);
                break;

            case LLOAD:
                pushByLocalLoad("J", dbc.getRegisterOperand());
                break;

            case LLOAD_0:
            case LLOAD_1:
            case LLOAD_2:
            case LLOAD_3:
                pushByLocalLoad("J", seen - LLOAD_0);
                break;

            case GETSTATIC: {
                FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();
                XField fieldOperand = dbc.getXFieldOperand();

                if (fieldOperand != null && fieldSummary.isComplete() && !fieldOperand.isPublic()) {
                    OpcodeStack.Item item = fieldSummary.getSummary(fieldOperand);
                    if (item != null) {
                        Item itm = new Item(item);
                        itm.setLoadedFromField(fieldOperand, Integer.MAX_VALUE);
                        push(itm);
                        break;
                    }
                }
                FieldAnnotation field = FieldAnnotation.fromReferencedField(dbc);
                Item i = new Item(dbc.getSigConstantOperand(), field, Integer.MAX_VALUE);
                if (field.getFieldName().equals("separator") && field.getClassName().equals("java.io.File")) {
                    i.setSpecialKind(Item.FILE_SEPARATOR_STRING);
                }

                push(i);
                break;
            }

            case LDC:
            case LDC_W:
            case LDC2_W:
                cons = dbc.getConstantRefOperand();
                pushByConstant(dbc, cons);
                break;

            case INSTANCEOF:
                pop();
                push(new Item("I"));
                break;

            case IFNONNULL:
            case IFNULL:
                // {
                // Item topItem = pop();
                // if (seen == IFNONNULL && topItem.isNull())
                // break;
                // seenTransferOfControl = true;
                // addJumpValue(dbc.getPC(), dbc.getBranchTarget());
                //
                // break;
                // }

            case IFEQ:
            case IFNE:
            case IFLT:
            case IFLE:
            case IFGT:
            case IFGE:

                seenTransferOfControl = true;
                {
                    Item topItem = pop();

                    // System.out.printf("%4d %10s %s%n",
                    // dbc.getPC(),OPCODE_NAMES[seen], topItem);
                    if (seen == IFLT || seen == IFLE) {
                        registerTestedFoundToBeNonnegative = topItem.registerNumber;
                    }
                    // if we see a test comparing a special negative value with
                    // 0,
                    // reset all other such values on the opcode stack
                    if (topItem.valueCouldBeNegative() && (seen == IFLT || seen == IFLE || seen == IFGT || seen == IFGE)) {
                        int specialKind = topItem.getSpecialKind();
                        for (Item item : stack)
                            if (item != null && item.getSpecialKind() == specialKind)
                                item.setSpecialKind(Item.NOT_SPECIAL);
                        for (Item item : lvValues)
                            if (item != null && item.getSpecialKind() == specialKind)
                                item.setSpecialKind(Item.NOT_SPECIAL);

                    }
                }
                addJumpValue(dbc.getPC(), dbc.getBranchTarget());

                break;
            case LOOKUPSWITCH:

            case TABLESWITCH:
                seenTransferOfControl = true;
                setReachOnlyByBranch(true);
                pop();
                addJumpValue(dbc.getPC(), dbc.getBranchTarget());
                int pc = dbc.getBranchTarget() - dbc.getBranchOffset();
                for (int offset : dbc.getSwitchOffsets())
                    addJumpValue(dbc.getPC(), offset + pc);

                break;
            case ARETURN:
            case DRETURN:
            case FRETURN:

            case IRETURN:
            case LRETURN:

                seenTransferOfControl = true;
                setReachOnlyByBranch(true);
                pop();
                break;
            case MONITORENTER:
            case MONITOREXIT:
            case POP:
            case PUTSTATIC:
                pop();
                break;

            case IF_ACMPEQ:
            case IF_ACMPNE:
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPLE:
            case IF_ICMPGT:
            case IF_ICMPGE:

            {
                seenTransferOfControl = true;
                Item right = pop();
                Item left = pop();
                
                Object lConstant = left.getConstant();
                Object rConstant = right.getConstant();
                if (lConstant instanceof Integer && rConstant instanceof Integer) {
                    boolean takeJump = false;
                    boolean handled = false;
                    int lC = ((Integer)lConstant).intValue();
                    int rC = ((Integer)rConstant).intValue();
                    switch(seen) {
                    case IF_ICMPEQ:
                        takeJump = lC == rC;
                        handled = true;
                        break;
                    case IF_ICMPNE:
                        takeJump = lC != rC;
                        handled = true;
                        break;
                    case IF_ICMPGE:
                    	 takeJump = lC >= rC;
                         handled = true;
                         break;
                    case IF_ICMPGT:
                        takeJump = lC > rC;
                        handled = true;
                        break;
                    case IF_ICMPLE:
                        takeJump = lC <= rC;
                        handled = true;
                        break;
                   case IF_ICMPLT:
                       takeJump = lC < rC;
                       handled = true;
                       break;
                    }
                    if (handled) {
                        if (takeJump) {
                            int branchTarget = dbc.getBranchTarget();
                            addJumpValue(dbc.getPC(), branchTarget);
                            setTop(true);
                            break;
                        } else {
                            break;
                        }
                    }
                    
                }
                if (right.hasConstantValue(Integer.MIN_VALUE) && left.mightRarelyBeNegative()
                        || left.hasConstantValue(Integer.MIN_VALUE) && right.mightRarelyBeNegative()) {
                    for (Item i : stack)
                        if (i != null && i.mightRarelyBeNegative())
                            i.setSpecialKind(Item.NOT_SPECIAL);
                    for (Item i : lvValues)
                        if (i != null && i.mightRarelyBeNegative())
                            i.setSpecialKind(Item.NOT_SPECIAL);
                }
                int branchTarget = dbc.getBranchTarget();
                addJumpValue(dbc.getPC(), branchTarget);
                break;
            }

            case POP2:
                it = pop();
                if (it.getSize() == 1)
                    pop();
                break;
            case PUTFIELD:
                pop(2);
                break;

            case IALOAD:
            case SALOAD:
                pop(2);
                push(new Item("I"));
                break;

            case DUP:
                handleDup();
                break;

            case DUP2:
                handleDup2();
                break;

            case DUP_X1:
                handleDupX1();
                break;

            case DUP_X2:

                handleDupX2();
                break;

            case DUP2_X1:
                handleDup2X1();
                break;

            case DUP2_X2:
                handleDup2X2();
                break;

            case IINC:
                register = dbc.getRegisterOperand();
                it = getLVValue(register);
                it2 = new Item("I", dbc.getIntConstant());
                pushByIntMath(dbc, IADD, it2, it);
                pushByLocalStore(register);
                break;

            case ATHROW:
                pop();
                seenTransferOfControl = true;
                setReachOnlyByBranch(true);
                setTop(true);
                break;

            case CHECKCAST: {
                String castTo = dbc.getClassConstantOperand();

                if (castTo.charAt(0) != '[')
                    castTo = "L" + castTo + ";";
                it = pop();

                if (!it.signature.equals(castTo)) {
                    it = new Item(it, castTo);
                }
                push(it);
                break;

            }
            case NOP:
                break;
            case RET:
            case RETURN:
                seenTransferOfControl = true;
                setReachOnlyByBranch(true);
                break;

            case GOTO:
            case GOTO_W:
                seenTransferOfControl = true;
                setReachOnlyByBranch(true);
                addJumpValue(dbc.getPC(), dbc.getBranchTarget());
                stack.clear();
                setTop(true);

                break;

            case SWAP:
                handleSwap();
                break;

            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
                push(new Item("I", (seen - ICONST_0)));
                break;

            case LCONST_0:
            case LCONST_1:
                push(new Item("J", Long.valueOf(seen - LCONST_0)));
                break;

            case DCONST_0:
            case DCONST_1:
                push(new Item("D", Double.valueOf(seen - DCONST_0)));
                break;

            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                push(new Item("F", Float.valueOf(seen - FCONST_0)));
                break;

            case ACONST_NULL:
                push(new Item());
                break;

            case ASTORE:
            case DSTORE:
            case FSTORE:
            case ISTORE:
            case LSTORE:
                pushByLocalStore(dbc.getRegisterOperand());
                break;

            case ASTORE_0:
            case ASTORE_1:
            case ASTORE_2:
            case ASTORE_3:
                pushByLocalStore(seen - ASTORE_0);
                break;

            case DSTORE_0:
            case DSTORE_1:
            case DSTORE_2:
            case DSTORE_3:
                pushByLocalStore(seen - DSTORE_0);
                break;

            case FSTORE_0:
            case FSTORE_1:
            case FSTORE_2:
            case FSTORE_3:
                pushByLocalStore(seen - FSTORE_0);
                break;

            case ISTORE_0:
            case ISTORE_1:
            case ISTORE_2:
            case ISTORE_3:
                pushByLocalStore(seen - ISTORE_0);
                break;

            case LSTORE_0:
            case LSTORE_1:
            case LSTORE_2:
            case LSTORE_3:
                pushByLocalStore(seen - LSTORE_0);
                break;

            case GETFIELD: {
                FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();
                XField fieldOperand = dbc.getXFieldOperand();
                if (fieldOperand != null && fieldSummary.isComplete() && !fieldOperand.isPublic()) {
                    OpcodeStack.Item item = fieldSummary.getSummary(fieldOperand);
                    if (item != null) {
                        Item addr = pop();
                        Item itm = new Item(item);
                        itm.setLoadedFromField(fieldOperand, addr.getRegisterNumber());
                        push(itm);
                        break;
                    }
                }
                Item item = pop();
                int reg = item.getRegisterNumber();
                push(new Item(dbc.getSigConstantOperand(), FieldAnnotation.fromReferencedField(dbc), reg));

            }
                break;

            case ARRAYLENGTH: {
                pop();
                Item newItem = new Item("I");
                newItem.setSpecialKind(Item.NON_NEGATIVE);
                push(newItem);
            }
                break;

            case BALOAD: {
                pop(2);
                Item newItem = new Item("I");
                newItem.setSpecialKind(Item.SIGNED_BYTE);
                push(newItem);
                break;
            }
            case CALOAD:
                pop(2);
                push(new Item("I"));
                break;

            case DALOAD:
                pop(2);
                push(new Item("D"));
                break;

            case FALOAD:
                pop(2);
                push(new Item("F"));
                break;

            case LALOAD:
                pop(2);
                push(new Item("J"));
                break;

            case AASTORE:
            case BASTORE:
            case CASTORE:
            case DASTORE:
            case FASTORE:
            case IASTORE:
            case LASTORE:
            case SASTORE:
                pop(3);
                break;

            case BIPUSH:
            case SIPUSH:
                push(new Item("I", Integer.valueOf(dbc.getIntConstant())));
                break;

            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IAND:
            case IOR:
            case IXOR:
            case ISHL:
            case ISHR:
            case IREM:
            case IUSHR:
                it = pop();
                it2 = pop();
                pushByIntMath(dbc, seen, it2, it);
                break;

            case INEG:
                it = pop();
                if (it.getConstant() instanceof Integer) {
                    push(new Item("I", Integer.valueOf(-constantToInt(it))));
                } else {
                    push(new Item("I"));
                }
                break;

            case LNEG:
                it = pop();
                if (it.getConstant() instanceof Long) {
                    push(new Item("J", Long.valueOf(-constantToLong(it))));
                } else {
                    push(new Item("J"));
                }
                break;
            case FNEG:
                it = pop();
                if (it.getConstant() instanceof Float) {
                    push(new Item("F", Float.valueOf(-constantToFloat(it))));
                } else {
                    push(new Item("F"));
                }
                break;
            case DNEG:
                it = pop();
                if (it.getConstant() instanceof Double) {
                    push(new Item("D", Double.valueOf(-constantToDouble(it))));
                } else {
                    push(new Item("D"));
                }
                break;

            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LAND:
            case LOR:
            case LXOR:
            case LSHL:
            case LSHR:
            case LREM:
            case LUSHR:

                it = pop();
                it2 = pop();
                pushByLongMath(seen, it2, it);
                break;

            case LCMP:
                handleLcmp();
                break;

            case FCMPG:
            case FCMPL:
                handleFcmp(seen);
                break;

            case DCMPG:
            case DCMPL:
                handleDcmp(seen);
                break;

            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
                it = pop();
                it2 = pop();
                pushByFloatMath(seen, it, it2);
                break;

            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                it = pop();
                it2 = pop();
                pushByDoubleMath(seen, it, it2);
                break;

            case I2B: {
                it = pop();
                Item newValue = new Item(it, "B");

                push(newValue);
            }
                break;



            case I2C: {
                it = pop();
                Item newValue = new Item(it, "C");

                push(newValue);
            }
                break;

            case I2L:
            case D2L:
            case F2L: {
                it = pop();
                Item newValue = new Item(it, "J");

                int specialKind = it.getSpecialKind();

                if (specialKind != Item.SIGNED_BYTE && seen == I2L)
                    newValue.setSpecialKind(Item.RESULT_OF_I2L);

                push(newValue);
            }
            break;

            case I2S:
                changeSignatureOfTopElement("S");
                break;

            case L2I:
            case D2I:
            case F2I:
                it = pop();
                int oldSpecialKind = it.getSpecialKind();
                it = new Item(it, "I");

                if (oldSpecialKind == Item.NOT_SPECIAL)
                    it.setSpecialKind(Item.RESULT_OF_L2I);
                push(it);

                break;

            case L2F:
            case D2F:
            case I2F:
                it = pop();
                if (it.getConstant() != null) {
                    push(new Item("F", Float.valueOf(constantToFloat(it))));
                } else {
                    push(new Item("F"));
                }
                break;

            case F2D:
            case I2D:
            case L2D:
                it = pop();
                if (it.getConstant() != null) {
                    push(new Item("D", Double.valueOf(constantToDouble(it))));
                } else {
                    push(new Item("D"));
                }
                break;

            case NEW: {
                Item item = new Item("L" + dbc.getClassConstantOperand() + ";", (Object) null);
                item.setSpecialKind(Item.NEWLY_ALLOCATED);
                push(item);
            }
                break;

            case NEWARRAY:
                pop();
                signature = "[" + BasicType.getType((byte) dbc.getIntConstant()).getSignature();
                pushBySignature(signature, dbc);
                break;

            // According to the VM Spec 4.4.1, anewarray and multianewarray
            // can refer to normal class/interface types (encoded in
            // "internal form"), or array classes (encoded as signatures
            // beginning with "[").

            case ANEWARRAY:
                pop();
                signature = dbc.getClassConstantOperand();
                if (signature.charAt(0) == '[')
                    signature = "[" + signature;
                else
                    signature = "[L" + signature + ";";
                pushBySignature(signature, dbc);
                break;

            case MULTIANEWARRAY:
                int dims = dbc.getIntConstant();
                for (int i = 0; i < dims; i++)
                    pop();

                signature = dbc.getClassConstantOperand();
                pushBySignature(signature, dbc);
                break;

            case AALOAD: {
                pop();
                it = pop();
                String arraySig = it.getSignature();
                if (arraySig.charAt(0) == '[')
                    pushBySignature(arraySig.substring(1), dbc);
                else
                    push(new Item());
            }
                break;

            case JSR:
                seenTransferOfControl = true;
                setReachOnlyByBranch(false);
                push(new Item("")); // push return address on stack
                addJumpValue(dbc.getPC(), dbc.getBranchTarget());
                pop();
                if (dbc.getBranchOffset() < 0) {
                    // OK, backwards JSRs are weird; reset the stack.
                    int stackSize = stack.size();
                    stack.clear();
                    for (int i = 0; i < stackSize; i++)
                        stack.add(new Item());
                }
                setTop(false);
                break;

            case INVOKEINTERFACE:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEVIRTUAL:
                processMethodCall(dbc, seen);
                break;

            default:
                throw new UnsupportedOperationException("OpCode " + OPCODE_NAMES[seen] + " not supported ");
            }
        }

        catch (RuntimeException e) {
            // If an error occurs, we clear the stack and locals. one of two
            // things will occur.
            // Either the client will expect more stack items than really exist,
            // and so they're condition check will fail,
            // or the stack will resync with the code. But hopefully not false
            // positives

            String msg = "Error processing opcode " + OPCODE_NAMES[seen] + " @ " + dbc.getPC() + " in "
                    + dbc.getFullyQualifiedMethodName();
            AnalysisContext.logError(msg, e);
            if (DEBUG)
                e.printStackTrace();
            clear();
        } finally {
            if (DEBUG) {
                System.out.println(dbc.getNextPC() + "pc : " + OPCODE_NAMES[seen] + "  stack depth: " + getStackDepth());
                System.out.println(this);
            }
        }
    }

    /**
     *
     */
    private void changeSignatureOfTopElement(String newSignature) {
        {
            Item item = pop();
            Item newValue = new Item(item, newSignature);

            push(newValue);
        }
    }

    public void precomputation(DismantleBytecode dbc) {
        if (registerTestedFoundToBeNonnegative >= 0) {
            for (int i = 0; i < stack.size(); i++) {
                Item item = stack.get(i);
                if (item != null && item.registerNumber == registerTestedFoundToBeNonnegative)
                    stack.set(i, item.cloneAndSetSpecialKind(Item.NON_NEGATIVE));
            }
            for (int i = 0; i < lvValues.size(); i++) {
                Item item = lvValues.get(i);
                if (item != null && item.registerNumber == registerTestedFoundToBeNonnegative)
                    lvValues.set(i, item.cloneAndSetSpecialKind(Item.NON_NEGATIVE));
            }
        }
        registerTestedFoundToBeNonnegative = -1;
        mergeJumps(dbc);
    }

    /**
     * @param it
     * @return
     */
    private int constantToInt(Item it) {
        Object constant = it.getConstant();
        if (constant instanceof Number) {
            return ((Number) constant).intValue();
        }
        if (constant instanceof Character) {
            return ((Character) constant).charValue();
        }
        throw new IllegalArgumentException(String.valueOf(constant));
    }

    /**
     * @param it
     * @return
     */
    private float constantToFloat(Item it) {
        return ((Number) it.getConstant()).floatValue();
    }

    /**
     * @param it
     * @return
     */
    private double constantToDouble(Item it) {
        return ((Number) it.getConstant()).doubleValue();
    }

    /**
     * @param it
     * @return
     */
    private long constantToLong(Item it) {
        return ((Number) it.getConstant()).longValue();
    }

    /**
     * handle dcmp
     *
     */
    private void handleDcmp(int opcode) {
        Item it = pop();
        Item it2 = pop();

        if ((it.getConstant() != null) && it2.getConstant() != null) {
            double d = constantToDouble(it);
            double d2 = constantToDouble(it2);
            if (Double.isNaN(d) || Double.isNaN(d2)) {
                if (opcode == DCMPG)
                    push(new Item("I", Integer.valueOf(1)));
                else
                    push(new Item("I", Integer.valueOf(-1)));
            }
            if (d2 < d)
                push(new Item("I", Integer.valueOf(-1)));
            else if (d2 > d)
                push(new Item("I", Integer.valueOf(1)));
            else
                push(new Item("I", Integer.valueOf(0)));
        } else {
            push(new Item("I"));
        }
    }

    /**
     * handle fcmp
     *
     */
    private void handleFcmp(int opcode) {
        Item it = pop();
        Item it2 = pop();
        if ((it.getConstant() != null) && it2.getConstant() != null) {
            float f = constantToFloat(it);
            float f2 = constantToFloat(it2);
            if (Float.isNaN(f) || Float.isNaN(f2)) {
                if (opcode == FCMPG)
                    push(new Item("I", Integer.valueOf(1)));
                else
                    push(new Item("I", Integer.valueOf(-1)));
            }
            if (f2 < f)
                push(new Item("I", Integer.valueOf(-1)));
            else if (f2 > f)
                push(new Item("I", Integer.valueOf(1)));
            else
                push(new Item("I", Integer.valueOf(0)));
        } else {
            push(new Item("I"));
        }
    }

    /**
     * handle lcmp
     */
    private void handleLcmp() {
        Item it = pop();
        Item it2 = pop();

        if ((it.getConstant() != null) && it2.getConstant() != null) {
            long l = constantToLong(it);
            long l2 = constantToLong(it2);
            if (l2 < l)
                push(new Item("I", Integer.valueOf(-1)));
            else if (l2 > l)
                push(new Item("I", Integer.valueOf(1)));
            else
                push(new Item("I", Integer.valueOf(0)));
        } else {
            push(new Item("I"));
        }

    }

    /**
     * handle swap
     */
    private void handleSwap() {
        Item i1 = pop();
        Item i2 = pop();
        push(i1);
        push(i2);
    }

    /**
     * handleDup
     */
    private void handleDup() {
        Item it;
        it = pop();
        push(it);
        push(it);
    }

    /**
     * handle dupX1
     */
    private void handleDupX1() {
        Item it;
        Item it2;
        it = pop();
        it2 = pop();
        push(it);
        push(it2);
        push(it);
    }

    /**
     * handle dup2
     */
    private void handleDup2() {
        Item it, it2;
        it = pop();
        if (it.getSize() == 2) {
            push(it);
            push(it);
        } else {
            it2 = pop();
            push(it2);
            push(it);
            push(it2);
            push(it);
        }
    }

    /**
     * handle Dup2x1
     */
    private void handleDup2X1() {
        String signature;
        Item it;
        Item it2;
        Item it3;

        it = pop();

        it2 = pop();
        signature = it.getSignature();
        if (signature.equals("J") || signature.equals("D")) {
            push(it);
            push(it2);
            push(it);
        } else {
            it3 = pop();
            push(it2);
            push(it);
            push(it3);
            push(it2);
            push(it);
        }
    }

    private void handleDup2X2() {
        Item it = pop();
        Item it2 = pop();

        if (it.isWide()) {
            if (it2.isWide()) {
                push(it);
                push(it2);
                push(it);
            } else {
                Item it3 = pop();
                push(it);
                push(it3);
                push(it2);
                push(it);
            }
        } else {
            Item it3 = pop();
            if (it3.isWide()) {
                push(it2);
                push(it);
                push(it3);
                push(it2);
                push(it);
            } else {
                Item it4 = pop();
                push(it2);
                push(it);
                push(it4);
                push(it3);
                push(it2);
                push(it);
            }
        }
    }

    /**
     * Handle DupX2
     */
    private void handleDupX2() {
        String signature;
        Item it;
        Item it2;
        Item it3;
        it = pop();
        it2 = pop();
        signature = it2.getSignature();
        if (signature.equals("J") || signature.equals("D")) {
            push(it);
            push(it2);
            push(it);
        } else {
            it3 = pop();
            push(it);
            push(it3);
            push(it2);
            push(it);
        }
    }

    static final HashMap<String, String> boxedTypes = new HashMap<String, String>();

    static private void addBoxedType(Class<?>... clss) {
        for (Class<?> c : clss) {
            Class<?> primitiveType;
            try {
                primitiveType = (Class<?>) c.getField("TYPE").get(null);
                boxedTypes.put(ClassName.toSlashedClassName(c.getName()), primitiveType.getName());
            } catch (Exception e) {
                throw new AssertionError(e);
            }

        }
    }

    static {
        addBoxedType(Integer.class, Long.class, Double.class, Short.class, Float.class, Boolean.class, Character.class,
                Byte.class);
    }

    private void processMethodCall(DismantleBytecode dbc, int seen) {
        @SlashedClassName String clsName = dbc.getClassConstantOperand();
        String methodName = dbc.getNameConstantOperand();
        String signature = dbc.getSigConstantOperand();
        String appenderValue = null;
        boolean servletRequestParameterTainted = false;
        boolean sawUnknownAppend = false;
        Item sbItem = null;
        Item topItem = null;
        if (getStackDepth() > 0)
            topItem = getStackItem(0);

        int numberArguments = PreorderVisitor.getNumberArguments(signature);

        if (boxedTypes.containsKey(clsName)
                && topItem != null
                && (methodName.equals("valueOf") && !signature.contains("String") || methodName.equals(boxedTypes.get(clsName)
                        + "Value"))) {
            // boxing/unboxing conversion
            Item value = pop();
            String newSignature = Type.getReturnType(signature).getSignature();
            Item newValue = new Item(value, newSignature);
            if (newValue.source == null)
                newValue.source = XFactory.createReferencedXMethod(dbc);
            if (newValue.specialKind == Item.NOT_SPECIAL) {
                if (newSignature.equals("B") || newSignature.equals("Ljava/lang/Boolean;"))
                    newValue.specialKind = Item.SIGNED_BYTE;
                else if (newSignature.equals("C") || newSignature.equals("Ljava/lang/Character;"))
                    newValue.specialKind = Item.NON_NEGATIVE;
            }
            push(newValue);
            return;
        }

        int firstArgument = seen == INVOKESTATIC ? 0 : 1;
        for (int i = firstArgument; i < firstArgument + numberArguments; i++) {
            if (i >= getStackDepth())
                break;
            Item item = getStackItem(i);
            String itemSignature = item.getSignature();
            if (itemSignature.equals("Ljava/lang/StringBuilder;") || itemSignature.equals("Ljava/lang/StringBuffer;"))
                item.constValue = null;
        }
        boolean initializingServletWriter = false;
        if (seen == INVOKESPECIAL && methodName.equals("<init>") && clsName.startsWith("java/io") && clsName.endsWith("Writer")
                && numberArguments > 0) {
            Item firstArg = getStackItem(numberArguments-1);
            if (firstArg.isServletWriter())
                initializingServletWriter = true;
        }
        boolean topIsTainted = topItem != null && topItem.isServletParameterTainted();
        HttpParameterInjection injection = null;
        if (topIsTainted)
            injection = topItem.injection;

        // TODO: stack merging for trinaries kills the constant.. would be nice
        // to maintain.
        if ("java/lang/StringBuffer".equals(clsName) || "java/lang/StringBuilder".equals(clsName)) {
            if ("<init>".equals(methodName)) {
                if ("(Ljava/lang/String;)V".equals(signature)) {
                    Item i = getStackItem(0);
                    appenderValue = (String) i.getConstant();
                    if (i.isServletParameterTainted())
                        servletRequestParameterTainted = true;
                } else if ("()V".equals(signature)) {
                    appenderValue = "";
                }
            } else if ("toString".equals(methodName) && getStackDepth() >= 1) {
                Item i = getStackItem(0);
                appenderValue = (String) i.getConstant();
                if (i.isServletParameterTainted())
                    servletRequestParameterTainted = true;
            } else if ("append".equals(methodName)) {
                if (signature.indexOf("II)") == -1 && getStackDepth() >= 2) {
                    sbItem = getStackItem(1);
                    Item i = getStackItem(0);
                    if (i.isServletParameterTainted() || sbItem.isServletParameterTainted())
                        servletRequestParameterTainted = true;
                    Object sbVal = sbItem.getConstant();
                    Object sVal = i.getConstant();
                    if ((sbVal != null) && (sVal != null)) {
                        appenderValue = sbVal + sVal.toString();
                    } else if (sbItem.registerNumber >= 0) {
                        OpcodeStack.Item item = getLVValue(sbItem.registerNumber);
                        if (item != null)
                            item.constValue = null;
                    }
                } else if (signature.startsWith("([CII)")) {
                    sawUnknownAppend = true;
                    sbItem = getStackItem(3);
                    if (sbItem.registerNumber >= 0) {
                        OpcodeStack.Item item = getLVValue(sbItem.registerNumber);
                        if (item != null)
                            item.constValue = null;
                    }
                } else {
                    sawUnknownAppend = true;
                }
            }
        } else if (seen == INVOKESPECIAL && clsName.equals("java/io/FileOutputStream") && methodName.equals("<init>")
                && (signature.equals("(Ljava/io/File;Z)V") || signature.equals("(Ljava/lang/String;Z)V")) && stack.size() > 3) {
            OpcodeStack.Item item = getStackItem(0);
            Object value = item.getConstant();
            if (value instanceof Integer && ((Integer) value).intValue() == 1) {
                pop(3);
                Item newTop = getStackItem(0);
                if (newTop.signature.equals("Ljava/io/FileOutputStream;")) {
                    newTop.setSpecialKind(Item.FILE_OPENED_IN_APPEND_MODE);
                    newTop.source = XFactory.createReferencedXMethod(dbc);
                    newTop.setPC(dbc.getPC());
                }
                return;
            }
        } else if (seen == INVOKESPECIAL && clsName.equals("java/io/BufferedOutputStream") && methodName.equals("<init>")
                && signature.equals("(Ljava/io/OutputStream;)V")) {

            if (getStackItem(0).getSpecialKind() == Item.FILE_OPENED_IN_APPEND_MODE
                    && getStackItem(2).signature.equals("Ljava/io/BufferedOutputStream;")) {

                pop(2);
                Item newTop = getStackItem(0);
                newTop.setSpecialKind(Item.FILE_OPENED_IN_APPEND_MODE);
                newTop.source = XFactory.createReferencedXMethod(dbc);
                newTop.setPC(dbc.getPC());
                return;
            }
        } else if (seen == INVOKEINTERFACE && methodName.equals("getParameter")
                && clsName.equals("javax/servlet/http/HttpServletRequest") || clsName.equals("javax/servlet/http/ServletRequest")) {
            Item requestParameter = pop();
            pop();
            Item result = new Item("Ljava/lang/String;");
            result.setServletParameterTainted();
            result.source = XFactory.createReferencedXMethod(dbc);
            String parameterName = null;
            if (requestParameter.getConstant() instanceof String)
                parameterName = (String) requestParameter.getConstant();

            result.injection = new HttpParameterInjection(parameterName, dbc.getPC());
            result.setPC(dbc.getPC());
            push(result);
            return;
        } else if (seen == INVOKEINTERFACE && methodName.equals("getQueryString")
                && clsName.equals("javax/servlet/http/HttpServletRequest") || clsName.equals("javax/servlet/http/ServletRequest")) {
            pop();
            Item result = new Item("Ljava/lang/String;");
            result.setServletParameterTainted();
            result.source = XFactory.createReferencedXMethod(dbc);
            result.setPC(dbc.getPC());
            push(result);
            return;
        } else if (seen == INVOKEINTERFACE && methodName.equals("getHeader")
                && clsName.equals("javax/servlet/http/HttpServletRequest") || clsName.equals("javax/servlet/http/ServletRequest")) {
            /* Item requestParameter = */pop();
            pop();
            Item result = new Item("Ljava/lang/String;");
            result.setServletParameterTainted();
            result.source = XFactory.createReferencedXMethod(dbc);
            result.setPC(dbc.getPC());
            push(result);
            return;
        } else if (seen == INVOKESTATIC && methodName.equals("asList") && clsName.equals("java/util/Arrays")) {
            /* Item requestParameter = */pop();
            Item result = new Item(JAVA_UTIL_ARRAYS_ARRAY_LIST);
            push(result);
            return;
        } else if (seen == INVOKESTATIC && signature.equals("(Ljava/util/List;)Ljava/util/List;")
                && clsName.equals("java/util/Collections")) {
            Item requestParameter = pop();
            if (requestParameter.getSignature().equals(JAVA_UTIL_ARRAYS_ARRAY_LIST)) {
                Item result = new Item(JAVA_UTIL_ARRAYS_ARRAY_LIST);
                push(result);
                return;
            }
            push(requestParameter); // fall back to standard logic
        }

        pushByInvoke(dbc, seen != INVOKESTATIC);

        if (initializingServletWriter)
            this.getStackItem(0).setIsServletWriter();

        if ((sawUnknownAppend || appenderValue != null || servletRequestParameterTainted) && getStackDepth() > 0) {
            Item i = this.getStackItem(0);
            i.constValue = appenderValue;
            if (!sawUnknownAppend && servletRequestParameterTainted) {
                i.injection = topItem.injection;
                i.setServletParameterTainted();
            }
            if (sbItem != null) {
                i.registerNumber = sbItem.registerNumber;
                i.source = sbItem.source;
                if (i.injection == null)
                    i.injection = sbItem.injection;
                if (sbItem.registerNumber >= 0)
                    setLVValue(sbItem.registerNumber, i);
            }
            return;
        }

        if ((clsName.equals("java/util/Random") || clsName.equals("java/security/SecureRandom")) && 
                (methodName.equals("nextInt")  && signature.equals("()I")
                        || methodName.equals("nextLong")  && signature.equals("()J"))
                ) {
            Item i = pop();
            i.setSpecialKind(Item.RANDOM_INT);
            push(i);
        } else if (methodName.equals("size") && signature.equals("()I") 
                && Subtypes2.instanceOf(ClassName.toDottedClassName(clsName), "java.util.Collection")) {
            Item i = pop();
            if (i.getSpecialKind() == Item.NOT_SPECIAL)
                    i.setSpecialKind(Item.NON_NEGATIVE);
            push(i);
        } else if (ClassName.isMathClass(clsName) && methodName.equals("abs")) {
            Item i = pop();
            if (i.getSpecialKind() == Item.HASHCODE_INT)
                i.setSpecialKind(Item.MATH_ABS_OF_HASHCODE);
            else if (i.getSpecialKind() == Item.RANDOM_INT)
                i.setSpecialKind(Item.MATH_ABS_OF_RANDOM);
            else
                i.setSpecialKind(Item.MATH_ABS);
            push(i);
        } else if (seen == INVOKEVIRTUAL && methodName.equals("hashCode") && signature.equals("()I") || seen == INVOKESTATIC
                && clsName.equals("java/lang/System") && methodName.equals("identityHashCode")
                && signature.equals("(Ljava/lang/Object;)I")) {
            Item i = pop();
            i.setSpecialKind(Item.HASHCODE_INT);
            push(i);
        } else if (topIsTainted
                && (methodName.startsWith("encode") && clsName.equals("javax/servlet/http/HttpServletResponse") || methodName
                        .equals("trim") && clsName.equals("java/lang/String"))) {
            Item i = pop();
            i.setSpecialKind(Item.SERVLET_REQUEST_TAINTED);
            i.injection = injection;
            push(i);
        }

        if (!signature.endsWith(")V")) {
            Item i = pop();
            i.source = XFactory.createReferencedXMethod(dbc);
            push(i);
        }

    }

    private void mergeLists(List<Item> mergeInto, List<Item> mergeFrom, boolean errorIfSizesDoNotMatch) {
        // merge stacks
        int intoSize = mergeInto.size();
        int fromSize = mergeFrom.size();
        if (errorIfSizesDoNotMatch && intoSize != fromSize) {
            if (DEBUG2) {
                System.out.println("Bad merging items");
                System.out.println("current items: " + mergeInto);
                System.out.println("jump items: " + mergeFrom);
            }
        } else {
            if (DEBUG2) {
                if (intoSize == fromSize)
                    System.out.println("Merging items");
                else
                    System.out.println("Bad merging items");
                System.out.println("current items: " + mergeInto);
                System.out.println("jump items: " + mergeFrom);
            }

            for (int i = 0; i < Math.min(intoSize, fromSize); i++)
                mergeInto.set(i, Item.merge(mergeInto.get(i), mergeFrom.get(i)));
            if (DEBUG2) {
                System.out.println("merged items: " + mergeInto);
            }
        }
    }

    public void clear() {
        stack.clear();
        lvValues.clear();
    }

    boolean encountedTop;

    boolean backwardsBranch;

    BitSet exceptionHandlers = new BitSet();

    private Map<Integer, List<Item>> jumpEntries = new HashMap<Integer, List<Item>>();

    private Map<Integer, List<Item>> jumpStackEntries = new HashMap<Integer, List<Item>>();

    private BitSet jumpEntryLocations = new BitSet();

    public static class JumpInfo {
        final Map<Integer, List<Item>> jumpEntries;

        final Map<Integer, List<Item>> jumpStackEntries;

        final BitSet jumpEntryLocations;

        JumpInfo(Map<Integer, List<Item>> jumpEntries, Map<Integer, List<Item>> jumpStackEntries, BitSet jumpEntryLocations) {
            this.jumpEntries = jumpEntries;
            this.jumpStackEntries = jumpStackEntries;
            this.jumpEntryLocations = jumpEntryLocations;
        }

        public int getNextJump(int pc) {
            return jumpEntryLocations.nextSetBit(pc);
        }
    }

    public static class JumpInfoFactory extends AnalysisFactory<JumpInfo> {

        public JumpInfoFactory() {
            super("Jump info for opcode stack", JumpInfo.class);
        }

        public JumpInfo analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
            Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);
            JavaClass jclass = getJavaClass(analysisCache, descriptor.getClassDescriptor());

            Code code = method.getCode();
            if (code == null) {
                return null;
            }
            final OpcodeStack stack = new OpcodeStack();

            DismantleBytecode branchAnalysis = new DismantleBytecode() {
                @Override
                public void sawOpcode(int seen) {
                    stack.sawOpcode(this, seen);
                }
            };
            return computeJumpInfo(jclass, method, stack, branchAnalysis);
        }

        /**
         * @param jclass
         * @param method
         * @param stack
         * @param branchAnalysis
         * @return
         */
        public static JumpInfo computeJumpInfo(JavaClass jclass, Method method, final OpcodeStack stack,
                DismantleBytecode branchAnalysis) {
            branchAnalysis.setupVisitorForClass(jclass);
            MethodInfo xMethod = (MethodInfo) XFactory.createXMethod(jclass, method);
            int oldCount = 0;
            while (true) {
                stack.resetForMethodEntry0(ClassName.toSlashedClassName(jclass.getClassName()), method);
                branchAnalysis.doVisitMethod(method);
                int newCount = stack.jumpEntries.size();
                if (xMethod.hasBackBranch() != stack.backwardsBranch) {
                    AnalysisContext.logError(
                            String.format("For %s, mismatch on existing of backedge: %s for precomputation, %s for bytecode analysis",
                                    xMethod, xMethod.hasBackBranch(), stack.backwardsBranch));
                }
                if (newCount == oldCount || !stack.encountedTop || !stack.backwardsBranch)
                    break;
                oldCount = newCount;
            }

            return new JumpInfo(stack.jumpEntries, stack.jumpStackEntries, stack.jumpEntryLocations);
        }
    }

    public boolean isJumpTarget(int pc) {
        return jumpEntryLocations.get(pc);
    }

    private void addJumpValue(int from, int target) {
        if (DEBUG)
            System.out.println("Set jump entry at " + methodName + ":" + target + "pc to " + stack + " : " + lvValues);

        if (from >= target)
            backwardsBranch = true;
        List<Item> atTarget = jumpEntries.get(Integer.valueOf(target));
        if (atTarget == null) {
            if (DEBUG)
                System.out.println("Was null");

            jumpEntries.put(Integer.valueOf(target), new ArrayList<Item>(lvValues));
            jumpEntryLocations.set(target);
            if (stack.size() > 0) {
                jumpStackEntries.put(Integer.valueOf(target), new ArrayList<Item>(stack));
            }
            return;
        }
        mergeLists(atTarget, lvValues, false);
        List<Item> stackAtTarget = jumpStackEntries.get(Integer.valueOf(target));
        if (stack.size() > 0 && stackAtTarget != null)
            mergeLists(stackAtTarget, stack, false);
        if (DEBUG)
            System.out.println("merge target for " + methodName + ":" + target + "pc is " + atTarget);
    }

    private String methodName;

    DismantleBytecode v;

    public void learnFrom(JumpInfo info) {
        jumpEntries = new HashMap<Integer, List<Item>>(info.jumpEntries);
        jumpStackEntries = new HashMap<Integer, List<Item>>(info.jumpStackEntries);
        jumpEntryLocations = (BitSet) info.jumpEntryLocations.clone();
    }

    public void initialize() {
        setTop(false);
        jumpEntries.clear();
        jumpStackEntries.clear();
        jumpEntryLocations.clear();
        encountedTop = false;
        backwardsBranch = false;
        lastUpdate.clear();
        convertJumpToOneZeroState = convertJumpToZeroOneState = 0;
        zeroOneComing = -1;
        registerTestedFoundToBeNonnegative = -1;
        setReachOnlyByBranch(false);
    }

    public int resetForMethodEntry(final DismantleBytecode visitor) {
        this.v = visitor;
        initialize();

        int result = resetForMethodEntry0(v);
        Code code = v.getMethod().getCode();
        if (code == null)
            return result;

        if (useIterativeAnalysis) {
            JumpInfo jump = null;
            if (visitor instanceof OpcodeStackDetector.WithCustomJumpInfo) {
                jump = ((OpcodeStackDetector.WithCustomJumpInfo) visitor).customJumpInfo();
            }

            if (jump == null)
                jump = getJumpInfo();
            if (jump != null) {
                learnFrom(jump);
            }

        }

        return result;

    }

    private JumpInfo getJumpInfo() {
        IAnalysisCache analysisCache = Global.getAnalysisCache();
        XMethod xMethod = XFactory.createXMethod(v.getThisClass(), v.getMethod());
        if (xMethod instanceof MethodInfo) {
            MethodInfo mi = (MethodInfo) xMethod;
            if (!mi.hasBackBranch())
                return null;
        }
        try {
            return analysisCache.getMethodAnalysis(JumpInfo.class, xMethod.getMethodDescriptor());
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error getting jump information", e);
            return null;
        }
    }

    private int resetForMethodEntry0(PreorderVisitor visitor) {
        return resetForMethodEntry0(visitor.getClassName(), visitor.getMethod());
    }

    private int resetForMethodEntry0(@SlashedClassName String className, Method m) {
        methodName = m.getName();

        if (DEBUG)
            System.out.println(" --- ");
        String signature = m.getSignature();
        stack.clear();
        lvValues.clear();
        top = false;
        encountedTop = false;
        backwardsBranch = false;

        setReachOnlyByBranch(false);
        seenTransferOfControl = false;
        exceptionHandlers.clear();
        Code code = m.getCode();
        if (code != null) {
            CodeException[] exceptionTable = code.getExceptionTable();
            if (exceptionTable != null)
                for (CodeException ex : exceptionTable)
                    exceptionHandlers.set(ex.getHandlerPC());
        }
        if (DEBUG)
            System.out.println(" --- " + className + " " + m.getName() + " " + signature);
        Type[] argTypes = Type.getArgumentTypes(signature);
        int reg = 0;
        if (!m.isStatic()) {
            Item it = new Item("L" + className + ";");
            it.setInitialParameter(true);
            it.registerNumber = reg;
            setLVValue(reg, it);
            reg += it.getSize();
        }
        for (Type argType : argTypes) {
            Item it = new Item(argType.getSignature());
            it.registerNumber = reg;
            it.setInitialParameter(true);
            setLVValue(reg, it);
            reg += it.getSize();
        }
        return reg;
    }

    public int getStackDepth() {
        return stack.size();
    }

    public Item getStackItem(int stackOffset) {
        if (stackOffset < 0 || stackOffset >= stack.size()) {
            AnalysisContext.logError("Can't get stack offset " + stackOffset + " from " + stack.toString() + " @ " + v.getPC()
                    + " in " + v.getFullyQualifiedMethodName(), new IllegalArgumentException(stackOffset
                    + " is not a value stack offset"));
            return new Item("Lfindbugs/OpcodeStackError;");

        }
        int tos = stack.size() - 1;
        int pos = tos - stackOffset;
        try {
            return stack.get(pos);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Requested item at offset " + stackOffset + " in a stack of size "
                    + stack.size() + ", made request for position " + pos);
        }
    }

    private Item pop() {
        return stack.remove(stack.size() - 1);
    }

    public void replaceTop(Item newTop) {
        pop();
        push(newTop);
    }

    private void pop(int count) {
        while ((count--) > 0)
            pop();
    }

    private void push(Item i) {
        stack.add(i);
    }

    private void pushByConstant(DismantleBytecode dbc, Constant c) {

        if (c instanceof ConstantClass)
            push(new Item("Ljava/lang/Class;", ((ConstantClass) c).getConstantValue(dbc.getConstantPool())));
        else if (c instanceof ConstantInteger)
            push(new Item("I", Integer.valueOf(((ConstantInteger) c).getBytes())));
        else if (c instanceof ConstantString) {
            int s = ((ConstantString) c).getStringIndex();
            push(new Item("Ljava/lang/String;", getStringFromIndex(dbc, s)));
        } else if (c instanceof ConstantFloat)
            push(new Item("F", Float.valueOf(((ConstantFloat) c).getBytes())));
        else if (c instanceof ConstantDouble)
            push(new Item("D", Double.valueOf(((ConstantDouble) c).getBytes())));
        else if (c instanceof ConstantLong)
            push(new Item("J", Long.valueOf(((ConstantLong) c).getBytes())));
        else
            throw new UnsupportedOperationException("Constant type not expected");
    }

    private void pushByLocalObjectLoad(DismantleBytecode dbc, int register) {
        Method m = dbc.getMethod();
        LocalVariableTable lvt = m.getLocalVariableTable();
        if (lvt != null) {
            LocalVariable lv = LVTHelper.getLocalVariableAtPC(lvt, register, dbc.getPC());
            if (lv != null) {
                String signature = lv.getSignature();
                pushByLocalLoad(signature, register);
                return;
            }
        }
        pushByLocalLoad("Ljava/lang/Object;", register);
    }

    private void pushByIntMath(DismantleBytecode dbc, int seen, Item lhs, Item rhs) {
        Item newValue = new Item("I");
        if (lhs == null || rhs == null) {
            push(newValue);
            return;
        }

        try {
            if (DEBUG)
                System.out.println("pushByIntMath " + dbc.getFullyQualifiedMethodName() + " @ " + dbc.getPC() + " : " + lhs
                        + OPCODE_NAMES[seen] + rhs);

            if (rhs.getConstant() != null && lhs.getConstant() != null) {
                int lhsValue = constantToInt(lhs);
                int rhsValue = constantToInt(rhs);
                if ((seen == IDIV || seen == IREM) && rhsValue == 0) {
                    push(newValue);
                    return;
                }
                switch (seen) {

                case IADD:
                    newValue = new Item("I", lhsValue + rhsValue);
                    break;
                case ISUB:
                    newValue = new Item("I", lhsValue - rhsValue);
                    break;
                case IMUL:
                    newValue = new Item("I", lhsValue * rhsValue);
                    break;
                case IDIV:
                    newValue = new Item("I", lhsValue / rhsValue);
                    break;
                case IREM:
                    newValue = new Item("I", lhsValue % rhsValue);
                    break;
                case IAND:
                    newValue = new Item("I", lhsValue & rhsValue);
                    if ((rhsValue & 0xff) == 0 && rhsValue != 0 || (lhsValue & 0xff) == 0 && lhsValue != 0)
                        newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);

                    break;
                case IOR:
                    newValue = new Item("I", lhsValue | rhsValue);
                    break;
                case IXOR:
                    newValue = new Item("I", lhsValue ^ rhsValue);
                    break;
                case ISHL:
                    newValue = new Item("I", lhsValue << rhsValue);
                    if (rhsValue >= 8)
                        newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);

                    break;
                case ISHR:
                    newValue = new Item("I", lhsValue >> rhsValue);

                    break;
                case IUSHR:
                    newValue = new Item("I", lhsValue >>> rhsValue);

                }

            } else if ((seen == ISHL || seen == ISHR || seen == IUSHR)) {
                if (rhs.getConstant() != null) {
                    int constant = constantToInt(rhs);
                    if ((constant & 0x1f) == 0)
                        newValue = new Item(lhs);
                    else if (seen == ISHL && (constant & 0x1f) >= 8)
                        newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);
                } else if (lhs.getConstant() != null) {
                    int constant = constantToInt(lhs);
                    if (constant == 0)
                        newValue = new Item("I", 0);
                }
            } else if (lhs.getConstant() != null && seen == IAND) {
                int value = constantToInt(lhs);
                if (value == 0)
                    newValue = new Item("I", 0);
                else if ((value & 0xff) == 0)
                    newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);
                else if (value >= 0)
                    newValue.setSpecialKind(Item.NON_NEGATIVE);
            } else if (rhs.getConstant() != null && seen == IAND) {
                int value = constantToInt(rhs);
                if (value == 0)
                    newValue = new Item("I", 0);
                else if ((value & 0xff) == 0)
                    newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);
                else if (value >= 0)
                    newValue.setSpecialKind(Item.NON_NEGATIVE);
            } else if (seen == IAND && lhs.getSpecialKind() == Item.ZERO_MEANS_NULL) {
                newValue.setSpecialKind(Item.ZERO_MEANS_NULL);
                newValue.setPC(lhs.getPC());
            } else if (seen == IAND && rhs.getSpecialKind() == Item.ZERO_MEANS_NULL) {
                newValue.setSpecialKind(Item.ZERO_MEANS_NULL);
                newValue.setPC(rhs.getPC());
            } else if (seen == IOR && lhs.getSpecialKind() == Item.NONZERO_MEANS_NULL) {
                newValue.setSpecialKind(Item.NONZERO_MEANS_NULL);
                newValue.setPC(lhs.getPC());
            } else if (seen == IOR && rhs.getSpecialKind() == Item.NONZERO_MEANS_NULL) {
                newValue.setSpecialKind(Item.NONZERO_MEANS_NULL);
                newValue.setPC(rhs.getPC());
            }
        } catch (ArithmeticException e) {
            assert true; // ignore it
        } catch (RuntimeException e) {
            String msg = "Error processing2 " + lhs + OPCODE_NAMES[seen] + rhs + " @ " + dbc.getPC() + " in "
                    + dbc.getFullyQualifiedMethodName();
            AnalysisContext.logError(msg, e);

        }
        if (lhs.getSpecialKind() == Item.INTEGER_SUM && rhs.getConstant() != null) {
            int rhsValue = constantToInt(rhs);
            if (seen == IDIV && rhsValue == 2 || seen == ISHR && rhsValue == 1)
                newValue.setSpecialKind(Item.AVERAGE_COMPUTED_USING_DIVISION);
        }
        if (seen == IADD && newValue.getSpecialKind() == Item.NOT_SPECIAL && lhs.getConstant() == null
                && rhs.getConstant() == null)
            newValue.setSpecialKind(Item.INTEGER_SUM);
        if (seen == IREM && lhs.getSpecialKind() == Item.HASHCODE_INT)
            newValue.setSpecialKind(Item.HASHCODE_INT_REMAINDER);
        if (seen == IREM && lhs.getSpecialKind() == Item.RANDOM_INT)
            newValue.setSpecialKind(Item.RANDOM_INT_REMAINDER);
        if (seen == IREM && lhs.checkForIntegerMinValue()) {
            if (rhs.getConstant() != null) {
                int rhsValue = constantToInt(rhs);
                if (!Util.isPowerOfTwo(rhsValue))
                    newValue.setSpecialKind(lhs.getSpecialKindForRemainder());
            } else
                newValue.setSpecialKind(lhs.getSpecialKindForRemainder());
        }
        if (DEBUG)
            System.out.println("push: " + newValue);
        newValue.setPC(dbc.getPC());
        push(newValue);
    }

    private void pushByLongMath(int seen, Item lhs, Item rhs) {
        Item newValue = new Item("J");
        try {

            if ((rhs.getConstant() != null) && lhs.getConstant() != null) {

                long lhsValue = constantToLong(lhs);
                if (seen == LSHL) {
                    newValue = new Item("J", Long.valueOf(lhsValue << constantToInt(rhs)));
                    if (constantToInt(rhs) >= 8)
                        newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);
                } else if (seen == LSHR)
                    newValue = new Item("J", Long.valueOf(lhsValue >> constantToInt(rhs)));
                else if (seen == LUSHR)
                    newValue = new Item("J", Long.valueOf(lhsValue >>> constantToInt(rhs)));

                else {
                    long rhsValue = constantToLong(rhs);
                    if (seen == LADD)
                        newValue = new Item("J", Long.valueOf(lhsValue + rhsValue));
                    else if (seen == LSUB)
                        newValue = new Item("J", Long.valueOf(lhsValue - rhsValue));
                    else if (seen == LMUL)
                        newValue = new Item("J", Long.valueOf(lhsValue * rhsValue));
                    else if (seen == LDIV)
                        newValue = new Item("J", Long.valueOf(lhsValue / rhsValue));
                    else if (seen == LAND) {
                        newValue = new Item("J", Long.valueOf(lhsValue & rhsValue));
                        if ((rhsValue & 0xff) == 0 && rhsValue != 0 || (lhsValue & 0xff) == 0 && lhsValue != 0)
                            newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);
                    } else if (seen == LOR)
                        newValue = new Item("J", Long.valueOf(lhsValue | rhsValue));
                    else if (seen == LXOR)
                        newValue = new Item("J", Long.valueOf(lhsValue ^ rhsValue));
                    else if (seen == LREM)
                        newValue = new Item("J", Long.valueOf(lhsValue % rhsValue));
                }
            } else if (rhs.getConstant() != null && seen == LSHL && constantToInt(rhs) >= 8)
                newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);
            else if (lhs.getConstant() != null && seen == LAND && (constantToLong(lhs) & 0xff) == 0)
                newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);
            else if (rhs.getConstant() != null && seen == LAND && (constantToLong(rhs) & 0xff) == 0)
                newValue.setSpecialKind(Item.LOW_8_BITS_CLEAR);
        } catch (RuntimeException e) {
            // ignore it
        }
        push(newValue);
    }

    private void pushByFloatMath(int seen, Item it, Item it2) {
        Item result;
        int specialKind = Item.FLOAT_MATH;
        if ((it.getConstant() instanceof Float) && it2.getConstant() instanceof Float) {
            if (seen == FADD)
                result = new Item("F", Float.valueOf(constantToFloat(it2) + constantToFloat(it)));
            else if (seen == FSUB)
                result = new Item("F", Float.valueOf(constantToFloat(it2) - constantToFloat(it)));
            else if (seen == FMUL)
                result = new Item("F", Float.valueOf(constantToFloat(it2) * constantToFloat(it)));
            else if (seen == FDIV)
                result = new Item("F", Float.valueOf(constantToFloat(it2) / constantToFloat(it)));
            else if (seen == FREM)
                result = new Item("F", Float.valueOf(constantToFloat(it2) % constantToFloat(it)));
            else
                result = new Item("F");
        } else {
            result = new Item("F");
            if (seen == DDIV)
                specialKind = Item.NASTY_FLOAT_MATH;
        }
        result.setSpecialKind(specialKind);
        push(result);
    }

    private void pushByDoubleMath(int seen, Item it, Item it2) {
        Item result;
        int specialKind = Item.FLOAT_MATH;
        if ((it.getConstant() instanceof Double) && it2.getConstant() instanceof Double) {
            if (seen == DADD)
                result = new Item("D", Double.valueOf(constantToDouble(it2) + constantToDouble(it)));
            else if (seen == DSUB)
                result = new Item("D", Double.valueOf(constantToDouble(it2) - constantToDouble(it)));
            else if (seen == DMUL)
                result = new Item("D", Double.valueOf(constantToDouble(it2) * constantToDouble(it)));
            else if (seen == DDIV)
                result = new Item("D", Double.valueOf(constantToDouble(it2) / constantToDouble(it)));
            else if (seen == DREM)
                result = new Item("D", Double.valueOf(constantToDouble(it2) % constantToDouble(it)));
            else
                result = new Item("D"); // ?
        } else {
            result = new Item("D");
            if (seen == DDIV)
                specialKind = Item.NASTY_FLOAT_MATH;
        }
        result.setSpecialKind(specialKind);
        push(result);
    }

    private void pushByInvoke(DismantleBytecode dbc, boolean popThis) {
        String signature = dbc.getSigConstantOperand();
        if (dbc.getNameConstantOperand().equals("<init>") && signature.endsWith(")V") && popThis) {
            pop(PreorderVisitor.getNumberArguments(signature));
            Item constructed = pop();
            if (getStackDepth() > 0) {
                Item next = getStackItem(0);
                if (constructed.equals(next)) {
                    next.source = XFactory.createReferencedXMethod(dbc);
                    next.pc = dbc.getPC();
                }
            }
            return;
        }
        pop(PreorderVisitor.getNumberArguments(signature) + (popThis ? 1 : 0));
        pushBySignature(Type.getReturnType(signature).getSignature(), dbc);
    }

    public Item getItemMethodInvokedOn(DismantleBytecode dbc) {
        int opcode = dbc.getOpcode();
        switch (opcode) {
        case INVOKEVIRTUAL:
        case INVOKEINTERFACE:
        case INVOKESPECIAL:
            String signature = dbc.getSigConstantOperand();
            int stackOffset = PreorderVisitor.getNumberArguments(signature);

            return getStackItem(stackOffset);
        }
        throw new IllegalArgumentException("Not visiting an instance method call");
    }

    private String getStringFromIndex(DismantleBytecode dbc, int i) {
        ConstantUtf8 name = (ConstantUtf8) dbc.getConstantPool().getConstant(i);
        return name.getBytes();
    }

    private void pushBySignature(String s, DismantleBytecode dbc) {
        if ("V".equals(s))
            return;
        Item item = new Item(s, (Object) null);
        if (dbc != null)
            item.setPC(dbc.getPC());
        if ("B".equals(s))
            item.setSpecialKind(Item.SIGNED_BYTE);
        else if ("C".equals(s))
            item.setSpecialKind(Item.NON_NEGATIVE);
        push(item);
    }

    private void pushByLocalStore(int register) {
        Item it = pop();
        if (it.getRegisterNumber() != register) {
            for (Item i : lvValues)
                if (i != null) {
                    if (i.registerNumber == register)
                        i.registerNumber = -1;
                    if (i.fieldLoadedFromRegister == register)
                        i.fieldLoadedFromRegister = -1;
                }
            for (Item i : stack)
                if (i != null) {
                    if (i.registerNumber == register)
                        i.registerNumber = -1;
                    if (i.fieldLoadedFromRegister == register)
                        i.fieldLoadedFromRegister = -1;
                }
        }
        setLVValue(register, it);
    }

    private void pushByLocalLoad(String signature, int register) {
        Item oldItem = getLVValue(register);

        Item newItem;
        if (oldItem == null) {
            newItem = new Item(signature);
            newItem.registerNumber = register;
        } else {
            newItem = oldItem;
            if (newItem.signature.equals("Ljava/lang/Object;") && !signature.equals("Ljava/lang/Object;")) {
                newItem = new Item(oldItem);
                newItem.signature = signature;
            }
            if (newItem.getRegisterNumber() < 0) {
                if (newItem == oldItem)
                    newItem = new Item(oldItem);
                newItem.registerNumber = register;
            }
        }

        push(newItem);

    }

    private void setLVValue(int index, Item value) {
        int addCount = index - lvValues.size() + 1;
        while ((addCount--) > 0)
            lvValues.add(null);
        if (!useIterativeAnalysis && seenTransferOfControl)
            value = Item.merge(value, lvValues.get(index));
        lvValues.set(index, value);
    }

    public Item getLVValue(int index) {
        if (index >= lvValues.size())
            return new Item();

        Item item = lvValues.get(index);
        if (item != null)
            return item;

        return new Item();
    }

    public int getNumLocalValues() {
        return lvValues.size();
    }

    /**
     * @param top
     *            The top to set.
     */
    private void setTop(boolean top) {
        if (top) {
            if (!this.top)
                this.top = true;
        } else if (this.top)
            this.top = false;
    }

    /**
     * @return Returns the top.
     */
    public boolean isTop() {
        if (top)
            return true;
        return false;
    }

    /**
     * @param reachOnlyByBranch
     *            The reachOnlyByBranch to set.
     */
    void setReachOnlyByBranch(boolean reachOnlyByBranch) {
        if (reachOnlyByBranch)
            setTop(true);
        this.reachOnlyByBranch = reachOnlyByBranch;
    }

    /**
     * @return Returns the reachOnlyByBranch.
     */
    boolean isReachOnlyByBranch() {
        return reachOnlyByBranch;
    }
}

// vim:ts=4

/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.analysis;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.bcel.Constants;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pugh
 */
public class MethodInfo extends MethodDescriptor implements XMethod, AnnotatedObject {

	public static final MethodInfo [] EMPTY_ARRAY = new MethodInfo[0];
	static public class Builder {
		int accessFlags;

		final @SlashedClassName String className;
		final String methodName, methodSignature;

		String [] exceptions;
		String methodSourceSignature;
		boolean isUnconditionalThrower;
		boolean isUnsupported;
		boolean usesConcurrency;
		boolean isStub;
		int methodCallCount;
		MethodDescriptor accessMethodFor;

		final Map<ClassDescriptor, AnnotationValue> methodAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(4);

		final Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations = new HashMap<Integer, Map<ClassDescriptor, AnnotationValue>>(4);

		public Builder(@SlashedClassName String className, String methodName, String methodSignature, int accessFlags) {
			this.className = className;
			this.methodName = methodName;
			this.methodSignature = methodSignature;
			this.accessFlags = accessFlags;
		}

		public void setAccessMethodFor(String owner, String name, String sig, boolean isStatic) {
			accessMethodFor = new MethodDescriptor(owner, name, sig, isStatic);
		}
		public void setSourceSignature(String methodSourceSignature) {
			this.methodSourceSignature = methodSourceSignature;
		}
		public void setUsesConcurrency() {
			this.usesConcurrency = true;
		}

		public void setIsStub() {
			this.isStub = true;
		}
		public void setThrownExceptions(String [] exceptions) {
			this.exceptions = exceptions;
		}

		public void setAccessFlags(int accessFlags) {
			this.accessFlags = accessFlags;
		}
		public void addAccessFlags(int accessFlags) {
			this.accessFlags |= accessFlags;
		}
		public void addAnnotation(String name, AnnotationValue value) {
			ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptorFromSignature(name);
			methodAnnotations.put(annotationClass, value);
		}

		public void addParameterAnnotation(int parameter, String name, AnnotationValue value) {
			ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptorFromSignature(name);
			Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(parameter);
			if (map == null) {
				map = new HashMap<ClassDescriptor, AnnotationValue>();
				methodParameterAnnotations.put(parameter, map);
			}
			map.put(annotationClass, value);
		}

		public MethodInfo build() {
			return new MethodInfo(className, methodName, methodSignature, methodSourceSignature, null, accessFlags, isUnconditionalThrower, isUnsupported, usesConcurrency, 
				 isStub, methodCallCount, exceptions, accessMethodFor, methodAnnotations, methodParameterAnnotations);
		}

        public void setIsUnconditionalThrower() {
	       isUnconditionalThrower = true;
	        
        }
        public void setUnsupported() {
 	       isUnsupported = true;
 	        
         }

		/**
         * @param methodCallCount
         */
        public void setNumberMethodCalls(int methodCallCount) {
	        this.methodCallCount = methodCallCount;
	        
        }
	}

	final int accessFlags;
	final int methodCallCount;
	
	final boolean usesConcurrency;
	final boolean isStub;

	final String methodSourceSignature;
	
	final @CheckForNull String [] exceptions;

	Map<ClassDescriptor, AnnotationValue> methodAnnotations;

	Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations;

	static IdentityHashMap<MethodInfo, Void> unconditionalThrowers = new IdentityHashMap<MethodInfo, Void>();
	static IdentityHashMap<MethodInfo, Void> unsupportedMethods = new IdentityHashMap<MethodInfo, Void>();
	static IdentityHashMap<MethodInfo, MethodDescriptor> accessMethodFor = new IdentityHashMap<MethodInfo, MethodDescriptor>();


	/**
	 * @param className
	 * @param methodName
	 * @param methodSignature
	 * @param methodSourceSignature
	 * @param isUnsupported 
	 * @param usesConcurrency TODO
	 * @param isStub TODO
	 * @param methodCallCount TODO
	 * @param accessMethodFor TODO
	 * @param isStatic
	 */
	 MethodInfo(@SlashedClassName String className, String methodName, String methodSignature, String methodSourceSignature, 
			 @CheckForNull String bridgeMethodSignature,
	        int accessFlags, boolean isUnconditionalThrower,
	        boolean isUnsupported, boolean usesConcurrency, boolean isStub, int methodCallCount,
	        @CheckForNull String[] exceptions,  @CheckForNull MethodDescriptor accessMethodFor, Map<ClassDescriptor, AnnotationValue> methodAnnotations, Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations) {
		super(className, methodName, methodSignature, bridgeMethodSignature, (accessFlags & Constants.ACC_STATIC) != 0);
		this.accessFlags = accessFlags;
		this.exceptions = exceptions;
		if (exceptions != null) 
			for(int i = 0; i < exceptions.length; i++) 
			exceptions[i] = DescriptorFactory.canonicalizeString(exceptions[i]);
		this.methodSourceSignature = DescriptorFactory.canonicalizeString(methodSourceSignature);
		this.methodAnnotations = Util.immutableMap(methodAnnotations);
		this.methodParameterAnnotations = Util.immutableMap(methodParameterAnnotations);
		if (isUnconditionalThrower) unconditionalThrowers.put(this, null);
		if (isUnsupported) unsupportedMethods.put(this, null);
		this.usesConcurrency = usesConcurrency;
		this.isStub = isStub;
		this.methodCallCount = methodCallCount;
	}
	 
	 public @CheckForNull String [] getThrownExceptions() {
		 return exceptions;
	 }
	 public boolean isUnconditionalThrower() {
		 return unconditionalThrowers.containsKey(this);
	 }
	 public boolean isUnsupported() {
		 return unsupportedMethods.containsKey(this);
	 }

	public int getNumParams() {
		return new SignatureParser(getSignature()).getNumParameters();
	}
	
	public int getMethodCallCount() {
		return methodCallCount;
	}

	private boolean checkFlag(int flag) {
		return (accessFlags & flag) != 0;
	}

	public boolean isNative() {
		return checkFlag(Constants.ACC_NATIVE);
	}

	public boolean isAbstract() {
		return checkFlag(Constants.ACC_ABSTRACT);
	}
	public boolean isSynchronized() {
		return checkFlag(Constants.ACC_SYNCHRONIZED);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XMethod#isReturnTypeReferenceType()
	 */
	public boolean isReturnTypeReferenceType() {
		SignatureParser parser = new SignatureParser(getSignature());
		String returnTypeSig = parser.getReturnTypeSignature();
		return SignatureParser.isReferenceType(returnTypeSig);
	}

	public @DottedClassName
	String getClassName() {
		return getClassDescriptor().toDottedClassName();
	}

	public @DottedClassName
	String getPackageName() {
		return getClassDescriptor().getPackageName();
	}
	
	public String getSourceSignature() {
		return methodSourceSignature;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
    public int compareTo(Object rhs) {
		if (rhs instanceof MethodDescriptor) {
			return super.compareTo((MethodDescriptor) rhs);
		}
		
		if (rhs instanceof XMethod) {
			return XFactory.compare((XMethod) this, (XMethod) rhs);
		}
		
		throw new ClassCastException("Can't compare a " + this.getClass().getName() + " to a " + rhs.getClass().getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#getAccessFlags()
	 */
	public int getAccessFlags() {
		return accessFlags;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isFinal()
	 */
	public boolean isFinal() {
		return checkFlag(Constants.ACC_FINAL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPrivate()
	 */
	public boolean isPrivate() {
		return checkFlag(Constants.ACC_PRIVATE);
	}
    public boolean isDeprecated() {
    	return checkFlag(Opcodes.ACC_DEPRECATED);
    }
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isProtected()
	 */
	public boolean isProtected() {
		return checkFlag(Constants.ACC_PROTECTED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isPublic()
	 */
	public boolean isPublic() {
		return checkFlag(Constants.ACC_PUBLIC);
	}
	
    public boolean isSynthetic() {
	    return checkFlag(Constants.ACC_SYNTHETIC);
    }
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isResolved()
	 */
	public boolean isResolved() {
		return true;
	}
	
	public Collection<ClassDescriptor> getParameterAnnotationDescriptors(int param) {
		Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
		if (map == null) return Collections.<ClassDescriptor>emptySet();
		return map.keySet();
	}
	
	public @Nullable AnnotationValue getParameterAnnotation(int param, ClassDescriptor desc) {
		Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
		if (map == null) return null;
		return map.get(desc);
	}
	
	public Collection<AnnotationValue> getParameterAnnotations(int param) {
		Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
		if (map == null) return Collections.<AnnotationValue>emptySet();
		return map.values();
	}
	
	public Collection<ClassDescriptor> getAnnotationDescriptors() {
		return methodAnnotations.keySet();
	}
	
	public AnnotationValue getAnnotation(ClassDescriptor desc) {
		return methodAnnotations.get(desc);
	}
	
	public Collection<AnnotationValue> getAnnotations() {
		return methodAnnotations.values();
	}

	/**
	 * Destructively add an annotation.
	 * We do this for "built-in" annotations that might not
	 * be directly evident in the code.
	 * It's not a great idea in general, but we can
	 * get away with it as long as it's done early
	 * enough (i.e., before anyone asks what annotations
	 * this method has.) 
	 * 
	 * @param annotationValue an AnnotationValue representing a method annotation
	 */
	public void addAnnotation(AnnotationValue annotationValue) {
		HashMap<ClassDescriptor, AnnotationValue> updatedAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(methodAnnotations);
		updatedAnnotations.put(annotationValue.getAnnotationClass(), annotationValue);
		methodAnnotations = updatedAnnotations;
	}
	
	/**
	 * Destructively add a parameter annotation.
	 * 
	 * @param param           parameter (0 == first parameter)
	 * @param annotationValue an AnnotationValue representing a parameter annotation
	 */
	public void addParameterAnnotation(int param, AnnotationValue annotationValue) {
		HashMap<Integer, Map<ClassDescriptor, AnnotationValue>> updatedAnnotations =
			new HashMap<Integer, Map<ClassDescriptor,AnnotationValue>>(methodParameterAnnotations);
		Map<ClassDescriptor, AnnotationValue> paramMap = updatedAnnotations.get(param);
		if (paramMap == null) {
			paramMap = new HashMap<ClassDescriptor, AnnotationValue>();
			updatedAnnotations.put(param, paramMap);
		}
		paramMap.put(annotationValue.getAnnotationClass(), annotationValue);
		
		methodParameterAnnotations = updatedAnnotations;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XMethod#getMethodDescriptor()
	 */
	public MethodDescriptor getMethodDescriptor() {
		return this;
	}
	
	public ElementType getElementType() {
		if (getName().equals("<init>")) return ElementType.CONSTRUCTOR;
		return ElementType.METHOD;
	}
	
	public @CheckForNull AnnotatedObject getContainingScope() {
		try {
	        return Global.getAnalysisCache().getClassAnalysis(XClass.class, getClassDescriptor());
        } catch (CheckedAnalysisException e) {
	         return null;
        }
	}

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#isVarArgs()
     */
    public boolean isVarArgs() {
    	return checkFlag(Constants.ACC_TRANSIENT);
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#usesConcurrency()
     */
    public boolean usesConcurrency() {
	   return usesConcurrency;
    }
    public boolean isStub() {
 	   return isStub;
     }

	final MethodInfo copyAndSetBridgeSignature(String bridgeSignature) {
		MethodDescriptor accessMethodFor = getAccessMethodFor();
		MethodInfo result = new MethodInfo(getSlashedClassName(), getName(), getSignature(), methodSourceSignature, bridgeSignature,
		        accessFlags, isUnconditionalThrower(), isUnsupported(), usesConcurrency, isStub, methodCallCount, exceptions, 
		        accessMethodFor, methodAnnotations, methodParameterAnnotations);
		return result;
		
	}


    public @CheckForNull MethodDescriptor getAccessMethodFor() {
	   return accessMethodFor.get(this);
    }
}

package edu.umd.cs.findbugs.ba;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * XMethod implementation for unresolvable methods.
 * Returns some kind of reasonable default answer to questions
 * that can't be answered (e.g., what are the access flags).
 */
class UnresolvedXMethod extends AbstractMethod  {
	protected UnresolvedXMethod(@DottedClassName String className, String methodName, String methodSig, int accessFlags) {
		super(className, methodName, methodSig, null, accessFlags);
		if (XFactory.DEBUG_UNRESOLVED) {
			System.out.println("Unresolved xmethod: " + this);
		}
	}
	protected UnresolvedXMethod(MethodDescriptor m) {
		super(m.getClassDescriptor().getDottedClassName(), m.getName(), m.getSignature(),m.getBridgeSignature(), 0);
		if (XFactory.DEBUG_UNRESOLVED) {
			System.out.println("Unresolved xmethod: " + this);
		}
	}
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XMethod#isReturnTypeReferenceType()
	 */
	public boolean isReturnTypeReferenceType() {
		SignatureParser parser = new SignatureParser(getSignature());
		String returnTypeSig = parser.getReturnTypeSignature();
		return SignatureParser.isReferenceType(returnTypeSig);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (o instanceof XMethod) {
			return XFactory.compare((XMethod)this, (XMethod)o);
		}
		throw new ClassCastException("Don't know how to compare " + this.getClass().getName() + " to " + o.getClass().getName());
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
     * @see edu.umd.cs.findbugs.ba.XMethod#getThrownExceptions()
     */
    public String[] getThrownExceptions() {
       
        return  new String[0];
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#isUnconditionalThrower()
     */
    public boolean isUnconditionalThrower() {
        return false;
    }

    public boolean isUnsupported() {
        return false;
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#isAbstract()
     */
    public boolean isAbstract() {
        return false;
    }

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isSynthetic()
     */
    public boolean isSynthetic() {
        return false;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isDeprecated()
     */
    public boolean isDeprecated() {
	    return false;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#isVarArgs()
     */
    public boolean isVarArgs() {
	    return false;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#usesConcurrency()
     */
    public boolean usesConcurrency() {
	    return false;
    }

    public @CheckForNull String getSourceSignature() {
	    return null;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#isStub()
     */
    public boolean isStub() {
	    // TODO Auto-generated method stub
	    return false;
    }
    
    Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations = Collections.emptyMap();

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#addParameterAnnotation(int, edu.umd.cs.findbugs.classfile.analysis.AnnotationValue)
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
	
	Map<ClassDescriptor, AnnotationValue> methodAnnotations = Collections.emptyMap();

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#addAnnotation(edu.umd.cs.findbugs.classfile.analysis.AnnotationValue)
     */
	public void addAnnotation(AnnotationValue annotationValue) {
		HashMap<ClassDescriptor, AnnotationValue> updatedAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(methodAnnotations);
		updatedAnnotations.put(annotationValue.getAnnotationClass(), annotationValue);
		methodAnnotations = updatedAnnotations;
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
}

package edu.umd.cs.findbugs.ba;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * XMethod implementation for unresolvable methods.
 * Returns some kind of reasonable default answer to questions
 * that can't be answered (e.g., what are the access flags).
 */
class UnresolvedXMethod extends AbstractMethod implements XMethod {
	protected UnresolvedXMethod(@DottedClassName String className, String methodName, String methodSig, int accessFlags) {
		super(className, methodName, methodSig, accessFlags);
		if (XFactory.DEBUG_UNRESOLVED) {
			System.out.println("Unresolved xmethod: " + this);
		}
	}
	protected UnresolvedXMethod(MethodDescriptor m) {
		super(m.getClassDescriptor().getDottedClassName(), m.getName(), m.getSignature(), 0);
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

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XMethod#getAnnotation(edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	public AnnotationValue getAnnotation(ClassDescriptor desc) {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XMethod#getAnnotationDescriptors()
	 */
	public Collection<ClassDescriptor> getAnnotationDescriptors() {
		return Collections.<ClassDescriptor>emptyList();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XMethod#getAnnotations()
	 */
	public Collection<AnnotationValue> getAnnotations() {
		return Collections.<AnnotationValue>emptyList();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XMethod#getParameterAnnotation(int, edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	public AnnotationValue getParameterAnnotation(int param, ClassDescriptor desc) {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XMethod#getParameterAnnotationDescriptors(int)
	 */
	public Collection<ClassDescriptor> getParameterAnnotationDescriptors(int param) {
		return Collections.<ClassDescriptor>emptyList();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XMethod#getParameterAnnotations(int)
	 */
	public Collection<AnnotationValue> getParameterAnnotations(int param) {
		return Collections.<AnnotationValue>emptyList();
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
        // TODO Auto-generated method stub
        return false;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isDeprecated()
     */
    public boolean isDeprecated() {
	    // TODO Auto-generated method stub
	    return false;
    }

}

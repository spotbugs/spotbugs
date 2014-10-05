package edu.umd.cs.findbugs.ba;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;

/**
 * XMethod implementation for unresolvable methods. Returns some kind of
 * reasonable default answer to questions that can't be answered (e.g., what are
 * the access flags).
 */
class UnresolvedXMethod extends AbstractMethod {

    protected UnresolvedXMethod(MethodDescriptor m) {
        super(m.getClassDescriptor().getDottedClassName(), m.getName(), m.getSignature(),
                m.isStatic() ? Constants.ACC_STATIC : 0);
        if (XFactory.DEBUG_UNRESOLVED) {
            System.out.println("Unresolved xmethod: " + this);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#isReturnTypeReferenceType()
     */
    @Override
    public boolean isReturnTypeReferenceType() {
        SignatureParser parser = new SignatureParser(getSignature());
        String returnTypeSig = parser.getReturnTypeSignature();
        return SignatureParser.isReferenceType(returnTypeSig);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ComparableMethod o) {
        if (o instanceof XMethod) {
            return XFactory.compare((XMethod) this, (XMethod) o);
        }
        throw new ClassCastException("Don't know how to compare " + this.getClass().getName() + " to " + o.getClass().getName());
    }

    @Override
    public ElementType getElementType() {
        if ("<init>".equals(getName())) {
            return ElementType.CONSTRUCTOR;
        }
        return ElementType.METHOD;
    }

    @Override
    public @CheckForNull
    AnnotatedObject getContainingScope() {
        try {
            return Global.getAnalysisCache().getClassAnalysis(XClass.class, getClassDescriptor());
        } catch (CheckedAnalysisException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#getThrownExceptions()
     */
    @Override
    public String[] getThrownExceptions() {

        return new String[0];
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#isUnconditionalThrower()
     */
    @Override
    public boolean isUnconditionalThrower() {
        return false;
    }

    @Override
    public boolean isUnsupported() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#isAbstract()
     */
    @Override
    public boolean isAbstract() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isSynthetic()
     */
    @Override
    public boolean isSynthetic() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.AccessibleEntity#isDeprecated()
     */
    @Override
    public boolean isDeprecated() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#isVarArgs()
     */
    @Override
    public boolean isVarArgs() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#usesConcurrency()
     */
    @Override
    public boolean usesConcurrency() {
        return false;
    }

    @Override
    public @CheckForNull
    String getSourceSignature() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#isStub()
     */
    @Override
    public boolean isStub() {
        return false;
    }

    @Override
    public boolean isIdentity() {
        return false;
    }

    Map<Integer, Map<ClassDescriptor, AnnotationValue>> methodParameterAnnotations = Collections.emptyMap();

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#addParameterAnnotation(int,
     * edu.umd.cs.findbugs.classfile.analysis.AnnotationValue)
     */
    @Override
    public void addParameterAnnotation(int param, AnnotationValue annotationValue) {
        HashMap<Integer, Map<ClassDescriptor, AnnotationValue>> updatedAnnotations = new HashMap<Integer, Map<ClassDescriptor, AnnotationValue>>(
                methodParameterAnnotations);
        Map<ClassDescriptor, AnnotationValue> paramMap = updatedAnnotations.get(param);
        if (paramMap == null) {
            paramMap = new HashMap<ClassDescriptor, AnnotationValue>();
            updatedAnnotations.put(param, paramMap);
        }
        paramMap.put(annotationValue.getAnnotationClass(), annotationValue);

        methodParameterAnnotations = updatedAnnotations;

    }

    @Override
    public Collection<ClassDescriptor> getParameterAnnotationDescriptors(int param) {
        Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
        if (map == null) {
            return Collections.<ClassDescriptor> emptySet();
        }
        return map.keySet();
    }

    @Override
    public boolean hasParameterAnnotations() {
        return !methodParameterAnnotations.isEmpty();
    }

    @Override
    public @Nullable
    AnnotationValue getParameterAnnotation(int param, ClassDescriptor desc) {
        Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
        if (map == null) {
            return null;
        }
        return map.get(desc);
    }

    @Override
    public Collection<AnnotationValue> getParameterAnnotations(int param) {
        Map<ClassDescriptor, AnnotationValue> map = methodParameterAnnotations.get(param);
        if (map == null) {
            return Collections.<AnnotationValue> emptySet();
        }
        return map.values();
    }

    Map<ClassDescriptor, AnnotationValue> methodAnnotations = Collections.emptyMap();

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.XMethod#addAnnotation(edu.umd.cs.findbugs.classfile
     * .analysis.AnnotationValue)
     */
    @Override
    public void addAnnotation(AnnotationValue annotationValue) {
        HashMap<ClassDescriptor, AnnotationValue> updatedAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(
                methodAnnotations);
        updatedAnnotations.put(annotationValue.getAnnotationClass(), annotationValue);
        methodAnnotations = updatedAnnotations;
    }

    @Override
    public Collection<ClassDescriptor> getAnnotationDescriptors() {
        return methodAnnotations.keySet();
    }

    @Override
    public AnnotationValue getAnnotation(ClassDescriptor desc) {
        return methodAnnotations.get(desc);
    }

    @Override
    public Collection<AnnotationValue> getAnnotations() {
        return methodAnnotations.values();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#bridgeFrom()
     */
    @Override
    public XMethod bridgeFrom() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.XMethod#bridgeTo()
     */
    @Override
    public XMethod bridgeTo() {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#getAccessMethodFor()
     */
    @Override
    public MethodDescriptor getAccessMethodForMethod() {
        return null;
    }
    @Override
    public FieldDescriptor getAccessMethodForField() {
        return null;
    }
    /* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XMethod#isVariableSynthetic(int)
     */
    @Override
    public boolean isVariableSynthetic(int param) {
        return false;
    }

    @Override
    public boolean usesInvokeDynamic() {
        return false;
    }


}

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

package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.ba.NullnessAnnotation.*;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.asm.ClassNodeDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.NullnessAnnotation.Parser;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.engine.asm.FindBugsASM;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Checks that overriding methods do not relax {@link Nonnull} (made
 * {@link CheckForNull}) on return values or {@link CheckForNull} (made
 * {@link Nonnull}) on parameters.
 *
 * The code accepts also old (deprecated) nullness annotations from
 * {@link edu.umd.cs.findbugs.annotations} package.
 *
 * @author alienisty (Alessandro Nistico)
 * @author Andrey Loskutov
 */
public class CheckRelaxingNullnessAnnotation extends ClassNodeDetector {

    XClass xclass;

    public CheckRelaxingNullnessAnnotation(BugReporter bugReporter) {
        super(bugReporter);
    }

    @Override
    public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
        xclass = getClassInfo(classDescriptor);
        if(xclass != null){
            super.visitClass(classDescriptor);
        }
    }

    @CheckForNull
    XClass getClassInfo(ClassDescriptor classDescr){
        if(classDescr == null){
            return null;
        }
        try {
            return Global.getAnalysisCache().getClassAnalysis(XClass.class, classDescr);
        } catch (CheckedAnalysisException e) {
            bugReporter.reportMissingClass(classDescr);
            return null;
        }
    }

    @Override
    public MethodVisitor visitMethod(int methodAccess, String methodName, String desc, String methodSignature, String[] exceptions) {
        if ((methodAccess & ACC_STATIC) != 0) {
            // skip static methods
            return null;
        }
        final XMethod xmethod = xclass.findMethod(methodName, desc, false);
        if (xmethod == null) {
            // unable to continue the analysis
            bugReporter.reportSkippedAnalysis(new MethodDescriptor(xclass.getClassDescriptor().getClassName(), methodName, desc,
                    false));
            return null;
        }
        return new DetectorNode(methodAccess, methodName, desc, methodSignature, exceptions, xmethod);
    }

    private final class DetectorNode extends MethodNode {

        private final XMethod xmethod;

        private Map<Integer, NullnessAnnotation> nonNullParameter;

        private boolean relaxedNullReturn;

        DetectorNode(int access, String name, String desc, String signature, String[] exceptions, XMethod xmethod) {
            super(FindBugsASM.ASM_VERSION, access, name, desc, signature, exceptions);
            this.xmethod = xmethod;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            // 1 test if we have suspicious annotations on method or parameters
            relaxedNullReturn = containsRelaxedNonNull(visibleAnnotations);
            if(!relaxedNullReturn){
                relaxedNullReturn = containsRelaxedNonNull(invisibleAnnotations);
            }
            boolean needsCheck = relaxedNullReturn;
            if (invisibleParameterAnnotations != null || visibleParameterAnnotations != null) {
                nonNullParameter = getNonnullOrNullableParams(visibleParameterAnnotations);
                Map<Integer, NullnessAnnotation> nnp = getNonnullOrNullableParams(invisibleParameterAnnotations);
                if (nnp != null) {
                    if (nonNullParameter == null) {
                        nonNullParameter = nnp;
                    } else {
                        nonNullParameter.putAll(nnp);
                    }
                }
                needsCheck |= !nonNullParameter.isEmpty();
            }

            if (!needsCheck) {
                // we can stop, there is no direct violations due annotations applied on the method.
                // However it would be nice to flag Bug2672946B violation too (where the entire class
                // relaxes the parent contract by applying default annotations)
                return;
                // If we continue here, we will flag Bug2672946B violation too on method level,
                // but it would be nice to do it on class or even package level (if package-info.java
                // has default annotations applied which conflicts with parent class contract)
            }

            // 2 look in the hierarchy if we have relaxed contract
            HierarchyIterator hierarchy = new HierarchyIterator(xclass);
            XClass superClass;
            boolean done = false;
            while (!done && (superClass = hierarchy.next()) != null) {
                XMethod method = superClass.findMethod(name, desc, false);
                if (method != null) {
                    done = checkMethod(method);
                } else {
                    for (XMethod superMethod : superClass.getXMethods()) {
                        if (name.equals(superMethod.getName()) && compatibleParameters(desc, superMethod.getSignature())) {
                            if (checkMethod(superMethod)) {
                                done = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        private final boolean checkMethod(@Nonnull XMethod method) {
            boolean foundAny = false;
            if (relaxedNullReturn && containsNullness(method.getAnnotations(), NONNULL)) {
                BugInstance bug = new BugInstance(CheckRelaxingNullnessAnnotation.this, "NP_METHOD_RETURN_RELAXING_ANNOTATION",
                        HIGH_PRIORITY);
                bug.addClassAndMethod(xmethod);
                bugReporter.reportBug(bug);
                foundAny = true;
            }
            if (nonNullParameter != null) {
                for(Map.Entry<Integer, NullnessAnnotation> e : nonNullParameter.entrySet()) {
                    int i = e.getKey();
                    if (containsNullness(method.getParameterAnnotations(i), CHECK_FOR_NULL)) {
                        NullnessAnnotation a = e.getValue();
                        BugInstance bug = new BugInstance(CheckRelaxingNullnessAnnotation.this,
                                "NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION", a.equals(NONNULL) ? HIGH_PRIORITY : NORMAL_PRIORITY);
                        bug.addClassAndMethod(xmethod);
                        LocalVariableAnnotation lva = null;
                        if (localVariables != null) {
                            for(LocalVariableNode lvn : localVariables) {
                                if (lvn.index == i+1) {
                                    lva = new LocalVariableAnnotation(lvn.name, i+1, 0);
                                    lva.setDescription(LocalVariableAnnotation.PARAMETER_NAMED_ROLE);
                                    break;
                                }
                            }
                        }
                        if (lva==null) {
                            lva = new LocalVariableAnnotation("?", i+1, 0);
                            lva.setDescription(LocalVariableAnnotation.PARAMETER_ROLE);
                        }
                        bug.add(lva);
                        bugReporter.reportBug(bug);
                        foundAny = true;
                    }
                }

            }
            return foundAny;
        }
    }

    private class HierarchyIterator {
        private XClass superclass;
        private Queue<ClassDescriptor> interfacesToVisit;
        private final Set<ClassDescriptor> visited;

        public HierarchyIterator(@Nonnull XClass xclass) {
            interfacesToVisit = new LinkedList<ClassDescriptor>(Arrays.asList(xclass.getInterfaceDescriptorList()));
            visited = new HashSet<ClassDescriptor>();
            superclass = getClassInfo(xclass.getSuperclassDescriptor());
        }

        public XClass next() {
            while (!interfacesToVisit.isEmpty()) {
                ClassDescriptor interfaceDescr = interfacesToVisit.poll();
                if (visited.add(interfaceDescr)) {
                    XClass xinterface = getClassInfo(interfaceDescr);
                    if(xinterface != null){
                        interfacesToVisit.addAll(Arrays.asList(xinterface.getInterfaceDescriptorList()));
                        return xinterface;
                    }
                }
            }
            // no interfaces => check super classes
            if (superclass == null) {
                return null;
            }
            XClass currentSuperclass = superclass;
            // compute next one
            superclass = getClassInfo(superclass.getSuperclassDescriptor());
            if(superclass != null){
                interfacesToVisit = new LinkedList<ClassDescriptor>(Arrays.asList(superclass.getInterfaceDescriptorList()));
            }
            return currentSuperclass;
        }
    }

    static boolean containsRelaxedNonNull(@CheckForNull List<AnnotationNode> methodAnnotations) {
        if (methodAnnotations == null) {
            return false;
        }
        for (AnnotationNode annotation : methodAnnotations) {
            NullnessAnnotation nullness = getNullness(annotation.desc);
            if (nullness == CHECK_FOR_NULL || nullness == NULLABLE) {
                return true;
            }
        }
        return false;
    }

    @CheckForNull
    static Map<Integer, NullnessAnnotation> getNonnullOrNullableParams(@CheckForNull List<AnnotationNode>[] parameterAnnotations) {
        if (parameterAnnotations == null) {
            return null;
        }
        Map<Integer, NullnessAnnotation> nonNullParameter = new HashMap<Integer, NullnessAnnotation>();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            List<AnnotationNode> annotations = parameterAnnotations[i];
            if (annotations == null) {
                continue;
            }
            for (AnnotationNode annotation : annotations) {
                NullnessAnnotation nullness = getNullness(annotation.desc);
                if (nullness == null || nullness == CHECK_FOR_NULL) {
                    continue;
                }
                nonNullParameter.put(i, nullness);
            }
        }
        return nonNullParameter;
    }

    @CheckForNull
    static NullnessAnnotation getNullness(@SlashedClassName String annotationDesc) {
        if (annotationDesc.length() < 2) {
            return null;
        }
        // remove L; from signature
        String substring = annotationDesc.substring(1, annotationDesc.length() - 1);
        return Parser.parse(ClassName.toDottedClassName(substring));
    }

    static boolean containsNullness(Collection<AnnotationValue> annotations, NullnessAnnotation nullness) {
        for (AnnotationValue annotation : annotations) {
            NullnessAnnotation check = Parser.parse(annotation.getAnnotationClass().getDottedClassName());
            if (check == nullness) {
                return true;
            }
        }
        return false;
    }

    static boolean compatibleParameters(String signature, String superSignature) {
        SignatureParser sig = new SignatureParser(signature);
        SignatureParser superSig = new SignatureParser(superSignature);
        if (sig.getNumParameters() == superSig.getNumParameters()) {
            Iterator<String> params = sig.parameterSignatureIterator();
            Iterator<String> superParams = superSig.parameterSignatureIterator();
            while (params.hasNext()) {
                String param = params.next();
                String superParam = superParams.next();
                if (areRelated(param, superParam)) {
                    continue;
                }
                return false;
            }
            String retSig = sig.getReturnTypeSignature();
            String superRetSig = superSig.getReturnTypeSignature();
            if (areRelated(retSig, superRetSig)) {
                // it is compatible
                return true;
            }
        }
        return false;
    }

    static boolean areRelated(String sig, String superSig) {
        try {
            if (sig.equals(superSig)) {
                return true;
            }
            if (sig.charAt(0) == 'L' && superSig.charAt(0) == 'L') {
                sig = sig.substring(1, sig.length() - 1);
                superSig = superSig.substring(1, superSig.length() - 1);
                return Hierarchy.isSubtype(sig, superSig);
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        return false;
    }
}

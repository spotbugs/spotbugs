/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;

public class FindMaskedFields extends BytecodeScanningDetector {
    private final BugReporter bugReporter;

    private int numParms;

    private final Map<String, Field> classFields = new HashMap<String, Field>();

    private boolean staticMethod;

    private final Collection<RememberedBug> rememberedBugs = new LinkedList<RememberedBug>();

    static class RememberedBug {
        BugInstance bug;

        XField maskingField, maskedField;

        RememberedBug(BugInstance bug, FieldAnnotation maskingField, FieldAnnotation maskedField) {
            this.bug = bug;
            this.maskingField = XFactory.createXField(maskingField);
            this.maskedField = XFactory.createXField(maskedField);
        }
    }

    public FindMaskedFields(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass obj = classContext.getJavaClass();
        if (!obj.isInterface()) {
            classContext.getJavaClass().accept(this);
        }
    }

    @Override
    public void visit(JavaClass obj) {
        classFields.clear();

        Field[] fields = obj.getFields();
        String fieldName;
        for (Field field : fields) {
            if (!field.isStatic() && !field.isPrivate()) {
                fieldName = field.getName();
                classFields.put(fieldName, field);
            }
        }

        // Walk up the super class chain, looking for name collisions

        XClass c = getXClass();
        while (true) {
            ClassDescriptor s = c.getSuperclassDescriptor();
            if (s == null || "java/lang/Object".equals(s.getClassName())) {
                break;
            }
            try {
                c = Global.getAnalysisCache().getClassAnalysis(XClass.class, s);
            } catch (CheckedAnalysisException e) {
                break;
            }
            XClass superClass = c;
            for (XField fld : c.getXFields()) {
                if (!fld.isStatic() && (fld.isPublic() || fld.isProtected())) {
                    fieldName = fld.getName();
                    if (fieldName.length() == 1) {
                        continue;
                    }
                    if ("serialVersionUID".equals(fieldName)) {
                        continue;
                    }
                    String superClassName = s.getClassName();
                    if (superClassName.startsWith("java/io")
                            && (superClassName.endsWith("InputStream") && "in".equals(fieldName) || superClassName
                                    .endsWith("OutputStream") && "out".equals(fieldName))) {
                        continue;
                    }
                    if (classFields.containsKey(fieldName)) {
                        Field maskingField = classFields.get(fieldName);
                        String mClassName = getDottedClassName();
                        FieldAnnotation fa = new FieldAnnotation(mClassName, maskingField.getName(), maskingField.getSignature(),
                                maskingField.isStatic());
                        int priority = NORMAL_PRIORITY;
                        if (maskingField.isStatic() || maskingField.isFinal()) {
                            priority++;
                        } else if (fld.getSignature().charAt(0) == 'L' && !fld.getSignature().startsWith("Ljava/lang/")
                                || fld.getSignature().charAt(0) == '[') {
                            priority--;
                        }
                        if (!fld.getSignature().equals(maskingField.getSignature())) {
                            priority += 2;
                        } else if (fld.getAccessFlags() != maskingField.getAccessFlags()) {
                            priority++;
                        }
                        if (fld.isSynthetic() || fld.getName().indexOf('$') >= 0) {
                            priority++;
                        }

                        FieldAnnotation maskedFieldAnnotation = FieldAnnotation.fromFieldDescriptor(fld.getFieldDescriptor());
                        BugInstance bug = new BugInstance(this, "MF_CLASS_MASKS_FIELD", priority).addClass(this).addField(fa)
                                .describe("FIELD_MASKING").addField(maskedFieldAnnotation).describe("FIELD_MASKED");
                        rememberedBugs.add(new RememberedBug(bug, fa, maskedFieldAnnotation));

                    }
                }
            }
        }

        super.visit(obj);
    }

    @Override
    public void visit(Method obj) {
        super.visit(obj);
        numParms = getNumberMethodArguments();
        if (!obj.isStatic()) {
            numParms++;
        }
        // System.out.println(obj);
        // System.out.println(numParms);
        staticMethod = obj.isStatic();
    }

    /**
     * This property enables production of warnings for locals which obscure
     * fields.
     */
    private static final boolean ENABLE_LOCALS = SystemProperties.getBoolean("findbugs.maskedfields.locals");

    @Override
    public void visit(LocalVariableTable obj) {
        if (ENABLE_LOCALS) {
            if (staticMethod) {
                return;
            }

            LocalVariable[] vars = obj.getLocalVariableTable();
            // System.out.println("Num params = " + numParms);
            for (LocalVariable var : vars) {
                if (var.getIndex() < numParms) {
                    continue;
                }
                String varName = var.getName();
                if ("serialVersionUID".equals(varName)) {
                    continue;
                }
                Field f = classFields.get(varName);
                // System.out.println("Checking " + varName);
                // System.out.println(" got " + f);
                // TODO: we could distinguish between obscuring a field in the
                // same class
                // vs. obscuring a field in a superclass. Not sure how important
                // that is.
                if (f != null) {
                    FieldAnnotation fa = FieldAnnotation.fromBCELField(getDottedClassName(), f);
                    if (true || var.getStartPC() > 0) {
                        bugReporter.reportBug(new BugInstance(this, "MF_METHOD_MASKS_FIELD", LOW_PRIORITY)
                        .addClassAndMethod(this).addField(fa).addSourceLine(this, var.getStartPC() - 1));
                    }
                }
            }
        }
        super.visit(obj);
    }

    @Override
    public void report() {
        UnreadFieldsData unreadFields = AnalysisContext.currentAnalysisContext().getUnreadFieldsData();
        for (RememberedBug rb : rememberedBugs) {
            BugInstance bug = rb.bug;
            int score1 = 0;
            int score2 = 0;
            int priority = bug.getPriority();
            if (unreadFields.classesScanned.contains(rb.maskedField.getClassName())) {
                if (unreadFields.getReadFields().contains(rb.maskedField)) {
                    score1++;
                }
                if (unreadFields.getWrittenFields().contains(rb.maskedField)) {
                    score1++;
                }
                if (unreadFields.isWrittenOutsideOfInitialization(rb.maskedField)) {
                    score1++;
                }
            } else {
                score1 += 2;
            }
            if (unreadFields.getReadFields().contains(rb.maskingField)) {
                score2++;
            }
            if (unreadFields.getWrittenFields().contains(rb.maskingField)) {
                score2++;
            }
            if (unreadFields.isWrittenOutsideOfInitialization(rb.maskingField)) {
                score2++;
            }
            int score = score1 + score2;
            if (score1 == 0 || score2 == 0) {
                bug.setPriority(priority + 1);
            } else if (score >= 5) {
                bug.setPriority(priority - 1);
            } else if (score < 3) {
                bug.setPriority(priority + 1);
            }
            bugReporter.reportBug(bug);
        }
    }
}


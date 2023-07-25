/*
 * SpotBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.util.ClassName;
import org.apache.bcel.classfile.*;

import java.util.ArrayList;
import java.util.ListIterator;

import static edu.umd.cs.findbugs.detect.PublicIdentifiers.PUBLIC_IDENTIFIERS;

public class DontReusePublicIdentifiers extends BytecodeScanningDetector {
    private final BugReporter bugReporter;
    private String sourceFileName = "";
    private JavaClass currentClass;

    public DontReusePublicIdentifiers(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass obj = classContext.getJavaClass();

        if (PublicIdentifiers.isPartOfStandardLibrary(classContext.getXClass().getClassDescriptor().getPackageName())) {
            return;
        }

        // save the source file name and class name for inner classes
        sourceFileName = obj.getFileName();
        currentClass = obj;
        classContext.getJavaClass().accept(this);
    }

    @Override
    public void visit(JavaClass obj) {
        String simpleName = ClassName.extractSimpleName(obj.getClassName());
        if (PUBLIC_IDENTIFIERS.contains(simpleName)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES", NORMAL_PRIORITY)
                    .addClass(this));
        }
    }

    @Override
    public void visit(Field obj) {
        String name = obj.getName();
        if (PUBLIC_IDENTIFIERS.contains(name)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES", NORMAL_PRIORITY)
                    .addClass(this)
                    .addField(this));
        }
    }

    private boolean lookUpMethod(Method method, JavaClass[] classes) {
        for (JavaClass cls : classes) {
            Method[] methods = cls.getMethods();
            for (Method m : methods) {
                if (method.equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void visit(Method obj) {
        String name = obj.getName();

        if (PUBLIC_IDENTIFIERS.contains(name)) {
            try {
                JavaClass[] supers = currentClass.getSuperClasses();
                boolean foundInSupers = lookUpMethod(obj, supers);

                JavaClass[] interfaces = currentClass.getAllInterfaces();
                boolean foundInInterfaces = lookUpMethod(obj, interfaces);

                if (!foundInSupers && !foundInInterfaces) {
                    bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_METHOD_NAMES", NORMAL_PRIORITY)
                            .addClassAndMethod(this)
                            .addSourceLine(SourceLineAnnotation.forEntireMethod(currentClass, obj)));
                }

            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
            }
        }
    }

    @Override
    public void visit(LocalVariableTable obj) {
        LocalVariable[] variables = obj.getLocalVariableTable();

        for (LocalVariable variable : variables) {
            String varName = variable.getName();

            if ("this".equals(varName)) {
                continue;
            }

            if (PUBLIC_IDENTIFIERS.contains(varName)) {
                LocalVariableAnnotation localVariableAnnotation = new LocalVariableAnnotation(varName, variable.getIndex(), this.getPC());
                bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_LOCAL_VARIABLE_NAMES", NORMAL_PRIORITY)
                        .addClassAndMethod(this)
                        .add(localVariableAnnotation)
                        .addSourceLine(getClassContext(), this, this.getPC()));
            }
        }
    }

    private String lookUpHierarchy(InnerClass clazz, InnerClasses obj) {
        ConstantPool constantPool = obj.getConstantPool();
        int outerClassIndex = currentClass.getClassNameIndex();

        if (clazz.getOuterClassIndex() == outerClassIndex) {
            int parentNameIndex = ((ConstantClass) constantPool.getConstant(outerClassIndex)).getNameIndex();
            return String.format("%s.%s",
                    ClassName.toDottedClassName(constantPool.getConstantUtf8(parentNameIndex).getBytes()),
                    constantPool.getConstantUtf8(clazz.getInnerNameIndex()).getBytes());
        }

        ArrayList<String> classHierarchy = new ArrayList<>();
        InnerClass[] innerClasses = obj.getInnerClasses();
        classHierarchy.add(constantPool.getConstantUtf8(clazz.getInnerNameIndex()).getBytes());

        InnerClass current = clazz;
        int parentIndex = current.getOuterClassIndex();
        while (parentIndex != outerClassIndex) {
            boolean foundParent = false;
            for (InnerClass cls : innerClasses) {
                if (cls.getInnerClassIndex() == parentIndex) {
                    current = cls;
                    parentIndex = current.getOuterClassIndex();
                    foundParent = true;
                    break;
                }
            }
            // fallback to inner class name only
            if (!foundParent) {
                return constantPool.getConstantUtf8(clazz.getInnerNameIndex()).getBytes();
            }
            classHierarchy.add(constantPool.getConstantUtf8(current.getInnerNameIndex()).getBytes());
        }

        final int outerClassNameIndex = ((ConstantClass) constantPool.getConstant(outerClassIndex)).getNameIndex();
        final StringBuilder sb = new StringBuilder();
        sb.append(constantPool.getConstantUtf8(outerClassNameIndex).getBytes());

        ListIterator<String> revIterator = classHierarchy.listIterator(classHierarchy.size());
        while (revIterator.hasPrevious()) {
            sb.append(".").append(revIterator.previous());
        }

        return sb.toString();
    }

    @Override
    public void visitInnerClasses(InnerClasses obj) {
        super.visitInnerClasses(obj);

        for (InnerClass cls : obj.getInnerClasses()) {

            // get the name of the cls through its name index
            int nameIndex = cls.getInnerNameIndex();
            if (nameIndex == 0) {
                continue;
            }

            // check whether the inner class is from the standard library
            int innerClassIndex = cls.getInnerClassIndex();
            if (innerClassIndex == 0) {
                continue;
            }

            ConstantClass constantClass = obj.getConstantPool().getConstant(innerClassIndex);
            int constantClassIndex = constantClass.getNameIndex();
            String fullQualifiedName = ClassName.toDottedClassName(obj.getConstantPool().getConstantUtf8(constantClassIndex).getBytes());

            boolean isPartOfJSL = PublicIdentifiers.isPartOfStandardLibrary(ClassName.extractPackageName(fullQualifiedName));
            if (isPartOfJSL) {
                continue;
            }

            String classHierarchy = lookUpHierarchy(cls, obj);
            String innerClassName = obj.getConstantPool().getConstantUtf8(nameIndex).getBytes();
            if (PUBLIC_IDENTIFIERS.contains(innerClassName)) {
                bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES", NORMAL_PRIORITY)
                        .addClass(this)
                        .addClass(innerClassName, sourceFileName)
                        .addString(classHierarchy));
            }
        }
    }
}

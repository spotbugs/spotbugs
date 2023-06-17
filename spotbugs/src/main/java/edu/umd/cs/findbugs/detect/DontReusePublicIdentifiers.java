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
import edu.umd.cs.findbugs.ba.ClassContext;
import org.apache.bcel.classfile.*;

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

        // save the source file name and class name for inner classes
        sourceFileName = obj.getFileName();
        currentClass = obj;
        classContext.getJavaClass().accept(this);
    }

    @Override
    public void visit(JavaClass obj) {
        String[] fullName = obj.getClassName().split("\\.");
        String simpleName = fullName[fullName.length - 1];

        if (PUBLIC_IDENTIFIERS.contains(simpleName)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES", NORMAL_PRIORITY)
                    .addClass(this)
                    .addUnknownSourceLine(simpleName, sourceFileName));
        }
    }

    @Override
    public void visit(Field obj) {
        String name = obj.getName();
        if (PUBLIC_IDENTIFIERS.contains(name)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES", NORMAL_PRIORITY)
                    .addClass(this)
                    .addField(this)
                    .addUnknownSourceLine(getClassName(), sourceFileName));
        }
    }

    private boolean lookUpMethod(Method method, JavaClass[] classes) {
        for (JavaClass cls : classes) {
            Method[] methods = cls.getMethods();
            for (Method m : methods) {
                if (m.getName().equals(method.getName()) && method.getSignature().equals(m.getSignature())) {
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
                boolean foundInSupers;
                boolean foundInInterfaces;

                JavaClass[] supers = currentClass.getSuperClasses();
                foundInSupers = lookUpMethod(obj, supers);

                JavaClass[] interfaces = currentClass.getAllInterfaces();
                foundInInterfaces = lookUpMethod(obj, interfaces);

                if (!foundInSupers && !foundInInterfaces) {
                    bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_METHOD_NAMES", NORMAL_PRIORITY)
                            .addClassAndMethod(this)
                            .addSourceLine(SourceLineAnnotation.forEntireMethod(currentClass, obj)));
                }

            } catch (ClassNotFoundException e) {
                // if supers couldn't be found, we can't check for the method
                // but should not let the bug go unnoticed
                bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_METHOD_NAMES", NORMAL_PRIORITY)
                        .addClassAndMethod(this)
                        .addSourceLine(SourceLineAnnotation.forEntireMethod(currentClass, obj)));
                System.err.println("Could not get supers for " + currentClass.getClassName());
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
                        .addSourceLine(getClassContext(), this, this.getPC())
                        .add(localVariableAnnotation));
            }
        }
    }

    @Override
    public void visitInnerClasses(InnerClasses obj) {
        super.visitInnerClasses(obj);

        ConstantPool constantPool = obj.getConstantPool();
        for (InnerClass cls : obj.getInnerClasses()) {

            // get the name of the cls through its name index
            int nameIndex = cls.getInnerNameIndex();
            if (nameIndex == 0) {
                continue;
            }
            String innerClassName = constantPool.getConstantUtf8(nameIndex).getBytes();
            if (PUBLIC_IDENTIFIERS.contains(innerClassName)) {
                bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_INNER_CLASS_NAMES", NORMAL_PRIORITY)
                        .addClass(this)
                        .addClass(innerClassName, sourceFileName)
                        .addUnknownSourceLine(innerClassName, sourceFileName));
            }
        }
    }
}

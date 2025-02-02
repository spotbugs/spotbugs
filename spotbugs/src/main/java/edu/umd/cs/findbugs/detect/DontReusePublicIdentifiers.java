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

import static edu.umd.cs.findbugs.detect.PublicIdentifiers.PUBLIC_IDENTIFIERS;

public class DontReusePublicIdentifiers extends BytecodeScanningDetector {
    private final BugReporter bugReporter;
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
}

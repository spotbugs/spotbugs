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
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.detect.publicidentifiers.PublicIdentifiers;
import org.apache.bcel.classfile.*;

import java.util.Set;

public class DontReusePublicIdentifiers extends BytecodeScanningDetector {

    private static final Set<String> PUBLIC_IDENTIFIERS = new PublicIdentifiers().getPublicIdentifiers();

    private final BugReporter bugReporter;
    private String sourceFileName = "";

    public DontReusePublicIdentifiers(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass obj = classContext.getJavaClass();

        // save the source file name and class name for inner classes
        sourceFileName = obj.getFileName();
        classContext.getJavaClass().accept(this);
    }

    //TODO:
    // One bug for Class names: PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES
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

    //TODO:
    // One bug for Class names: PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES
    @Override
    public void visit(Field obj) {
        String name = obj.getName();
        XMethod idk = getXMethod();
        if (PUBLIC_IDENTIFIERS.contains(name)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES", NORMAL_PRIORITY)
                    .addClass(this)
                    .addField(this)
                    .addUnknownSourceLine(getClassName(), sourceFileName));
            // .addSourceLine(this)); //here    java.lang.IllegalStateException: getMethod called while not visiting method
        }

    }

    //TODO:
    // One bug for Class names: PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_METHOD_NAMES
    @Override
    public void visit(Method obj) {
        String name = obj.getName();
        if (PUBLIC_IDENTIFIERS.contains(name)) {
            bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_METHOD_NAMES", NORMAL_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this));
        }
    }

    //TODO:
    // One bug for Class names: PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_LOCAL_VARIABLE_NAMES
    @Override
    public void visit(LocalVariableTable obj) {
        LocalVariable[] vars = obj.getLocalVariableTable();

        for (LocalVariable var : vars) {
            String varName = var.getName();

            if ("this".equals(varName)) {
                continue;
            }

            if (PUBLIC_IDENTIFIERS.contains(varName)) {
                bugReporter.reportBug(new BugInstance(this, "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_LOCAL_VARIABLE_NAMES", NORMAL_PRIORITY)
                        .addClassAndMethod(this)
                        .addSourceLine(getClassContext(), this, this.getPC())
                        .addString(varName));
            }
        }
    }

    //TODO:
    // One bug for Class names: PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_INNER_CLASS_NAMES
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
                // .addUnknownSourceLine(innerClassName, sourceFileName));
            }
        }
    }
}

/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.internalAnnotations.StaticConstant;

public class XMLFactoryBypass extends BytecodeScanningDetector {
    private final BugReporter bugReporter;

    @StaticConstant
    private static final Set<String> xmlInterfaces = new HashSet<String>() {
        static final long serialVersionUID = -9117982073509840017L;
        {
            add("javax.xml.parsers.DocumentBuilder");
            add("org.w3c.dom.Document");
            add("javax.xml.parsers.SAXParser");
            add("org.xml.sax.XMLReader");
            add("org.xml.sax.XMLFilter");
            add("javax.xml.transform.Transformer");
            add("org.w3c.dom.Attr");
            add("org.w3c.dom.CDATASection");
            add("org.w3c.dom.Comment");
            add("org.w3c.dom.Element");
            add("org.w3c.dom.Text");
        }
    };

    private final Set<String> rejectedXMLClasses = new HashSet<String>();

    private JavaClass curClass;

    public XMLFactoryBypass(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        curClass = classContext.getJavaClass();
        super.visitClassContext(classContext);
    }

    @Override
    public void sawOpcode(int seen) {
        try {
            if (seen == INVOKESPECIAL) {
                String newClsName = getClassConstantOperand();
                if (rejectedXMLClasses.contains(newClsName)) {
                    return;
                }
                rejectedXMLClasses.add(newClsName);

                if (newClsName.startsWith("java/") || newClsName.startsWith("javax/")) {
                    return;
                }

                if (newClsName.endsWith("Adapter")) {
                    return;
                }

                if (!"<init>".equals(getNameConstantOperand())) {
                    return;
                }

                String invokerClsName = this.getClassName();
                if (samePackageBase(invokerClsName, newClsName)) {
                    return;
                }

                JavaClass newCls = Repository.lookupClass(getDottedClassConstantOperand());

                JavaClass superCls = curClass.getSuperClass();
                if (superCls.getClassName().equals(newClsName.replace('/', '.'))) {
                    return;
                }

                JavaClass[] infs = newCls.getAllInterfaces();
                for (JavaClass inf : infs) {
                    if (xmlInterfaces.contains(inf.getClassName())) {
                        bugReporter.reportBug(new BugInstance(this, "XFB_XML_FACTORY_BYPASS", LOW_PRIORITY).addClassAndMethod(
                                this).addSourceLine(this));
                        rejectedXMLClasses.remove(newClsName);
                    }
                }
            }
        } catch (ClassNotFoundException cnfe) {
            bugReporter.reportMissingClass(cnfe);
        }
    }

    public boolean samePackageBase(String invokerClsName, String newClsName) {
        String[] invokerParts = invokerClsName.split("/");
        String[] newClsParts = newClsName.split("/");

        if (newClsParts.length < 3) {
            return false;
        }
        if (invokerParts.length < 3) {
            return false;
        }

        if (!invokerParts[0].equals(newClsParts[0])) {
            return false;
        }

        return invokerParts[1].equals(newClsParts[1]);
    }
}

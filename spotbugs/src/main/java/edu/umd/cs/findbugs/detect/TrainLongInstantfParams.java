/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import java.util.Iterator;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.TrainingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.ParameterProperty;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class TrainLongInstantfParams extends PreorderVisitor implements Detector, TrainingDetector {

    static class LongInstantParameterDatabase extends MethodPropertyDatabase<ParameterProperty> {
        @Override
        protected ParameterProperty decodeProperty(String propStr) throws PropertyDatabaseFormatException {
            try {
                int longInstants = Integer.parseInt(propStr);
                ParameterProperty prop = new ParameterProperty(longInstants);
                return prop;
            } catch (NumberFormatException e) {
                throw new PropertyDatabaseFormatException("Invalid unconditional deref param set: " + propStr);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.ba.interproc.MethodPropertyDatabase#encodeProperty
         * (Property)
         */

        @Override
        protected String encodeProperty(ParameterProperty property) {
            return String.valueOf(property.getParamsWithProperty());
        }

    }

    LongInstantParameterDatabase database = new LongInstantParameterDatabase();

    public TrainLongInstantfParams(BugReporter bugReporter) {
    }

    @Override
    public void visit(Code obj) {

        if (!getMethod().isPublic() && !getMethod().isProtected()) {
            return;
        }
        SignatureParser p = new SignatureParser(getMethodSig());
        LocalVariableTable t = obj.getLocalVariableTable();

        if (t == null) {
            return;
        }
        ParameterProperty property = new ParameterProperty();

        int index = getMethod().isStatic() ? 0 : 1;
        int parameterNumber = 0;
        for (Iterator<String> i = p.parameterSignatureIterator(); i.hasNext();) {
            String s = i.next();
            LocalVariable localVariable = t.getLocalVariable(index, 0);
            if (localVariable != null) {
                String name = localVariable.getName();
                if ("J".equals(s) && (name.toLowerCase().indexOf("instant") >= 0 || name.startsWith("date"))) {

                    // System.out.println(getFullyQualifiedMethodName() + " " + s + " " + index + " " + name);
                    property.setParamWithProperty(parameterNumber, true);
                }
            }
            if ("J".equals(s) || "D".equals(s)) {
                index += 2;
            } else {
                index += 1;
            }
            parameterNumber++;
        }
        if (!property.isEmpty()) {
            // System.out.println(getFullyQualifiedMethodName() + " " + property);
            database.setProperty(getMethodDescriptor(), property);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.Detector#report()
     */
    @Override
    public void report() {
        // System.out.println(database.entrySet().size() + " methods");
        AnalysisContext.currentAnalysisContext().storePropertyDatabase(database, "longInstant.db", "long instant database");

    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        classContext.getJavaClass().accept(this);
    }

}

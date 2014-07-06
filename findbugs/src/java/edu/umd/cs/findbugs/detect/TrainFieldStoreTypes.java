/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.TrainingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.type.FieldStoreType;
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;

/**
 * Build a database of reference types stored into fields. This can be used in
 * the future to improve the precision of type analysis when values are loaded
 * from fields.
 *
 * @author David Hovemeyer
 */
public class TrainFieldStoreTypes implements Detector, TrainingDetector {
    private final BugReporter bugReporter;

    private final FieldStoreTypeDatabase database;

    public TrainFieldStoreTypes(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.database = new FieldStoreTypeDatabase();
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Method[] methodList = classContext.getJavaClass().getMethods();
        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (CFGBuilderException e) {
                bugReporter.logError("Error compting field store types", e);
            } catch (DataflowAnalysisException e) {
                bugReporter.logError("Error compting field store types", e);
            }
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException
    {
        CFG cfg = classContext.getCFG(method);
        TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            Instruction ins = location.getHandle().getInstruction();
            short opcode = ins.getOpcode();

            // Field store instruction?
            if (opcode != Constants.PUTFIELD && opcode != Constants.PUTSTATIC) {
                continue;
            }

            // Check if field type is a reference type
            FieldInstruction fins = (FieldInstruction) ins;
            Type fieldType = fins.getType(cpg);
            if (!(fieldType instanceof ReferenceType)) {
                continue;
            }

            // Find the exact field being stored into
            XField xfield = Hierarchy.findXField(fins, cpg);
            if (xfield == null) {
                continue;
            }

            // Skip public and protected fields, since it is reasonable to
            // assume
            // we won't see every store to those fields
            if (xfield.isPublic() || xfield.isProtected()) {
                continue;
            }

            // The top value on the stack is the one which will be stored
            // into the field
            TypeFrame frame = typeDataflow.getFactAtLocation(location);
            if (!frame.isValid()) {
                continue;
            }
            Type storeType = frame.getTopValue();
            if (!(storeType instanceof ReferenceType)) {
                continue;
            }

            // Get or create the field store type set
            FieldStoreType property = database.getProperty(xfield.getFieldDescriptor());
            if (property == null) {
                property = new FieldStoreType();
                database.setProperty(xfield.getFieldDescriptor(), property);
            }

            // Add the store type to the set
            property.addTypeSignature(storeType.getSignature());
        }
    }

    @Override
    public void report() {
        database.purgeBoringEntries();
        AnalysisContext.currentAnalysisContext().storePropertyDatabase(database, FieldStoreTypeDatabase.DEFAULT_FILENAME,
                "store type database");
    }

}

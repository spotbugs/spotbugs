/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.util.BitSet;
import java.util.Iterator;

import javax.annotation.CheckForNull;

import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.IndexedInstruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LiveLocalStoreAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.util.EditDistance;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Bug annotation class for local variable names
 *
 * @author William Pugh
 * @see BugAnnotation
 */
public class LocalVariableAnnotation implements BugAnnotation {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_ROLE = "LOCAL_VARIABLE_DEFAULT";

    public static final String NAMED_ROLE = "LOCAL_VARIABLE_NAMED";

    public static final String UNKNOWN_ROLE = "LOCAL_VARIABLE_UNKNOWN";

    public static final String PARAMETER_ROLE = "LOCAL_VARIABLE_PARAMETER";

    public static final String PARAMETER_NAMED_ROLE = "LOCAL_VARIABLE_PARAMETER_NAMED";

    public static final String PARAMETER_VALUE_SOURCE_ROLE = "LOCAL_VARIABLE_PARAMETER_VALUE_SOURCE";

    public static final String PARAMETER_VALUE_SOURCE_NAMED_ROLE = "LOCAL_VARIABLE_PARAMETER_VALUE_SOURCE_NAMED";

    public static final String VALUE_DOOMED_ROLE = "LOCAL_VARIABLE_VALUE_DOOMED";

    public static final String VALUE_DOOMED_NAMED_ROLE = "LOCAL_VARIABLE_VALUE_DOOMED_NAMED";

    public static final String DID_YOU_MEAN_ROLE = "LOCAL_VARIABLE_DID_YOU_MEAN";

    public static final String INVOKED_ON_ROLE = "LOCAL_VARIABLE_INVOKED_ON";

    public static final String ARGUMENT_ROLE = "LOCAL_VARIABLE_ARGUMENT";

    public static final String VALUE_OF_ROLE = "LOCAL_VARIABLE_VALUE_OF";

    final private String name;

    final int register, pc;

    final int line;

    private String description;

    /**
     * Constructor.
     *
     * @param name
     *            the name of the local variable
     * @param register
     *            the local variable index
     * @param pc
     *            the bytecode offset of the instruction that mentions this
     *            local variable
     */
    public LocalVariableAnnotation(String name, int register, int pc) {
        this.name = name;
        this.register = register;
        this.pc = pc;
        this.line = -1;
        this.description = DEFAULT_ROLE;
        this.setDescription("?".equals(name) ? "LOCAL_VARIABLE_UNKNOWN" : "LOCAL_VARIABLE_NAMED");
    }

    /**
     * Constructor.
     *
     * @param name
     *            the name of the local variable
     * @param register
     *            the local variable index
     * @param pc
     *            the bytecode offset of the instruction that mentions this
     *            local variable
     */
    public LocalVariableAnnotation(String name, int register, int pc, int line) {
        this.name = name;
        this.register = register;
        this.pc = pc;
        this.line = line;
        this.description = DEFAULT_ROLE;
        this.setDescription("?".equals(name) ? "LOCAL_VARIABLE_UNKNOWN" : "LOCAL_VARIABLE_NAMED");
    }

    public static LocalVariableAnnotation getLocalVariableAnnotation(Method method, Location location, IndexedInstruction ins) {
        int local = ins.getIndex();
        InstructionHandle handle = location.getHandle();
        int position1 = handle.getNext().getPosition();
        int position2 = handle.getPosition();
        return getLocalVariableAnnotation(method, local, position1, position2);
    }

    public static LocalVariableAnnotation getLocalVariableAnnotation(Method method, int local, int position1, int position2) {

        LocalVariableTable localVariableTable = method.getLocalVariableTable();
        String localName = "?";
        if (localVariableTable != null) {
            LocalVariable lv1 = localVariableTable.getLocalVariable(local, position1);
            if (lv1 == null) {
                lv1 = localVariableTable.getLocalVariable(local, position2);
                position1 = position2;
            }
            if (lv1 != null) {
                localName = lv1.getName();
            } else {
                for (LocalVariable lv : localVariableTable.getLocalVariableTable()) {
                    if (lv.getIndex() == local) {
                        if (!"?".equals(localName) && !localName.equals(lv.getName())) {
                            // not a single consistent name
                            localName = "?";
                            break;
                        }
                        localName = lv.getName();
                    }
                }
            }
        }
        LineNumberTable lineNumbers = method.getLineNumberTable();
        if (lineNumbers == null) {
            return new LocalVariableAnnotation(localName, local, position1);
        }
        int line = lineNumbers.getSourceLine(position1);
        return new LocalVariableAnnotation(localName, local, position1, line);
    }

    /**
     * Get a local variable annotation describing a parameter.
     *
     * @param method
     *            a Method
     * @param local
     *            the local variable containing the parameter
     * @return LocalVariableAnnotation describing the parameter
     */
    public static LocalVariableAnnotation getParameterLocalVariableAnnotation(Method method, int local) {
        LocalVariableAnnotation lva = getLocalVariableAnnotation(method, local, 0, 0);
        if (lva.isNamed()) {
            lva.setDescription(LocalVariableAnnotation.PARAMETER_NAMED_ROLE);
        } else {
            lva.setDescription(LocalVariableAnnotation.PARAMETER_ROLE);
        }
        return lva;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void accept(BugAnnotationVisitor visitor) {
        visitor.visitLocalVariableAnnotation(this);
    }

    @Override
    public String format(String key, ClassAnnotation primaryClass) {
        // System.out.println("format: " + key + " reg: " + register + " name: "
        // + value);
        if ("hash".equals(key)) {
            if (register < 0) {
                return "??";
            }
            return name;
        }
        if (register < 0) {
            return "?";
        }
        if ("register".equals(key)) {
            return String.valueOf(register);
        } else if ("pc".equals(key)) {
            return String.valueOf(pc);
        } else if ("name".equals(key) || "givenClass".equals(key)) {
            return name;
        } else if (!"?".equals(name)) {
            return name;
        }
        return "$L" + register;
    }

    @Override
    public void setDescription(String description) {
        this.description = description.intern();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LocalVariableAnnotation)) {
            return false;
        }
        return name.equals(((LocalVariableAnnotation) o).name);
    }

    @Override
    public int compareTo(BugAnnotation o) {
        if (!(o instanceof LocalVariableAnnotation)) {
            // Comparable with any type
            // of BugAnnotation
            return this.getClass().getName().compareTo(o.getClass().getName());
        }
        return name.compareTo(((LocalVariableAnnotation) o).name);
    }

    @Override
    public String toString() {
        String pattern = I18N.instance().getAnnotationDescription(description);
        FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
        return format.format(new BugAnnotation[] { this }, null);
    }

    /*
     * ----------------------------------------------------------------------
     * XML Conversion support
     * ----------------------------------------------------------------------
     */

    private static final String ELEMENT_NAME = "LocalVariable";

    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        writeXML(xmlOutput, false, false);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean addMessages, boolean isPrimary) throws IOException {
        XMLAttributeList attributeList = new XMLAttributeList().addAttribute("name", name)
                .addAttribute("register", String.valueOf(register)).addAttribute("pc", String.valueOf(pc));

        String role = getDescription();
        if (!DEFAULT_ROLE.equals(role)) {
            attributeList.addAttribute("role", role);
        }

        BugAnnotationUtil.writeXML(xmlOutput, ELEMENT_NAME, this, attributeList, addMessages);
    }

    public boolean isNamed() {
        return register >= 0 && !"?".equals(name);
    }

    /**
     * @return name of local variable
     */
    public String getName() {
        return name;
    }

    public int getPC() {
        return pc;
    }

    public int getRegister() {
        return register;
    }

    @Override
    public boolean isSignificant() {
        return !"?".equals(name);
    }

    public static @CheckForNull
    LocalVariableAnnotation getLocalVariableAnnotation(Method method, Item item, int pc) {
        int reg = item.getRegisterNumber();
        if (reg < 0) {
            return null;
        }
        return getLocalVariableAnnotation(method, reg, pc, item.getPC());

    }

    public static @CheckForNull
    LocalVariableAnnotation getLocalVariableAnnotation(DismantleBytecode visitor, Item item) {
        int reg = item.getRegisterNumber();
        if (reg < 0) {
            return null;
        }
        return getLocalVariableAnnotation(visitor.getMethod(), reg, visitor.getPC(), item.getPC());

    }

    public static @CheckForNull
    LocalVariableAnnotation findMatchingIgnoredParameter(ClassContext classContext, Method method, String name, String signature) {
        try {
            Dataflow<BitSet, LiveLocalStoreAnalysis> llsaDataflow = classContext.getLiveLocalStoreDataflow(method);
            CFG cfg;

            cfg = classContext.getCFG(method);
            LocalVariableAnnotation match = null;
            int lowestCost = Integer.MAX_VALUE;
            BitSet liveStoreSetAtEntry = llsaDataflow.getAnalysis().getResultFact(cfg.getEntry());
            int localsThatAreParameters = PreorderVisitor.getNumberArguments(method.getSignature());
            int startIndex = 0;
            if (!method.isStatic()) {
                startIndex = 1;
            }
            SignatureParser parser = new SignatureParser(method.getSignature());
            Iterator<String> signatureIterator = parser.parameterSignatureIterator();
            for (int i = startIndex; i < localsThatAreParameters + startIndex; i++) {
                String sig = signatureIterator.next();
                if (!liveStoreSetAtEntry.get(i) && signature.equals(sig)) {
                    // parameter isn't live and signatures match
                    LocalVariableAnnotation potentialMatch = LocalVariableAnnotation.getLocalVariableAnnotation(method, i, 0, 0);
                    potentialMatch.setDescription(DID_YOU_MEAN_ROLE);
                    if (!potentialMatch.isNamed()) {
                        return potentialMatch;
                    }
                    int distance = EditDistance.editDistance(name, potentialMatch.getName());
                    if (distance < lowestCost) {
                        match = potentialMatch;
                        match.setDescription(DID_YOU_MEAN_ROLE);
                        lowestCost = distance;
                    } else if (distance == lowestCost) {
                        // not unique best match
                        match = null;
                    }

                }
            }
            return match;
        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError("", e);
        } catch (CFGBuilderException e) {
            AnalysisContext.logError("", e);
        }
        return null;
    }

    public static @CheckForNull
    LocalVariableAnnotation findUniqueBestMatchingParameter(ClassContext classContext, Method method, String name,
            String signature) {
        LocalVariableAnnotation match = null;
        int localsThatAreParameters = PreorderVisitor.getNumberArguments(method.getSignature());
        int startIndex = 0;
        if (!method.isStatic()) {
            startIndex = 1;
        }
        SignatureParser parser = new SignatureParser(method.getSignature());
        Iterator<String> signatureIterator = parser.parameterSignatureIterator();
        int lowestCost = Integer.MAX_VALUE;
        for (int i = startIndex; i < localsThatAreParameters + startIndex; i++) {
            String sig = signatureIterator.next();
            if (signature.equals(sig)) {
                LocalVariableAnnotation potentialMatch = LocalVariableAnnotation.getLocalVariableAnnotation(method, i, 0, 0);
                if (!potentialMatch.isNamed()) {
                    continue;
                }
                int distance = EditDistance.editDistance(name, potentialMatch.getName());
                if (distance < lowestCost) {
                    match = potentialMatch;
                    match.setDescription(DID_YOU_MEAN_ROLE);
                    lowestCost = distance;
                } else if (distance == lowestCost) {
                    // not unique best match
                    match = null;
                }
                // signatures match
            }
        }
        if (lowestCost < 5) {
            return match;
        }
        return null;
    }

    @Override
    public String toString(ClassAnnotation primaryClass) {
        return toString();
    }
}


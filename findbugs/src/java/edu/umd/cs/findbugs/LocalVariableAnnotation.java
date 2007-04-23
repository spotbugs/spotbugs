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

import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.IndexedInstruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.Location;
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

	private static final String DEFAULT_ROLE = "LOCAL_VARIABLE_DEFAULT";

	final private String value;
	final int register, pc;
	 private String description;

	/**
	 * Constructor.
	 *
	 * @param name     the name of the local variable
	 * @param register the local variable index
	 * @param pc       the bytecode offset of the instruction that mentions
	 *                 this local variable
	 */
	public LocalVariableAnnotation(String name, int register, int pc) {
		this.value = name;
		this.register = register;
		this.pc = pc;
		this.description = DEFAULT_ROLE;
		this.setDescription(name.equals("?") ? "LOCAL_VARIABLE_UNKNOWN" : "LOCAL_VARIABLE_NAMED");
	}

	public static LocalVariableAnnotation getLocalVariableAnnotation(
			Method method, Location location, IndexedInstruction ins) {
		int local = ins.getIndex();
		InstructionHandle handle = location.getHandle();
		int position1 = handle.getNext().getPosition();
		int position2 = handle.getPosition();
		return getLocalVariableAnnotation(method, local, position1, position2);
	}

	public static LocalVariableAnnotation getLocalVariableAnnotation(
			Method method, int local, int position1, int position2) {

		LocalVariableTable localVariableTable = method.getLocalVariableTable();
		String localName = "?";
		if (localVariableTable != null) {
			LocalVariable lv1 = localVariableTable.getLocalVariable(local, position1);
			if (lv1 == null) {
				lv1 = localVariableTable.getLocalVariable(local, position2);
				position1 = position2;
			}
			if (lv1 != null) localName = lv1.getName();
		}
		return new LocalVariableAnnotation(localName, local, position1);
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}


	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitLocalVariableAnnotation(this);
	}

	public String format(String key, ClassAnnotation primaryClass) {
		// System.out.println("format: " + key + " reg: " + register + " name: " + value);
		if (key.equals("hash")) {
			if (register < 0) return "??";
			return value;
		}
		if (register < 0) return "?";
		if (key.equals("register")) return String.valueOf(register);
		else if (key.equals("pc")) return String.valueOf(pc);
		else if (key.equals("name") || key.equals("givenClass")) return value;
		else if (!value.equals("?")) return value;
		return "$L"+register;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LocalVariableAnnotation))
			return false;
		return value.equals(((LocalVariableAnnotation) o).value);
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof LocalVariableAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		return value.compareTo(((LocalVariableAnnotation) o).value);
	}

	@Override
	public String toString() {
		String pattern = I18N.instance().getAnnotationDescription(description);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format(new BugAnnotation[]{this}, null);
	}

	/* ----------------------------------------------------------------------
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	private static final String ELEMENT_NAME = "LocalVariable";

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		writeXML(xmlOutput, false);
	}

	public void writeXML(XMLOutput xmlOutput, boolean addMessages) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList()
			.addAttribute("name", value)
		.addAttribute("register", String.valueOf(register))
		.addAttribute("pc", String.valueOf(pc));

		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			attributeList.addAttribute("role", role);

		BugAnnotationUtil.writeXML(xmlOutput, ELEMENT_NAME, this, attributeList, addMessages);
	}

	/**
	 * @return name of local variable
	 */
	public String getName() {

		return value;
	}


	public boolean isSignificant() {
		return !value.equals("?");
	}
}

// vim:ts=4

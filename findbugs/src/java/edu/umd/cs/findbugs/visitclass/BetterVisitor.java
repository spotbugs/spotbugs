/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.visitclass;

import java.io.PrintStream;

import org.apache.bcel.classfile.*;


/**
 * Fixedup of from org.apache.bcel.classfile.Visitor
 *
 * @author <A HREF="http://www.cs.umd.edu/~pugh">William Pugh</A>
 * @version 980818
 */
public abstract class BetterVisitor implements Visitor {

	////////////////// In short form //////////////////////
	// General classes
	public void visit(JavaClass obj) {
	}

	public void visit(ConstantPool obj) {
	}

	public void visit(Field obj) {
	}

	public void visit(Method obj) {
	}

	// Constants

	public void visit(Constant obj) {
	}

	public void visit(ConstantCP obj) {
		visit((Constant) obj);
	}

	public void visit(ConstantMethodref obj) {
		visit((ConstantCP) obj);
	}

	public void visit(ConstantFieldref obj) {
		visit((ConstantCP) obj);
	}

	public void visit(ConstantInterfaceMethodref obj) {
		visit((ConstantCP) obj);
	}

	public void visit(ConstantClass obj) {
		visit((Constant) obj);
	}

	public void visit(ConstantDouble obj) {
		visit((Constant) obj);
	}

	public void visit(ConstantFloat obj) {
		visit((Constant) obj);
	}

	public void visit(ConstantInteger obj) {
		visit((Constant) obj);
	}

	public void visit(ConstantLong obj) {
		visit((Constant) obj);
	}

	public void visit(ConstantNameAndType obj) {
		visit((Constant) obj);
	}

	public void visit(ConstantString obj) {
		visit((Constant) obj);
	}

	public void visit(ConstantUtf8 obj) {
		visit((Constant) obj);
	}

	// Attributes
	public void visit(Attribute obj) {
	}

	public void visit(Code obj) {
		visit((Attribute) obj);
	}

	public void visit(ConstantValue obj) {
		visit((Attribute) obj);
	}

	public void visit(ExceptionTable obj) {
		visit((Attribute) obj);
	}

	public void visit(InnerClasses obj) {
		visit((Attribute) obj);
	}

	public void visit(LineNumberTable obj) {
		visit((Attribute) obj);
	}

	public void visit(LocalVariableTable obj) {
		visit((Attribute) obj);
	}

	public void visit(SourceFile obj) {
		visit((Attribute) obj);
	}

	public void visit(Synthetic obj) {
		visit((Attribute) obj);
	}

	public void visit(org.apache.bcel.classfile.Deprecated obj) {
		visit((Attribute) obj);
	}

	public void visit(Unknown obj) {
		visit((Attribute) obj);
	}

	public void visit(Signature obj) {
		visit((Attribute) obj);
	}

	// Extra classes (i.e. leaves in this context)
	public void visit(InnerClass obj) {
	}

	public void visit(LocalVariable obj) {
	}

	public void visit(LineNumber obj) {
	}

	public void visit(CodeException obj) {
	}

	public void visit(StackMapEntry obj) {
	}

	// Attributes
	public void visitCode(Code obj) {
		visit(obj);
	}

	public void visitCodeException(CodeException obj) {
		visit(obj);
	}

	// Constants
	public void visitConstantClass(ConstantClass obj) {
		visit(obj);
	}

	public void visitConstantDouble(ConstantDouble obj) {
		visit(obj);
	}

	public void visitConstantFieldref(ConstantFieldref obj) {
		visit(obj);
	}

	public void visitConstantFloat(ConstantFloat obj) {
		visit(obj);
	}

	public void visitConstantInteger(ConstantInteger obj) {
		visit(obj);
	}

	public void visitConstantInterfaceMethodref(ConstantInterfaceMethodref obj) {
		visit(obj);
	}

	public void visitConstantLong(ConstantLong obj) {
		visit(obj);
	}

	public void visitConstantMethodref(ConstantMethodref obj) {
		visit(obj);
	}

	public void visitConstantNameAndType(ConstantNameAndType obj) {
		visit(obj);
	}

	public void visitConstantPool(ConstantPool obj) {
		visit(obj);
	}

	public void visitConstantString(ConstantString obj) {
		visit(obj);
	}

	public void visitConstantUtf8(ConstantUtf8 obj) {
		visit(obj);
	}

	public void visitConstantValue(ConstantValue obj) {
		visit(obj);
	}

	public void visitDeprecated(org.apache.bcel.classfile.Deprecated obj) {
		visit(obj);
	}

	public void visitExceptionTable(ExceptionTable obj) {
		visit(obj);
	}

	public void visitField(Field obj) {
		visit(obj);
	}

	// Extra classes (i.e. leaves in this context)
	public void visitInnerClass(InnerClass obj) {
		visit(obj);
	}

	public void visitInnerClasses(InnerClasses obj) {
		visit(obj);
	}

	// General classes
	public void visitJavaClass(JavaClass obj) {
		visit(obj);
	}

	public void visitLineNumber(LineNumber obj) {
		visit(obj);
	}

	public void visitLineNumberTable(LineNumberTable obj) {
		visit(obj);
	}

	public void visitLocalVariable(LocalVariable obj) {
		visit(obj);
	}

	public void visitLocalVariableTable(LocalVariableTable obj) {
		visit(obj);
	}

	public void visitMethod(Method obj) {
		visit(obj);
	}

	public void visitSignature(Signature obj) {
		visit(obj);
	}

	public void visitSourceFile(SourceFile obj) {
		visit(obj);
	}

	public void visitSynthetic(Synthetic obj) {
		visit(obj);
	}

	public void visitUnknown(Unknown obj) {
		visit(obj);
	}

	public void visitStackMapEntry(StackMapEntry obj) {
		visit(obj);
	}

	public void visitStackMap(StackMap obj) {
		visit(obj);
	}

	public void report(PrintStream out) {
	}

}

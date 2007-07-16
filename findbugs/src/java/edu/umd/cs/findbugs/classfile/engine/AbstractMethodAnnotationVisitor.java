/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * @author pwilliam
 */
public abstract class AbstractMethodAnnotationVisitor implements MethodVisitor {

	

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitAnnotationDefault()
	 */
	public AnnotationVisitor visitAnnotationDefault() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitAttribute(org.objectweb.asm.Attribute)
	 */
	public void visitAttribute(Attribute attr) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitCode()
	 */
	public void visitCode() {
		// TODO Auto-generated method stub

	}


	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitFieldInsn(int, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitFrame(int, int, java.lang.Object[], int, java.lang.Object[])
	 */
	public void visitFrame(int type, int local, Object[] local2, int stack, Object[] stack2) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitIincInsn(int, int)
	 */
	public void visitIincInsn(int var, int increment) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitInsn(int)
	 */
	public void visitInsn(int opcode) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitIntInsn(int, int)
	 */
	public void visitIntInsn(int opcode, int operand) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitJumpInsn(int, org.objectweb.asm.Label)
	 */
	public void visitJumpInsn(int opcode, Label label) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitLabel(org.objectweb.asm.Label)
	 */
	public void visitLabel(Label label) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitLdcInsn(java.lang.Object)
	 */
	public void visitLdcInsn(Object cst) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitLineNumber(int, org.objectweb.asm.Label)
	 */
	public void visitLineNumber(int line, Label start) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitLocalVariable(java.lang.String, java.lang.String, java.lang.String, org.objectweb.asm.Label, org.objectweb.asm.Label, int)
	 */
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitLookupSwitchInsn(org.objectweb.asm.Label, int[], org.objectweb.asm.Label[])
	 */
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitMaxs(int, int)
	 */
	public void visitMaxs(int maxStack, int maxLocals) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitMethodInsn(int, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitMultiANewArrayInsn(java.lang.String, int)
	 */
	public void visitMultiANewArrayInsn(String desc, int dims) {
		// TODO Auto-generated method stub

	}


	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitTableSwitchInsn(int, int, org.objectweb.asm.Label, org.objectweb.asm.Label[])
	 */
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitTryCatchBlock(org.objectweb.asm.Label, org.objectweb.asm.Label, org.objectweb.asm.Label, java.lang.String)
	 */
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitTypeInsn(int, java.lang.String)
	 */
	public void visitTypeInsn(int opcode, String type) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.MethodVisitor#visitVarInsn(int, int)
	 */
	public void visitVarInsn(int opcode, int var) {
		// TODO Auto-generated method stub

	}

}

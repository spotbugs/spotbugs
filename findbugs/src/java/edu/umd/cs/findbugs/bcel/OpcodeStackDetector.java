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

package edu.umd.cs.findbugs.bcel;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.JumpInfo;

/**
 * Base class for Detectors that want to scan the bytecode of a method and use
 * an opcode stack.
 *
 * @see BytecodeScanningDetector
 */
abstract public class OpcodeStackDetector extends BytecodeScanningDetector {

    private final boolean isUsingCustomUserValue;

    public OpcodeStackDetector() {
        super();
        isUsingCustomUserValue = getClass().isAnnotationPresent(OpcodeStack.CustomUserValue.class);
    }

    /**
     * @author pwilliam
     */
    public abstract static class WithCustomJumpInfo extends OpcodeStackDetector {
        public abstract JumpInfo customJumpInfo();
    }

    protected OpcodeStack stack;

    public OpcodeStack getStack() {
        return stack;
    }

    @Override
    public final void visitCode(Code obj) {
        if (!shouldVisitCode(obj)) {
            return;
        }
        stack = new OpcodeStack();
        stack.resetForMethodEntry(this);
        super.visitCode(obj);
        stack = null;
    }

    @Override
    public boolean beforeOpcode(int seen) {
        stack.precomputation(this);
        return !stack.isTop();
    }

    /**
     * <p>Note that stack might be TOP when this method is called.</p>
     * @see #sawOpcode(int)
     */
    @Override
    public void afterOpcode(int seen) {
        stack.sawOpcode(this, seen);
    }

    /**
     * <p>By default, this method will not be called when
     * stack is TOP. To change this behavior, override {@code #beforeOpcode(int)}
     * and change to return true even if stack is TOP.</p>
     * <p>see <a href="http://findbugs-tutorials.googlecode.com/files/uffr-talk.pdf">Using FindBugs for Research</a> to learn lattice and what TOP means.</p>
     * @see #beforeOpcode(int)
     */
    @Override
    abstract public void sawOpcode(int seen);

    /**
     * @return true if this detector is annotated with  {@link edu.umd.cs.findbugs.OpcodeStack.CustomUserValue}
     * and thus should not reuse generic OpcodeStack information
     * from an iterative evaluation of the opcode stack. Such detectors
     * will not use iterative opcode stack evaluation.
     * @see OpcodeStack#resetForMethodEntry(edu.umd.cs.findbugs.visitclass.DismantleBytecode)
     */
    public final boolean isUsingCustomUserValue() {
        return isUsingCustomUserValue;
    }
}

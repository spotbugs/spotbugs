/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.asm;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class FBClassReader extends ClassReader {

    // boolean needOffsets; // optional optimisation (not thread safe)

	public FBClassReader(byte[] b) {
	    super(b);
	    }
    public FBClassReader(byte[] b, int off, int len) {
        super(b, off, len);
    }

    @Override
    public void accept(ClassVisitor cv, Attribute[] attrs, int flags)
    {
        super.accept(new MyClassAdapter(cv), attrs, flags);
    }

    @Override
    protected Label readLabel(int offset, Label[] labels)
    {
        // if (!needOffsets) return super.readLabel(offset, labels);
        if (labels[offset] == null) {
            for (int i = 0; i < labels.length; ++i) {
                labels[i] = new MyLabel(i);
            }
        }
        ((MyLabel) labels[offset]).realLabel = true;
        return labels[offset];    
    }

    private static class MyClassAdapter extends ClassAdapter {

        public MyClassAdapter(ClassVisitor cv) {
            super(cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            // needOffsets = mv instanceof MyMethodVisitor;
            if (mv instanceof FBMethodVisitor) {
                mv = new MyMethodAdapter((FBMethodVisitor)mv);
            }
            return mv;
        }
    }

    private static class MyMethodAdapter extends MethodAdapter {

        public MyMethodAdapter(FBMethodVisitor mv) {
            super(mv);
        }

        @Override
        public void visitLabel(Label label)
        {
            MyLabel l = (MyLabel) label;
            ((FBMethodVisitor) mv).visitOffset(l.originalOffset);
            if (l.realLabel) {
                mv.visitLabel(label);
            }
        }
    }

    private static class MyLabel extends Label {

        final int originalOffset;

        boolean realLabel;

        MyLabel(int originalOffset) {
            this.originalOffset = originalOffset;
        }
    }
}

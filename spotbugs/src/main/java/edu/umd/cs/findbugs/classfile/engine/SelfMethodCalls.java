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

import java.util.HashSet;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.asm.FBClassReader;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.engine.asm.FindBugsASM;
import edu.umd.cs.findbugs.util.MultiMap;

/**
 * @author pugh
 */
public class SelfMethodCalls {

    static private boolean interestingSignature(String signature) {
        return !"()V".equals(signature);
    }

    public static <T> MultiMap<T, T> getSelfCalls(final ClassDescriptor classDescriptor, final Map<String, T> methods) {
        final MultiMap<T, T> map = new MultiMap<T, T>(HashSet.class);

        FBClassReader reader;
        try {
            reader = Global.getAnalysisCache().getClassAnalysis(FBClassReader.class, classDescriptor);
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error finding self method calls for " + classDescriptor, e);
            return map;
        }
        reader.accept(new ClassVisitor(FindBugsASM.ASM_VERSION) {

            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String desc, String signature,
                    String[] exceptions) {
                return new MethodVisitor(FindBugsASM.ASM_VERSION) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name2, String desc2, boolean itf) {
                        if (owner.equals(classDescriptor.getClassName()) && interestingSignature(desc2)) {
                            T from = methods.get(name + desc + ((access & Opcodes.ACC_STATIC) != 0));
                            T to = methods.get(name2 + desc2 + (opcode == Opcodes.INVOKESTATIC));
                            map.add(from, to);
                        }

                    }

                };
            }

        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return map;
    }

    //    private final ClassReader classReader;

    public SelfMethodCalls(ClassReader classReader) {
        //        this.classReader = classReader;
    }
}

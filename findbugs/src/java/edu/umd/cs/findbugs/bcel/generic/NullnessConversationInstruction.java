/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.bcel.generic;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConversionInstruction;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.Visitor;

/** A synthetic instruction that converts a the nullness of a reference to a boolean value.
 *
 */
public abstract class NullnessConversationInstruction extends ConversionInstruction {

    protected NullnessConversationInstruction(short opcode) {
        super(opcode);
    }

    @Override
    public Type getType( ConstantPoolGen cp ) {
        return Type.BOOLEAN;
    }

    @Override
    public int consumeStack( ConstantPoolGen cpg ) {
        return 1;
    }
    @Override
    public int produceStack( ConstantPoolGen cpg ) {
        return 1;
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
    /**
     * Call corresponding visitor method(s). The order is:
     * Call visitor methods of implemented interfaces first, then
     * call methods according to the class hierarchy in descending order,
     * i.e., the most specific visitXXX() call comes last.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( Visitor v ) {
        v.visitTypedInstruction(this);
        v.visitStackProducer(this);
        v.visitStackConsumer(this);
        v.visitConversionInstruction(this);
    }
}

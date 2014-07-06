/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import javax.annotation.Nonnull;

import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.classfile.MethodDescriptor;


/**
 * Factory object to create CFGBuilders for methods. Using a CFGBuilderFactory
 * is preferable to direct instantiation of CFGBuilders, because it gives us an
 * easy hook for plugging in new CFGBuilder implementations. (CFGs for Java are
 * a little tricky to get right.)
 *
 * @author David Hovemeyer
 * @see CFG
 * @see CFGBuilder
 */
public class CFGBuilderFactory {

    /**
     * Create a CFGBuilder to build a CFG for given method.
     *
     * @param methodGen
     *            the method
     * @return a CFGBuilder for the method
     */
    public static CFGBuilder create(@Nonnull MethodDescriptor descriptor, @Nonnull MethodGen methodGen) {
        return new BetterCFGBuilder2(descriptor, methodGen);
    }
}


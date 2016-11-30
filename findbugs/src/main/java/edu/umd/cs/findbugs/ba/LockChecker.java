/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import java.util.BitSet;
import java.util.HashMap;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Front-end for LockDataflow that can avoid doing unnecessary work (e.g.,
 * actually performing the lock dataflow) if the method analyzed does not
 * contain explicit monitorenter/monitorexit instructions.
 *
 * <p>
 * Note that because LockSets use value numbers, ValueNumberAnalysis must be
 * performed for all methods that are synchronized or contain explicit
 * monitorenter/monitorexit instructions.
 * </p>
 *
 * @see LockSet
 * @see LockDataflow
 * @see LockAnalysis
 * @author David Hovemeyer
 */
public class LockChecker {
    private final MethodDescriptor methodDescriptor;

    private Method method;

    private LockDataflow lockDataflow;

    private ValueNumberDataflow vnaDataflow;

    private final HashMap<Location, LockSet> cache;

    /**
     * Constructor.
     */
    public LockChecker(MethodDescriptor methodDescriptor) {
        this.cache = new HashMap<Location, LockSet>();
        this.methodDescriptor = methodDescriptor;
    }

    /**
     * Execute dataflow analyses (only if required).
     *
     * @throws CheckedAnalysisException
     */
    public void execute() throws CheckedAnalysisException {
        method = Global.getAnalysisCache().getMethodAnalysis(Method.class, methodDescriptor);
        ClassContext classContext = Global.getAnalysisCache().getClassAnalysis(ClassContext.class,
                methodDescriptor.getClassDescriptor());

        BitSet bytecodeSet = classContext.getBytecodeSet(method);
        if (bytecodeSet == null) {
            return;
        }
        if (bytecodeSet.get(Constants.MONITORENTER) || bytecodeSet.get(Constants.MONITOREXIT)) {
            this.lockDataflow = classContext.getLockDataflow(method);
        } else if (method.isSynchronized()) {
            this.vnaDataflow = classContext.getValueNumberDataflow(method); // will
            // need
            // this
            // later
        }
    }

    /**
     * Get LockSet at given Location.
     *
     * @param location
     *            the Location
     * @return the LockSet at that Location
     * @throws DataflowAnalysisException
     */
    public LockSet getFactAtLocation(Location location) throws DataflowAnalysisException {
        if (lockDataflow != null) {
            return lockDataflow.getFactAtLocation(location);
        } else {
            LockSet lockSet = cache.get(location);
            if (lockSet == null) {
                lockSet = new LockSet();
                lockSet.setDefaultLockCount(0);
                if (method.isSynchronized() && !method.isStatic()) {
                    // LockSet contains just the "this" reference
                    ValueNumber instance = vnaDataflow.getAnalysis().getThisValue();
                    lockSet.setLockCount(instance.getNumber(), 1);
                } else {
                    // LockSet is completely empty - nothing to do
                }
                cache.put(location, lockSet);
            }
            return lockSet;
        }
    }
}

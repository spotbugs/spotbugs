/*
 * SpotBugs - Find bugs in Java programs
 * Copyright (C) 2026, the SpotBugs authors
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

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.classfile.FieldDescriptor;

/**
 * Stores a collection of {@link BugInstance} and reports them similarly to {@link BugAccumulator}.
 *
 * @author Guillaume Toison
 */
public class BugPcMap {
    private final BugReporter reporter;
    private final Map<Integer, BugInstance> bugInstancesByPc = new HashMap<>();
    private final Map<Integer, BugInstance> bugInstancesByNextPc = new HashMap<>();

    public BugPcMap(BugReporter reporter) {
        this.reporter = reporter;
    }

    public void accumulateBug(BugInstance sourceLine, BytecodeScanningDetector visitor) {
        bugInstancesByPc.put(visitor.getPC(), sourceLine);
        bugInstancesByNextPc.put(visitor.getNextPC(), sourceLine);
    }

    /**
     * To be called when the visitor has seen {@link Const#PUTSTATIC} or {@link Const#PUTFIELD}.
     * If a bug instance was created at the previous PC a field annotation will be added.
     *
     * @param visitor
     */
    public void addFieldAnnotation(BytecodeScanningDetector visitor) {
        BugInstance bugInstance = bugInstancesByNextPc.get(visitor.getPC());
        if (bugInstance != null) {
            FieldDescriptor fieldDescriptor = visitor.getFieldDescriptorOperand();

            bugInstance.addField(fieldDescriptor);
        }
    }

    /**
     * Reports accumulated bug instances to the {@link BugReporter} and clears itself.
     */
    public void reportAccumulatedBugs() {
        bugInstancesByPc.values().forEach(reporter::reportBug);

        bugInstancesByPc.clear();
        bugInstancesByNextPc.clear();
    }
}

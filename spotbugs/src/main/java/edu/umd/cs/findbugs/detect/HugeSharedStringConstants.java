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

package edu.umd.cs.findbugs.detect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantValue;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.IntAnnotation;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;

public class HugeSharedStringConstants extends BytecodeScanningDetector {

    /**
     *
     */
    private static final int SIZE_OF_HUGE_CONSTANT = 500;

    String getStringKey(String s) {
        return s.length() + ":" + s.hashCode();
    }

    HashMap<String, SortedSet<String>> map = new HashMap<String, SortedSet<String>>();

    HashMap<String, XField> definition = new HashMap<String, XField>();

    HashMap<String, Integer> stringSize = new HashMap<String, Integer>();

    BugReporter bugReporter;

    public HugeSharedStringConstants(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(ConstantString s) {
        String value = s.getBytes(getConstantPool());
        if (value.length() < SIZE_OF_HUGE_CONSTANT) {
            return;
        }
        String key = getStringKey(value);
        SortedSet<String> set = map.get(key);
        if (set == null) {
            set = new TreeSet<String>();
            map.put(key, set);
        }
        set.add(getDottedClassName());
    }

    @Override
    public void visit(ConstantValue s) {
        if (!visitingField()) {
            return;
        }
        int i = s.getConstantValueIndex();
        Constant c = getConstantPool().getConstant(i);
        if (c instanceof ConstantString) {
            String value = ((ConstantString) c).getBytes(getConstantPool());
            if (value.length() < SIZE_OF_HUGE_CONSTANT) {
                return;
            }
            String key = getStringKey(value);
            definition.put(key, XFactory.createXField(this));
            stringSize.put(key, value.length());
        }

    }

    @Override
    public void report() {
        for (Map.Entry<String, SortedSet<String>> e : map.entrySet()) {
            Set<String> occursIn = e.getValue();
            if (occursIn.size() == 1) {
                continue;
            }
            XField field = definition.get(e.getKey());
            if (field == null) {
                continue;
            }
            Integer length = stringSize.get(e.getKey());
            int overhead = length * (occursIn.size() - 1);
            if (overhead < 3 * SIZE_OF_HUGE_CONSTANT) {
                continue;
            }
            String className = field.getClassName();

            BugInstance bug = new BugInstance(this, "HSC_HUGE_SHARED_STRING_CONSTANT",
                    overhead > 20 * SIZE_OF_HUGE_CONSTANT ? HIGH_PRIORITY
                            : (overhead > 8 * SIZE_OF_HUGE_CONSTANT ? NORMAL_PRIORITY : LOW_PRIORITY)).addClass(className)
                            .addField(field).addInt(length).addInt(occursIn.size()).describe(IntAnnotation.INT_OCCURRENCES);
            for (String c : occursIn) {
                if (!c.equals(className)) {
                    bug.addClass(c);
                }
            }

            bugReporter.reportBug(bug);

        }

    }

}

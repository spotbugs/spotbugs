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

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;

/**
 * This detector is currently disabled by default.
 * It generates false positives when creating directory entries.
 *
 */
public class EmptyZipFileEntry extends BytecodeScanningDetector implements StatelessDetector {

    private final BugReporter bugReporter;

    private int sawPutEntry;

    private String streamType;

    public EmptyZipFileEntry(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(JavaClass obj) {
    }

    @Override
    public void visit(Method obj) {
        sawPutEntry = -10000;
        streamType = "";
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKEVIRTUAL && "putNextEntry".equals(getNameConstantOperand())) {
            streamType = getClassConstantOperand();
            if ("java/util/zip/ZipOutputStream".equals(streamType) || "java/util/jar/JarOutputStream".equals(streamType)) {
                sawPutEntry = getPC();
            } else {
                streamType = "";
            }
        } else {
            if (getPC() - sawPutEntry <= 7 && seen == INVOKEVIRTUAL && "closeEntry".equals(getNameConstantOperand())
                    && getClassConstantOperand().equals(streamType)) {
                bugReporter
                .reportBug(new BugInstance(this,
                        "java/util/zip/ZipOutputStream".equals(streamType) ? "AM_CREATES_EMPTY_ZIP_FILE_ENTRY"
                                : "AM_CREATES_EMPTY_JAR_FILE_ENTRY", NORMAL_PRIORITY).addClassAndMethod(this)
                                .addSourceLine(this));
            }

        }

    }

}

/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, Tom Truscott <trt@unx.sas.com>
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.classfile.*;

public class EmptyZipFileEntry extends BytecodeScanningDetector implements Constants2 {

	private BugReporter bugReporter;
	private int sawPutEntry;

	public EmptyZipFileEntry(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	public void visit(JavaClass obj) {
	}

	public void visit(Method obj) {
		sawPutEntry = -10000;
	}

	public void sawOpcode(int seen) {


		if (seen == INVOKEVIRTUAL 
			&& getNameConstantOperand().equals("putNextEntry") 
			&& getClassConstantOperand()
				.equals("java/util/zip/ZipOutputStream")) {
			sawPutEntry = getPC();
			}
		else {
			if (getPC() - sawPutEntry <= 7  && seen == INVOKEVIRTUAL 
				&& getNameConstantOperand().equals("closeEntry")
			&& getClassConstantOperand()
				.equals("java/util/zip/ZipOutputStream") )
                        bugReporter.reportBug(new BugInstance(
				"AM_CREATES_EMPTY_ZIP_FILE_ENTRY", NORMAL_PRIORITY)
                                .addClassAndMethod(this)
                                .addSourceLine(this));

			}
	
		


	}

}

/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 Tom Truscott <trt@unx.sas.com>
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


import edu.umd.cs.findbugs.*;
import org.apache.bcel.classfile.*;

public class EmptyZipFileEntry extends BytecodeScanningDetector implements  StatelessDetector {

	private BugReporter bugReporter;
	private int sawPutEntry;
	private String streamType;

	public EmptyZipFileEntry(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	@Override
         public Object clone() throws CloneNotSupportedException {
		return super.clone();
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
		if (seen == INVOKEVIRTUAL 
			&& getNameConstantOperand().equals("putNextEntry")) {
			streamType = getClassConstantOperand();
			if (streamType.equals("java/util/zip/ZipOutputStream") 
			||  streamType.equals("java/util/jar/JarOutputStream"))
				sawPutEntry = getPC();
			else
				streamType = "";
			}
		else {
			if (getPC() - sawPutEntry <= 7  && seen == INVOKEVIRTUAL 
				&& getNameConstantOperand().equals("closeEntry")
			&& getClassConstantOperand()
				.equals(streamType) )
                        bugReporter.reportBug(new BugInstance(
                        	this,
                        	streamType.equals("java/util/zip/ZipOutputStream") ?
								"AM_CREATES_EMPTY_ZIP_FILE_ENTRY" :
								"AM_CREATES_EMPTY_JAR_FILE_ENTRY", 
							NORMAL_PRIORITY)
                            .addClassAndMethod(this)
                            .addSourceLine(this));

			}
	
		


	}

}

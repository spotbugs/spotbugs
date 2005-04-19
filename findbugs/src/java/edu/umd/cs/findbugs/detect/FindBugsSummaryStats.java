/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, Mike Fagan <mfagan@tde.com>
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugReporterObserver;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class FindBugsSummaryStats extends PreorderVisitor
        implements Detector, BugReporterObserver {
	private ProjectStats stats;

	public FindBugsSummaryStats(BugReporter bugReporter) {
		this.stats = bugReporter.getProjectStats();
		bugReporter.addObserver(this);
	}

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	public void report() {
	}

	public void visit(JavaClass obj) {
		super.visit(obj);
		stats.addClass(getDottedClassName(), obj.isInterface(),
			calculateClassSize(obj));
	}

	/**
		Calculate a normalized class size value that can be used 
		across projects as the basis for a bug density metric.
		<p>
		The size value is calculated as the sum of the following
		(somewhat arbitrary) values: 
		</p>
		<ul>
		  <li>8 for the class or interface itself
			(to prevent tag interfaces from having zero size)</li>
		  <li>+8 for each field</li>
		  <li>+8 for each method</li>
		  <li>For each method, the length of the method body
			  in bytes.</li>
		</ul>
		<p>
		This is a compromise between speed/complexity and accuracy.
		It would be faster to use the total size of the class, but this
		would include the constant pool, which would skew results.
		Another alternative might be to use the number of statements
		in each method (similar to NCSS), but this would require the
		method code to be parsed.
		</p>
		<p>
		This value is calculated here instead of in ProjectStats
		so that ProjectStats can have a single method to support
		both this class and SAXBugCollectionHandler.
		</p>
	**/
	private int calculateClassSize(JavaClass obj) {
		int result = 8;
		result += 8 * obj.getFields().length;
		org.apache.bcel.classfile.Method[] methods = obj.getMethods();
		result += 8 * methods.length;
		int i = methods.length;
		while ( --i >= 0 ) {
			org.apache.bcel.classfile.Code code = methods[i].getCode();
			if ( code != null ) {
				result += code.getCode().length;
			}
		}

		return result;
	}
	
	public void reportBug(BugInstance bug) {
		stats.addBug(bug);
	}

}

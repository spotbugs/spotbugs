/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, Mike Fagan <mfagan@tde.com>
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
import edu.umd.cs.pugh.visitclass.PreorderVisitor;
import edu.umd.cs.daveho.ba.ClassContext;

import java.util.*;
import java.io.*;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

import org.dom4j.Element;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;


public class FindBugsSummaryStats extends PreorderVisitor 
                                  implements Detector, BugReporterObserver, SummaryReport {
  private BugReporter bugReporter;
  private ProjectStats stats;

  public FindBugsSummaryStats(BugReporter bugReporter) {
    this.bugReporter = bugReporter;
    this.stats = new ProjectStats();
    bugReporter.addObserver( this );
  }

  public void visitClassContext(ClassContext classContext) {
    classContext.getJavaClass().accept(this);
  }

  public void report() { }

  public void reportSummary( OutputStream out ) {
     Document document = DocumentHelper.createDocument();

     stats.setDate(new Date());
     stats.toElement(document);

     try {
       XMLWriter writer = new XMLWriter(out, OutputFormat.createPrettyPrint());
       writer.write(document);
     }
     catch ( Exception e ) {
       e.printStackTrace();
     }
  }

  public void visit(JavaClass obj)     {
    super.visit(obj);
    stats.addClass( betterClassName, obj.isInterface() );
  }

  public void reportBug( BugInstance bug ) {
    stats.addBug(bug);
  }

}

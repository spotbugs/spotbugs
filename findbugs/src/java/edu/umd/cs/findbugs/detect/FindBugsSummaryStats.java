/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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
import java.text.SimpleDateFormat;
import java.text.DateFormat;
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
  private HashMap<String, PackageStats> packageStatsMap = new HashMap<String, PackageStats>();
  private BugReporter bugReporter;
  int totalErrors;
  int totalClasses;


  public FindBugsSummaryStats(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
    bugReporter.addObserver( this );
    this.totalErrors = 0;
    this.totalClasses = 0;
  }

  public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
  }

  public void report() { }

  public void reportSummary( OutputStream out ) {
     Document document = DocumentHelper.createDocument();
     Element root = document.addElement("FindBugsSummary");
     DateFormat df = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss Z" );
     root.addAttribute("timestamp", df.format(new Date() ));
     root.addAttribute( "total_classes", String.valueOf( totalClasses ) );
     root.addAttribute( "total_bugs", String.valueOf( totalErrors ) );
     root.addAttribute( "num_packages", String.valueOf( packageStatsMap.size() ) );
	 Iterator<PackageStats> i = packageStatsMap.values().iterator();
     while( i.hasNext() ) {
       PackageStats stats = i.next();
       stats.toElement( root );
     } 

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
    PackageStats stat = getPackageStats( packageName );
    stat.addClass( betterClassName );
    totalClasses++;
  }

  public void reportBug( BugInstance bug ) {
    PackageStats stat = getPackageStats( bug.getPrimaryClass().getPackageName() );
    stat.addError( bug );
    totalErrors++;
  }

  private PackageStats getPackageStats( String packageName ) {
    PackageStats stat = packageStatsMap.get( packageName );
    if ( stat == null ) {
      stat = new PackageStats( packageName );
      packageStatsMap.put( packageName, stat );
    }
    return stat;
  }

}
  // class to store package structures
class PackageStats implements XMLConvertible {
    public static final String ELEMENT_NAME = "PackageStats";
    public static final int ALL_ERRORS = 0;
    private final String packageName;
    // list of errors for this package
    private LinkedList<BugInstance> packageErrors = new LinkedList<BugInstance>();
    // map of classnames and there type
    private LinkedList<String> packageClasses = new LinkedList<String>();

    public PackageStats( String packageName ) {
      this.packageName = packageName;
    }

    public int getTotalPackageTypes() { return packageClasses.size(); }

    public int getTotalPackageErrors() { return packageErrors.size(); }

    public int getNumClasses() throws ClassNotFoundException { 
      return countClasses( false ); 
    }

    public int getNumInnerClasses() { 
      int count = 0;
      Iterator<String> itr = packageClasses.iterator();
      while ( itr.hasNext() ) {
        String name = itr.next();
        if ( name.indexOf("$") >=0 ) {
          count++;
        }
      }
      return count;
    }

    public int getNumInterfaces() throws ClassNotFoundException { 
      return countClasses( true ); 
    }

    public void addError( BugInstance bug ) { packageErrors.add( bug ); } 

    public void addClass( String name ) { packageClasses.add(name); } 

    public String getPackageName() { return packageName; }

    private int countClasses( boolean isInterface ) throws ClassNotFoundException {
      int count = 0;
      Iterator<String> itr = packageClasses.iterator();
      while ( itr.hasNext() ) {
        JavaClass clss = Repository.lookupClass( itr.next() ); 
        if ( clss.isInterface() == isInterface ) {
          count++;
        }
      }
      return count;
    }

    private List<BugInstance> getErrors( boolean isInterface, int priority ) throws ClassNotFoundException { 
      ArrayList<BugInstance> items = new ArrayList<BugInstance>();
      Iterator<BugInstance> itr = packageErrors.iterator();
      while ( itr.hasNext() ) {
        BugInstance bug = itr.next(); 
        JavaClass clss = Repository.lookupClass( bug.getPrimaryClass().getClassName() );
        if ( clss != null && clss.isInterface() == isInterface 
             && ( priority == bug.getPriority() || priority == ALL_ERRORS )) {
          items.add( bug );
        }
      }
      return items; 
    }
 

    public Element toElement( Branch parent ) {
      List<BugInstance> classErrorList= null;
      List<BugInstance> interfaceErrorList = null;
      int classCount = 0;
      int interfaceCount = 0;
      Element element = parent.addElement(ELEMENT_NAME);

      try {
        classErrorList= getErrors( false, ALL_ERRORS ); 
        interfaceErrorList = getErrors( true, ALL_ERRORS ); 
        classCount = getNumClasses();
        interfaceCount = getNumInterfaces();
      }
      catch ( ClassNotFoundException e ) {
        e.printStackTrace();
        return element;
      }
      element.addAttribute("package", packageName);
      element.addAttribute("total_bugs", 
             String.valueOf(classErrorList.size() + interfaceErrorList.size()));
      element.addAttribute("total_types", 
                            String.valueOf(getTotalPackageTypes()));

      Element classes = element.addElement("Classes");
      classes.addAttribute("outer", String.valueOf(classCount - getNumInnerClasses()));
      classes.addAttribute("inner", String.valueOf(getNumInnerClasses()));
      classes.addAttribute("total_bugs", 
                            String.valueOf(classErrorList.size()));
     
      if ( classErrorList.size() > 0 ) { 
		  Element classErrors = classes.addElement( "ClassErrors" );
          Iterator<BugInstance> itr = classErrorList.iterator();
          while( itr.hasNext() ) {
            BugInstance bug = itr.next();
            bug.toElement( classErrors );
          }
      }
      
      Element interfaces = element.addElement("Interfaces");
      interfaces.addAttribute("count", String.valueOf(interfaceCount));
      interfaces.addAttribute("total_bugs", 
                               String.valueOf(interfaceErrorList.size()));
      if ( interfaceErrorList.size() > 0 ) { 
		  Element interfaceErrors = classes.addElement( "InterfaceErrors" );
          Iterator<BugInstance> itr = interfaceErrorList.iterator();
          while( itr.hasNext() ) {
            BugInstance bug = itr.next();
            bug.toElement( interfaceErrors );
          }
      }
      return element;
    }

}

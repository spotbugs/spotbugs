/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package edu.umd.cs.findbugs.anttask;

import edu.umd.cs.findbugs.ExitCodes;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.taskdefs.Java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * FindBugs in Java class files. This task can take the following
 * arguments:
 * <ul>
 * <li>auxClasspath    (classpath or classpathRef)
 * <li>home            (findbugs install dir)
 * <li>quietErrors     (boolean - default false)
 * <li>reportLevel     (enum low|medium|high)
 * <li>sort            (boolean default true)
 * <li>debug           (boolean default false) 
 * <li>output          (enum text|xml - default xml)
 * <li>visitors        (collection - comma seperated)
 * <li>omitVisitors    (collection - comma seperated)
 * <li>excludeFilter   (filter filename)
 * <li>includeFilter   (filter filename)
 * <li>projectFile     (project filename)
 * <li>jvmargs          (any additional jvm arguments)
 * </ul>
 * Of these arguments, the <b>home</b> is required.
 * <b>projectFile</b> is required if nested &lt;class&gt; are not
 * specified. the &lt;class&gt; tag defines the location of either a 
 *  class, jar file, zip file, or directory containing classes
 * <p>
 *
 * @author Mike Fagan <a href="mailto:mfagan@tde.com">mfagan@tde.com</a>
 *
 * @version $Revision: 2.0
 *
 * @since Ant 1.5
 *
 * @ant.task category="utility"
 */

public class FindBugsTask extends Task {

    private static final String FAIL_MSG
        = "Findbugs found errors; see the error output for details.";
    private static final String FINDBUGS_JAR = "findbugs.jar";
    private static final long TIMEOUT = 300000; //five minutes

	private boolean debug = false;
	private boolean sorted = true;
	private boolean quietErrors = false;
	private File homeDir = null;
	private File projectFile = null;
	private File excludeFile = null;
	private File includeFile = null;
	private Path auxClasspath = null;
	private Path sourcePath = null;
	private String outputFormat = "xml";
	private String reportLevel = null;
	private String jvmargs = "";
	private String visitors = null;
	private String omitVisitors = null;
	private String outputFileName = null;
    private List classLocations = new ArrayList();


    //define the inner class to store class locations
	public class ClassLocation {
		File classLocation = null;

		public void setLocation( File location ) {
			classLocation = location;
		}

		public File getLocation( ) {
			return classLocation;
		}
      
        public String toString( ) {
           return classLocation!=null?classLocation.toString():"";
        }
 
	}

	/**
	 * Set any specific jvm args
	 */
	public void setJvmargs(String args) {
		this.jvmargs = args;
	}

	/**
	 * Set the specific visitors to use 
	 */
	public void setVisitors(String commaSeperatedString) {
		this.visitors = commaSeperatedString;
	}

	/**
	 * Set the specific visitors to use 
	 */
	public void setOmitVisitors(String commaSeperatedString) {
		this.omitVisitors = commaSeperatedString;
	}

	/**
	 * Set the home directory into which findbugs was installed
	 */
	public void setHome(File homeDir) {
		this.homeDir = homeDir;
	}

	/**
	 * Set the output format
	 */
	public void setOutput(String format) {
		this.outputFormat = format;
	}

	/**
	 * Set the report level 
	 */
	public void setReportLevel(String level) {
		this.reportLevel = level;
	}

	/**
	 * Set the sorted flag
	 */
	public void setSort(boolean flag) {
		this.sorted = flag;
	}

	/**
	 * Set the quietError flag
	 */
	public void setQuietErrors(boolean flag) {
		this.quietErrors = flag;
	}

	/**
	 * Set the debug flag
	 */
	public void setDebug(boolean flag) {
		this.debug = flag;
	}

	/**
	 * Set the exclude filter file 
	 */
	public void setExcludeFilter(File filterFile) {
		this.excludeFile = filterFile;
	}

	/**
	 * Set the exclude filter file 
	 */
	public void setIncludeFilter(File filterFile) {
		this.includeFile = filterFile;
	}

	/**
	 * Set the project file 
	 */
	public void setProjectFile(File projectFile) {
		this.projectFile = projectFile;
	}

	/**
	 * the auxclasspath to use.
	 */
	public void setAuxClasspath(Path src) {
		if (auxClasspath == null) {
			auxClasspath = src;
		} else {
			auxClasspath.append(src);
		}
	}

	/**
	 * Path to use for auxclasspath.
	 */
	public Path createAuxClasspath() {
		if (auxClasspath == null) {
			auxClasspath = new Path(project);
		}
		return auxClasspath.createPath();
	}

	/**
	 * Adds a reference to a sourcepath defined elsewhere.
	 */
	public void setAuxClasspathRef(Reference r) {
		createAuxClasspath().setRefid(r);
	}

	/**
	 * the sourcepath to use.
	 */
	public void setSourcePath(Path src) {
		if (sourcePath == null) {
			sourcePath = src;
		} else {
			sourcePath.append(src);
		}
	}

	/**
	 * Path to use for sourcepath.
	 */
	public Path createSourcePath() {
		if (sourcePath == null) {
			sourcePath = new Path(project);
		}
		return sourcePath.createPath();
	}

	/**
	 * Adds a reference to a source path defined elsewhere.
	 */
	public void setSourcePathRef(Reference r) {
		createSourcePath().setRefid(r);
	}

	/**
     * Add a class location
     */
	public ClassLocation createClass() {
		ClassLocation cl = new ClassLocation();
		classLocations.add( cl );
		return cl;
	}

	/**
	 * Set name of output file.
	 */
	public void setOutputFile(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public void execute() throws BuildException {
		checkParameters();
		execFindbugs();
	}

    /**
     * Check that all required attributes have been set
     *
     * @since Ant 1.5
     */
	private void checkParameters() {
		if ( homeDir == null ) {
			throw new BuildException( "home attribute must be defined for task <"
										+ getTaskName() + "/>",
									  getLocation() );
		}

		if ( projectFile == null && classLocations.size() == 0 ) {
			throw new BuildException( "either projectfile or <class/> child " +
									  "elements must be defined for task <"
										+ getTaskName() + "/>",
									  getLocation() );
		}
 
		if ( outputFormat != null  && 
			!( outputFormat.trim().equalsIgnoreCase("xml" ) || 
			   outputFormat.trim().equalsIgnoreCase("text" ) ) ) { 
			throw new BuildException( "output attribute must be either " +
  									  "'text' or 'xml' for task <"
										+ getTaskName() + "/>",
									  getLocation() );
		}
	
		if ( reportLevel != null  && 
			!( reportLevel.trim().equalsIgnoreCase("low" ) || 
			   reportLevel.trim().equalsIgnoreCase("medium" ) ||
			   reportLevel.trim().equalsIgnoreCase("high" ) ) ) { 
			throw new BuildException( "reportlevel attribute must be either " +
  									  "'low' or 'medium' or 'high' for task <" + 
										getTaskName() + "/>",
									  getLocation() );
		}
    } 

    /**
     * Create a new JVM to do the work.
     *
     * @since Ant 1.5
     */
	private void execFindbugs() throws BuildException {
		Java findbugsEngine = (Java) project.createTask("java");
		findbugsEngine.setTaskName( getTaskName() );
		findbugsEngine.setFork( true );
		findbugsEngine.setDir( new File(homeDir + File.separator + "lib"));
		findbugsEngine.setJar( new File( homeDir + File.separator + "lib" + 
                                         File.separator + FINDBUGS_JAR ) );
		findbugsEngine.setTimeout( new Long( TIMEOUT ) );
		findbugsEngine.createJvmarg().setLine( jvmargs ); 

		if ( outputFileName != null) {
			findbugsEngine.setOutput(new File(outputFileName));
		}

		StringBuffer sb = new StringBuffer( 1024 );
		sb.append( "-home " + homeDir );
		if ( debug ) sb.append( " -debug");
		if ( sorted ) sb.append( " -sortByClass");
		if ( outputFormat != null && 
			 outputFormat.trim().equalsIgnoreCase("xml") ) {
			sb.append( " -xml");
		}
		if ( quietErrors ) sb.append( " -quiet" );
		if ( reportLevel != null ) sb.append( " -" + reportLevel.trim().toLowerCase() );
		if ( projectFile != null ) sb.append( " -project " + projectFile );
		if ( excludeFile != null) sb.append( " -exclude " + excludeFile );
		if ( includeFile != null) sb.append( " -include " + includeFile );
		if ( visitors != null) sb.append( " -visitors " + visitors );
		if ( omitVisitors != null ) sb.append( " -omitvisitors " + omitVisitors );
		if ( auxClasspath != null ) sb.append( " -auxclasspath " + auxClasspath );
		if ( sourcePath != null) sb.append( " -sourcepath " + sourcePath );
		sb.append( " -exitcode" );
        Iterator itr = classLocations.iterator();
        while ( itr.hasNext() ) {
        	sb.append( " " + itr.next().toString() );
      	} 

		findbugsEngine.createArg().setLine( sb.toString() );

		log("Running FindBugs...");

		int rc = findbugsEngine.executeJava();

		if ((rc & ExitCodes.ERROR_FLAG) != 0) {
			throw new BuildException("Execution of findbugs failed.");
		}
		if ((rc & ExitCodes.BUGS_FOUND_FLAG) != 0) {
			log("Bugs were found");
		}
		if ((rc & ExitCodes.MISSING_CLASS_FLAG) != 0) {
			log("Some classes needed for analysis were missing");
		}
		if (outputFileName != null) {
			log("Output saved to " + outputFileName);
		}
    } 

}

// vim:ts=4

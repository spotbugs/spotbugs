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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.FindBugs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.taskdefs.MatchingTask;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * FindBugs in Java class files. This task can take the following
 * arguments:
 * <ul>
 * <li>classpath
 * <li>debug
 * <li>failonerror
 * <li>classdir
 * </ul>
 * Of these arguments, the <b>classdir</b> is required.
 * <p>
 * When this task executes, it will recursively scan the classdir 
 * looking for Java class files to check. 
 *
 * @author Mike Fagan <a href="mailto:mfagan@tde.com">mfagan@tde.com</a>
 *
 * @version $Revision: 1.0
 *
 * @since Ant 1.5
 *
 * @ant.task category="utility"
 */

public class FindBugsTask extends MatchingTask {

    private static final String FAIL_MSG
        = "Findbugs found errors; see the error output for details.";

    private Path classes;
    private Path checkClasspath;
    private boolean debug = false;
    private boolean sorted = true;
    private boolean listFiles = false;
    private boolean failOnError = true;
    private int classCount = 0;
    private int errorCount = 0;
    private int archiveCount = 0;
    private ArrayList checkList = new ArrayList();
    private ArrayList suppressList = new ArrayList();
	private String homeDir;

	/**
	 * Set findbugs.home.
	 */
	public void setHome(String homeDir) {
		this.homeDir = homeDir;
	}

    /**
     * Add a error suppress
     */
    public SuppressError createSuppress() {
        SuppressError e = new SuppressError();
        suppressList.add( e );
        return e;
    }
    /**
     * Adds a path for source compilation.
     *
     * @return a nested class element.
     */
    public Path createClassDir() {
        if (classes == null) {
            classes = new Path(getProject());
        }
        return classes.createPath();
    }

    /**
     * Set the class directories to find the source Java files.
     */
    public void setClassDir(Path classDir) {
        if (classes == null) {
            classes = classDir;
        } else {
            classes.append(classDir);
        }
    }

    /**
     * Set the classpath to be used for this compilation.
     * 
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public void setClasspath(Path classpath) {
        if (checkClasspath == null) {
            checkClasspath = classpath;
        } else {
            checkClasspath.append(classpath);
        }
    }

    /**
     * Adds a path to the classpath.
     */
    public Path createClasspath() {
        if (checkClasspath == null) {
            checkClasspath = new Path(getProject());
        }
        return checkClasspath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Indicates whether the build will continue
     * even if there are compilation errors; defaults to true.
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    /**
     * Set the failonerror flag
     */
    public void setProceed(boolean proceed) {
        failOnError = !proceed;
    }

    /**
     * Gets the failonerror flag.
     */
    public boolean getFailonerror() {
        return failOnError;
    }

    /**
     * Indicates whether source should be compiled
     * with debug information; defaults to off.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /** Gets the debug flag. */
    public boolean getDebug() {
        return debug;
    }

    /**
     * Indicates whether output is sorted by class
     * defaults to true.
     */
    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    /**
     * Indicates whether files should be listed first
     * defaults to off.
     */
    public void setListFiles(boolean listFiles) {
        this.listFiles = listFiles;
    }

    /**
     * Executes the task.
     */
    public void execute() throws BuildException {
        checkParameters();
        resetFileLists();

        // scan class directories ato build up
        // check lists
        String[] list = classes.list();
        for (int i = 0; i < list.length; i++) {
            File classDir = getProject().resolveFile(list[i]);
            if (!classDir.exists()) {
                throw new BuildException("classdir \""
                                         + classDir.getPath()
                                         + "\" does not exist!", getLocation());
            }

            DirectoryScanner ds = this.getDirectoryScanner(classDir);
            String filenames[] = ds.getIncludedFiles();
            for ( int j = 0; j < filenames.length; j++ ) {
                if ( filenames[j].endsWith( ".class" ) ) {
                  classCount++;
                  checkList.add( new File(classDir,filenames[j]).getAbsolutePath());
                }
                else if ( filenames[j].endsWith( ".zip" )
                     || filenames[j].endsWith( ".jar" ) ) {
                  archiveCount++;
                  checkList.add( new File(classDir,filenames[j]).getAbsolutePath());
                }
            }
        }
               
        checkCode();
    }

    protected void incErrorCount() {
      errorCount++;
    }

    /**
     * Clear the list of files to be checked.
     */
    protected void resetFileLists() {
        checkList = new ArrayList();
        classCount = 0; 
        archiveCount = 0; 
    }

    /**
     * Check that all required attributes have been set and nothing
     * silly has been entered.
     *
     * @since Ant 1.5
     */
    protected void checkParameters() throws BuildException {
        if (classes == null || classes.size() == 0 ) {
            throw new BuildException("classdir attribute must be set!",
                                     getLocation());
        }

		if (homeDir == null)
			throw new BuildException("home attribute must be set!", getLocation());
    }

    /**
     * Perform the work.
     *
     * @since Ant 1.5
     */
    protected void checkCode() {
		// Make sure findbugs.home property is set
		System.setProperty("findbugs.home", homeDir);

        if ( listFiles ) {
            for ( int i = 0; i < checkList.size(); i++ ) {
                 String filename = (String)checkList.get(i);
                 log(filename) ;
            }
        }

        if ( (archiveCount + classCount) > 0) {
          StringBuffer sb = new StringBuffer( 40 );

            sb.append( "Checking " );
            if ( classCount > 0 ) {
				sb.append(classCount + " class" + (classCount==1? " " : "es ") );
            }
            if ( archiveCount > 0 ) {
				sb.append( (classCount>0?"and ":"") + archiveCount 
                           + " archive" + (archiveCount==1? " " : "s") );
            }
            log( sb.toString() );

            errorCount = 0;            
	        BugReporter bugReporter = new Reporter( sorted, suppressList ); 
         
            try { 
			  edu.umd.cs.findbugs.Project findBugsProject = new edu.umd.cs.findbugs.Project();
	          FindBugs findBugs = new FindBugs(bugReporter, findBugsProject);

			  Iterator i = checkList.iterator();
			  while (i.hasNext()) {
				String fileName = (String) i.next();
				findBugsProject.addJar(fileName);
			  }

              findBugs.execute(); 
            } catch ( Exception e ) {
				log( e.getMessage(), Project.MSG_ERR );
                errorCount++;
            }
            if ( errorCount > 0 ) { 
				if (failOnError) {
				    log( "Found " + errorCount + " error" + (errorCount ==1?"":"s"), Project.MSG_ERR );
					throw new BuildException(FAIL_MSG, getLocation());
				} else {
					log(FAIL_MSG, Project.MSG_ERR);
				}
            }
        }
    }

	private static final Pattern missingClassPattern = Pattern.compile("^.*while looking for class ([^:]*):.*$");

    /**
     * Our BugReporter. 
     */
    private class Reporter implements BugReporter {
    	private Set bugInstanceSet;
		private Set missingClassSet;
        private boolean sorted;
        private ArrayList suppressList;
		private int verbosity;

        public Reporter( boolean sorted, ArrayList suppressList ) {
            this.sorted = sorted;
            this.suppressList = suppressList;
			this.verbosity = BugReporter.NORMAL;
			this.missingClassSet = new HashSet();

            if ( sorted ) {
              bugInstanceSet = new TreeSet(new Comparator() {
				public int compare(Object lhs, Object rhs) {
					ClassAnnotation lca = ((BugInstance)lhs).getPrimaryClass();
					ClassAnnotation rca = ((BugInstance)rhs).getPrimaryClass();
					if (lca == null || rca == null)
						throw new IllegalStateException("null class annotation: " + lca + "," + rca);
					int cmp = lca.getClassName().compareTo(rca.getClassName());
					if (cmp != 0)
						return cmp;
					return ((BugInstance)lhs).compareTo(rhs);
				}
			});
            }
            else {
              bugInstanceSet = new HashSet();
            }
        }

		public void setErrorVerbosity(int level) {
			this.verbosity = level;
		}

		public void reportBug(BugInstance bug) {
			if ( !suppressed( bug ) && !bugInstanceSet.contains( bug ) ) {
				bugInstanceSet.add( bug );
                incErrorCount();
                if ( !sorted ) {
			      log( bug.getMessage(), Project.MSG_ERR );
                }
			}
		}

		public void logError(String message) {
			outputErrorMessage(message);
		}

		public void mapClassToSource(String className, String sourceFileName) { }

		public String getSourceForClass(String className) { return null; }

		public void reportMissingClass(ClassNotFoundException ex) {
			String message = ex.getMessage();

			// Try to extract class name from exception message
			Matcher matcher = missingClassPattern.matcher(message);
			if (matcher.matches())
				message = matcher.group(1);

			if (!missingClassSet.contains(message)) {
				outputErrorMessage("A class required by the analysis is missing: " + message);
				missingClassSet.add(message);
			}
		}

        private boolean suppressed( BugInstance bug ) {
           Iterator i = suppressList.iterator();
           while ( i.hasNext() ) {
             SuppressError se = (SuppressError) i.next();
             if ( se.suppress( bug ) ) {
               return true;
             }
           }
           return false;
        }

		public void finish() {
            if ( !sorted ) return;
			Iterator i = bugInstanceSet.iterator();
			while (i.hasNext()) {
				BugInstance bugInstance = (BugInstance) i.next();
				log(bugInstance.getMessage(), Project.MSG_ERR);
			}
		}

		public void reportQueuedErrors() {
			// We don't queue errors.
		}

		private void outputErrorMessage(String message) {
			if (verbosity != BugReporter.SILENT)
				log(message, Project.MSG_ERR);
		}
	}

    // nested element <suppress/>
	public class SuppressError {
	  String mError = "";
	  String mClassPattern;
	  String mMethodName;

	  public void setError( String pError ) {
		mError = pError.trim();
	  }

	  public void setClass( String pClassPattern ) {
        mClassPattern = pClassPattern;
        if ( mClassPattern != null && mClassPattern.length() == 0 ) {
          mClassPattern = null;
        }
      }

	  public void setMethod( String pMethodName ) {
        mMethodName = pMethodName;
        if ( mMethodName != null && mMethodName.length() == 0 ) {
          mMethodName = null;
        }
      }

      public boolean suppress( BugInstance pBug ) {
        if ( !mError.equalsIgnoreCase( pBug.getType().substring(0, pBug.getType().indexOf( '_' ) ) ) ) { 
          return false;
        }
       
        boolean classesMatch = (mClassPattern == null || 
				SelectorUtils.match( mClassPattern, 
									 pBug.getPrimaryClass().getClassName() ) );

        boolean methodsMatch = (mMethodName == null || 
             ( pBug.getPrimaryMethod() != null && 
               SelectorUtils.match( mMethodName, 
                                    pBug.getPrimaryMethod().getMethodName() )));

        return classesMatch && methodsMatch;

      }
	}
}

// vim:ts=4

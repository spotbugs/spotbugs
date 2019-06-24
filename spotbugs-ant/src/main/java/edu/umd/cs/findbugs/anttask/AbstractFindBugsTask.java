/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.anttask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Abstract base class for Ant tasks that run programs (main() methods) in
 * findbugs.jar or findbugsGUI.jar.
 *
 * @author David Hovemeyer
 */
public abstract class AbstractFindBugsTask extends Task {
    public static final String FINDBUGS_JAR = "spotbugs.jar";

    public static final long DEFAULT_TIMEOUT = 1200000; // twenty minutes

    public static final String RESULT_PROPERTY_SUFFIX = "executeReturnCode";

    // A System property to set when FindBugs is run
    public static class SystemProperty {
        private String name;

        private String value;

        public SystemProperty() {
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    private final String mainClass;

    private boolean debug = false;

    private File homeDir = null;

    private String jvm = "";

    private String jvmargs = "";

    private long timeout = DEFAULT_TIMEOUT;

    private boolean failOnError = false;

    protected String errorProperty = null;

    private final List<SystemProperty> systemPropertyList = new ArrayList<>();

    private Path classpath = null;

    private Path pluginList = null;

    private Java findbugsEngine = null;

    public String execResultProperty = "edu.umd.cs.findbugs.anttask.AbstractFindBugsTask" + "." + RESULT_PROPERTY_SUFFIX;

    /**
     * Constructor.
     */
    protected AbstractFindBugsTask(String mainClass) {
        this.mainClass = mainClass;
        execResultProperty = mainClass + "." + RESULT_PROPERTY_SUFFIX;
    }

    /**
     * Set the home directory into which findbugs was installed
     *
     * @param homeDir
     *            installation directory
     */
    public void setHome(File homeDir) {
        this.homeDir = homeDir;
    }

    /**
     * Set the debug flag
     *
     * @param flag
     *            {@code true} to enable debugging
     */
    public void setDebug(boolean flag) {
        debug = flag;
    }

    /**
     * Get the debug flag.
     */
    protected boolean getDebug() {
        return debug;
    }

    /**
     * Set any specific jvm args
     *
     * @param args
     *            JVM arguments
     */
    public void setJvmargs(String args) {
        jvmargs = args;
    }

    /**
     * Set the command used to start the VM
     *
     * @param jvm
     *            command used to start the VM
     */
    public void setJvm(String jvm) {
        this.jvm = jvm;
    }

    /**
     * Set timeout in milliseconds.
     *
     * @param timeout
     *            the timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Set the failOnError flag
     *
     * @param flag
     *            {@code true} to enable
     */
    public void setFailOnError(boolean flag) {
        failOnError = flag;
    }

    /**
     * Tells this task to set the property with the given name to "true" when there were errors.
     *
     * @param name
     *            property to set to "true" on errors
     */
    public void setErrorProperty(String name) {
        errorProperty = name;
    }

    /**
     * Create a SystemProperty (to handle &lt;systemProperty&gt; elements).
     *
     * @return new property
     */
    public SystemProperty createSystemProperty() {
        SystemProperty systemProperty = new SystemProperty();
        systemPropertyList.add(systemProperty);
        return systemProperty;
    }

    /**
     * Set the classpath to use.
     *
     * @param src
     *            classpath to use
     */
    public void setClasspath(Path src) {
        if (classpath == null) {
            classpath = src;
        } else {
            classpath.append(src);
        }
    }

    /**
     * Path to use for classpath.
     *
     * @return path to use for classpath
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     *
     * @param r
     *            reference to a classpath defined elsewhere
     */
    public void setClasspathRef(Reference r) {
        Path path = createClasspath();
        path.setRefid(r);
        path.toString(); // Evaluated for its side-effects (throwing a
        // BuildException)
    }

    /**
     * the plugin list to use.
     *
     * @param src
     *            plugin list to use
     */
    public void setPluginList(Path src) {
        if (pluginList == null) {
            pluginList = src;
        } else {
            pluginList.append(src);
        }
    }

    /**
     * Path to use for plugin list.
     *
     * @return path to use for plugin list
     */
    public Path createPluginList() {
        if (pluginList == null) {
            pluginList = new Path(getProject());
        }
        return pluginList.createPath();
    }

    /**
     * Adds a reference to a plugin list defined elsewhere.
     *
     * @param r
     *            reference to a plugin list defined elsewhere
     */
    public void setPluginListRef(Reference r) {
        createPluginList().setRefid(r);
    }

    @Override
    public void execute() throws BuildException {
        checkParameters();
        try {
            execFindbugs();
        } catch (BuildException e) {
            // log("Oops: " + e.getMessage());
            if (errorProperty != null) {
                getProject().setProperty(errorProperty, "true");
            }
            if (failOnError) {
                throw e;
            }
        }
    }

    /**
     * Check that all required attributes have been set.
     */
    protected void checkParameters() {
        if (homeDir == null && classpath == null) {
            throw new BuildException("either home attribute or " + "classpath attributes "
                    + " must be defined for task <" + getTaskName() + "/>", getLocation());
        }

        if (pluginList != null) {
            // Make sure that all plugins are actually Jar files.
            String[] pluginFileList = pluginList.list();
            for (String pluginFile : pluginFileList) {
                if (!pluginFile.endsWith(".jar")) {
                    throw new BuildException("plugin file " + pluginFile + " is not a Jar file " + "in task <" + getTaskName()
                            + "/>", getLocation());
                }
            }
        }

        for (SystemProperty systemProperty : systemPropertyList) {
            if (systemProperty.getName() == null || systemProperty.getValue() == null) {
                throw new BuildException("systemProperty elements must have name and value attributes");
            }
        }
    }

    /**
     * Create the FindBugs engine (the Java process that will run whatever
     * FindBugs-related program this task is going to execute).
     */
    protected void createFindbugsEngine() {
        findbugsEngine = new Java();
        findbugsEngine.setProject(getProject());
        findbugsEngine.setTaskName(getTaskName());
        findbugsEngine.setFork(true);
        if (jvm.length() > 0) {
            findbugsEngine.setJvm(jvm);
        }
        findbugsEngine.setTimeout(timeout);

        if (debug) {
            jvmargs = jvmargs + " -Dfindbugs.debug=true";
        }
        jvmargs = jvmargs + " -Dfindbugs.hostApp=FBAntTask";
        findbugsEngine.createJvmarg().setLine(jvmargs);

        // Add JVM arguments for system properties
        for (SystemProperty systemProperty : systemPropertyList) {
            String jvmArg = "-D" + systemProperty.getName() + "=" + systemProperty.getValue();
            findbugsEngine.createJvmarg().setValue(jvmArg);
        }

        if (homeDir != null) {
            // Use findbugs.home to locate findbugs.jar and the standard
            // plugins. This is the usual means of initialization.
            File findbugsLib = new File(homeDir, "lib");
            if (!findbugsLib.exists() && "lib".equals(homeDir.getName())) {
                findbugsLib = homeDir;
                homeDir = homeDir.getParentFile();
            }
            File findbugsLibFindBugs = new File(findbugsLib, "spotbugs.jar");
            // log("executing using home dir [" + homeDir + "]");
            if (findbugsLibFindBugs.exists()) {
                findbugsEngine.setClasspath(new Path(getProject(), findbugsLibFindBugs.getPath()));
            } else {
                throw new IllegalArgumentException("Can't find spotbugs.jar in " + findbugsLib);
            }
            findbugsEngine.createJvmarg().setValue("-Dspotbugs.home=" + homeDir.getPath());
        } else {
            // Use an explicitly specified classpath and list of plugin Jars
            // to initialize. This is useful for other tools which may have
            // FindBugs installed using a non-standard directory layout.

            findbugsEngine.setClasspath(classpath);
        }
        if (pluginList != null) {
            addArg("-pluginList");
            addArg(pluginList.toString());
        }
        // Set the main class to be whatever the subclass's constructor
        // specified.
        findbugsEngine.setClassname(mainClass);
    }

    /**
     * Get the Findbugs engine.
     */
    protected Java getFindbugsEngine() {
        return findbugsEngine;
    }

    /**
     * Add an argument to the JVM used to execute FindBugs.
     *
     * @param arg
     *            the argument
     */
    protected void addArg(String arg) {
        findbugsEngine.createArg().setValue(arg.trim());
    }

    /**
     * Sets the given string to be piped to standard input of the FindBugs JVM
     * upon launching.
     */
    protected void setInputString(String input) {
        findbugsEngine.setInputString(input);
    }

    /**
     * Create a new JVM to do the work.
     *
     * @since Ant 1.5
     */
    private void execFindbugs() throws BuildException {

        System.out.println("Executing SpotBugs " + this.getClass().getSimpleName() + " from ant task");
        createFindbugsEngine();
        configureFindbugsEngine();

        beforeExecuteJavaProcess();

        if (getDebug()) {
            log(getFindbugsEngine().getCommandLine().describeCommand());
        }

        /*
         * set property containing return code of child process using a task
         * identifier and a UUID to ensure exit code corresponds to this
         * execution (the base Ant Task won't overwrite return code once it's
         * been set, so unique identifiers must be used for each execution if we
         * want to get the exit code)
         */
        String execReturnCodeIdentifier = execResultProperty + "." + UUID.randomUUID().toString();
        getFindbugsEngine().setResultProperty(execReturnCodeIdentifier);

        /*
         * if the execution fails, we'll report it ourself -- prevent the
         * underlying Ant Java object from throwing an exception
         */
        getFindbugsEngine().setFailonerror(false);
        try {
            getFindbugsEngine().execute();
        } catch (BuildException be) {
            // setFailonerror(false) should ensure that this doesn't happen,
            // but...
            log(be.toString());
        }
        String returnProperty = getFindbugsEngine().getProject().getProperty(execReturnCodeIdentifier);
        int rc = returnProperty == null ? 0 : Integer.parseInt(returnProperty);

        afterExecuteJavaProcess(rc);
    }

    protected abstract void configureFindbugsEngine();

    protected abstract void beforeExecuteJavaProcess();

    protected void afterExecuteJavaProcess(int rc) {
        if (rc != 0) {
            throw new BuildException("execution of " + getTaskName() + " failed");
        }

    }

}

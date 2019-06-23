/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2013 University of Maryland
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
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.ExitCodes;

/**
 * FindBugs in Java class files. This task can take the following arguments:
 * <ul>
 * <li>adjustExperimental (boolean default false)</li>
 * <li>adjustPriority (passed to -adjustPriority)</li>
 * <li>applySuppression (exclude any warnings that match a suppression filter
 * supplied in a project file)</li>
 * <li>auxAnalyzepath (class, jar, zip files or directories containing classes
 * to analyze)</li>
 * <li>auxClasspath (classpath or classpathRef)</li>
 * <li>baselineBugs (xml file containing baseline bugs)</li>
 * <li>class (class, jar, zip or directory containing classes to analyze)</li>
 * <li>classpath (classpath for running FindBugs)</li>
 * <li>conserveSpace (boolean - default false)</li>
 * <li>debug (boolean default false)</li>
 * <li>effort (enum min|default|max)</li>
 * <li>excludeFilter (filter filename)</li>
 * <li>excludePath (classpath or classpathRef to filters)</li>
 * <li>failOnError (boolean - default false)</li>
 * <li>home (findbugs install dir)</li>
 * <li>includeFilter (filter filename)</li>
 * <li>includePath (classpath or classpathRef to filters)</li>
 * <li>maxRank (maximum rank issue to be reported)</li>
 * <li>jvm (Set the command used to start the VM)</li>
 * <li>jvmargs (any additional jvm arguments)</li>
 * <li>omitVisitors (collection - comma separated)</li>
 * <li>onlyAnalyze (restrict analysis to find bugs to given comma-separated list
 * of classes and packages - See the textui argument description for details)</li>
 * <li>output (enum text|xml|xml:withMessages|html - default xml)</li>
 * <li>outputFile (name of output file to create)</li>
 * <li>nested (boolean default true)</li>
 * <li>noClassOk (boolean default false)</li>
 * <li>pluginList (list of plugin Jar files to load)</li>
 * <li>projectFile (project filename)</li>
 * <li>projectName (project name, for display in generated HTML)</li>
 * <li>userPreferencesFile (user preferences filename)</li>
 * <li>quietErrors (boolean - default false)</li>
 * <li>relaxed (boolean - default false)</li>
 * <li>reportLevel (enum experimental|low|medium|high)</li>
 * <li>sort (boolean default true)</li>
 * <li>stylesheet (name of stylesheet to generate HTML: default is
 * "default.xsl")</li>
 * <li>systemProperty (a system property to set)</li>
 * <li>timestampNow (boolean - default false)</li>
 * <li>visitors (collection - comma separated)</li>
 * <li>chooseVisitors (selectively enable/disable visitors)</li>
 * <li>workHard (boolean default false)</li>
 * <li>setSetExitCode (boolean default true)</li>
 * </ul>
 * <p>Of these arguments, the <b>home</b> is required. <b>projectFile</b> is
 * required if nested &lt;class&gt; or &lt;auxAnalyzepath&gt; elements are not
 * specified. the &lt;class&gt; tag defines the location of either a class, jar
 * file, zip file, or directory containing classes.
 * </p>
 *
 * @author Mike Fagan <a href="mailto:mfagan@tde.com">mfagan@tde.com</a>
 * @author Michael Tamm <a href="mailto:mail@michaeltamm.de">mail@michaeltamm.de</a>
 * @author Scott Wolk
 * @version $Revision: 1.56 $
 *
 * @since Ant 1.5
 *
 * @ant.task category="utility"
 */

public class FindBugsTask extends AbstractFindBugsTask {

    private String effort;

    private boolean conserveSpace;

    private boolean sorted = true;

    private boolean timestampNow = true;

    private boolean quietErrors;

    private String warningsProperty;

    private int maxRank;

    private String projectName;

    private boolean workHard;

    private boolean relaxed;

    private boolean adjustExperimental;

    private String adjustPriority;

    private File projectFile;

    private File userPreferencesFile;

    private File baselineBugs;

    private boolean applySuppression;

    private File excludeFile;

    private Path excludePath;

    private File includeFile;

    private Path includePath;

    private Path auxClasspath;

    private Path auxAnalyzepath;

    private Path sourcePath;

    private String outputFormat = "xml";

    private String reportLevel;

    private String visitors;

    private String chooseVisitors;

    private String omitVisitors;

    private String outputFileName;

    private String stylesheet;

    private final List<ClassLocation> classLocations = new ArrayList<>();

    private String onlyAnalyze;

    private boolean noClassOk;

    private boolean nested = true;

    private boolean setExitCode = true;

    private final List<FileSet> filesets = new ArrayList<>();

    private final List<DirSet> dirsets = new ArrayList<>();

    public FindBugsTask() {
        super("edu.umd.cs.findbugs.FindBugs2");
    }

    // define the inner class to store class locations
    public static class ClassLocation {
        File classLocation = null;

        public void setLocation(File location) {
            classLocation = location;
        }

        public File getLocation() {
            return classLocation;
        }

        @Override
        public String toString() {
            return classLocation != null ? classLocation.toString() : "";
        }

    }

    /**
     * Set the workHard flag.
     *
     * @param workHard
     *            true if we want findbugs to run with workHard option enabled
     */
    public void setWorkHard(boolean workHard) {
        this.workHard = workHard;
    }

    /**
     * Set the exit code flag.
     *
     * @param setExitCode
     *            If true then the exit code will be returned to
     *            the main ant job
     */
    public void setSetExitCode(boolean setExitCode) {
        this.setExitCode = setExitCode;
    }

    /**
     * Set the nested flag.
     *
     * @param nested
     *            This option enables or disables scanning of
     *            nested jar and zip files found in the list of files
     *            and directories to be analyzed. By default, scanning
     *            of nested jar/zip files is enabled
     */
    public void setNested(boolean nested) {
        this.nested = nested;
    }

    /**
     * Set the noClassOk flag.
     *
     * @param noClassOk
     *            true if we should generate no-error output if no classfiles
     *            are specified
     */
    public void setNoClassOk(boolean noClassOk) {
        this.noClassOk = noClassOk;
    }

    /**
     * Set the relaxed flag.
     *
     * @param relaxed
     *            true if we want findbugs to run with relaxed option enabled
     */
    public void setRelaxed(boolean relaxed) {
        this.relaxed = relaxed;
    }

    /**
     * Set the adjustExperimental flag
     *
     * @param adjustExperimental
     *            true if we want experimental bug patterns to have lower
     *            priority
     */
    public void setAdjustExperimental(boolean adjustExperimental) {
        this.adjustExperimental = adjustExperimental;
    }

    public void setAdjustPriority(String adjustPriorityString) {
        adjustPriority = adjustPriorityString;
    }

    /**
     * Set the specific visitors to use
     *
     * @param commaSeperatedString
     *            visitors to use
     */
    public void setVisitors(String commaSeperatedString) {
        visitors = commaSeperatedString;
    }

    /**
     * Set the specific visitors to use
     *
     * @param commaSeperatedString
     *            visitors to use
     */
    public void setChooseVisitors(String commaSeperatedString) {
        chooseVisitors = commaSeperatedString;
    }

    /**
     * Set the specific visitors to use
     *
     * @param commaSeperatedString
     *            visitors to use
     */
    public void setOmitVisitors(String commaSeperatedString) {
        omitVisitors = commaSeperatedString;
    }

    /**
     * Set the output format
     *
     * @param format
     *            output format
     */
    public void setOutput(String format) {
        outputFormat = format;
    }

    /**
     * Set the stylesheet filename for HTML generation.
     *
     * @param stylesheet
     *            stylesheet filename for HTML generation
     */
    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    /**
     * Set the report level
     *
     * @param level
     *            the report level
     */
    public void setReportLevel(String level) {
        reportLevel = level;
    }

    /**
     * Set the sorted flag
     *
     * @param flag
     *            sorted
     */
    public void setSort(boolean flag) {
        sorted = flag;
    }

    /**
     * Set the timestampNow flag
     *
     * @param flag
     *            timestampNow
     */
    public void setTimestampNow(boolean flag) {
        timestampNow = flag;
    }

    /**
     * Set the quietErrors flag
     *
     * @param flag
     *            quietErrors
     */
    public void setQuietErrors(boolean flag) {
        quietErrors = flag;
    }

    /**
     * Set the applySuppression flag
     *
     * @param flag
     *            applySuppression
     */
    public void setApplySuppression(boolean flag) {
        applySuppression = flag;
    }

    /**
     * Tells this task to set the property with the given name to "true" when bugs were found.
     *
     * @param name
     *            property with the given name
     */
    public void setWarningsProperty(String name) {
        warningsProperty = name;
    }

    /**
     * Set effort level.
     *
     * @param effort
     *            the effort level
     */
    public void setEffort(String effort) {
        this.effort = effort;
    }

    public void setMaxRank(int maxRank) {
        this.maxRank = maxRank;
    }

    /**
     * Set project name
     *
     * @param projectName
     *            the project name
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Set the conserveSpace flag.
     *
     * @param flag
     *            conserveSpace
     */
    public void setConserveSpace(boolean flag) {
        conserveSpace = flag;
    }

    /**
     * Set the exclude filter file
     *
     * @param filterFile
     *            exclude filter file
     */
    public void setExcludeFilter(File filterFile) {
        if (filterFile != null && filterFile.length() > 0) {
            excludeFile = filterFile;
        } else {
            if (filterFile != null) {
                log("Warning: exclude filter file " + filterFile
                        + (filterFile.exists() ? " is empty" : " does not exist"));
            }
            excludeFile = null;
        }
    }

    /**
     * Set the include filter file
     *
     * @param filterFile
     *            include filter file
     */
    public void setIncludeFilter(File filterFile) {
        if (filterFile != null && filterFile.length() > 0) {
            includeFile = filterFile;
        } else {
            if (filterFile != null) {
                log("Warning: include filter file " + filterFile
                        + (filterFile.exists() ? " is empty" : " does not exist"));
            }
            includeFile = null;
        }
    }

    /**
     * Set the baseline bugs file
     *
     * @param baselineBugs
     *            baseline bugs file
     */
    public void setBaselineBugs(File baselineBugs) {
        if (baselineBugs != null && baselineBugs.length() > 0) {
            this.baselineBugs = baselineBugs;
        } else {
            if (baselineBugs != null) {
                log("Warning: baseline bugs file " + baselineBugs
                        + (baselineBugs.exists() ? " is empty" : " does not exist"));
            }
            this.baselineBugs = null;
        }
    }

    /**
     * Set the project file
     *
     * @param projectFile
     *            project file
     */
    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }

    /**
     * Set the user preferences file
     *
     * @param userPreferencesFile
     *            user preferences file
     */
    public void setUserPreferencesFile(File userPreferencesFile) {
        this.userPreferencesFile = userPreferencesFile;
    }

    /**
     * the auxclasspath to use.
     *
     * @param src
     *            auxclasspath to use
     */
    public void setAuxClasspath(Path src) {
        boolean nonEmpty = false;

        String[] elementList = src.list();
        for (String anElementList : elementList) {
            if (!"".equals(anElementList)) {
                nonEmpty = true;
                break;
            }
        }

        if (nonEmpty) {
            if (auxClasspath == null) {
                auxClasspath = src;
            } else {
                auxClasspath.append(src);
            }
        }
    }

    /**
     * Path to use for auxclasspath.
     *
     * @return auxclasspath
     */
    public Path createAuxClasspath() {
        if (auxClasspath == null) {
            auxClasspath = new Path(getProject());
        }
        return auxClasspath.createPath();
    }

    /**
     * Adds a reference to a sourcepath defined elsewhere.
     *
     * @param r
     *            reference to a sourcepath defined elsewhere
     */
    public void setAuxClasspathRef(Reference r) {
        Path path = createAuxClasspath();
        path.setRefid(r);
        path.toString(); // Evaluated for its side-effects (throwing a
        // BuildException)
    }

    /**
     * the auxAnalyzepath to use.
     *
     * @param src
     *            auxAnalyzepath
     */
    public void setAuxAnalyzepath(Path src) {
        boolean nonEmpty = false;

        String[] elementList = src.list();
        for (String anElementList : elementList) {
            if (!"".equals(anElementList)) {
                nonEmpty = true;
                break;
            }
        }

        if (nonEmpty) {
            if (auxAnalyzepath == null) {
                auxAnalyzepath = src;
            } else {
                auxAnalyzepath.append(src);
            }
        }
    }

    /**
     * Path to use for auxAnalyzepath.
     *
     * @return auxAnalyzepath
     */
    public Path createAuxAnalyzepath() {
        if (auxAnalyzepath == null) {
            auxAnalyzepath = new Path(getProject());
        }
        return auxAnalyzepath.createPath();
    }

    /**
     * Adds a reference to a auxAnalyzepath defined elsewhere.
     *
     * @param r
     *            reference to a auxAnalyzepath defined elsewhe
     */
    public void setAuxAnalyzepathRef(Reference r) {
        createAuxAnalyzepath().setRefid(r);
    }

    /**
     * the sourcepath to use.
     *
     * @param src
     *            sourcepath
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
     *
     * @return sourcepath
     */
    public Path createSourcePath() {
        if (sourcePath == null) {
            sourcePath = new Path(getProject());
        }
        return sourcePath.createPath();
    }

    /**
     * Adds a reference to a source path defined elsewhere.
     *
     * @param r
     *            reference to a source path defined elsewhere
     */
    public void setSourcePathRef(Reference r) {
        createSourcePath().setRefid(r);
    }

    /**
     * the excludepath to use.
     *
     * @param src
     *            excludepath
     */
    public void setExcludePath(Path src) {
        if (excludePath == null) {
            excludePath = src;
        } else {
            excludePath.append(src);
        }
    }

    /**
     * Path to use for excludepath.
     *
     * @return excludepath
     */
    public Path createExcludePath() {
        if (excludePath == null) {
            excludePath = new Path(getProject());
        }
        return excludePath.createPath();
    }

    /**
     * Adds a reference to a source path defined elsewhere.
     *
     * @param r
     *            reference to a exclude path defined elsewhe
     */
    public void setExcludePathRef(Reference r) {
        createExcludePath().setRefid(r);
    }

    /**
     * the includepath to use.
     *
     * @param src
     *            includepath
     */
    public void setIncludePath(Path src) {
        if (includePath == null) {
            includePath = src;
        } else {
            includePath.append(src);
        }
    }

    /**
     * Path to use for includepath.
     *
     * @return includepath
     */
    public Path createIncludePath() {
        if (includePath == null) {
            includePath = new Path(getProject());
        }
        return includePath.createPath();
    }

    /**
     * Adds a reference to a include path defined elsewhere.
     *
     * @param r
     *            reference to a include path defined elsewher
     */
    public void setIncludePathRef(Reference r) {
        createIncludePath().setRefid(r);
    }

    /**
     * Add a class location
     *
     * @return class location
     */
    public ClassLocation createClass() {
        ClassLocation cl = new ClassLocation();
        classLocations.add(cl);
        return cl;
    }

    /**
     * Set name of output file.
     *
     * @param outputFileName
     *            name of output file
     */
    public void setOutputFile(String outputFileName) {
        if (outputFileName != null && outputFileName.length() > 0) {
            this.outputFileName = outputFileName;
        }
    }

    /**
     * Set the packages or classes to analyze
     *
     * @param filter
     *            packages or classes to analyze
     */
    public void setOnlyAnalyze(String filter) {
        onlyAnalyze = filter;
    }

    /**
     * Add a nested fileset of classes or jar files.
     *
     * @param fs
     *            nested fileset of classes or jar files
     */
    public void addFileset(FileSet fs) {
        filesets.add(fs);
    }

    /**
     * Add a nested dirset of classes dirs.
     *
     * @param fs
     *            nested dirset of classes dirs
     */
    public void addDirset(DirSet fs) {
        dirsets.add(fs);
    }

    /**
     * Check that all required attributes have been set
     */
    @Override
    protected void checkParameters() {
        super.checkParameters();

        if (projectFile == null && classLocations.size() == 0 && filesets.size() == 0 && dirsets.size() == 0 && auxAnalyzepath == null) {
            throw new BuildException("either projectfile, <class/>, <fileset/> or <auxAnalyzepath/> child "
                    + "elements must be defined for task <" + getTaskName() + "/>", getLocation());
        }

        if (outputFormat != null
                && !("xml".equalsIgnoreCase(outputFormat.trim()) || "xml:withMessages".equalsIgnoreCase(outputFormat.trim())
                        || "html".equalsIgnoreCase(outputFormat.trim()) || "text".equalsIgnoreCase(outputFormat.trim())
                        || "xdocs".equalsIgnoreCase(outputFormat.trim()) || "emacs".equalsIgnoreCase(outputFormat.trim()))) {
            throw new BuildException("output attribute must be either " + "'text', 'xml', 'html', 'xdocs' or 'emacs' for task <"
                    + getTaskName() + "/>", getLocation());
        }

        if (reportLevel != null
                && !("experimental".equalsIgnoreCase(reportLevel.trim()) || "low".equalsIgnoreCase(reportLevel.trim())
                        || "medium".equalsIgnoreCase(reportLevel.trim()) || "high".equalsIgnoreCase(reportLevel.trim()))) {
            throw new BuildException("reportlevel attribute must be either "
                    + "'experimental' or 'low' or 'medium' or 'high' for task <" + getTaskName() + "/>", getLocation());
        }

        // FindBugs allows both, so there's no apparent reason for this check
        // if ( excludeFile != null && includeFile != null ) {
        // throw new BuildException("only one of excludeFile and includeFile " +
        // " attributes may be used in task <" + getTaskName() + "/>",
        // getLocation());
        // }

        List<String> efforts = Arrays.asList("min", "less", "default", "more", "max");
        if (effort != null && !efforts.contains(effort)) {
            throw new BuildException("effort attribute must be one of " + efforts);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#beforeExecuteJavaProcess
     * ()
     */
    @Override
    protected void beforeExecuteJavaProcess() {
        log("Running SpotBugs...");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#afterExecuteJavaProcess
     * (int)
     */
    @Override
    protected void afterExecuteJavaProcess(int rc) {
        if ((rc & ExitCodes.ERROR_FLAG) != 0) {
            throw new BuildException("Execution of SpotBugs failed.");
        }
        if ((rc & ExitCodes.MISSING_CLASS_FLAG) != 0) {
            log("Classes needed for analysis were missing");
        }
        if (warningsProperty != null && (rc & ExitCodes.BUGS_FOUND_FLAG) != 0) {
            getProject().setProperty(warningsProperty, "true");
        }

        if (outputFileName != null) {
            log("Output saved to " + outputFileName);
        }
    }

    @Override
    protected void configureFindbugsEngine() {
        if (projectName != null) {
            addArg("-projectName");
            addArg(projectName);
        }
        if (adjustExperimental) {
            addArg("-adjustExperimental");
        }

        if (conserveSpace) {
            addArg("-conserveSpace");
        }
        if (workHard) {
            addArg("-workHard");
        }
        if (effort != null) {
            addArg("-effort:" + effort);
        }
        if (maxRank >= BugRanker.VISIBLE_RANK_MIN && maxRank <= BugRanker.VISIBLE_RANK_MAX) {
            addArg("-maxRank ");
            addArg(Integer.toString(maxRank));
        }
        if (adjustPriority != null) {
            addArg("-adjustPriority");
            addArg(adjustPriority);
        }

        if (sorted) {
            addArg("-sortByClass");
        }
        if (timestampNow) {
            addArg("-timestampNow");
        }

        if (outputFormat != null && !"text".equalsIgnoreCase(outputFormat.trim())) {
            outputFormat = outputFormat.trim();
            String outputArg = "-";
            int colon = outputFormat.indexOf(':');
            if (colon >= 0) {
                outputArg += outputFormat.substring(0, colon).toLowerCase();
                outputArg += ":";
                outputArg += outputFormat.substring(colon + 1);
            } else {
                outputArg += outputFormat.toLowerCase();
                if (stylesheet != null) {
                    outputArg += ":";
                    outputArg += stylesheet.trim();
                }
            }
            addArg(outputArg);
        }
        if (quietErrors) {
            addArg("-quiet");
        }
        if (reportLevel != null) {
            addArg("-" + reportLevel.trim().toLowerCase());
        }
        if (projectFile != null) {
            addArg("-project");
            addArg(projectFile.getPath());
        }
        if (userPreferencesFile != null) {
            addArg("-userPrefs");
            addArg(userPreferencesFile.getPath());
        }
        if (applySuppression) {
            addArg("-applySuppression");
        }

        if (baselineBugs != null) {
            addArg("-excludeBugs");
            addArg(baselineBugs.getPath());
        }
        if (excludeFile != null) {
            addArg("-exclude");
            addArg(excludeFile.getPath());
        }
        if (excludePath != null) {
            String[] result = excludePath.toString().split(java.io.File.pathSeparator);
            for (String element : result) {
                addArg("-exclude");
                addArg(element);
            }
        }
        if (includeFile != null) {
            addArg("-include");
            addArg(includeFile.getPath());
        }
        if (includePath != null) {
            String[] result = includePath.toString().split(java.io.File.pathSeparator);
            for (String element : result) {
                addArg("-include");
                addArg(element);
            }
        }
        if (visitors != null) {
            addArg("-visitors");
            addArg(visitors);
        }
        if (omitVisitors != null) {
            addArg("-omitVisitors");
            addArg(omitVisitors);
        }
        if (chooseVisitors != null) {
            addArg("-chooseVisitors");
            addArg(chooseVisitors);
        }

        if (auxClasspath != null) {
            try {
                // Try to dereference the auxClasspath.
                // If it throws an exception, we know it
                // has an invalid path entry, so we complain
                // and tolerate it.
                @SuppressWarnings("unused")
                String unreadReference = auxClasspath.toString();
                String auxClasspathString = auxClasspath.toString();
                if (!auxClasspathString.isEmpty()) {
                    if (auxClasspathString.length() > 100) {
                        addArg("-auxclasspathFromInput");
                        setInputString(auxClasspathString);
                    } else {
                        addArg("-auxclasspath");
                        addArg(auxClasspathString);
                    }
                }
            } catch (Throwable t) {
                log("Warning: auxClasspath " + t + " not found.");
            }
        }
        if (sourcePath != null) {
            addArg("-sourcepath");
            addArg(sourcePath.toString());
        }
        if (outputFileName != null) {
            addArg("-outputFile");
            addArg(outputFileName);
        }
        if (relaxed) {
            addArg("-relaxed");
        }
        if (!nested) {
            addArg("-nested:false");
        }
        if (noClassOk) {
            addArg("-noClassOk");
        }

        if (onlyAnalyze != null) {
            addArg("-onlyAnalyze");
            addArg(onlyAnalyze);
        }

        if (setExitCode) {
            addArg("-exitcode");
        }

        for (ClassLocation classLocation : classLocations) {
            addArg(classLocation.toString());
        }

        for (FileSet fs : filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner();
            for (String fileName : ds.getIncludedFiles()) {
                File file = new File(ds.getBasedir(), fileName);
                addArg(file.toString());
            }
        }

        for (DirSet fs : dirsets) {
            DirectoryScanner ds = fs.getDirectoryScanner();
            for (String fileName : ds.getIncludedDirectories()) {
                File file = new File(ds.getBasedir(), fileName);
                addArg(file.toString());
            }
        }

        if (auxAnalyzepath != null) {
            String[] result = auxAnalyzepath.toString().split(java.io.File.pathSeparator);
            for (String element : result) {
                addArg(element);
            }
        }
    }
}

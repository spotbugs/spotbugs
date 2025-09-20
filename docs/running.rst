Running SpotBugs
================

SpotBugs has two user interfaces: a graphical user interface (GUI) and a command line user interface.
This chapter describes how to run each of these user interfaces.

Quick Start
-----------

If you are running SpotBugs on a Windows system, double-click on the file ``%SPOTBUGS_HOME%\lib\spotbugs.jar`` to start the SpotBugs GUI.

On a Unix, Linux, or macOS system, run the ``$SPOTBUGS_HOME/bin/spotbugs`` script, or run the command ``java -jar $SPOTBUGS_HOME/lib/spotbugs.jar`` to run the SpotBugs GUI.

Refer to :doc:`gui` for information on how to use the GUI.

Executing SpotBugs
------------------

This section describes how to invoke the SpotBugs program.
There are two ways to invoke SpotBugs: directly, or using a wrapper script.

Direct invocation of SpotBugs
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The preferred method of running SpotBugs is to directly execute ``$SPOTBUGS_HOME/lib/spotbugs.jar`` using the -jar command line switch of the JVM (java) executable.
(Versions of SpotBugs prior to 1.3.5 required a wrapper script to invoke SpotBugs.)

The general syntax of invoking SpotBugs directly is the following:

.. code:: sh

    java [JVM arguments] -jar $SPOTBUGS_HOME/lib/spotbugs.jar options...

Choosing the User Interface
***************************

The first command line option chooses the SpotBugs user interface to execute. Possible values are:

-gui:
  runs the graphical user interface (GUI)

-textui:
  runs the command line user interface

-version:
  displays the SpotBugs version number

-help:
  displays help information for the SpotBugs command line user interface

-gui1:
  executes the original (obsolete) SpotBugs graphical user interface

Java Virtual Machine (JVM) arguments
************************************

Several Java Virtual Machine arguments are useful when invoking SpotBugs.

-XmxNNm:
  Set the maximum Java heap size to NN megabytes.
  SpotBugs generally requires a large amount of memory.
  For a very large project, using 1500 megabytes is not unusual.

-Dname=value:
  Set a Java system property.
  For example, you might use the argument ``-Duser.language=ja`` to display GUI messages in Japanese.

Invocation of SpotBugs using a wrapper script
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Another way to run SpotBugs is to use a wrapper script.

On Unix-like systems, use the following command to invoke the wrapper script:

.. code:: sh

    $ $SPOTBUGS_HOME/bin/spotbugs options...

On Windows systems, the command to invoke the wrapper script is

.. code:: sh

    C:\My Directory>%SPOTBUGS_HOME%\bin\spotbugs.bat options...

On both Unix-like and Windows systems, you can simply add the ``$SPOTBUGS_HOME/bin`` directory to your ``PATH`` environment variable and then invoke SpotBugs using the ``spotbugs`` command.

Wrapper script command line options
***********************************

The SpotBugs wrapper scripts support the following command-line options.
Note that these command line options are not handled by the SpotBugs program per se; rather, they are handled by the wrapper script.

-jvmArgs *args*:
  Specifies arguments to pass to the JVM. For example, you might want to set a JVM property:

  .. code:: sh

      $ spotbugs -textui -jvmArgs "-Duser.language=ja" myApp.jar

-javahome *directory*:
  Specifies the directory containing the JRE (Java Runtime Environment) to use to execute FindBugs.

-maxHeap *size*:
  Specifies the maximum Java heap size in megabytes. The default is 256.
  More memory may be required to analyze very large programs or libraries.

-debug:
  Prints a trace of detectors run and classes analyzed to standard output.
  Useful for troubleshooting unexpected analysis failures.

-property *name=value*:
  This option sets a system property.
  SpotBugs uses system properties to configure analysis options.
  See :doc:`analysisprops`.
  You can use this option multiple times in order to set multiple properties.
  Note: In most versions of Windows, the name=value string must be in quotes.

Command-line Options
--------------------

This section describes the command line options supported by SpotBugs.
These command line options may be used when invoking SpotBugs directly, or when using a wrapper script.

Common command-line options
^^^^^^^^^^^^^^^^^^^^^^^^^^^

These options may be used with both the GUI and command-line interfaces.

-effort[:min|less|default|more|max]:
  Set analysis effort level. 
  The -effort:min disables several analyses that increase precision but also increase memory consumption. You may want to try this option if you find that SpotBugs with the -effort:less still runs out of memory, or takes an unusually long time to complete its analysis.
  The -effort:less disables some analyses that increase precision but also increase memory consumption. You may want to try this option if you find that SpotBugs with the -effort:more/-effort:default runs out of memory, or takes an unusually long time to complete its analysis.
  The -effort:more runs several analyses to find bugs, this is the -effort:default.
  The -effort:max enable analyses which increase precision and find more bugs, but which may require more memory and take more time to complete.
  See :doc:`effort`.

-project *project*:
  Specify a project to be analyzed. The project file you specify should be one that was created using the GUI interface.
  It will typically end in the extension .fb or .fbp.
  
-pluginList <jar1[;jar2...]>:
  Specify list of plugin Jar files to load.
  
-home <home directory>:
  Specify SpotBugs home directory.
    
-adjustExperimental:
  Lower priority of experimental Bug Patterns.
  
-workHard:
  Ensure analysis effort is at least 'default'.

-conserveSpace:
  Same as -effort:min (for backward compatibility).

GUI Options
^^^^^^^^^^^

These options are only accepted by the Graphical User Interface.

-look:plastic|gtk|native:
  Set Swing look and feel.

Text UI Options
^^^^^^^^^^^^^^^

These options are only accepted by the Text User Interface.

-sortByClass=filepath:
  Sort reported bug instances by class name.

  From SpotBugs 4.5.0, this option receives a file path like ``-sortByClass=path/to/spotbugs.txt``.
  It is also supported to set multiple reports like ``-xml=spotbugs.xml -sortByClass=spotbugs.txt``.

-include *filterFile.xml*:
  Only report bug instances that match the filter specified by filterFile.xml.
  See :doc:`filter`.

-exclude *filterFile.xml*:
  Report all bug instances except those matching the filter specified by filterFile.xml.
  See :doc:`filter`.

-onlyAnalyze *com.foobar.MyClass,com.foobar.mypkg.*,!com.foobar.mypkg.ExcludedClass*:
  Restrict analysis to find bugs to given comma-separated list of classes and packages.
  Unlike filtering, this option avoids running analysis on classes and packages that are not explicitly matched: for large projects, this may greatly reduce the amount of time needed to run the analysis.
  (However, some detectors may produce inaccurate results if they aren't run on the entire application.)
  Classes should be specified using their full classnames (including package), and packages should be specified in the same way they would in a Java import statement to import all classes in the package (i.e., add .* to the full name of the package).
  Replace ``.*`` with ``.-`` to also analyze all subpackages.
  Items starting with ``!`` are treated as exclusions, removing otherwise-included classes from analysis.

-low:
  Report all bugs.

-medium:
  Report medium and high priority bugs. This is the default setting.

-high:
  Report only high priority bugs.

-relaxed:
  Relaxed reporting mode.
  For many detectors, this option suppresses the heuristics used to avoid reporting false positives.

-xml=filepath:
  Produce the bug reports as XML.
  The XML data produced may be viewed in the GUI at a later time.
  You may also specify this option as ``-xml:withMessages``; when this variant of the option is used, the XML output will contain human-readable messages describing the warnings contained in the file.
  XML files generated this way are easy to transform into reports.

  From SpotBugs 4.5.0, this option receives a file path like ``-xml:withMessages=path/to/spotbugs.xml``.
  It is also supported to set multiple reports like ``-xml=spotbugs.xml -html=spotbugs.html``.

-html=filepath:
  Generate HTML output. By default, SpotBugs will use the default.xsl XSLT stylesheet to generate the HTML: you can find this file in spotbugs.jar, or in the SpotBugs source or binary distributions.
  Variants of this option include ``-html:plain.xsl``, ``-html:fancy.xsl`` and ``-html:fancy-hist.xsl``.
  The ``plain.xsl`` stylesheet does not use Javascript or DOM, and may work better with older web browsers, or for printing.
  The ``fancy.xsl`` stylesheet uses DOM and Javascript for navigation and CSS for visual presentation.
  The ``fancy-hist.xsl`` an evolution of ``fancy.xsl`` stylesheet. It makes an extensive use of DOM and Javascript for dynamically filtering the lists of bugs.

  If you want to specify your own XSLT stylesheet to perform the transformation to HTML, specify the option as ``-html:myStylesheet.xsl``, where ``myStylesheet.xsl`` is the filename of the stylesheet you want to use.

  From SpotBugs 4.5.0, this option receives a file path like ``-html:fancy-hist.xsl=path/to/spotbugs.html``.
  It is also supported to set multiple reports like ``-xml=spotbugs.xml -html=spotbugs.html``.

-sarif=filepath:
  Produce the bug reports in `SARIF 2.1.0 <https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html>`_.

  From SpotBugs 4.5.0, this option receives a file path like ``-sarif=path/to/spotbugs.sarif``.
  It is also supported to set multiple reports like ``-xml=spotbugs.xml -sarif=spotbugs.sarif``.

-emacs=filepath:
  Produce the bug reports in Emacs format.

-xdocs=filepath:
  Produce the bug reports in xdoc XML format for use with Apache Maven.

-output *filename*:
  This argument is deprecated. Use report type option like ``-xml=spotbugs.xml`` instead.

-outputFile *filename*:
  This argument is deprecated. Use report type option like ``-xml=spotbugs.xml`` instead.

-nested[:true|false]:
  This option enables or disables scanning of nested jar and zip files found in the list of files and directories to be analyzed.
  By default, scanning of nested jar/zip files is enabled. To disable it, add ``-nested:false`` to the command line arguments.

-auxclasspath *classpath*:
  Set the auxiliary classpath for analysis.
  This classpath should include all jar files and directories containing classes that are part of the program being analyzed but you do not want to have analyzed for bugs.

-auxclasspathFromInput:
  Read the auxiliary classpath for analysis from standard input, each line adds new entry to the auxiliary classpath for analysis.

-auxclasspathFromFile *filepath*:
  Read the auxiliary classpath for analysis from file, each line adds new entry to the auxiliary classpath for analysis.

-analyzeFromFile *filepath*:
  Read the files to analyze from file, each line adds new entry to the classpath for analysis.

-userPrefs *edu.umd.cs.findbugs.core.prefs*:
  Set the path of the user preferences file to use, which might override some of the options above.
  Specifying userPrefs as first argument would mean some later options will override them, as last argument would mean they will override some previous options).
  This rationale behind this option is to reuse SpotBugs Eclipse project settings for command line execution.

-showPlugins:
  Show list of available detector plugins.

Output options
**************
-timestampNow:
  Set timestamp of results to be current time.

-quiet:
  Suppress error messages.

-longBugCodes:
  Report long bug codes.

-progress:
  Display progress in terminal window.

-release <release name>:
  Set the release name of the analyzed application.

-maxRank <rank>:
  Only report issues with a bug rank at least as scary as that provided.

-dontCombineWarnings:
  Don't combine warnings that differ only in line number.

-train[:outputDir]:
  Save training data (experimental); output dir defaults to '.'.

-useTraining[:inputDir]:
  Use training data (experimental); input dir defaults to '.'.

-redoAnalysis <filename>:
  Redo analysis using configuration from previous analysis.

-sourceInfo <filename>:
  Specify source info file (line numbers for fields/classes).

-projectName <project name>:
  Descriptive name of project.

-reanalyze <filename>:
  Redo analysis in provided file.
  
Output filtering options
************************
-bugCategories <cat1[,cat2...]>:
  Only report bugs in given categories.

-excludeBugs <baseline bugs>:
  Exclude bugs that are also reported in the baseline xml output.

-applySuppression:
  Exclude any bugs that match suppression filter loaded from fbp file.

Detector (visitor) configuration options
****************************************
-visitors <v1[,v2...]>:
  Run only named visitors.

-omitVisitors <v1[,v2...]>:
  Omit named visitors.

-chooseVisitors <+v1,-v2,...>:
  Selectively enable/disable detectors.

-choosePlugins <+p1,-p2,...>:
  Selectively enable/disable plugins.

-adjustPriority <v1=(raise|lower|suppress)[,...]>:
  Raise/lower priority of warnings for given detectors (simple or fully qualified class names) or bug patterns, or suppress them completely

Project configuration options
*****************************
-sourcepath <source path>:
  Set source path for analyzed classes.

-exitcode:
  Set exit code of process.

-noClassOk:
  Output empty warning file if no classes are specified.

-xargs:
  Get list of classfiles/jarfiles from standard input rather than command line.

-bugReporters <name,name2,-name3>:
  Bug reporter decorators to explicitly enable/disable.

-printConfiguration:
  Print configuration and exit, without running analysis.

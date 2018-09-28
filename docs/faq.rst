SpotBugs FAQ
============

This document contains answers to frequently asked questions about SpotBugs.
If you just want general information about SpotBugs, have a look at the manual.

Q1: I'm getting java.lang.UnsupportedClassVersionError when I try to run SpotBugs
---------------------------------------------------------------------------------

SpotBugs requires JRE8 or later to run.
If you use an earlier version, you will see an exception error message similar to the following:

  Exception in thread "main" java.lang.UnsupportedClassVersionError:
  edu/umd/cs/findbugs/gui/FindBugsFrame (Unsupported major.minor version 52.0)

The solution is to upgrade to JRE8 or later.

Q2: SpotBugs is running out of memory, or is taking a long time to finish
-------------------------------------------------------------------------

In general, SpotBugs requires lots of memory and a relatively fast CPU.
For large applications, 1024M or more of heap space may be required.

By default, SpotBugs allocates 768M of heap space.
You can increase this using the ``-maxHeap n`` option, where n is the number of megabytes of heap space to allocate.

Q3: What is the "auxiliary classpath"? Why should I specify it?
---------------------------------------------------------------

Many important facts about a Java class require information about the classes that it references.  For example:

* What other classes and interfaces the class inherits from
* What exceptions can be thrown by methods in external classes and interfaces

The "auxiliary classpath" is a list of Jar files, directories, and class files containing classes that are used by the code you want SpotBugs to analyze, but should not themselves be analyzed by SpotBugs.

If SpotBugs doesn't have complete information about referenced classes, it will not be able to produce results that are as accurate as possible.
For example, having a complete repository of referenced classes allows SpotBugs to prune control flow information so it can concentrate on paths through methods that are most likely to be feasible at runtime.
Also, some bug detectors (such as the suspicious reference comparison detector) rely on being able to perform type inference, which requires complete type hierarchy information.

For these reasons, we strongly recommend that you completely specify the auxiliary classpath when you run SpotBugs.
You can do this by using the ``-auxclasspath`` command line option, or the "Classpath entries" list in the GUI project editor dialog.

If SpotBugs cannot find a class referenced by your application, it will print out a message when the analysis completes, specifying the classes that were missing.
You should modify the auxiliary classpath to specify how to find the missing classes, and then run SpotBugs again.


Q4: The Eclipse plugin doesn't load
-----------------------------------

The symptom of this problem is that Eclipse fails to load the SpotBugs UI plugin with the message:

    Plug-in "edu.umd.cs.findbugs.plugin.eclipse" was disabled due to missing or disabled prerequisite plug-in "org.eclipse.ui.ide"

The reason for this problem is that the Eclipse plugin distributed with SpotBugs does not work with older 3.x versions of Eclipse.
Please use Eclipse Neon (version 4.6) or newer.

Q5: I'm getting a lot of false "OS" and "ODR" warnings
------------------------------------------------------

By default, SpotBugs assumes that any method invocation can throw an unchecked runtime exception.
As a result, it may assume that an unchecked exception thrown out of the method could bypass a call to a ``close()`` method for a stream or database resource.

You can use the ``-workHard`` command line argument or the ``findbugs.workHard`` boolean analysis property to make SpotBugs work harder to prune unlikely exception edges.
This generally reduces the number of false warnings, at the expense of slowing down the analysis.

Q6: The Eclipse plugin loads, but doesn't work correctly
--------------------------------------------------------

* Make sure the Java code you trying to analyze is built properly and has no classpath or compile errors.
* Make sure the project and workspace SpotBugs settings are valid - in doubt, revert them to defaults.
* Make sure the Error log view does not show errors.

Q7: Where is the Maven plugin for SpotBugs?
-------------------------------------------

The Maven Plugin for SpotBugs may be found `here <https://github.com/spotbugs/spotbugs-maven-plugin/>`_.

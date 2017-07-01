Implement SpotBugs plugin
=========================

Create Maven project
--------------------

Use `spotbugs-archetype <https://github.com/spotbugs/spotbugs-archetype>`_ to create Maven project.
Then Maven archetype plugin will ask you to decide plugin's groupId, artifactId, package and initial version.

.. code-block:: bash

  $ mvn archetype:generate \
        -DarchetypeArtifactId=spotbugs-archetype \
        -DarchetypeGroupId=com.github.spotbugs \
        -DarchetypeVersion=0.1.0

Write java codes to represent bug to find
-----------------------------------------

In generated project, you can find a file named as `BadCase.java <https://github.com/spotbugs/spotbugs-archetype/blob/spotbugs-archetype-0.1.0/src/main/resources/archetype-resources/src/test/java/BadCase.java>`_.
Update this file to represent the target bug to find.

If you have multiple patterns to represent, add more classes into ``src/test/java`` directory.


Write test case to ensure your detector can find bug
----------------------------------------------------

In generated project, you can find another file named as `MyDetectorTest.java <https://github.com/spotbugs/spotbugs-archetype/blob/spotbugs-archetype-0.1.0/src/main/resources/archetype-resources/src/test/java/MyDetectorTest.java>`_.
The ``spotbugs.performAnalysis(Path)`` in this test runs SpotBugs with your plugin, and return all found bugs (here 1st argument of this method is a path of class file compiled from ``BadCase.java``).

You can use `BugInstanceMatcher <https://github.com/spotbugs/spotbugs/blob/master/test-harness/src/main/java/edu/umd/cs/findbugs/test/matcher/BugInstanceMatcher.java>`_ to verify that your plugin can find bug as expected.

Currently this test should fail, because we've not updated detector itself yet.


Write java codes to avoid false-positive
----------------------------------------

To avoid false-positive, it is good to ensure that in which case detector should NOT find bug.

Update `GoodCase.java <https://github.com/spotbugs/spotbugs-archetype/blob/spotbugs-archetype-0.1.0/src/main/resources/archetype-resources/src/test/java/GoodCase.java>`_ in your project, and represent such cases.
After that, add a test method into ``MyDetectorTest.java`` which verify that no bug found from this ``GoodCase`` class.

If you have multiple patterns to represent, add more classes into ``src/test/java`` directory.


Update detector to pass all unit tests
--------------------------------------

Now you have tests to ensure that your detector can work as expected.

.. note::

  TBU


Which super class you should choose
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`AnnotationDetector <https://javadoc.io/page/com.github.spotbugs/spotbugs/latest/edu/umd/cs/findbugs/bcel/AnnotationDetector.html>`_
  Base detector which analyzes annotations on classes, fields, methods, and method parameters.

`BytecodeScanningDetector <https://javadoc.io/page/com.github.spotbugs/spotbugs/latest/edu/umd/cs/findbugs/BytecodeScanningDetector.html>`_
  Base detector which analyzes java bytecode in class files.

`OpcodeStackDetector <https://javadoc.io/page/com.github.spotbugs/spotbugs/latest/edu/umd/cs/findbugs/bcel/OpcodeStackDetector.html>`_
  Sub class of ``BytecodeScanningDetector``, which can scan the bytecode of a method and use an `operand stack <https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html#jvms-2.6.2>`_.


Update findbugs.xml and messages.xml
------------------------------------

.. note::

  TBU

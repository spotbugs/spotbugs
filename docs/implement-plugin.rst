Implement SpotBugs plugin
=========================

Create Maven project
--------------------

Use `spotbugs-archetype <https://github.com/spotbugs/spotbugs-archetype>`_ to create Maven project.
Then Maven archetype plugin will ask you to decide plugin's groupId, artifactId, package and initial version.

.. literalinclude:: generated/use-archetype.template.inc
    :language: bash

Write java code to represent bug to find
----------------------------------------

In generated project, you can find a file named as `BadCase.java <https://github.com/spotbugs/spotbugs-archetype/blob/spotbugs-archetype-0.1.0/src/main/resources/archetype-resources/src/test/java/BadCase.java>`_.
Update this file to represent the target bug to find.

If you have multiple patterns to represent, add more classes into ``src/test/java`` directory.


Write test case to ensure your detector can find bug
----------------------------------------------------

In generated project, you can find another file named as `MyDetectorTest.java <https://github.com/spotbugs/spotbugs-archetype/blob/spotbugs-archetype-0.1.0/src/main/resources/archetype-resources/src/test/java/MyDetectorTest.java>`_.
The ``spotbugs.performAnalysis(Path)`` in this test runs SpotBugs with your plugin, and return all found bugs (here 1st argument of this method is a path of class file compiled from ``BadCase.java``).

You can use `BugInstanceMatcher <https://github.com/spotbugs/spotbugs/blob/master/test-harness/src/main/java/edu/umd/cs/findbugs/test/matcher/BugInstanceMatcher.java>`_ to verify that your plugin can find bug as expected.

Currently this test should fail, because we've not updated detector itself yet.


Write java code to avoid false-positive
---------------------------------------

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


Update findbugs.xml
-------------------

SpotBugs reads ``findbugs.xml`` in each plugin to find detectors and bugs.
So when you add new detector, you need to add new ``<Detector>`` element like below:

.. code-block:: xml

  <Detector class="com.github.plugin.MyDetector" reports="MY_BUG" speed="fast" />

It is also necessary to add ``<BugPattern>``, to describe type and category of your bug pattern.

.. code-block:: xml

  <BugPattern type="MY_BUG" category="CORRECTNESS" />

You can find ``findbugs.xml`` in ``src/main/resources`` directory of generated Maven project.



Update messages.xml
-------------------

SpotBugs reads ``messages.xml`` in each plugin to construct human readable message to report detected bug.
It also supports reading localized messages from ``messages_ja.xml``, ``messages_fr.xml`` and so on.

You can find ``messages.xml`` in ``src/main/resources`` directory of generated Maven project.

Update message of Detector
^^^^^^^^^^^^^^^^^^^^^^^^^^

In ``<Detector>`` element, you can add detector's description message. Note that it should be plain text, HTML is not supported.

.. code-block:: xml

  <Detector class="com.github.plugin.MyDetector">
    <Details>
      Original detector to detect MY_BUG bug pattern.
    </Details>
  </Detector>

Update message of Bug Pattern
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In ``<BugPattern>`` element, you can add bug pattern's description message.
There are three kinds of messages:

ShortDescription
  Short description for bug pattern. Useful to tell its intent and character for users.
  It should be plain text, HTML is not supported.

LongDescription
  Longer description for bug pattern.
  You can use placeholder like ``{0}`` (0-indexed), then added data into `BugInstance <https://javadoc.io/page/com.github.spotbugs/spotbugs/latest/edu/umd/cs/findbugs/BugInstance.html>`_ will be inserted at there.
  So this ``LongDescription`` is useful to tell detailed information about detected bug.

  It should be plain text, HTML is not supported.

Details
  Detailed description for bug pattern. It should be HTML format, so this is useful to tell detailed specs/examples with table, list and code snippets.

.. code-block:: xml

  <BugPattern type="MY_BUG">
    <ShortDescription>Explain bug pattern shortly.</ShortDescription>
    <LongDescription>
      Explain existing problem in code, and how developer should improve their implementation.
    </LongDescription>
    <Details>
      <![CDATA[
        <p>Explain existing problem in code, and how developer should improve their implementation.</p>
      ]]>
    </Details>
  </BugPattern>

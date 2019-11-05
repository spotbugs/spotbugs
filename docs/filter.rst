Filter file
===========

Filter files may be used to include or exclude bug reports for particular classes and methods. This chapter explains how to use filter files.

Introduction to Filter Files
----------------------------

Conceptually, a filter matches bug instances against a set of criteria. By defining a filter, you can select bug instances for special treatment;
for example, to exclude or include them in a report.

A filter file is an XML document with a top-level ``FindBugsFilter`` element which has some number of Match elements as children.
Each Match element represents a predicate which is applied to generated bug instances.
Usually, a filter will be used to exclude bug instances. For example::

    $ spotbugs -textui -exclude myExcludeFilter.xml myApp.jar

However, a filter could also be used to select bug instances to specifically report::

    $ spotbugs -textui -include myIncludeFilter.xml myApp.jar

``Match`` elements contain children, which are conjuncts of the predicate.
In other words, each of the children must be ``true`` for the predicate to be ``true``.

Types of Match clauses
----------------------

<Bug>
^^^^^

This element specifies a particular bug ``pattern`` or ``patterns`` to match. The ``pattern`` attribute is a comma-separated list of bug pattern types.
You can find the bug pattern types for particular warnings by looking at the output produced by the **-xml** output option (the type attribute of BugInstance elements), or from the :doc:`bugDescriptions`.

For more coarse-grained matching, use ``code`` attribute. It takes a comma-separated list of bug abbreviations. For most-coarse grained matching use ``category`` attribute, that takes a comma separated list of bug category names: ``CORRECTNESS``, ``MT_CORRECTNESS``, ``BAD_PRACTICICE``, ``PERFORMANCE``, ``STYLE``.

If more than one of the attributes mentioned above are specified on the same <Bug> element, all bug patterns that match either one of specified pattern names, or abbreviations, or categories will be matched.

As a backwards compatibility measure, <BugPattern> and <BugCode> elements may be used instead of <Bug> element. Each of these uses a name attribute for specifying accepted values list. Support for these elements may be removed in a future release.

<Confidence>
^^^^^^^^^^^^

This element matches warnings with a particular bug confidence. The ``value`` attribute should be an integer value: 1 to match high-confidence warnings, 2 to match normal-confidence warnings, or 3 to match low-confidence warnings. ``<Confidence>`` replaced ``<Priority>`` in 2.0.0 release.

<Priority>
^^^^^^^^^^

Same as ``<Confidence>``, exists for backward compatibility.

<Rank>
^^^^^^

This element matches warnings with a particular bug rank. The ``value`` attribute should be an integer value between 1 and 20, where 1 to 4 are scariest, 5 to 9 scary, 10 to 14 troubling, and 15 to 20 of concern bugs.

<Package>
^^^^^^^^^

This element matches warnings associated with classes within the package specified using ``name`` attribute. Nested packages are not included (along the lines of Java import statement). However matching multiple packages can be achieved easily using regex name match.

<Class>
^^^^^^^

This element matches warnings associated with a particular class. The ``name`` attribute is used to specify the exact or regex match pattern for the class name. The ``role`` attribute is the class role.

As a backward compatibility measure, instead of element of this type, you can use ``class`` attribute on a ``Match`` element to specify exact an class name or ``classregex`` attribute to specify a regular expression to match the class name against.

If the ``Match`` element contains neither a ``Class`` element, nor a ``class`` / ``classregex`` attribute, the predicate will apply to all classes. Such predicate is likely to match more bug instances than you want, unless it is refined further down with appropriate method or field predicates.

<Source>
^^^^^^^^

This element matches warnings associated with a particular source file. The ``name`` attribute is used to specify the exact or regex match pattern for the source file name.

<Method>
^^^^^^^^

This element specifies a method. The ``name`` attribute is used to specify the exact or regex match pattern for the method name. The ``params`` attribute is a comma-separated list of the types of the method's parameters. The ``returns`` attribute is the method's return type. The ``role`` attribute is the method role. In ``params`` and ``returns``, class names must be fully qualified. (E.g., ``"java.lang.String"`` instead of just ``"String"``.) If one of the latter attributes is specified the other is required for creating a method signature. Note that you can provide either ``name`` attribute or ``params`` and ``returns`` attributes or all three of them. This way you can provide various kinds of name and signature based matches.

<Field>
^^^^^^^

This element specifies a field. The ``name`` attribute is used to specify the exact or regex match pattern for the field name. You can also filter fields according to their signature - use ``type`` attribute to specify fully qualified type of the field. You can specify either or both of these attributes in order to perform name / signature based matches. The ``role`` attribute is the field role.

<Local>
^^^^^^^

This element specifies a local variable. The ``name`` attribute is used to specify the exact or regex match pattern for the local variable name. Local variables are variables defined within a method.

<Type>
^^^^^^

This element matches warnings associated with a particular type. The ``descriptor`` attribute is used to specify the exact or regex match pattern for type descriptor. If the descriptor starts with the ~ character the rest of attribute content is interpreted as a Java regular expression. The ``role`` attribute is the class role, and the ``typeParameters`` is the type parameters. Both of ``role`` and ``typeParameters`` are optional attributes.

<Or>
^^^^

This element combines ``Match`` clauses as disjuncts. I.e., you can put two ``Method`` elements in an ``Or`` clause in order to match either method.

<And>
^^^^^

This element combines ``Match`` clauses which both must evaluate to ``true``. I.e., you can put ``Bug`` and ``Confidence`` elements in an ``And`` clause in order to match specific bugs with given confidence only.

<Not>
^^^^^

This element inverts the included child ``Match``. I.e., you can put a ``Bug`` element in a ``Not`` clause in order to match any bug excluding the given one.

Java element name matching
--------------------------

If the ``name`` attribute of ``Class``, ``Source``, ``Method`` or ``Field`` starts with the ``~`` character the rest of attribute content is interpreted as a Java regular expression that is matched against the names of the Java element in question.

Note that the pattern is matched against whole element name and therefore ``.*`` clauses need to be used at pattern beginning and/or end to perform substring matching.

See `java.util.regex.Pattern <https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html>`_ documentation for pattern syntax.

Caveats
-------

``Match`` clauses can only match information that is actually contained in the bug instances.
Every bug instance has a class, so in general, excluding bugs by class will work.

Some bug instances have two (or more) classes.
For example, the DE (dropped exception) bugs report both the class containing the method where the dropped exception happens, and the class which represents the type of the dropped exception.
Only the *first* (primary) class is matched against ``Match`` clauses.
So, for example, if you want to suppress IC (initialization circularity) reports for classes "com.foobar.A" and "com.foobar.B", you would use two ``Match`` clauses:

.. code:: xml

  <Match>
     <Class name="com.foobar.A" />
     <Bug code="IC" />
  </Match>
  <Match>
     <Class name="com.foobar.B" />
     <Bug code="IC" />
  </Match>

By explicitly matching both classes, you ensure that the IC bug instance will be matched regardless of which class involved in the circularity happens to be listed first in the bug instance. (Of course, this approach might accidentally suppress circularities involving "com.foobar.A" or "com.foobar.B" and a third class.)

Many kinds of bugs report what method they occur in. For those bug instances, you can put Method clauses in the Match element and they should work as expected.

Examples
--------

Match all bug reports for a class
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <Match>
    <Class name="com.foobar.MyClass" />
  </Match>

Match certain tests from a class by specifying their abbreviations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <Match>
    <Class name="com.foobar.MyClass"/ >
    <Bug code="DE,UrF,SIC" />
  </Match>

Match certain tests from all classes by specifying their abbreviations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <Match>
    <Bug code="DE,UrF,SIC" />
  </Match>

Match certain tests from all classes by specifying their category
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <Match>
    <Bug category="PERFORMANCE" />
  </Match>

Match bug types from specified methods of a class by their abbreviations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <Match>
    <Class name="com.foobar.MyClass" />
    <Or>
      <Method name="frob" params="int,java.lang.String" returns="void" />
      <Method name="blat" params="" returns="boolean" />
    </Or>
    <Bug code="DC" />
  </Match>

Match a particular bug pattern in a particular method
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <!-- A method with an open stream false positive. -->
  <Match>
    <Class name="com.foobar.MyClass" />
    <Method name="writeDataToFile" />
    <Bug pattern="OS_OPEN_STREAM" />
  </Match>

Match a particular bug pattern with a given priority in a particular method
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <!-- A method with a dead local store false positive (medium priority). -->
  <Match>
    <Class name="com.foobar.MyClass" />
    <Method name="someMethod" />
    <Bug pattern="DLS_DEAD_LOCAL_STORE" />
    <Priority value="2" />
  </Match>

Match minor bugs introduced by AspectJ compiler (you are probably not interested in these unless you are an AspectJ developer)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <Match>
    <Class name="~.*\$AjcClosure\d+" />
    <Bug pattern="DLS_DEAD_LOCAL_STORE" />
    <Method name="run" />
  </Match>
  <Match>
    <Bug pattern="UUF_UNUSED_FIELD" />
    <Field name="~ajc\$.*" />
  </Match>

Match bugs in specific parts of the code base
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <!-- match unused fields warnings in Messages classes in all packages -->
  <Match>
    <Class name="~.*\.Messages" />
    <Bug code="UUF" />
  </Match>
  <!-- match mutable statics warnings in all internal packages -->
  <Match>
    <Package name="~.*\.internal" />
    <Bug code="MS" />
  </Match>
  <!-- match anonymous inner classes warnings in ui package hierarchy -->
  <Match>
    <Package name="~com\.foobar\.fooproject\.ui.*" />
    <Bug pattern="SIC_INNER_SHOULD_BE_STATIC_ANON" />
  </Match>

Match bugs on fields or methods with specific signatures
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <!-- match System.exit(...) usage warnings in void main(String[]) methods in all classes -->
  <Match>
    <Method returns="void" name="main" params="java.lang.String[]" />
    <Bug pattern="DM_EXIT" />
  </Match>
  <!-- match UuF warnings on fields of type com.foobar.DebugInfo on all classes -->
  <Match>
    <Field type="com.foobar.DebugInfo" />
    <Bug code="UuF" />
  </Match>

Match bugs using the Not filter operator
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <!-- ignore all bugs in test classes, except for those bugs specifically relating to JUnit tests -->
  <!-- i.e. filter bug if ( classIsJUnitTest && ! bugIsRelatedToJUnit ) -->
  <Match>
    <!-- the Match filter is equivalent to a logical 'And' -->

    <Class name="~.*\.*Test" />
    <!-- test classes are suffixed by 'Test' -->

    <Not>
        <Bug code="IJU" /> <!-- 'IJU' is the code for bugs related to JUnit test code -->
    </Not>
  </Match>

Full exclusion filter file to match all classes generated from Groovy source files
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: xml

  <?xml version="1.0" encoding="UTF-8"?>
  <FindBugsFilter>
  <Match>
    <Source name="~.*\.groovy" />
  </Match>
  </FindBugsFilter>

Complete Example
----------------

.. code:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <FindBugsFilter
		xmlns="https://github.com/spotbugs/filter/3.0.0"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="https://github.com/spotbugs/filter/3.0.0 https://raw.githubusercontent.com/spotbugs/spotbugs/3.1.0/spotbugs/etc/findbugsfilter.xsd">
    <Match>
      <Class name="com.foobar.ClassNotToBeAnalyzed" />
    </Match>

    <Match>
      <Class name="com.foobar.ClassWithSomeBugsMatched" />
      <Bug code="DE,UrF,SIC" />
    </Match>

    <!-- Match all XYZ violations. -->
    <Match>
      <Bug code="XYZ" />
    </Match>

    <!-- Match all doublecheck violations in these methods of "AnotherClass". -->
    <Match>
      <Class name="com.foobar.AnotherClass" />
      <Or>
        <Method name="nonOverloadedMethod" />
        <Method name="frob" params="int,java.lang.String" returns="void" />
        <Method name="blat" params="" returns="boolean" />
      </Or>
      <Bug code="DC" />
    </Match>

    <!-- A method with a dead local store false positive (medium priority). -->
    <Match>
      <Class name="com.foobar.MyClass" />
      <Method name="someMethod" />
      <Bug pattern="DLS_DEAD_LOCAL_STORE" />
      <Priority value="2" />
    </Match>

    <!-- All bugs in test classes, except for JUnit-specific bugs -->
    <Match>
    <Class name="~.*\.*Test" />
    <Not>
      <Bug code="IJU" />
    </Not>
    </Match>
  </FindBugsFilter>

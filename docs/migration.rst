Guide for migration from FindBugs 3.0 to SpotBugs 3.1
=====================================================

com.google.code.findbugs:findbugs
---------------------------------

Simply replace ``com.google.code.findbugs:findbugs`` with ``com.github.spotbugs:spotbugs``.

.. literalinclude:: generated/migration-findbugs-maven.template.inc
    :language: xml

.. literalinclude:: generated/migration-findbugs-gradle.template.inc
    :language: groovy

com.google.code.findbugs:jsr305
-------------------------------

JSR305 is already Dormant status, so SpotBugs does not release ``jsr305`` jar file.
Please continue using findbugs' one.

com.google.code.findbugs:findbugs-annotations
---------------------------------------------

Please depend on ``spotbugs-annotations`` instead.

.. literalinclude:: generated/migration-findbugs-annotations-maven.template.inc
    :language: xml

.. literalinclude:: generated/migration-findbugs-annotations-gradle.template.inc
    :language: groovy

com.google.code.findbugs:annotations
------------------------------------

Please depend on both of ``spotbugs-annotations`` and ``net.jcip:jcip-annotations:1.0`` instead.

.. literalinclude:: generated/migration-annotations-maven.template.inc
    :language: xml

.. literalinclude:: generated/migration-annotations-gradle.template.inc
    :language: groovy

FindBugs Ant task
-----------------

Please replace ``findbugs-ant.jar`` with ``spotbugs-ant.jar``.

.. code-block:: xml

  <taskdef
    resource="edu/umd/cs/findbugs/anttask/tasks.properties"
    classpath="path/to/spotbugs-ant.jar" />
  <property name="spotbugs.home" value="/path/to/spotbugs/home" />

  <target name="spotbugs" depends="jar">
    <spotbugs home="${spotbugs.home}"
              output="xml"
              outputFile="bcel-fb.xml" >
      <auxClasspath path="${basedir}/lib/Regex.jar" />
      <sourcePath path="${basedir}/src/java" />
      <class location="${basedir}/bin/bcel.jar" />
    </spotbugs>
  </target>

FindBugs Maven plugin
---------------------

Please use `com.github.spotbugs:spotbugs-maven-plugin` instead of `org.codehaus.mojo:findbugs-maven-plugin`.

.. literalinclude:: generated/migration-findbugs-maven-plugin.template.inc
    :language: xml

FindBugs Gradle plugin
----------------------

Please use spotbugs plugin found on https://plugins.gradle.org/plugin/com.github.spotbugs

.. literalinclude:: generated/migration-findbugs-gradle-plugin.template.inc
    :language: groovy

FindBugs Eclipse plugin
-----------------------

Please use following update site instead.

* https://spotbugs.github.io/eclipse/ (to use stable version)
* https://spotbugs.github.io/eclipse-candidate/ (to use candidate version)
* https://spotbugs.github.io/eclipse-latest/ (to use latest build)

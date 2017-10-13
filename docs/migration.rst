Guide for migration from FindBugs 3.0 to SpotBugs 3.1
=====================================================

com.google.code.findbugs:findbugs
---------------------------------

Simply replace ``com.google.code.findbugs:findbugs`` with ``com.github.spotbugs:spotbugs``.

.. code-block:: xml

  <!-- for Maven -->
  <dependency>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs</artifactId>
    <version>3.1.0-RC7</version>
  </dependency>

.. code-block:: groovy

  // for Gradle
  compileOnly 'com.github.spotbugs:spotbugs:3.1.0-RC7'

com.google.code.findbugs:jsr305
-------------------------------

JSR305 is already Dormant status, so SpotBugs does not release ``jsr305`` jar file.
Please continue using findbugs' one.

com.google.code.findbugs:findbugs-annotations
---------------------------------------------

Please depend on ``spotbugs-annotations`` instead.

.. code-block:: xml

  <!-- for Maven -->
  <dependency>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-annotations</artifactId>
    <version>3.1.0-RC7</version>
    <optional>true</optional>
  </dependency>

.. code-block:: groovy

  // for Gradle
  compileOnly 'com.github.spotbugs:spotbugs-annotations:3.1.0-RC7'

com.google.code.findbugs:annotations
------------------------------------

Please depend on both of ``spotbugs-annotations`` and ``net.jcip:jcip-annotations:1.0`` instead.

.. code-block:: xml

  <!-- for Maven -->
  <dependency>
    <groupId>net.jcip</groupId>
    <artifactId>jcip-annotations</artifactId>
    <version>1.0</version>
    <optional>true</optional>
  </dependency>
  <dependency>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-annotations</artifactId>
    <version>3.1.0-RC7</version>
    <optional>true</optional>
  </dependency>

.. code-block:: groovy

  // for Gradle
  compileOnly 'net.jcip:jcip-annotations:1.0'
  compileOnly 'com.github.spotbugs:spotbugs-annotations:3.1.0-RC7'

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

.. code-block:: xml

  <plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>3.1.0-RC6</version>
    <dependencies>
      <!-- overwrite dependency on spotbugs if you want to specify the version of spotbugs -->
      <dependency>
        <groupId>com.github.spotbugs</groupId>
        <artifactId>spotbugs</artifactId>
        <version>3.1.0-RC7</version>
      </dependency>
    </dependencies>
  </plugin>

FindBugs Gradle plugin
----------------------

Please use spotbugs plugin found on https://plugins.gradle.org/plugin/com.github.spotbugs

.. code-block:: groovy

  plugins {
    id  'com.github.spotbugs' version '1.3'
  }
  spotbugs {
    toolVersion = '3.1.0-RC7'
  }

  // To generate an HTML report instead of XML
  tasks.withType(com.github.spotbugs.SpotBugsTask) {
    reports {
      xml.enabled = false
      html.enabled = true
    }
  }

FindBugs Eclipse plugin
-----------------------

Please use following update site instead.

* https://spotbugs.github.io/eclipse/ (to use stable version, not ready yet)
* https://spotbugs.github.io/eclipse-candidate/ (to use candidate version)
* https://spotbugs.github.io/eclipse-latest/ (to use latest build)

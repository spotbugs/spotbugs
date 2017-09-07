Using the SpotBugs Gradle Plugin
================================

This chapter describes how to integrate SpotBugs into a build script for Gradle.

Use SpotBugs Gradle Plugin
--------------------------

Please follow instruction found on `official Gradle Plugin page <https://plugins.gradle.org/plugin/com.github.spotbugs>`_.

Note that SpotBugs Gradle Plugin does not support Gradle v3, you need to use v4 or later.

Tasks introduced by this Gradle Plugin
--------------------------------------

This Gradle Plugin introduces two tasks: `spotbugsMain` and `spotbugsTest`.

`spotbugsMain` task runs SpotBugs for your production Java source files. This task depends on `classes` task.
`spotbugsTest` task runs SpotBugs for your test Java source files. This task depends on `testClasses` task.

SpotBugs Gradle Plugin adds task dependency from `check` to these these tasks, so you can simply run ``./gradlew check`` to run SpotBugs.

Configure Gradle Plugin
-----------------------

Current version of SpotBugs Gradle Plugin uses the same way with FindBugs Gradle Plugin to configure. Please check the document for `FindBugsExtension <http://gradle.monochromeroad.com/docs/dsl/org.gradle.api.plugins.quality.FindBugsExtension.html>`_.

For instance, to specify the version of SpotBugs, you can configure like below:

.. code-block:: groovy

  spotbugs {
    toolVersion = '3.1.0-RC5'
  }

Introduce SpotBugs Plugin
-------------------------

To introduce SpotBugs Plugin, please declare dependency in ``dependencies`` like below:

.. code-block:: groovy

  dependencies {
    spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.7.1'
  }

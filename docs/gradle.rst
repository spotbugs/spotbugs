Using the SpotBugs Gradle Plugin
================================

This chapter describes how to integrate SpotBugs into a build script for Gradle.

Use SpotBugs Gradle Plugin
--------------------------

Please follow instruction found on `official Gradle Plugin page <https://plugins.gradle.org/plugin/com.github.spotbugs>`_.

Note that SpotBugs Gradle Plugin does not support Gradle v4, you need to use v5.1 or later.

Tasks introduced by this Gradle Plugin
--------------------------------------

This Gradle Plugin introduces two tasks: `spotbugsMain` and `spotbugsTest`.

`spotbugsMain` task runs SpotBugs for your production Java source files. This task depends on `classes` task.
`spotbugsTest` task runs SpotBugs for your test Java source files. This task depends on `testClasses` task.

SpotBugs Gradle Plugin adds task dependency from `check` to these tasks, so you can simply run ``./gradlew check`` to run SpotBugs.

Configure Gradle Plugin
-----------------------

Current version of SpotBugs Gradle Plugin uses the same way with FindBugs Gradle Plugin to configure. Please check the document for `FindBugsExtension <http://gradle.monochromeroad.com/docs/dsl/org.gradle.api.plugins.quality.FindBugsExtension.html>`_.

For instance, to specify the version of SpotBugs, you can configure like below:

.. literalinclude:: generated/gradle.template.inc
    :language: groovy

Introduce SpotBugs Plugin
-------------------------

To introduce SpotBugs Plugin, please declare dependency in ``dependencies`` like below:

.. code-block:: groovy

  dependencies {
    spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.7.1'
  }

Generate SpotBugs Tasks with Android Gradle Plugin
--------------------------------------------------

SpotBugs Gradle Plugin generates task for each sourceSet.
But Android Gradle Plugin does not generate sourceSet by default (Java plugin does).

So define sourceSets explicitly, then SpotBugs Gradle plugin generates tasks for each of them.

.. code-block:: groovy

  sourceSets {
    // we define `main` sourceSet here, so SpotBugs Gradle Plugin generates `spotbugsMain` task
    main {
      java.srcDirs = ['src/main/java']
    }
  }

  tasks.withType(com.github.spotbugs.SpotBugsTask) {
    // configure automatically generated tasks
  }

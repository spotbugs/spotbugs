Using the SpotBugs Maven Plugin
===============================

This chapter describes how to integrate SpotBugs into a Maven project.

Add spotbugs-maven-plugin to your pom.xml
-----------------------------------------

Add ``<plugin>`` into your ``pom.xml`` like below:

.. literalinclude:: generated/maven.template.inc
    :language: xml

Goals of spotbugs-maven-plugin
------------------------------

spotbugs goal
^^^^^^^^^^^^^

``spotbugs`` goal analyses target project by SpotBugs.
For detail, please refer `spotbugs goal description in maven site <https://spotbugs.github.io/spotbugs-maven-plugin/spotbugs-mojo.html>`_.

check goal
^^^^^^^^^^

``check`` goal runs analysis like ``spotbugs`` goal, and make the build failed if it found any bugs.
For detail, please refer `check goal description in maven site <https://spotbugs.github.io/spotbugs-maven-plugin/check-mojo.html>`_.

gui goal
^^^^^^^^

``gui`` goal launches SpotBugs GUI to check analysis result.
For detail, please refer `gui goal description in maven site <https://spotbugs.github.io/spotbugs-maven-plugin/gui-mojo.html>`_.

help goal
^^^^^^^^^

``help`` goal displays usage of this Maven plugin.

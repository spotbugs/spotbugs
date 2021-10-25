Using the SpotBugs Maven Plugin
===============================

This chapter describes how to integrate SpotBugs into a Maven project.

Add spotbugs-maven-plugin to your pom.xml
-----------------------------------------

Add ``<plugin>`` into your ``pom.xml`` like below:

.. literalinclude:: generated/maven.template.inc
    :language: xml


Integrate Find Security Bugs into spotbugs-maven-plugin
-------------------------------------------------------

Are you looking for additional security detectors for SpotBugs? We suggest you to check the `Find Security Bugs <https://find-sec-bugs.github.io/>`_ a SpotBugs plugin for security audits of Java web and Android applications. It can detect 138 different vulnerability types, including SQL/HQL Injection, Command Injection, XPath Injection, and Cryptography weaknesses.

To integrate Find Security Bugs into SpotBugs plugin, you can configure your ``pom.xml`` like below:

.. literalinclude:: generated/maven-findsecbugs.template.inc
    :language: xml

The ``<plugins>`` option defines a collection of PluginArtifact to work on. Please, specify "Find Security Bugs" by adding its groupId, artifactId, version.

The ``<includeFilterFile>`` and ``<excludeFilterFile>`` specify the filter files to include and exclude bug reports, respectively (see `Filter file <https://spotbugs.readthedocs.io/en/latest/filter.html>`_ for more details). Optionally, you can limit the research to the security category by adding files like below:


*spotbugs-security-include.xml*

.. code-block:: xml

    <FindBugsFilter>
        <Match>
            <Bug category="SECURITY"/>
        </Match>
    </FindBugsFilter>

*spotbugs-security-exclude.xml*

.. code-block:: xml

    <FindBugsFilter>
    </FindBugsFilter>


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

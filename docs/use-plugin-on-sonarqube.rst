Use SpotBugs Plugin on SonarQube
================================

`SpotBugs SonarQube Plugin <https://github.com/spotbugs/sonar-findbugs>`_ uses major SpotBugs plugin such as `fb-contrib <http://fb-contrib.sourceforge.net/>`_ and `Find Security Bugs <http://h3xstream.github.io/find-sec-bugs/>`_. However, if you want to use other SpotBugs plugin, you need to build own SonarQube plugin. For detailed requirements on SonarQube plugin, see `SonarQube official guideline <https://docs.sonarqube.org/display/DEV/Developing+a+Plugin>`_.

Create Maven Project
--------------------

Follow interaction in `SonarQube official guideline <https://docs.sonarqube.org/display/DEV/Build+Plugin#BuildPlugin-CreateaMavenProject>`_.
It is recommended to use sub-module, to manage both of SpotBugs plugin and SonarQube plugin in one project. You can refer `this module <https://github.com/KengoTODA/guava-helper-for-java-8/tree/master/sonarqube-plugin>`_ as example.

You also need to configure ``sonar-packaging-maven-plugin``, to make your plugin depends on `SpotBugs SonarQube Plugin <https://github.com/spotbugs/sonar-findbugs>`_. For instance, if you're using SonarQube 6.7 LTS, your plugin requires SpotBugs SonarQube Plugin version 3.7, so configuration should be like below:

.. code:: xml

  <configuration>
    <basePlugin>findbugs</basePlugin>
    <requirePlugins>findbugs:3.7</requirePlugins>
    ...
  </configuration>

Generate rules.xml
------------------

SonarQube doesn't understand the Bug Pattern metadata provided for SpotBugs, so we need to convert ``findbugs.xml`` and ``messages.xml`` to SonarQube format named ``rules.xml``.

If your SpotBugs plugin isn't complex, you can simply introduce `SonarQube rule xml generator Maven Plugin <https://github.com/KengoTODA/sonarqube-rule-xml-generator>`_ to generate ``rules.xml``. Follow `the interaction described in its README <https://github.com/KengoTODA/sonarqube-rule-xml-generator#how-to-use>`_.

Update RulesDefinision.java
---------------------------

Your ``SonarQubeRulesDefinition.java`` should load generated ``rules.xml`` to FindBugs repository.

When you create a ``NewRepository`` instance, use ``FindbugsRulesDefinition.REPOSITORY_KEY`` as repository key, and do _not_ rename it by calling ``NewRepository#setName(String)``. It is necessary to fulfill the requirement from `SonarQube API <https://github.com/SonarSource/sonarqube/blob/6.7.4/sonar-plugin-api/src/main/java/org/sonar/api/server/rule/RulesDefinition.java#L393-L395>`_. Here is example:

.. code:: java

  @Override
  public void define(Context context) {
    NewRepository repository =
        context.createRepository(FindbugsRulesDefinition.REPOSITORY_KEY, Java.KEY);

    RulesDefinitionXmlLoader ruleLoader = new RulesDefinitionXmlLoader();
    ruleLoader.load(
        repository,
        getClass().getResourceAsStream(
            "/path/to/rules.xml"),
        "UTF-8");
    repository.done();
  }

Update Plugin.java
------------------

``Plugin.java`` should be simple implementation that just loads your ``RulesDefinition`` class. Here is example:

.. code:: java

  @Override
  public void define(Context context) {
    context.addExtensions(Arrays.asList(SonarQubeRulesDefinition.class));
  }

Deploy onto SonarQube
---------------------

``mvn package`` will generate ``.jar`` file that works as SonarQube plugin. Follow `SonarQube official guideline <https://docs.sonarqube.org/display/DEV/Build+Plugin#BuildPlugin-Deploy>`_ to deploy it onto SonarQube.

Note that you need to enable new rules manually in your SonarQube profile, or newly added rules will not be used at analysis.

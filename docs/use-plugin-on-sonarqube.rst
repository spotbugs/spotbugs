Use SpotBugs Plugin on SonarQube
================================

`The SpotBugs SonarQube Plugin <https://github.com/spotbugs/sonar-findbugs>`_ uses major SpotBugs plugins such as `fb-contrib <http://fb-contrib.sourceforge.net/>`_ and `Find Security Bugs <http://h3xstream.github.io/find-sec-bugs/>`_. However, if you want to use another SpotBugs plugin, you need to build your own SonarQube plugin. For detailed requirements on SonarQube plugins, see `the SonarQube official guidelines <https://docs.sonarqube.org/display/DEV/Developing+a+Plugin>`_.

Create Maven Project
--------------------

Follow the interaction in the `SonarQube official guidelines <https://docs.sonarqube.org/display/DEV/Build+Plugin#BuildPlugin-CreateaMavenProject>`_.
It is recommended to use sub-modules, to manage both the SpotBugs plugin and the SonarQube plugin in one project. You can refer to `this module <https://github.com/KengoTODA/guava-helper-for-java-8/tree/master/sonarqube-plugin>`_ as an example.

You also need to configure the ``sonar-packaging-maven-plugin``, to make your plugin depend on `the SpotBugs SonarQube Plugin <https://github.com/spotbugs/sonar-findbugs>`_. For instance, if you're using SonarQube 6.7 LTS, your plugin requires SpotBugs SonarQube Plugin version 3.7, so your configuration should be like below:

.. code:: xml

  <configuration>
    <basePlugin>findbugs</basePlugin>
    <requirePlugins>findbugs:3.7</requirePlugins>
    ...
  </configuration>

Generate rules.xml
------------------

SonarQube doesn't understand the Bug Pattern metadata provided for SpotBugs, so we need to convert ``findbugs.xml`` and ``messages.xml`` to the SonarQube format named ``rules.xml``.

If your SpotBugs plugin isn't complex, you can simply introduce `the SonarQube rule xml generator Maven Plugin <https://github.com/KengoTODA/sonarqube-rule-xml-generator>`_ to generate ``rules.xml``. Follow `the interaction described in its README <https://github.com/KengoTODA/sonarqube-rule-xml-generator#how-to-use>`_.

Update RulesDefinition.java
---------------------------

Your ``SonarQubeRulesDefinition.java`` should load the generated ``rules.xml`` to the FindBugs repository.

When you create a ``NewRepository`` instance, use ``FindbugsRulesDefinition.REPOSITORY_KEY`` as the repository key, and do _not_ rename it by calling ``NewRepository#setName(String)``. It is necessary to fulfill the requirement from `SonarQube API <https://github.com/SonarSource/sonarqube/blob/6.7.4/sonar-plugin-api/src/main/java/org/sonar/api/server/rule/RulesDefinition.java#L393-L395>`_. Here is an example:

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

``Plugin.java`` should be a simple implementation that just loads your ``RulesDefinition`` class. Here is an example:

.. code:: java

  @Override
  public void define(Context context) {
    context.addExtensions(Arrays.asList(SonarQubeRulesDefinition.class));
  }

Deploy onto SonarQube
---------------------

``mvn package`` will generate a ``.jar`` file that works as a SonarQube plugin. Follow `the SonarQube official guidelines <https://docs.sonarqube.org/display/DEV/Build+Plugin#BuildPlugin-Deploy>`_ to deploy it onto SonarQube.

Note that you need to enable new rules manually in your SonarQube profile, or newly added rules will not be used at analysis.

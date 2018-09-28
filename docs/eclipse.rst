Using the SpotBugs Eclipse plugin
=================================

The SpotBugs Eclipse plugin allows SpotBugs to be used within the Eclipse IDE.
The SpotBugs Eclipse plugin was generously contributed by Peter Friese.
Phil Crosby and Andrey Loskutov contributed major improvements to the plugin.

Requirements
------------

To use the SpotBugs Plugin for Eclipse, you need Eclipse Neon (4.6) or later.

Installation
------------

We provide update sites that allow you to automatically install SpotBugs into Eclipse and also query and install updates.
There are three different update sites:

https://spotbugs.github.io/eclipse/
  Only provides official releases of SpotBugs Eclipse plugin.

https://spotbugs.github.io/eclipse-candidate/
  Provides official releases and release candidates of SpotBugs Eclipse plugin.

https://spotbugs.github.io/eclipse-latest/
  Provides latest SpotBugs Eclipse plugin built from master branch.

https://spotbugs.github.io/eclipse-stable-latest/
  Provides latest SpotBugs Eclipse plugin built from release-3.1 branch.

Or just use `Eclipse marketplace <https://marketplace.eclipse.org/content/spotbugs-eclipse-plugin>`_ to install SpotBugs Eclipse plugin.

Using the Plugin
----------------

To get started, right click on a Java project in Package Explorer, and select the option labeled "Spot Bugs".
SpotBugs will run, and problem markers (displayed in source windows, and also in the Eclipse Problems view) will point to locations in your code which have been identified as potential instances of bug patterns.

You can also run SpotBugs on existing java archives (jar, ear, zip, war etc).
Simply create an empty Java project and attach archives to the project classpath.
Having that, you can now right click the archive node in Package Explorer and select the option labeled "Spot Bugs".
If you additionally configure the source code locations for the binaries, SpotBugs will also link the generated warnings to the right source files.

You may customize how SpotBugs runs by opening the Properties dialog for a Java project, and choosing the "SpotBugs" property page.
Options you may choose include:

* Enable or disable the "Run SpotBugs Automatically" checkbox. When enabled, SpotBugs will run every time you modify a Java class within the project.

* Choose minimum warning priority and enabled bug categories. These options will choose which warnings are shown. For example, if you select the "Medium" warning priority, only Medium and High priority warnings will be shown. Similarly, if you uncheck the "Style" checkbox, no warnings in the Style category will be displayed.

* Select detectors. The table allows you to select which detectors you want to enable for your project.

Extending the Eclipse Plugin (since 2.0.0)
------------------------------------------

Eclipse plugin supports contribution of custom SpotBugs detectors (see also AddingDetectors.txt for more information). There are two ways to contribute custom plugins to the Eclipse:

* Existing standard SpotBugs detector packages can be configured via ``Window → Preferences → Java → FindBugs → Misc. Settings → Custom Detectors``. Simply specify there locations of any additional plugin libraries.
  The benefit of this solution is that already existing detector packages can be used "as is", and that you can quickly verify the quality of third party detectors. The drawback is that you have to apply this settings in each new Eclipse workspace, and this settings can't be shared between team members.

* It is possible to contribute custom detectors via standard Eclipse extensions mechanism.

  Please check the documentation of the ``eclipsePlugin/schema/detectorPlugins.exsd`` extension point how to update the plugin.xml.
  Existing FindBugs detector plugins can be easily "extended" to be full featured SpotBugs AND Eclipse detector plugins.
  Usually you only need to add ``META-INF/MANIFEST.MF`` and ``plugin.xml`` to the jar and update your build scripts to not to override the ``MANIFEST.MF`` during the build.

  The benefit of this solution is that for given (shared) Eclipse installation each team member has exactly same detectors set, and there is no need to configure anything anymore.
  The (really small) precondition is that you have to convert your existing detectors package to the valid Eclipse plugin. You can do this even for third-party detector packages.
  Another major differentiator is the ability to extend the default SpotBugs classpath at runtime with required third party libraries (see AddingDetectors.txt for more information).

Troubleshooting
---------------

This section lists common problems with the plugin and (if known) how to resolve them.

* If you see OutOfMemory error dialogs after starting SpotBugs analysis in Eclipse, please increase JVM available memory:
  change ``eclipse.ini`` and add the lines below to the end of the file:

  .. code-block:: none

    -vmargs
    -Xmx1000m

  Important: the configuration arguments starting with the line ``-vmargs`` must be last lines in the ``eclipse.ini`` file, and only one argument per line is allowed!

* If you do not see any SpotBugs problem markers (in your source windows or in the Problems View), you may need to change your ``Problems View`` filter settings.
  See `FAQ <faq.html#q6-the-eclipse-plugin-loads-but-doesn-t-work-correctly>`__ for more information.

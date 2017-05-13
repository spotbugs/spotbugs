# Release procedure

When you release fixed version of SpotBugs, please follow these procedures.

## Release to Maven Central

Add necessary properties to `~/.gradle/gradle.properties` and make sure that you have proper `eclipsePlugin/local.properties` file to release Eclipse plugin at the same time. Then run `./gradlew build smoketest uploadArchives`.

Check [SonaType official page](http://central.sonatype.org/pages/gradle.html) for detail.

## Release to Eclipse Update Site

Send pull-request to spotbugs/spotbugs.github.io, to update contents.
As files to upload, you can use the zip file which is uploaded to Maven Central when you release to there.

See [this pull-request](https://github.com/spotbugs/spotbugs.github.io/pull/12) as example.

## Release to Eclipse Marketplace

No action necessary. Just push latest plugin to Eclipse Update Site then it's enough.
If you need to update [entry at Eclipse Marketplace](https://marketplace.eclipse.org/content/spotbugs-eclipse-plugin), please contact with @KengoTODA or @iloveeclipse.

## Release to Gradle Plugin Portal

Add necessary properties to `~/.gradle/gradle.properties` and run `./gradlew publishPlugins`.

## Release old manuals

Send pull-request to spotbugs/spotbugs.github.io, to update contents.
To generate files to upload, add following properties to `spotbugs/local.properties` and run `./gradlew ant-docs`, then you can get built contents at `spotbugs/build/doc`.

```properties
saxon.home=path/to/saxon6-5-5
xsl.stylesheet.home=path/to/docbook-xsl-1.71.1
```

Use sourceforge to download [Saxon 6.5.5](https://sourceforge.net/projects/saxon/files/saxon6/6.5.5/) and [docbook-xsl 1.71.1](https://sourceforge.net/projects/docbook/files/docbook-xsl/1.71.1/).

## Release to ReadTheDocs

Install `docker 1.13.1` or later in your local, and make sure `docs/build.sh` can run without error. This script should generate HTML pages under `docs/.build/html`. If there is no problem, ReadTheDocs should be possible to build and publish documents to their site. Simply merge your change to `master` then it will be used by [the `latest` document](http://spotbugs.readthedocs.io/en/latest/).

Note that we manage manuals by two ReadTheDocs projects: [spotbugs](https://readthedocs.org/projects/spotbugs/) and [spotbugs-ja](https://readthedocs.org/projects/spotbugs-ja/).  
When we want to add active versions for documents, visit [version page](https://readthedocs.org/projects/spotbugs/versions/). When we want to add more languages, follow [official document](http://docs.readthedocs.io/en/latest/localization.html#project-with-multiple-translations) and create a new ReadTheDocs project for new language.

If you need maintainer access to Read the Docs, please contact with @KengoTODA.

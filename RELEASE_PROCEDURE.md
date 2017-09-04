# Release procedure

When you release fixed version of SpotBugs, please follow these procedures.

## Update version info

* `version` in `build.gradle` and `gradlePlugin/build.gradle`
* version number in `CHANGELOG.md` and `gradlePlugin/CHANGELOG.md`
* `version` and `full_version` in `docs/conf.py`
* version numbers in `docs/migration.rst` and `docs/introduction.rst`

## Release to Maven Central

When we push tag, the build result on Travis CI will be deployed to [SonaType Nexus](https://oss.sonatype.org/). Check [SonaType official page](http://central.sonatype.org/pages/gradle.html) for detail.

After that, please visit SonaType Nexus and [release staging repository](http://central.sonatype.org/pages/releasing-the-deployment.html). Then we can find artifacts after several hours.

## Release to Eclipse Update Site

It's automated by Travis CI.

When we push tag, the build result will be deployed to [eclipse-candidate repository](https://github.com/spotbugs/eclipse-candidate).
When we push tag and its name doesn't contain `_RC`, the build result will be deployed to [eclipse repository](https://github.com/spotbugs/eclipse).

See `deploy` phase in `.travis.yml` for detail.

## Release to Eclipse Marketplace

No action necessary. Just push latest plugin to Eclipse Update Site then it's enough.
If you need to update [entry at Eclipse Marketplace](https://marketplace.eclipse.org/content/spotbugs-eclipse-plugin), please contact with @KengoTODA or @iloveeclipse.

## Release to Gradle Plugin Portal

No action necessary. When we push tag, the build result on Travis CI will be deployed to Gradle Plugin Portal.

See `deploy` phase in `.travis.yml` for detail.

## Update installation manual

`docs/installing.rst` includes link to released binaries, update them.
It is also necessary to change filename in command line example.

## Release old manuals

Send pull-request to spotbugs/spotbugs.github.io, to update contents.
To generate files to upload, add following properties to `spotbugs/local.properties` and run `./gradlew ant-docs`, then you can get built contents at `spotbugs/build/doc`.

```properties
saxon.home=path/to/saxon6-5-5
xsl.stylesheet.home=path/to/docbook-xsl-1.71.1
```

Use sourceforge to download [Saxon 6.5.5](https://sourceforge.net/projects/saxon/files/saxon6/6.5.5/) and [docbook-xsl 1.71.1](https://sourceforge.net/projects/docbook/files/docbook-xsl/1.71.1/).

## Release to ReadTheDocs

See [docs/README.md](docs/README.md) for detail.

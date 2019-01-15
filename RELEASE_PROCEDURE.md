# Release procedure

When you release fixed version of SpotBugs, please follow these procedures.

## Update version info

* `version` in `build.gradle`
* version number in `CHANGELOG.md`
* `version`, `full_version`, `maven_plugin_version` and `gradle_plugin_version` in `docs/conf.py`

## Release to Maven Central

When we push tag, the build result on Travis CI will be deployed to [SonaType Nexus](https://oss.sonatype.org/). Check [SonaType official page](http://central.sonatype.org/pages/gradle.html) for detail.

After that, please visit SonaType Nexus and [release staging repository](http://central.sonatype.org/pages/releasing-the-deployment.html). Then we can find artifacts after several hours.

## Release to Eclipse Update Site

It's automated by Travis CI.

When we push tag, the build result will be deployed to [eclipse-candidate repository](https://github.com/spotbugs/eclipse-candidate).
When we push tag and its name doesn't contain `_RC`, the build result will be deployed to [eclipse repository](https://github.com/spotbugs/eclipse).

See `deploy` phase in `.travis.yml` for detail.

## Release to Eclipse Marketplace

Update version in [Eclipse Marketplace page](https://marketplace.eclipse.org/content/spotbugs-eclipse-plugin). If you have no permission, please contact with @KengoTODA or @iloveeclipse.

## Release to ReadTheDocs

See [docs/README.md](docs/README.md) for detail.

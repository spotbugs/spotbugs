# Changelog

This is the changelog for SpotBugs. This follows [Keep a Changelog v0.3](http://keepachangelog.com/en/0.3.0/).

## Unreleased (2017/??/??)

### Added

* The Eclipse SpotBugs plugin is eligible as an update for FindBugs 3.0.2 and earlier ([#209](https://github.com/spotbugs/spotbugs/issues/209))
* `<EarlierSubtypes>` and `<LaterSubtypes>` can now refer to supertypes from custom plug-ins ([#215](https://github.com/spotbugs/spotbugs/issues/215))

### Removed

* The `AbstractIntegrationTest.containsExactly` and `SpotBugsRule.containsExactly` methods have been replaced by `CountMatcher.containsExactly` ([#269](https://github.com/spotbugs/spotbugs/pull/269))

### Changed

* `jdepend:jdepend:2.9.1` is no longer a compile-scoped dependency but only test-scoped. ([#242](https://github.com/spotbugs/spotbugs/issues/242))
* `ICodeBase`, `IClassPath`, and `URLClassPath` now implement `AutoCloseable` ([#258](https://github.com/spotbugs/spotbugs/issues/258))

### Deprecated

* In future versions of SpotBugs, classes currently implementing the deprecated `org.apache.bcel.Constants` interface may no longer do so.
  Subclasses should either implement this interface themselves or, preferably, use the constants defined in the (non-deprecated) `org.apache.bcel.Const` class instead.
  ([#262](https://github.com/spotbugs/spotbugs/issues/262))

## 3.1.0-RC3 (2017/Jun/10)

### Added

* Make TypeQualifierResolver recognize android.support.annotation.NonNull and Nullable ([#182](https://github.com/spotbugs/spotbugs/pull/182))

### Fixed

* Fix wrong version in Eclipse Plugin ([#173](https://github.com/spotbugs/spotbugs/pull/173))
* When AnalysisRunner has findbugs.xml in jar, don't create temp jar ([#183](https://github.com/spotbugs/spotbugs/pull/183))

## 3.1.0-RC2 (2017/May/16)

### Added

* First release for SpotBugs Gradle Plugin ([#142](https://github.com/spotbugs/spotbugs/pull/142))
* Support plugin development by test harness ([#140](https://github.com/spotbugs/spotbugs/pull/140))

### Changed

* Change Eclipse Plugin ID to avoid conflict with FindBugs Eclipse Plugin ([#157](https://github.com/spotbugs/spotbugs/pull/157))

### Fixed

* Enhance performance of Eclipse Plugin ([#159](https://github.com/spotbugs/spotbugs/pull/1579))
* Fix HTML format in `messages.xml` and others ([#166](https://github.com/spotbugs/spotbugs/pull/166))
* Fix Japanese message in `messages_ja.xml` ([#164](https://github.com/spotbugs/spotbugs/pull/164))

## 3.1.0-RC1 (2017/Feb/21)

### Added

* Make TypeQualifierResolver recognize JetBrains NotNull annotations ([Patch #248](https://sourceforge.net/p/findbugs/patches/248/))
* excludePath and includePath in AntTask ([6668a9](https://github.com/spotbugs/spotbugs/commit/6668a9))
* Cancellation of queueing FindBugsJob in Eclipse plugin ([bceec81](https://github.com/spotbugs/spotbugs/commit/bceec81))
* Artifact which contains only SpotBugs annotations ([Bug#1341](https://sourceforge.net/p/findbugs/bugs/1341/))
* Warn if excludeFilter is empty ([4b7e93f](https://github.com/spotbugs/spotbugs/commit/4b7e93f))
* Partial Java9 support ([FindBugs#105](https://github.com/findbugsproject/findbugs/issues/105))
* `spotbugs.home` is available like `findbugs.home` ([#33](https://github.com/spotbugs/spotbugs/pull/33))

### Changed

* Support user preferences exported by the Export->Preferences wizard in Eclipse ([01b7df7](https://github.com/spotbugs/spotbugs/commit/01b7df7))
* No more dependency in annotations on BugRanker and Priorities ([2f9d672](https://github.com/findbugsproject/findbugs/commit/2f9d672), [725be6e](https://github.com/findbugsproject/findbugs/commit/725be6e))
* Several classes are now not Serializable ([#85](https://github.com/spotbugs/spotbugs/pull/85))

### Deprecated

* `OpcodeStack.Item.defineNewSpecialKind(String)` ([#27](https://github.com/spotbugs/spotbugs/pull/27))
* `Version.RELEASE` ([#125](https://github.com/spotbugs/spotbugs/pull/125))
* `DescriptorFactory.canonicalizeString(String)` ([#128](https://github.com/spotbugs/spotbugs/pull/128))

### Removed

* Java7 Support ([Issue #19](https://github.com/spotbugs/spotbugs/issues/19))
* WebCloud and other plugins
* BlueJ Support
* Artifact which packages not only SpotBugs annotations but also JSR305 annotations

### Fixed

* Typos in description, documentation and so on
* StackOverflowError in ValueRangeAnalysisFactory ([Bug#1369](https://sourceforge.net/p/findbugs/bugs/1369/))
* Command line "@" feature ([Bug#1375](https://sourceforge.net/p/findbugs/bugs/1375/))
* SOAPMessage.getSOAPHeader() can and does return null ([Bug#1368](https://sourceforge.net/p/findbugs/bugs/1368/))
* False positive in UC_USELESS_OBJECT  ([Bug#1373](https://sourceforge.net/p/findbugs/bugs/1373/))
* False positive in NP_LOAD_OF_KNOWN_NULL_VALUE  ([Bug#1372](https://sourceforge.net/p/findbugs/bugs/1372/))
* Missing java.nio.file.Files support in  OS_OPEN_STREAM ([Bugs#1399](https://sourceforge.net/p/findbugs/bugs/1399/)])
* False negative in GC_UNRELATED_TYPES  ([Bug#1387](https://sourceforge.net/p/findbugs/bugs/1387/))
* Not reliable BIT_SIGNED_CHECK ([Bug#1408](https://sourceforge.net/p/findbugs/bugs/1408/))
* Annotation of SIC_INNER_SHOULD_BE_STATIC_ANON ([Bug#1418](https://sourceforge.net/p/findbugs/bugs/1418/))
* Bug in ClassName.isAnonymous ([dcfb934](https://github.com/findbugsproject/findbugs/commit/dcfb934))
* long/double arguments handling in BuildStringPassthruGraph ([370808a](https://github.com/findbugsproject/findbugs/commit/370808a))
* long/double arguments handling in FindSqlInjection ([32a20db](https://github.com/findbugsproject/findbugs/commit/32a20db))
* getEntryValueForParameter in ValueNumberAnalysis ([fb11839](https://github.com/findbugsproject/findbugs/commit/fb11839))
* Do not generate non-constant SQL warnings for passthru methods ([Bug#1416](https://sourceforge.net/p/findbugs/bugs/1416/))
* Too eager "may expose internal representation by storing an externally mutable object" ([Bug#1397](https://sourceforge.net/p/findbugs/bugs/1397/))
* Do not report WrongMapIterator for EnumMap ([Bug#1422](https://sourceforge.net/p/findbugs/bugs/1422/))
* Default Case is Missing With Alias Enum Constants ([Bug#1392](https://sourceforge.net/p/findbugs/bugs/1392/))
* NPE when launched using IBM JDK on Linux ([Bug#1383](https://sourceforge.net/p/findbugs/bugs/1383/))
* Serializable should be out of target for  RI_REDUNDANT_INTERFACES   ([FindBugs#49](https://github.com/findbugsproject/findbugs/pull/49/files))
* nonnull annotations database for java.util.concurrent.ForkJoinPool ((fb8a953)[https://github.com/spotbugs/spotbugs/commit/fb8a953])
* Better handling for JDT illegal signatures([#55](https://github.com/spotbugs/spotbugs/pull/55))
* StaticCalendarDetector is constantly throwing ClassNotFoundExceptions ([#76](https://github.com/spotbugs/spotbugs/pull/76))
* ClassFormatException when analyze class with lambda (INVOKEDYNAMIC) ([#60](https://github.com/spotbugs/spotbugs/issues/60))

## FindBugs 3.0.1 or older

Check [changelog at SourceForge](http://findbugs.sourceforge.net/Changes.html).

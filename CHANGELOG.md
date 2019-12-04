# Changelog

This is the changelog for SpotBugs. This follows [Keep a Changelog v1.0.0](http://keepachangelog.com/en/1.0.0/).

Currently the versioning policy of this project follows [Semantic Versioning v2.0.0](http://semver.org/spec/v2.0.0.html).

## Unreleased - 2019-??-??

### Changed

* Rejuvenated log levels.

### Fixed

* Suppress `Error resolving Real SourcePath (only relative source path will be available)` warning. [#1009](https://github.com/spotbugs/spotbugs/issues/1009)

### Changed

* Bump up Apache Commons BCEL to the version 6.4.1
* update ASM to 7.2 that supports Java 14

## 4.0.0-beta4 - 2019-08-20

### Fixed

* default.xsl declares it is a 2.0 stylesheet, but it appears to have issues with a 2.0 processor ([#958](https://github.com/spotbugs/spotbugs/issues/958))

## 4.0.0-beta3 - 2019-06-24

### Added

* Provide support for CheckerFramework `@NonNull` annotation
* Recognize CheckerFramework type annotations on method return values ([#960](https://github.com/spotbugs/spotbugs/pull/960))
* The feature toggle `spotbugs.experimental.multiThread` for experimental multi-thread analysis
* Add management for source filter using full source path, if available and simple filename does not already match ([#694](https://github.com/spotbugs/spotbugs/issues/694))

## 4.0.0-beta2 - 2019-05-21

### Fixed

* HTML report cannot be generated with `fancy-hist.xsl` ([#944](https://github.com/spotbugs/spotbugs/issues/944))

### Added

* Depend on XSLT 2 engine explicitly ([#944](https://github.com/spotbugs/spotbugs/issues/944))

### Changed

* Replace to try-with-resources
* Reset DataAnalysis.DEBUG back when analysis reaches MAX_ITER
* Remove unused methods in `BCELUtil`
* Remove unused methods and deperecated methods in `edu.umd.cs.findbugs.util.Util`
* Change to removeIf from Iterator and Iterator.remove
* Use Map.computeIfAbsent instead of Map.get and Map.put
* Use for-each instead of for-loop and while-loop
* Bump up SLF4J API to `1.8.0-beta4`

## 4.0.0-beta1 - 2019-03-27

### Added

* update ASM to 7.1 that supports Java 13

### Removed

* non thread-safe implementation in `OpcodeStack.Item` ([#28](https://github.com/spotbugs/spotbugs/issues/28))

### Changed

* Start migrating STDOUT/STDERR usage to a logging framework
* Improvements and bug-fixes for fancy-hist.xsl
* Bump up Apache Commons BCEL to the version 6.3.1

### Deprecated

* SQL files
* JNLP files
* `speed` attribute of `Detector` element in `findbugs.xml`

### Fixed

* Fixed bug priority calculation logic in FindNonShortCircuit#reportBug

## 3.1.12 - 2019-02-28

### Added

* Make TypeQualifierResolver recognize androidx.annotation.NonNull and Nullable ([#880](https://github.com/spotbugs/spotbugs/pull/880))

### Changed
* Bump up Apache Commons BCEL to [the version 6.3](http://mail-archives.apache.org/mod_mbox/commons-user/201901.mbox/%3CCACZkXPy3VgLmD2jppzEPwOqVDJYMM2QG%2BtWQCyzfKmZrDwem6A%40mail.gmail.com%3E)

### Security
* Update dom4j to 2.1.1 to fix security vulnerability. ([#864](https://github.com/spotbugs/spotbugs/issues/864))

## 3.1.11 - 2019-01-18

### Fixed
* False positive: parameter must be non-null in inner class constructor ([#772](https://github.com/spotbugs/spotbugs/issues/772))

## 3.1.10 - 2018-12-19

### Fixed
* Fix bug that enhanced xml options not recognized as textui mode
* Dataflow generates too much log ([#601](https://github.com/spotbugs/spotbugs/issues/601))
* Delete redundant put plugin ([#720](https://github.com/spotbugs/spotbugs/pull/720))

### Added
* Add new detector IRA\_INEFFICIENT\_REPLACEALL for detecting usage of String.replaceAll where no regex is being used ([#705](https://github.com/spotbugs/spotbugs/issues/705))

### Changed
* Eclipse plugin is now signed to establish validity ([#779](https://github.com/spotbugs/spotbugs/issues/779))
* edu.umd.cs.findbugs.util.ClassName#assertIsDotted return type is changed to void
* edu.umd.cs.findbugs.util.ClassName#assertIsSlashed return type is changed to void

### Deprecated
* edu.umd.cs.findbugs.classfile.ClassDescriptor#toDottedClassName() is depricated and getDottedClassName() can be used instead.

## 3.1.9 - 2018-11-20

### Fixed
* Fix some out-of-bounds reports from LGTM
* Update asm to 7.0 for better Java 11 support ([#785](https://github.com/spotbugs/spotbugs/pull/785))
* Ignore @FXML annotated fields in UR\_UNIT\_READ ([#702](https://github.com/spotbugs/spotbugs/issues/702))

### CHANGED
* Allow parallel workspace builds in Eclipse with Spotbugs installed
* Detect method parameter type annotations ([#743](https://github.com/spotbugs/spotbugs/issues/592))

## 3.1.8 - 2018-10-16

### Fixed
* Update asm to 6.2.1 for better Java 12 support ([#741](https://github.com/spotbugs/spotbugs/issues/741))
* Fix hash code collision ([#751](https://github.com/spotbugs/spotbugs/pull/751))
* Partially revert [#688](https://github.com/spotbugs/spotbugs/pull/688) because of the error in specific case with `checkcast` opcode ([#760](https://github.com/spotbugs/spotbugs/pull/760))

## 3.1.7 - 2018-09-12

### Fixed
* Don't print exit code related output if '-quiet' is passed ([#714](https://github.com/spotbugs/spotbugs/pull/714))
* Don't underflow the stack at INVOKEDYNAMIC when modeling stack frame types ([#500](https://github.com/spotbugs/spotbugs/issues/500))

### CHANGED
* ASM_VERSION=ASM7_EXPERIMENTAL by default to support Java 11
* Removed dependency to jFormatString (GPL) code ([#725](https://github.com/spotbugs/spotbugs/issues/725))
* Read User Preferences exported from SpotBugs Eclipse Plugin  ([#728](https://github.com/spotbugs/spotbugs/issues/728))

### ADDED
* Set ASM_VERSION=ASM6 if system property spotbugs.experimental=false

## 3.1.6 - 2018-07-18

### Fixed

* Potential NPE in test-harness-core ([#671](https://github.com/spotbugs/spotbugs/issues/671))
* Support project path with spaces in test-harness-core ([#683](https://github.com/spotbugs/spotbugs/issues/683))
* Processing of "J" (long value constants) was not processed in `OpcodeStack.Item(OpcodeStack.Item, String)`
* Processing of "Z" (boolean value constants) was not processed in `OpcodeStack.Item(OpcodeStack.Item, String)`
* Processing of Box classes like `java.lang.Integer` was not processed in `OpcodeStack.Item(OpcodeStack.Item, String)`

## 3.1.5 - 2018-06-15

### Fixed

* Keep IO.close(Closeable) that was deleted by 3.1.4 ([#661](https://github.com/spotbugs/spotbugs/issues/661))

## 3.1.4 - 2018-06-11 [YANKED]

### Fixed

* RANGE_ARRAY_LENGTH and RANGE_ARRAY_OFFSET false negative ([#595](https://github.com/spotbugs/spotbugs/issues/595))
* Close source file after analysis ([#591](https://github.com/spotbugs/spotbugs/issues/591))
* Inconsistent reporting for EI_EXPOSE_REP2 ([#603](https://github.com/spotbugs/spotbugs/issues/603))
* Update asm to 6.2 for better Java 11 support ([#648](https://github.com/spotbugs/spotbugs/issues/648))
* False positive: 'return value ignored' on Guavas Preconditions.checkNotNull() ([#578](https://github.com/spotbugs/spotbugs/issues/578))
* spotbugs-ant Ant dependency in wrong scope ([#655](https://github.com/spotbugs/spotbugs/issues/655))

## 3.1.3 - 2018-04-18

### Added

* Support for errorprone @CheckReturnValue annotation ([#592](https://github.com/spotbugs/spotbugs/issues/592))

### Fixed

* Handle annotation on `package-info.class` properly ([#592](https://github.com/spotbugs/spotbugs/issues/592))
* Update asm to 6.1.1 to support Java 10
* Update Apache BCEL to 6.2 to support Java 9 package & module reference

## 3.1.2 - 2018-02-24

### Added

* Support for errorprone @CanIgnoreReturnValue annotation ([#463](https://github.com/spotbugs/spotbugs/issues/463))
* Added support for Checker Framework's Nullable annotations.

### Fixed

* Error on lambda analysis: "Constant pool at index 0 is null." ([#547](https://github.com/spotbugs/spotbugs/issues/547))
* Lambda methods reported as missing classes ([#527](https://github.com/spotbugs/spotbugs/issues/527))
* Unused variable reported with wrong name ([#516](https://github.com/spotbugs/spotbugs/issues/516))
* Require gradle 4.2.1 to fix gradle build failures on Java 9.0.1
* Do not print exceptions for unsupported classpath files ([#497](https://github.com/spotbugs/spotbugs/issues/497))
* Update dom4j to 2.1.0 to fix Illegal reflective access on Java 9

## 3.1.1 - 2017-11-29

### Fixed

* NP_NONNULL_PARAM_VIOLATION false positive ([#484](https://github.com/spotbugs/spotbugs/issues/484))
* Add missing package exports to plugin manifest ([#478](https://github.com/spotbugs/spotbugs/issues/478))

## 3.1.0 - 2017-10-25

### Fixed

* Do not try to parse module-info.class ([#408](https://github.com/spotbugs/spotbugs/issues/408))

## 3.1.0-RC7 - 2017-10-14

### Changed

* SpotBugs annotation is recommended instead of JSR305 annotation ([#130](https://github.com/spotbugs/spotbugs/pull/130))
* Improve color in HTML output ([#433](https://github.com/spotbugs/spotbugs/pull/433))

### Fixed

* Wrong Class-Path in MANIFEST.MF ([#407](https://github.com/spotbugs/spotbugs/pull/407))
* Avoid ArithmeticExceptions while interpreting ldiv/lrem values ([#413](https://github.com/spotbugs/spotbugs/issues/413))
* Parse `@CheckReturnValue` even in package-info from aux classpath ([#429](https://github.com/spotbugs/spotbugs/issues/429))

## 3.1.0-RC6 - 2017-09-25

### Removed

* Delete needless bundled libraries from Eclipse plugin ([#330](https://github.com/spotbugs/spotbugs/pull/330))

### Changed

* Upgrade BCEL from 6.1 SNAPSHOT to 6.1 STABLE ([#388](https://github.com/spotbugs/spotbugs/pull/388))
* Upgrade ASM from 6.0 BETA to 6.0 STABLE ([#373](https://github.com/spotbugs/spotbugs/issues/373))

### Added

* Add plugin/README into the distribution ([#331](https://github.com/spotbugs/spotbugs/pull/331))

### Fixed

* Fix broken command line script ([#323](https://github.com/spotbugs/spotbugs/issues/323))
* Fix broken Eclipse classpath variables ([#379](https://github.com/spotbugs/spotbugs/issues/379))
* Fix errors on processing INVOKEDYNAMIC instructions ([#371](https://github.com/spotbugs/spotbugs/issues/371))
* Fix errors on processing i2f, i2d and i2l instructions if the lhs is a character ([#389](https://github.com/spotbugs/spotbugs/issues/389))

## 3.1.0-RC5 - 2017-08-16

### Removed

* The `YourKitProfiler` class has been removed and the `findbugs.yourkit.enabled` system property is no longer supported ([#289](https://github.com/spotbugs/spotbugs/issues/289))

### Changed

* SpotBugs now consumes ASM 6.0 *beta* rather than *alpha* ([#268](https://github.com/spotbugs/spotbugs/issues/268))

## 3.1.0-RC4 - 2017-07-21

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

## 3.1.0-RC3 - 2017-06-10

### Added

* Make TypeQualifierResolver recognize android.support.annotation.NonNull and Nullable ([#182](https://github.com/spotbugs/spotbugs/pull/182))

### Fixed

* Fix wrong version in Eclipse Plugin ([#173](https://github.com/spotbugs/spotbugs/pull/173))
* When AnalysisRunner has findbugs.xml in jar, don't create temp jar ([#183](https://github.com/spotbugs/spotbugs/pull/183))

## 3.1.0-RC2 - 2017-05-16

### Added

* First release for SpotBugs Gradle Plugin ([#142](https://github.com/spotbugs/spotbugs/pull/142))
* Support plugin development by test harness ([#140](https://github.com/spotbugs/spotbugs/pull/140))

### Changed

* Change Eclipse Plugin ID to avoid conflict with FindBugs Eclipse Plugin ([#157](https://github.com/spotbugs/spotbugs/pull/157))

### Fixed

* Enhance performance of Eclipse Plugin ([#159](https://github.com/spotbugs/spotbugs/pull/1579))
* Fix HTML format in `messages.xml` and others ([#166](https://github.com/spotbugs/spotbugs/pull/166))
* Fix Japanese message in `messages_ja.xml` ([#164](https://github.com/spotbugs/spotbugs/pull/164))

## 3.1.0-RC1 - 2017-02-21

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

# Changelog

This is the changelog for SpotBugs. This follows [Keep a Changelog v1.0.0](http://keepachangelog.com/en/1.0.0/).

Currently the versioning policy of this project follows [Semantic Versioning v2.0.0](http://semver.org/spec/v2.0.0.html).

## Unreleased - 2021-??-??
### Fixed
- Ant task does not produce XML anymore ([#1827](https://github.com/spotbugs/spotbugs/issues/1827))

## 4.5.0 - 2021-11-05
### Changed
- Replace "分析" with "解析" in Japanese document ([#1573](https://github.com/spotbugs/spotbugs/issues/1573))
- Add a section to document how to integrate find-sec-bugs into spotbugs-maven-plugin ([#540](https://github.com/spotbugs/spotbugs/issues/540))
- Bump gson from 2.8.8 to 2.8.9 ([#1784](https://github.com/spotbugs/spotbugs/pull/1784))
- Changes related to dominators analysis in package `edu.umd.cs.findbugs.classfile.engine.bcel` ([#1741](https://github.com/spotbugs/spotbugs/pull/1741)):
  - `DominatorsAnalysisFactory` renamed to `NonExceptionDominatorsAnalysisFactory` (clarification)
  - `NonExceptionPostdominatorsAnalysisFactory` renamed to `NonExceptionPostDominatorsAnalysisFactory` (spelling)
  - `NonImplicitExceptionDominatorsAnalysis` introduced (API consistency)

### Added
* Rule `DCN_NULLPOINTER_EXCEPTION` covers catching NullPointerExceptions in accordance with SEI Cert rule [ERR08-J](https://wiki.sei.cmu.edu/confluence/display/java/ERR08-J.+Do+not+catch+NullPointerException+or+any+of+its+ancestors) ([#1740](https://github.com/spotbugs/spotbugs/pull/1740))
* Multiple types of report can be generated in batch. Set multiple commandline options for report configuration like `-html=report/spotbugs.html -xml:withMessages=report/spotbugs.xml`.
* New rule `REFL_REFLECTION_INCREASES_ACCESSIBILITY_OF_CLASS` to detect public methods instantiating a class they get in their parameter. This rule based on the SEI CERT rule *SEC05-J. Do not use reflection to increase accessibility of classes, methods, or fields*. ([#SEC05-J](https://wiki.sei.cmu.edu/confluence/display/java/SEC05-J.+Do+not+use+reflection+to+increase+accessibility+of+classes%2C+methods%2C+or+fields))
* New detector `FindOverridableMethodCall` to detect invocation of overridable method in constructors (`MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR`) and clone() method (`MC_OVERRIDABLE_METHOD_CALL_IN_CLONE`), according to SEI CERT rules [MET05-J. Ensure that constructors do not call overridable methods](https://wiki.sei.cmu.edu/confluence/display/java/MET05-J.+Ensure+that+constructors+do+not+call+overridable+methods) and [MET06-J. Do not invoke overridable methods in clone()](https://wiki.sei.cmu.edu/confluence/pages/viewpage.action?pageId=88487921).
* [Translation of online manual to Brazilian Portuguese (PT-BR)](https://spotbugs.readthedocs.io/pt_BR/latest/).

### Fixed
* False negative about the rule ES_COMPARING_STRINGS_WITH_EQ ([#1764](https://github.com/spotbugs/spotbugs/issues/1764))
* False negative about the rule IM_MULTIPLYING_RESULT_OF_IREM ([#1498])(https://github.com/spotbugs/spotbugs/issues/1498)

### Deprecated
* `-output` commandline option is deprecated. Use commandline options for report configuration like `-xml=spotbugs.xml` instead.

## 4.4.2 - 2021-10-08
### Changed
- Add bug code to report in fancy-hist.xsl ([#1688](https://github.com/spotbugs/spotbugs/pull/1688))
- Bump Saxon-HE from 10.5 to 10.6 ([#1715](https://github.com/spotbugs/spotbugs/pull/1715))

### Fixed
- Fixed immutable java.lang.Class as being flagged as EI ([#1695](https://github.com/spotbugs/spotbugs/pull/1695))
- Agree verb with plural subject in the description of
`SW_SWING_METHODS_INVOKED_IN_SWING_THREAD` ([#1664](https://github.com/spotbugs/spotbugs/pull/1664))
- Wrong description of the `SE_TRANSIENT_FIELD_OF_NONSERIALIZABLE_CLASS` ([#1664](https://github.com/spotbugs/spotbugs/pull/1664))
- Fixed java.util.Locale as being flagged as EI  ([#1702](https://github.com/spotbugs/spotbugs/pull/1702))
- Fixed reference to java.awt.Cursor which caused it to be flagged as EI ([#1702](https://github.com/spotbugs/spotbugs/pull/1702))
- Treat types with `@com.google.errorprone.annotations.Immutable` as immutable ([#1705](https://github.com/spotbugs/spotbugs/pull/1705))
- Fix annotation check for `jdk.internal.ValueBased` ([#1706](https://github.com/spotbugs/spotbugs/pull/1706))
- `DMI_RANDOM_USED_ONLY_ONCE` false positive ([#1539](https://github.com/spotbugs/spotbugs/issues/1539))
- `NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR` false negative ([#1642](https://github.com/spotbugs/spotbugs/issues/1642))
- Immutable java.util.regex.Pattern as being flagged as EI ([#1695](https://github.com/spotbugs/spotbugs/pull/1738))
- Resource leak in the JrtfsCodeBase ([#1732](https://github.com/spotbugs/spotbugs/pull/1732))

## 4.4.1 - 2021-09-07
### Changed
- Bump gson from 2.8.7 to 2.8.8 ([#1658](https://github.com/spotbugs/spotbugs/pull/1658))
- Lower `ExitCodes` logger to debug level ([#1661](https://github.com/spotbugs/spotbugs/issues/1661))
- Fixed SARIF format to be compatible with Github code scanning API requirements ([#1630](https://github.com/spotbugs/spotbugs/issues/1630))

### Fixed
- Fixed immutable classes in java.net.* as being flagged as EI ([#1653](https://github.com/spotbugs/spotbugs/issues/1653)
- Classes containing only static methods with setter-like names are no longer considered as mutable ([#1601](https://github.com/spotbugs/spotbugs/issues/1601))
- Handle all immutable collections in the Guava library as immutable ([#1601](https://github.com/spotbugs/spotbugs/issues/1601))
- Classes annotated with @Immutable or @jdk.internal.ValueBased are considered as immutable ([#1601](https://github.com/spotbugs/spotbugs/issues/1601))
- All classes in packages java.time and java.math are now correctly handled as immutable ([#1601](https://github.com/spotbugs/spotbugs/issues/1601))

## 4.4.0 - 2021-08-12

### Fixed
- Fixed False positives for RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE ([#600](https://github.com/spotbugs/spotbugs/issues/600) and [#1338](https://github.com/spotbugs/spotbugs/issues/1338))
- Inconsistent bug description on `EQ_COMPARING_CLASS_NAMES` ([#1523](https://github.com/spotbugs/spotbugs/issues/1523))
- Add a declaration of charset encoding in generated reports ([#1623](https://github.com/spotbugs/spotbugs/pull/1623))
- Fixed regression in Bug Info view for Eclipse 2021-03+ ([#1477](https://github.com/spotbugs/spotbugs/issues/1477))

### Added
* New detector `FindBadEndOfStreamCheck` for new bug type `EOS_BAD_END_OF_STREAM_CHECK`. This bug is reported whenever the return value of java.io.FileInputStream.read() or java.io.FileReader.read() is first converted to byte/int and only thereafter checked against -1. (See [SEI CERT rule FIO08-J](https://wiki.sei.cmu.edu/confluence/display/java/FIO08-J.+Distinguish+between+characters+or+bytes+read+from+a+stream+and+-1))

## 4.3.0 - 2021-07-01

### Fixed
- `MS_EXPOSE_REP` and `EI_EXPOSE_REP` are now reported for code returning a reference to a mutable object indirectly (e.g. via a local variable)

### Changed
* Bump ObjectWeb ASM from 9.1 to 9.2 supporting JDK 18 ([#1591](https://github.com/spotbugs/spotbugs/pull/1591))
* Bump Saxon-HE from 10.3 to 10.5 ([#1513](https://github.com/spotbugs/spotbugs/pull/1513))
* Bump gson from 2.8.6 to 2.8.7 ([#1556](https://github.com/spotbugs/spotbugs/pull/1556))
* Function `mutableSignature()` improved and factored out from the `MutableStaticFields` detector

### Added
* New bugs `MS_EXPOSE_BUF`, `EI_EXPOSE_BUF`, `EI_EXPOSE_STATIC_BUF2` and `EI_EXPOSE_BUF2` by the `FindReturnRef` detector to detect cases where buffers or their backing arrays are exposed (see [SEI CERT rule FIO05-J](https://wiki.sei.cmu.edu/confluence/display/java/FIO05-J.+Do+not+expose+buffers+or+their+backing+arrays+methods+to+untrusted+code))
*  `MS_EXPOSE_REP`, `EI_EXPOSE_REP`, `EI_EXPOSE_STATIC_REP2` and `EI_EXPOSE_REP2` now report for shallowly copied arrays (using clone()) of mutable objects

## 4.2.3 - 2021-04-12

### Fixed
- Inconsistency in the description of `DLS_DEAD_LOCAL_INCREMENT_IN_RETURN`, `VO_VOLATILE_INCREMENT` and `QF_QUESTIONABLE_FOR_LOOP` ([#1470](https://github.com/spotbugs/spotbugs/issues/1470))
- Should issue warning for SecureRandom object created and used only once ([#1464](https://github.com/spotbugs/spotbugs/issues/1464))
- False positive OBL_UNSATIFIED_OBLIGATION with try with resources ([#79](https://github.com/spotbugs/spotbugs/issues/79))
- `SA_LOCAL_SELF_COMPUTATION` bug  ([#1472](https://github.com/spotbugs/spotbugs/issues/1472))
- False positive `EQ_UNUSUAL` with record classes ([#1367](https://github.com/spotbugs/spotbugs/issues/1367))

## 4.2.2 - 2021-03-03

### Fixed
* `UWF_NULL_FIELD` doesn't report line number ([#1368](https://github.com/spotbugs/spotbugs/issues/1368))
* UnsupportedOperationException in BugRanker.trimToMaxRank ([#1161](https://github.com/spotbugs/spotbugs/issues/1161))

### Changed
* Bump ASM from 9.0 to 9.1 supporting JDK17
* Bump commons-lang from 3.11 to 3.12.0
* Replace org.json:json:20201115 with com.google.code.gson:gson:2.8.6

## 4.2.1 - 2021-02-04

### Fixed
* Invalid HTML in the description of `LI_LAZY_INIT_UPDATE_STATIC` bug pattern ([#1383](https://github.com/spotbugs/spotbugs/pull/1383))
* NP_NONNULL_PARAM_VIOLATION false-positive in CompletableFuture.completedStage(value) ([#1397](https://github.com/spotbugs/spotbugs/issues/1397))

### Changed
* Bump json from 20200518 to 20201115 ([#1384](https://github.com/spotbugs/spotbugs/pull/1384))

## 4.2.0 - 2020-11-28
### Fixed
* spotbugs reports `VO_VOLATILE_REFERENCE_TO_ARRAY` in synthetic code generated by Eclipse 4.17+ Java compiler ([#1313](https://github.com/spotbugs/spotbugs/issues/1313))
* spotbugs reports `DM_BOXED_PRIMITIVE_FOR_PARSING` for Double and Float (previously only reported for Integer and Long) ([#744](https://github.com/spotbugs/spotbugs/issues/744))
* sarif report not showing correctly the physical and logical location ([#1281](https://github.com/spotbugs/spotbugs/issues/1281))

### Added
* The class search (in the GUI's class name filter) is now case-insensitive and forgives typos (part of ([#749](https://github.com/spotbugs/spotbugs/issues/749)))

### Changed
* Bump Saxon-HE from 10.2 to 10.3

## 4.1.4 - 2020-10-15
### Fixed
* `IllegalArgumentException` during XML report generation ([#1272](https://github.com/spotbugs/spotbugs/issues/1272))
* Error dialog on cancelling SpotBugs job in Eclipse ([#1314](https://github.com/spotbugs/spotbugs/issues/1314))
* IllegalArgumentException in OpcodeStack.constantToInt ([#893](https://github.com/spotbugs/spotbugs/issues/893))
* Typos in description, documentation and so on
* spotbugs reports `VR_UNRESOLVABLE_REFERENCE` and `UPM_UNCALLED_PRIVATE_METHOD` when code is compiled with Java 11 ([#1254](https://github.com/spotbugs/spotbugs/issues/1254))

### Changed
* Bump jaxen from 1.1.6 to 1.2.0 supporting Java 11 compilation ([#1316](https://github.com/spotbugs/spotbugs/issues/1316))
* Bump ASM from 8.0.1 to 9.0 supporting JDK16 (sealed classes)
* Bump Saxon-HE from 10.1 to 10.2
* The dependency from `test-harness` to `spotbugs` is now `testImplementation` ([#1317](https://github.com/spotbugs/spotbugs/pull/1317))
* The dependency from `test-harness-core` to `spotbugs` is now `api` ([#1317](https://github.com/spotbugs/spotbugs/pull/1317))

## 4.1.3 - 2020-09-25
### Fixed
* False positive `RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE` on try-with-resources ([#259](https://github.com/spotbugs/spotbugs/issues/259))
* Misconfiguration which makes ASM not supporting Java 14 ([#1276](https://github.com/spotbugs/spotbugs/issues/1276))
* Resolved fatal exception in html report if BugInstance contains multiple Class elements and use the plain.xsl XSLT stylesheet to generate the HTML ([#1025](https://github.com/spotbugs/spotbugs/issues/1025))

## 4.1.2 - 2020-08-18
### Fixed
* [A meaningless exception data from `SAXBugCollectionHandler`](https://lgtm.com/projects/g/spotbugs/spotbugs/rev/a77ab08634687b7791e902636996ab6184462693)
* Use URI for files instead of converting string to URI each time. Fixes tests on Windows.
* Allow private methods to inherit default annotations from package or class scope. ([#374](https://github.com/spotbugs/spotbugs/issues/374))

### Added
* Implement [issue 390](https://github.com/spotbugs/spotbugs/issues/390) as a detector, `DontAssertInstanceofInTests`, which reports bugs of type `JUA_DONT_ASSERT_INSTANCEOF_IN_TESTS`.

## 4.1.1 - 2020-07-31
### Fixed
* Missing the version of commons-lang3 for Maven ([#1239](https://github.com/spotbugs/spotbugs/issues/1239))

## 4.1.0 - 2020-07-30
### Added
* Support custom bug annotation
* Experimental support for the SARIF 2.1.0 report ([discuss#95](https://github.com/spotbugs/discuss/issues/95))

### Fixed
* Fixed not working detector 'CbeckMustOverrideSuperAnnotation' and renamed to 'OverridingMethodsMustInvokeSuperDetector'

### Changed
* Bump commons-lang3 from 3.10 to 3.11 ([#1231](https://github.com/spotbugs/spotbugs/pull/1231))
* Bump commons-text from 1.8 to 1.9

## 4.0.6 - 2020-06-23
### Fixed
* Use method call instead of reflection to get BCEL frame type ([#1176](https://github.com/spotbugs/spotbugs/issues/1176))

## 4.0.5 - 2020-06-20
### Fixed

* dependency conflict around apache-commons-lang3 ([#1135](https://github.com/spotbugs/spotbugs/issues/1135))
* plain.xsl declares it is a 2.0 stylesheet, but it appears to have issues with a 2.0 processor
* eclipse plugin does not contain `lib/spotbugs.jar`  ([#1158](https://github.com/spotbugs/spotbugs/issues/1158))

### Changed

* Bump up Apache Commons BCEL to the version 6.5.0

## 4.0.4 - 2020-06-09
### Security

* Update dom4j to 2.1.3 to fix security vulnerability. ([#1122](https://github.com/spotbugs/spotbugs/issues/1122))

## 4.0.3 - 2020-05-13

### Fixed

* Avoid changing the SecurityManager when launched as an IntelliJ IDEA plugin.

## 4.0.2 - 2020-04-15

### Fixed

* GUI was using older version of jdom2 compared to spotbugs in general, bumped it to match at 2.1.1
* Numerous places in manifest, jnlp files, and sample analysis xml were indicating older asm that was already upgraded to 7.3.1, fixed
* Added commons-text 1.8 which treats &#955; properly in xml as it is allowed as λ.  Associated test was corrected to use proper junit and &#955; was changed to λ.  The escape only was applicable to html.  Commons-lang original treatment was incorrect.
* Resolved fatal exception in html report if BugInstance contains multiple Class elements ([#1025](https://github.com/spotbugs/spotbugs/issues/1025))

### Changed

* Upgrade ASM to 8.0.1 which supports Java14
* Upgraded junit4 to 4.13
* Upgraded ant to 1.10.7
* Upgraded log4j2 to 2.13.1
* Upgraded from commons-lang2 to commons-lang3 3.10
* Added commons-text 1.8 due to items deprecated in commons-lang3 and moved to this project
* replaced usage of org.xml.sax.helpers.XMLReaderFactory (deprecated since jdk9) with javax.xml.parsers.SAXParserFactory

## 4.0.1 - 2020-03-19

### Fixed

* Resolved Saxon warning ([#1077](https://github.com/spotbugs/spotbugs/issues/1077))
* Unclear message of `SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION` ([#1091](https://github.com/spotbugs/spotbugs/pull/1091))

## 4.0.0 - 2020-02-15

### Fixed

* [Duplicated word in bug descriptions](https://github.com/spotbugs/spotbugs/commit/0d50f0056d7b34e09b472079120bf5ea2abddc45)

## 4.0.0-RC3 - 2020-02-04

This version contains no change, except for the solution for [a deployment problem](https://issues.sonatype.org/browse/MVNCENTRAL-5548).

## 4.0.0-RC2 - 2020-01-29

### Fixed

* Latest 4.0.0 Eclipse plugin is not functional ([#1067](https://github.com/spotbugs/spotbugs/issues/1067))

## 4.0.0-RC1 - 2020-01-17

### Changed

* change the dependency on `jaxen` to `runtime` scope
* change the dependency on `saxon` to `runtime` scope

## 4.0.0-beta5 - 2020-01-14

### Fixed

* Suppress `Error resolving Real SourcePath (only relative source path will be available)` warning. [#1009](https://github.com/spotbugs/spotbugs/issues/1009)

### Changed

* Bump up Apache Commons BCEL to the version 6.4.1
* update ASM to 7.3.1 that supports Java 14 and 15

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

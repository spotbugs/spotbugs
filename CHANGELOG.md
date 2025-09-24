
# Changelog

This is the changelog for SpotBugs. This follows [Keep a Changelog v1.0.0](http://keepachangelog.com/en/1.0.0/).

Currently the versioning policy of this project follows [Semantic Versioning v2.0.0](http://semver.org/spec/v2.0.0.html).

## Unreleased - 2025-??-??
### Fixed
- Fix for missing -adjustPriority parameter in Eclipse preferences ([#3687](https://github.com/spotbugs/spotbugs/issues/3687))
- Documentation of -adjustPriority parameter

### Changed
- Support for fully qualified class names for detectors in -adjustPriority parameter

## 4.9.6 - 2025-09-16
### Fixed
- Fix exception throw when analyzing `jakarta.servlet.http.HttpServletRequest` method calls ([#3711](https://github.com/spotbugs/spotbugs/issues/3711))

## 4.9.5 - 2025-09-14
### Fixed
- Fix for an error when a record method has the `@SuppressFBWarnings` annotation ([#3622](https://github.com/spotbugs/spotbugs/pull/3622))
- Fix `SF_SWITCH_FALLTHROUGH` false positive when continuing a loop ([#3617](https://github.com/spotbugs/spotbugs/issues/3617))
- `CWO_CLOSED_WITHOUT_OPENED` false positive ([#3616](https://github.com/spotbugs/spotbugs/issues/3616))
- `SF_SWITCH_NO_DEFAULT` false positive fix for switch-arrow ([#3645](https://github.com/spotbugs/spotbugs/issues/3645))
- Fix the issue with BCEL logging `Duplicating value: ...` ([#3621](https://github.com/spotbugs/spotbugs/issues/3621))
- Add missing jakarta support for servlets / pre/post destroy ([#3694](https://github.com/spotbugs/spotbugs/pull/3694))

### Added
- Add 'java.nio.file.Path.of' to known types for path traversal checks ([#3699](https://github.com/spotbugs/spotbugs/pull/3699))

### Cleanup
- S1481: Unused local variables should be removed ([#3654](https://github.com/spotbugs/spotbugs/pull/3654))
- Moved test libraries to jakarta namespace including switching off jsr305 where possible for jakarta.annotatoin ([#3695](https://github.com/spotbugs/spotbugs/pull/3695))

## 4.9.4 - 2025-08-07
### Changed
- `AnnotationMatcher` can now ignore bugs if annotation is also applied on methods or fields. Previously only annotations on classes were considered.
- Add relevant CWE ids to bugs and refer the CWEs in the bug messages ([#3354](https://github.com/spotbugs/spotbugs/pull/3354)).
- Replace `LOCAL_VARIABLE_UNKNOWN` with exact method name for `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` ([#3485](https://github.com/spotbugs/spotbugs/pull/3485))

### Fixed
- Widen main method recognition according to [JEP 445](https://openjdk.org/jeps/445). ([#3371](https://github.com/spotbugs/spotbugs/pull/3371))
- Do not report `US_USELESS_SUPPRESSION_ON_*` on methods, fields, parameters, packages or classes with an `*.Generated` annotation with retention >= class ([#3350](https://github.com/spotbugs/spotbugs/issues/3350))([#3409](https://github.com/spotbugs/spotbugs/pull/3409))
- Rewrite some member in `ResourceValueFrame.java` to Enum ([#2061](https://github.com/spotbugs/spotbugs/issues/2061))
- Ignore non-interpreted text when looking for `FS_BAD_DATE_FORMAT_FLAG_COMBO` ([#3387](https://github.com/spotbugs/spotbugs/issues/3387))
- Fix IllegalArgumentException thrown from `FindNoSideEffectMethods` detector ([#3320](https://github.com/spotbugs/spotbugs/issues/3320))
- Do not report `RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT` when part of a Mockito `doAnswer()`, `doCallRealMethod()`, `doNothing()`, `doThrow()` or `doReturn()` call ([#3334](https://github.com/spotbugs/spotbugs/issues/3334))
- Fix `CT_CONSTRUCTOR_THROW` false positive with public and private constructors in specific order of methods ([#3417](https://github.com/spotbugs/spotbugs/issues/3417))
- Fix `AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE`, `AT_NONATOMIC_64BIT_PRIMITIVE` and `AT_STALE_THREAD_WRITE_OF_PRIMITIVE` FP when the relevant code is in private method, which is only called with proper synchronization ([#3428](https://github.com/spotbugs/spotbugs/issues/3428)) 
- Do not report `RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT` when part of a BDDMockito call ([#3441](https://github.com/spotbugs/spotbugs/issues/3441))
- Fix `AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE` when field of a local variable is set. ([#3459](https://github.com/spotbugs/spotbugs/pull/3459))
- Fix `AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE` FP when there was no compound operation ([#3363](https://github.com/spotbugs/spotbugs/issues/3363))
- Fix `NM_FIELD_NAMING_CONVENTION` crash in the TestASM detector ([#3489](https://github.com/spotbugs/spotbugs/pull/3489))
- Do not report `UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR` for fields initialized in JUnit 3/4 `setUp()` method. ([#3169](https://github.com/spotbugs/spotbugs/issues/3169))
- Fix `US_USELESS_SUPPRESSION_ON_FIELD`/`UUF_UNUSED_FIELD` false positive ([#3496](https://github.com/spotbugs/spotbugs/pull/3496))
- Make the osgi manifest of the annotations jar Java 8 compatible  ([#3498](https://github.com/spotbugs/spotbugs/pull/3498)) ([#3500](https://github.com/spotbugs/spotbugs/pull/3500))
- `TextUICommandLine` supports all options encoded in Eclipse preferences file ([#3520](https://github.com/spotbugs/spotbugs/issues/3520))
- Unnecessary suppressions fix for records headers ([#3471](https://github.com/spotbugs/spotbugs/issues/3471))
- Dead store fix when switch case contains loops  ([#3530](https://github.com/spotbugs/spotbugs/issues/3530))  ([#3449](https://github.com/spotbugs/spotbugs/issues/3449))
-  Consider PUTFIELD and PUTSTATIC when looking for assertions with side effects ([#3463](https://github.com/spotbugs/spotbugs/issues/3463))
- Detect cases when equals() unconditionally returns true or false ([#3528](https://github.com/spotbugs/spotbugs/issues/3528))
- Do not report that an Iterator does not throw `NoSuchElementException` when `hasNext()` returns true ([#3501](https://github.com/spotbugs/spotbugs/issues/3501))
- Detect random value cast to int when stored in temporary variable ([#3461](https://github.com/spotbugs/spotbugs/issues/3461))
- Look for interfaces default methods when searching uncalled private methods ([#1988](https://github.com/spotbugs/spotbugs/issues/1988))
- Fixed field self assignment false positive ([#2258](https://github.com/spotbugs/spotbugs/issues/2258))
- Fixed `DMI_INVOKING_TOSTRING_ON_ARRAY` on newer JDK ([#1147](https://github.com/spotbugs/spotbugs/issues/1147))
- Fix `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` false positive with `Objects.requireNonNull` ([#2965](https://github.com/spotbugs/spotbugs/issues/2965)) ([#3573](https://github.com/spotbugs/spotbugs/issues/3573))
- Track inner classes access methods to correctly report the bugs ([#2029](https://github.com/spotbugs/spotbugs/issues/2029))
- `SF_SWITCH_NO_DEFAULT` false positive fix ([#1148](https://github.com/spotbugs/spotbugs/issues/1148)) ([#3572](https://github.com/spotbugs/spotbugs/issues/3572))

### Added
- Added the unnecessary annotation to the `US_USELESS_SUPPRESSION_ON_*` messages ([#3395](https://github.com/spotbugs/spotbugs/issues/3395))
- Multi-threaded code checks can be skipped with `@NotThreadSafe` ([#3390](https://github.com/spotbugs/spotbugs/issues/3390))
- New bug type `CWO_CLOSED_WITHOUT_OPENED` for locks that might be released without even being acquired. (See [SEI CERT rule LCK08-J](https://wiki.sei.cmu.edu/confluence/display/java/LCK08-J.+Ensure+actively+held+locks+are+released+on+exceptional+conditions)) ([#2055](https://github.com/spotbugs/spotbugs/pull/2055))
  - Breaking change: changed values and new items in `ResourceValueFrame`.
- Inline access method for method. ([#3481](https://github.com/spotbugs/spotbugs/issues/3481))
- Added `DMI_MISLEADING_SUBSTRING` for calling `subString(0)` on a StringBuffer/StringBuilder ([#1928](https://github.com/spotbugs/spotbugs/issues/1928))

### Signing
- Signing for Eclipse plugin has been removed at the current time due to signing keys being expired.  The expired key produced a warning during install, the same is true without signing.

## 4.9.3 - 2025-03-14
### Added
- Introduced `UselessSuppressionDetector` to report the useless annotations instead of `NoteSuppressedWarnings` ([#3348](https://github.com/spotbugs/spotbugs/issues/3348))

### Fixed
- Do not report `US_USELESS_SUPPRESSION_ON_METHOD` on synthetic methods ([#3351](https://github.com/spotbugs/spotbugs/issues/3351))

## 4.9.2 - 2025-03-01
### Added
- Reporting useless `@SuppressFBWarnings` annotations ([#641](https://github.com/spotbugs/spotbugs/issues/641))

### Fixed
- Fixed html bug descriptions for AT_STALE_THREAD_WRITE_OF_PRIMITIVE and AT_NONATOMIC_64BIT_PRIMITIVE ([#3303](https://github.com/spotbugs/spotbugs/issues/3303))
- Fixed an `HSM_HIDING_METHOD` false positive when ECJ generates a synthetic method for an enum switch ([#3305](https://github.com/spotbugs/spotbugs/issues/3305))
- Fix `AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD` false negatives, detector depending on method order.
- Fix `THROWS_METHOD_THROWS_CLAUSE_THROWABLE` reported in a method calling `MethodHandle.invokeExact` due to its polymorphic signature ([#3309](https://github.com/spotbugs/spotbugs/issues/3309))
- Fix `AT_STALE_THREAD_WRITE_OF_PRIMITIVE` false positive in inner class ([#3310](https://github.com/spotbugs/spotbugs/issues/3310)).
- Fix `AT_STALE_THREAD_WRITE_OF_PRIMITIVE` false positive for ECJ compiled enum switches ([#3316](https://github.com/spotbugs/spotbugs/issues/3316))
- Fix `RC_REF_COMPARISON` false positive with Lombok With annotation ([#3319](https://github.com/spotbugs/spotbugs/pull/3319))
- Avoid calling File.getCanonicalPath twice to improve performance ([#3325](https://github.com/spotbugs/spotbugs/pull/3325))
- Fix `MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR` and `MC_OVERRIDABLE_METHOD_CALL_IN_CLONE` false positive when the overridable method is outside the class ([#3328](https://github.com/spotbugs/spotbugs/issues/3328)).
- Fix NullPointerException thrown from `ThrowingExceptions` detector ([#3337](https://github.com/spotbugs/spotbugs/pull/3337)).

### Removed
- Removed the `TLW_TWO_LOCK_NOTIFY`, `LI_LAZY_INIT_INSTANCE`, `BRSA_BAD_RESULTSET_ACCESS`, `BC_NULL_INSTANCEOF`, `NP_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR` and `RCN_REDUNDANT_CHECKED_NULL_COMPARISON` deprecated bug patterns.

## 4.9.1 - 2025-02-02
### Added
- New detector `SharedVariableAtomicityDetector` for new bug types `AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE`, `AT_NONATOMIC_64BIT_PRIMITIVE` and `AT_STALE_THREAD_WRITE_OF_PRIMITIVE` (See SEI CERT rules [VNA00-J](https://wiki.sei.cmu.edu/confluence/display/java/VNA00-J.+Ensure+visibility+when+accessing+shared+primitive+variables), [VNA02-J](https://wiki.sei.cmu.edu/confluence/display/java/VNA02-J.+Ensure+that+compound+operations+on+shared+variables+are+atomic) and [VNA05-J](https://wiki.sei.cmu.edu/confluence/display/java/VNA05-J.+Ensure+atomicity+when+reading+and+writing+64-bit+values)).
- New detector `FindHiddenMethod` for bug type `HSM_HIDING_METHOD`. This bug is reported whenever a subclass method hides the static method of super class. (See [SEI CERT MET07-J](https://wiki.sei.cmu.edu/confluence/display/java/MET07-J.+Never+declare+a+class+method+that+hides+a+method+declared+in+a+superclass+or+superinterface)).

### Fixed
- Fixed the parsing of generics methods in `ThrowingExceptions` ([#3267](https://github.com/spotbugs/spotbugs/issues/3267))
- Accept the 1st parameter of `java.util.concurrent.CompletableFuture`'s `completeOnTimeout()`, `getNow()` and `obtrudeValue()` functions as nullable ([#1001](https://github.com/spotbugs/spotbugs/issues/1001)).
- Fixed the analysis error when `FindReturnRef` was checking instructions corresponding to a CFG branch that was optimized away ([#3266](https://github.com/spotbugs/spotbugs/issues/3266))
- Added execute file permission to files in the distribution archive ([#3274](https://github.com/spotbugs/spotbugs/issues/3274))
- Fixed a stack overflow in `MultipleInstantiationsOfSingletons` when a singleton initializer makes recursive calls ([#3280](https://github.com/spotbugs/spotbugs/issues/3280))
- Fixed NPE in `FindReturnRef` on inner class fields ([#3283](https://github.com/spotbugs/spotbugs/issues/3283))
- Fixed NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE false positive when add edu.umd.cs.findbugs.annotations.Nullable ([#3243](https://github.com/spotbugs/spotbugs/issues/3243))

## 4.9.0 - 2025-01-15
### Added
- Updated the `SuppressFBWarnings` annotation to support finer grained bug suppressions ([#3102](https://github.com/spotbugs/spotbugs/pull/3102))
- SimpleDateFormat, DateTimeFormatter, FastDateFormat string check for bad combinations of flag formatting ([#637](https://github.com/spotbugs/spotbugs/issues/637))
- New detector `ResourceInMultipleThreadsDetector` and introduced new bug type:
  - `AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD` is reported in case of unsafe resource access in multiple threads.

### Fixed
- Do not consider Records as Singletons ([#2981](https://github.com/spotbugs/spotbugs/issues/2981))
- Keep a maximum of 10000 cached analysis entries for plugin's analysis engines ([#3025](https://github.com/spotbugs/spotbugs/pull/3025))
- Only report `MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT` when calling own methods ([#2957](https://github.com/spotbugs/spotbugs/issues/2957))
- Check the actual caught exceptions (instead of their common type) when analyzing multi-catch blocks ([#2968](https://github.com/spotbugs/spotbugs/issues/2968))
- System property `findbugs.refcomp.reportAll` is now being used. For some new conditions, it will emit an experimental warning ([#2988](https://github.com/spotbugs/spotbugs/pull/2988))
- `-version` flag prints the version to the standard output ([#2797](https://github.com/spotbugs/spotbugs/issues/2797))
- Revert the changes from ([#2894](https://github.com/spotbugs/spotbugs/pull/2894)) to get HTML stylesheets to work again ([#2969](https://github.com/spotbugs/spotbugs/issues/2969))
- Fix FP `SING_SINGLETON_GETTER_NOT_SYNCHRONIZED` report when the synchronization is in a called method ([#3045](https://github.com/spotbugs/spotbugs/issues/3045))
- Let `BetterCFGBuilder2.isPEI` handle `dup2` bytecode used by Spring AOT ([#3059](https://github.com/spotbugs/spotbugs/issues/3059))
- Detect failure to close RocksDB's ReadOptions ([#3069](https://github.com/spotbugs/spotbugs/issues/3069))
- Fix FP `EI_EXPOSE_REP` when there are multiple immutable assignments ([#3023](https://github.com/spotbugs/spotbugs/issues/3023))
- Fixed false positive `NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR` for Kotlin, handle Kotlin's `Intrinsics.checkNotNullParameter()` ([#3094](https://github.com/spotbugs/spotbugs/issues/3094))
- Fixed some CWE mappings ([#3124](https://github.com/spotbugs/spotbugs/pull/3124))
- Recognize some classes as immutable, fixing EI_EXPOSE and MS_EXPOSE FPs ([#3137](https://github.com/spotbugs/spotbugs/pull/3137))
- Do not report UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR for fields initialized in method annotated with TestNG's @BeforeClass. ([#3152](https://github.com/spotbugs/spotbugs/issues/3152))
- Fixed detector `FindReturnRef` not finding references exposed from nested and inner classes ([#2042](https://github.com/spotbugs/spotbugs/issues/2042))
- Fix call graph, include non-parametric void methods ([#3160](https://github.com/spotbugs/spotbugs/pull/3160))
- Fix multiple reporting of identical bugs messing up statistics ([#3185](https://github.com/spotbugs/spotbugs/issues/3185))
- Added missing comma between line number and confidence when describing matching and mismatching bugs for tests ([#3187](https://github.com/spotbugs/spotbugs/pull/3187))
- Fixed method matchers with array types ([#3203](https://github.com/spotbugs/spotbugs/issues/3203))
- Fix SARIF report's message property in Exception to meet the standard ([#3197](https://github.com/spotbugs/spotbugs/issues/3197))
- Fixed `FI_FINALIZER_NULLS_FIELDS` FPs for functions called finalize() but not with the correct signature. ([#3207](https://github.com/spotbugs/spotbugs/issues/3207))
- Fixed an error in the detection of bridge methods causing analysis crashes ([#3208](https://github.com/spotbugs/spotbugs/issues/3208))
- Fixed detector `ThrowingExceptions` by removing false positive reports, such as synthetic methods (lambdas), methods which inherited their exception specifications and methods which call throwing methods ([#2040](https://github.com/spotbugs/spotbugs/issues/2040))
- Do not report `DP_DO_INSIDE_DO_PRIVILEGED`, `DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED` and `USC_POTENTIAL_SECURITY_CHECK_BASED_ON_UNTRUSTED_SOURCE` in code targeting Java 17 and above, since it advises the usage of deprecated method ([#1515](https://github.com/spotbugs/spotbugs/issues/1515)).
- Fixed a `RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT` false positive for a builder delegating to another builder ([#3235](https://github.com/spotbugs/spotbugs/issues/3235))

### Cleanup
- Cleanup thread issue and regex issue in test-harness ([#3130](https://github.com/spotbugs/spotbugs/issues/3130))
- Remove extra blank lines and remove public from interface objects as inherently already public ([#3131](https://github.com/spotbugs/spotbugs/issues/3131))
- Fix order of modifiers on properties/methods and ensure correct location in file ([#3132](https://github.com/spotbugs/spotbugs/issues/3132), [#3177](https://github.com/spotbugs/spotbugs/pull/3177))
- Return objects directly instead of creating more garbage collection by defining them ([#3133](https://github.com/spotbugs/spotbugs/pull/3133), [#3175](https://github.com/spotbugs/spotbugs/pull/3175))
- Restrict the constructor of abstract classes visibility to protected ([#3178](https://github.com/spotbugs/spotbugs/pull/3178)) 
- Cleanup double initialization and fix comments referring to findbugs instead of spotbugs([#3134](https://github.com/spotbugs/spotbugs/issues/3134))
- Use diamond operator in constructor calls of Collections ([#3176](https://github.com/spotbugs/spotbugs/pull/3176))
- Use `Collection.isEmpty()` or `String.isEmpty()` to test for emptiness ([#3180](https://github.com/spotbugs/spotbugs/pull/3180), [#3219](https://github.com/spotbugs/spotbugs/pull/3219))
- Use method references instead of lambdas where possible ([#3179](https://github.com/spotbugs/spotbugs/pull/3179))
- Move default clauses to the end of switches ([#3222](https://github.com/spotbugs/spotbugs/pull/3222))
- Remove unnecessary throws declarations ([#3220](https://github.com/spotbugs/spotbugs/pull/3220))
- Use `Boolean.parseBoolean()` for string-to-boolean conversion. ([#3217](https://github.com/spotbugs/spotbugs/pull/3217))
- Rename shadowing fields ([#3221](https://github.com/spotbugs/spotbugs/pull/3221))
- Combine catch blocks with the same body ([#3223](https://github.com/spotbugs/spotbugs/pull/3223))
- Merge conditions of nested ifs ([#3231](https://github.com/spotbugs/spotbugs/pull/3231))
- Use non deprecated 'getDottedClassName' instead of 'toDottedClassName'([#3251](https://github.com/spotbugs/spotbugs/pull/3251))
- Use try with resources where possible ([#3253](https://github.com/spotbugs/spotbugs/pull/3253))

### Changed
- Bump up Java version to 11

## 4.8.6 - 2024-06-17
### Fixed
- Do not report BC_UNCONFIRMED_CAST for Java 21's type switches when the switch instruction is TABLESWITCH ([#2782](https://github.com/spotbugs/spotbugs/issues/2782))
- Do not throw exception when inspecting empty switch statements ([#2995](https://github.com/spotbugs/spotbugs/issues/2995))
- Adjust priority since relaxed mode reports even `IGNORED_PRIORITY` ([#2994](https://github.com/spotbugs/spotbugs/issues/2994))
- Fix duplicated log4j2 jar in distribution ([#3001](https://github.com/spotbugs/spotbugs/issues/3001))

## 4.8.5 - 2024-05-03
### Fixed
- Fix FP `SING_SINGLETON_GETTER_NOT_SYNCHRONIZED` with eager instances ([#2932](https://github.com/spotbugs/spotbugs/issues/2932))
- Fix FPs when looking for multiple initialization of Singletons ([#2934](https://github.com/spotbugs/spotbugs/issues/2934))
- Do not report DLS_DEAD_LOCAL_STORE for Java 21's type switches when switch instruction is TABLESWITCH([#2736](https://github.com/spotbugs/spotbugs/issues/2736))
- Fix FP `SE_BAD_FIELD` for record fields ([#2935](https://github.com/spotbugs/spotbugs/issues/2935))

## 4.8.4 - 2024-04-07
### Fixed
- Fix FP in SE_PREVENT_EXT_OBJ_OVERWRITE when the if statement checking for null value, checking multiple variables or the method exiting in the if branch with an exception. ([#2750](https://github.com/spotbugs/spotbugs/issues/2750))
- Fix possible null value in taxonomies of SARIF output ([#2744](https://github.com/spotbugs/spotbugs/issues/2744))
- Fix `executionSuccessful` flag in SARIF report being set to false when bugs were found ([#2116](https://github.com/spotbugs/spotbugs/issues/2116))
- Move information contained in the SARIF property `exitSignalName` to `exitCodeDescription` ([#2739](https://github.com/spotbugs/spotbugs/issues/2739))
- Do not report SE_NO_SERIALVERSIONID or other serialization issues for records ([#2793](https://github.com/spotbugs/spotbugs/issues/2793))
- Added support for CONSTANT_Dynamic ([#2759](https://github.com/spotbugs/spotbugs/issues/2759))
- Ignore generic variable types when looking for BC_UNCONFIRMED_CAST_OF_RETURN_VALUE ([#1219](https://github.com/spotbugs/spotbugs/issues/1219))
- Do not report BC_UNCONFIRMED_CAST for Java 21's type switches ([#2813](https://github.com/spotbugs/spotbugs/pull/2813))
- Remove AppleExtension library (note: menus slightly changed) ([#2823](https://github.com/spotbugs/spotbugs/pull/2823))
- Fix false positive NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE even if Objects.requireNonNull is used. ([#651](https://github.com/spotbugs/spotbugs/issues/651), [#456](https://github.com/spotbugs/spotbugs/issues/456))
- Fixed error preventing SpotBugs from reporting FE_FLOATING_POINT_EQUALITY ([#2843](https://github.com/spotbugs/spotbugs/pull/2843))
- Fixed NP_LOAD_OF_KNOWN_NULL_VALUE and RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE false positives in try-with-resources generated finally blocks ([#2844](https://github.com/spotbugs/spotbugs/pull/2844))
- Do not report DLS_DEAD_LOCAL_STORE for Java 21's type switches ([#2828](https://github.com/spotbugs/spotbugs/pull/2828))
- Update UnreadFields detector to ignore warnings for fields with certain annotations ([#574](https://github.com/spotbugs/spotbugs/issues/574))
- Do not report UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR for fields initialized in method annotated with @PostConstruct, @BeforeEach, etc. ([#2872](https://github.com/spotbugs/spotbugs/pull/2872) [#2870](https://github.com/spotbugs/spotbugs/issues/2870) [#453](https://github.com/spotbugs/spotbugs/issues/453))
- Do not report DLS_DEAD_LOCAL_STORE for Hibernate bytecode enhancements ([#2865](https://github.com/spotbugs/spotbugs/pull/2865))
- Fixed NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE false positives due to source code formatting ([#2874](https://github.com/spotbugs/spotbugs/pull/2874))
- Added more nullability annotations in TypeQualifierResolver ([#2558](https://github.com/spotbugs/spotbugs/issues/2558) [#2694](https://github.com/spotbugs/spotbugs/pull/2694))
- Improved the bug description for VA_FORMAT_STRING_USES_NEWLINE when using text blocks, check the usage of String.formatted() ([#2881](https://github.com/spotbugs/spotbugs/pull/2881))
- Fixed crash in ValueRangeAnalysisFactory when looking for redundant conditions used in assertions ([#2887](https://github.com/spotbugs/spotbugs/pull/2887))
- Revert again commons-text from 1.11.0 to 1.10.0 to resolve a version conflict ([#2686](https://github.com/spotbugs/spotbugs/issues/2686))
- Fixed false positive MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR when referencing but not calling an overridable method ([#2837](https://github.com/spotbugs/spotbugs/pull/2837))
- Update the filter XSD namespace and location for the upcoming 4.8.4 release ([#2909](https://github.com/spotbugs/spotbugs/issues/2909))

### Added
- New detector `MultipleInstantiationsOfSingletons` and introduced new bug types:
  - `SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR` is reported in case of a non-private constructor,
  - `SING_SINGLETON_IMPLEMENTS_CLONEABLE` is reported in case of a class directly implementing the `Cloneable` interface,
  - `SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE` is reported when a class indirectly implements the `Cloneable` interface,
  - `SING_SINGLETON_IMPLEMENTS_CLONE_METHOD` is reported when a class does not implement the `Cloneable` interface, but has a `clone()` method,
  - `SING_SINGLETON_IMPLEMENTS_SERIALIZABLE` is reported when a class directly or indirectly implements the `Serializable` interface and
  - `SING_SINGLETON_GETTER_NOT_SYNCHRONIZED` is reported when the instance-getter method of the singleton class is not synchronized.
    (See [SEI CERT MSC07-J](https://wiki.sei.cmu.edu/confluence/display/java/MSC07-J.+Prevent+multiple+instantiations+of+singleton+objects))
- Extend `FindOverridableMethodCall` detector with new bug type: `MC_OVERRIDABLE_METHOD_CALL_IN_READ_OBJECT`. It's reported when an overridable method is called from `readObject()`, according to SEI CERT rule [SER09-J. Do not invoke overridable methods from the readObject() method](https://wiki.sei.cmu.edu/confluence/display/java/SER09-J.+Do+not+invoke+overridable+methods+from+the+readObject%28%29+method).

### Changed
- Minor cleanup in connection with slashed and dotted names ([#2805](https://github.com/spotbugs/spotbugs/pull/2805))

### Build
- Fix sonar coverage for project ([#2796](https://github.com/spotbugs/spotbugs/issues/2796))
- Upgraded the build to compile bug samples using Java 21 language features ([#2813](https://github.com/spotbugs/spotbugs/pull/2813))
- Add 'configurations.checkstyle resolution starategy' to control bug in gradle on exclusions not being excluded properly as seen in checkstyle usage.  See https://github.com/checkstyle/checkstyle/issues/14211 for more information. ([#2798](https://github.com/spotbugs/spotbugs/issues/2798))
- Allow our builds to work with jdk 11 with drop back on Eclipse to 4.24 and spring to 5.3.31.  ([#2604](https://github.com/spotbugs/spotbugs/pull/2604/))

## 4.8.3 - 2023-12-12
### Fixed
- Fix FP in CT_CONSTRUCTOR_THROW when the finalizer does not run, since the exception is thrown before java.lang.Object's constructor exits for checked exceptions ([#2710](https://github.com/spotbugs/spotbugs/issues/2710))
- Applied changes for bcel 6.8.0 with adjustments to constant pool ([#2756](https://github.com/spotbugs/spotbugs/pull/2756))
    - More information bcel changes can be found on ([#2757](https://github.com/spotbugs/spotbugs/pull/2757))
- Fix FN in CT_CONSTRUCTOR_THROW when the return value of the called method is not void or primitive type.
- Fix FP in CT_CONSTRUCTOR_THROW when exception throwing lambda is created, but not called in constructor ([#2695](https://github.com/spotbugs/spotbugs/issues/2695)) 

### Changed
- Improved Matcher checks for empty strings ([#2755](https://github.com/spotbugs/spotbugs/pull/2755))
- Allow 'onlyAnalyze' option to specify negative matches, such that this facility can be used to prevent a subset of classes to be excluded from analysis ([#2754](https://github.com/spotbugs/spotbugs/pull/2754))
- Strictly require logback 1.2.13 due to CVE-2023-6481 and CVE-23-6378 ([#2760](https://github.com/spotbugs/spotbugs/pull/2760))
- Prefer log4j2 at 2.22.0 and logback at 1.4.14 ([#2760](https://github.com/spotbugs/spotbugs/pull/2760))

## 4.8.2 - 2023-11-28

### Fixed
- Fixed false positive UPM_UNCALLED_PRIVATE_METHOD for method used in JUnit's MethodSource ([#2379](https://github.com/spotbugs/spotbugs/issues/2379))
- Use java.nio to load filter files ([#2684](https://github.com/spotbugs/spotbugs/pull/2684))
- Eclipse: Do not export javax.annotation packages ([#2699](https://github.com/spotbugs/spotbugs/pull/2699))
- Fixed not thread safe FindOverridableMethodCall detector ([#2701](https://github.com/spotbugs/spotbugs/issues/2701))
- Fix the weird messages of PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS bugs. ([#2646](https://github.com/spotbugs/spotbugs/issues/2646))
- Revert commons-text from 1.11.0 to 1.10.0 to resolve a version conflict ([#2686](https://github.com/spotbugs/spotbugs/issues/2686))
- Fix FP in CT_CONSTRUCTOR_THROW when the finalizer does not run, since the exception is thrown before java.lang.Object's constructor exits ([#2710](https://github.com/spotbugs/spotbugs/issues/2710))

### Added
- New detector finding `System.getenv()` calls, where the corresponding Java property could be used (See [ENV02-J](https://wiki.sei.cmu.edu/confluence/display/java/ENV02-J.+Do+not+trust+the+values+of+environment+variables)).

### Build
- Run build using jdk 17 and 21 without usage of toolchains so we do not defeat the purpose of building on both. ([#2722](https://github.com/spotbugs/spotbugs/pull/2722))

## 4.8.1 - 2023-11-06

### Fixed
- Fixed schema location for findbugsfilter.xsd ([#1416](https://github.com/spotbugs/spotbugs/issues/1416))
- Fixed missing null checks ([#2629](https://github.com/spotbugs/spotbugs/issues/2629))
- Disabled DontReusePublicIdentifiers due to the high false positives rate ([#2627](https://github.com/spotbugs/spotbugs/issues/2627))
- Removed signature of methods using UTF-8 in DefaultEncodingDetector ([#2634](https://github.com/spotbugs/spotbugs/issues/2634))
- Fix exception escapes when calling functions of JUnit Assert or Assertions ([#2640](https://github.com/spotbugs/spotbugs/issues/2640))
- Fixed an error in the SARIF export when a bug annotation is missing ([#2632](https://github.com/spotbugs/spotbugs/issues/2632))
- Fixed false positive RV_EXCEPTION_NOT_THROWN when asserting to exception throws ([#2628](https://github.com/spotbugs/spotbugs/issues/2628))
- Fix false positive CT_CONSTRUCTOR_THROW when supertype has final finalize ([#2665](https://github.com/spotbugs/spotbugs/issues/2665))
- Lowered the priority of `PA_PUBLIC_MUTABLE_OBJECT_ATTRIBUTE` bug ([#2652](https://github.com/spotbugs/spotbugs/issues/2652))
- Eclipse: fixed startup overhead (on computing classpath) for PDE projects ([#2671](https://github.com/spotbugs/spotbugs/pull/2671))

### Build
- Fix deprecated GHA on '::set-output' by using GITHUB_OUTPUT ([#2651](https://github.com/spotbugs/spotbugs/pull/2651))

## 4.8.0 - 2023-10-11

### Changed
- Bump up Apache Commons BCEL to the version 6.6.1 ([#2223](https://github.com/spotbugs/spotbugs/pull/2223))
- Bump up slf4j-api to 2.0.3 ([#2220](https://github.com/spotbugs/spotbugs/pull/2220))
- Bump up gson to 2.10 ([#2235](https://github.com/spotbugs/spotbugs/pull/2235))
- Allowed for large command line through writing arguments to file (UnionResults/UnionBugs2)
- Use com.github.stephenc.jcip for jcip-annotations fixing ([#887](https://github.com/spotbugs/spotbugs/issues/887))
- Bump ObjectWeb ASM from 9.4 to 9.6, supporting JDK 21 ([#2578](https://github.com/spotbugs/spotbugs/pull/2578))

### Fixed
- Fixed missing classes not in report if using IErrorLogger.reportMissingClass(ClassDescriptor) ([#219](https://github.com/spotbugs/spotbugs/issues/219))
- Stop exposing junit-bom to consumers ([#2255](https://github.com/spotbugs/spotbugs/pull/2255))
- Fixed AbstractBugReporter emits wrong non-sensical debug output during filtering ([#184](https://github.com/spotbugs/spotbugs/issues/184))
- Added support for jakarta namespace ([#2289](https://github.com/spotbugs/spotbugs/pull/2289))
- Report a low priority bug for an unread field in reflective classes ([#2325](https://github.com/spotbugs/spotbugs/issues/2325))
- Fixed "Unhandled event loop exception" opening Bug Filter Configuration dialog in Eclipse ([#2327](https://github.com/spotbugs/spotbugs/issues/2327))
- Fixed detector `RandomOnceSubDetector` to not report when `doubles`, `ints`, or `longs` are called on a new `Random` or `SecureRandom` ([#2370](https://github.com/spotbugs/spotbugs/issues/2325))
- Fixed detector `TestASM` throwing error during analysis, because it doesn't note that it reports bugs.
- Eclipse annotation classpath initializer is hard-coded to jsr305 version 3.0.1, fix to 3.0.2 per #2470
- Fixed annotation on generic or array incorrectly considered for the nullability of a method parameter or return type ([#2502](https://github.com/spotbugs/spotbugs/issues/2502))
- Added support for CONSTANT_Dynamic in constant class pool ([#2506](https://github.com/spotbugs/spotbugs/issues/2506))
- Recognise enums and records as immutable ([#2356](https://github.com/spotbugs/spotbugs/issues/2356))
- Added detections of reliance on default encoding in java.nio.file.Files ([#2114](https://github.com/spotbugs/spotbugs/issues/2114))
- Fixed a regression in the Value Number Analysis ([#2465](https://github.com/spotbugs/spotbugs/issues/2465))
- Fix XML Output incorrectly escaped in Eclipse Bug Info view ([#2520](https://github.com/spotbugs/spotbugs/pull/2520))
- Updated the MS_EXPOSE_REP description to mention mutable objects, not just arrays ([#1669](https://github.com/spotbugs/spotbugs/issues/1669))
- Described Configuration option frc.suspicious for bug RC_REF_COMPARISON in bug description ([#2297](https://github.com/spotbugs/spotbugs/issues/2297))
- Fixed FindHEMismatch not reporting HE_SIGNATURE_DECLARES_HASHING_OF_UNHASHABLE_CLASS for some classes ([#2402](https://github.com/spotbugs/spotbugs/issues/2402))
- Added execute file permission to files in the distribution zip  ([#2540](https://github.com/spotbugs/spotbugs/issues/2540))
- Do not report RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT when part of a Mockito.verify() call check ([#872](https://github.com/spotbugs/spotbugs/issues/872))
- Do not report SIC_INNER_SHOULD_BE_STATIC for classes annotated with JUnit Nested ([#560](https://github.com/spotbugs/spotbugs/issues/560))
- Detect created, but not-thrown exceptions, which are created by not the constructor ([#2547](https://github.com/spotbugs/spotbugs/issues/2547))
- Fixed eclipse plugin Effort.values pass to effortViewer as required cast to varargs ([#2579](https://github.com/spotbugs/spotbugs/pull/2579))

### Added
- New simple name-based AnnotationMatcher for exclude files (now bug annotations store the class java annotations in an attribute called `classAnnotationNames`). For example, use like <Match><Annotation name="org.immutables.value.Generated" /></Match> in an excludeFilter.xml to ignore classes generated by the Immutable framework. This ignores all class, method or field bugs in classes with that annotation.
- Added the Common Weakness Enumeration (CWE) taxonomy to the Static Analysis Results Interchange Format (SARIF) report. The short and long description for the CWEs are retrived from a JSON file which is a slimmed down version of the official comprehensive CWE XML from MITRE. The JSON contains information about all CWEs. ([#2410](https://github.com/spotbugs/spotbugs/pull/2410)).
- New detector `FindAssertionsWithSideEffects` detecting bug `ASSERTION_WITH_SIDE_EFFECT` and `ASSERTION_WITH_SIDE_EFFECT_METHOD` in case of assertions which may have side effects (See [EXP06-J. Expressions used in assertions must not produce side effects](https://wiki.sei.cmu.edu/confluence/display/java/EXP06-J.+Expressions+used+in+assertions+must+not+produce+side+effects))
- New rule set `PA_PUBLIC_PRIMITIVE_ATTRIBUTE`, `PA_PUBLIC_ARRAY_ATTRIBUTE` and `PA_PUBLIC_MUTABLE_OBJECT_ATTRIBUTE` to warn for public attributes which are written by the methods of the class. This rule is loosely based on the SEI CERT rule *OBJ01-J Limit accessibility of fields*. ([#OBJ01-J](https://wiki.sei.cmu.edu/confluence/display/java/OBJ01-J.+Limit+accessibility+of+fields))
- Extend `SerializableIdiom` detector with new bug type: `SE_PREVENT_EXT_OBJ_OVERWRITE`. It's reported in case of the `readExternal()` method allows any caller to reset any value of an object
- New Detector `FindVulnerableSecurityCheckMethods` for new bug type `VSC_VULNERABLE_SECURITY_CHECK_METHODS`. This bug is reported whenever a non-final and non-private method of a non-final class performs a security check using the `java.lang.SecurityManager`. (See [SEI CERT MET03-J] (https://wiki.sei.cmu.edu/confluence/display/java/MET03-J.+Methods+that+perform+a+security+check+must+be+declared+private+or+final))
- New function added to detector `SynchronizationOnSharedBuiltinConstant`to detect `DL_SYNCHRONIZATION_ON_INTERNED_STRING` ([#2266](https://github.com/spotbugs/spotbugs/pull/2266))
- Make TypeQualifierResolver recognize org.apache.avro.reflect.Nullable ([#2066](https://github.com/spotbugs/spotbugs/pull/2066))
- New detector `FindArgumentAssertions` detecting bug `ASSERTION_OF_ARGUMENTS` in case of validation of arguments of public functions using assertions (See [MET01-J. Never use assertions to validate method arguments](https://wiki.sei.cmu.edu/confluence/display/java/MET01-J.+Never+use+assertions+to+validate+method+arguments))
- Add new detector `CT_CONSTRUCTOR_THROW` for detecting constructors that throw exceptions.
- New detector `DontReusePublicIdentifiers` for new bug type `PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS`. This bug is reported whenever a new class, interface, field, method or variable is created reusing an identifier from the _Java Standard Library_ . (See [SEI CERT rule DCL01-J](https://wiki.sei.cmu.edu/confluence/display/java/DCL01-J.+Do+not+reuse+public+identifiers+from+the+Java+Standard+Library))

### Security
- Disable access to external entities when processing XML ([#2217](https://github.com/spotbugs/spotbugs/pull/2217))

### Build
- Bump Eclipse from 4.6.3 to 4.14 ([#2314](https://github.com/spotbugs/spotbugs/pull/2314))
- Use jakarta annotation 1.3.5 instead of legacy javax annotation 1.3.2 ([#2315](https://github.com/spotbugs/spotbugs/pull/2315))
- Change hamcrest-all to hamcrest-core as that is what was actually used and then update to 2.2 ([#2316](https://github.com/spotbugs/spotbugs/pull/2316))
- Only run release action on 'spotbugs' and use Eclipse 4.14 ([#2317](https://github.com/spotbugs/spotbugs/pull/2317))
- Prefer log4j2 2.20.0 ([#2480](https://github.com/spotbugs/spotbugs/pull/2480))
- Prefer logback 1.4.8 ([#2480](https://github.com/spotbugs/spotbugs/pull/2480))
- Prefer logback 1.4.11 ([#2580](https://github.com/spotbugs/spotbugs/pull/2580))
- Switch junit 4 for junit 5 vintage engine ([#2483](https://github.com/spotbugs/spotbugs/pull/2483))
- LineEndings and Spotless ([#2343](https://github.com/spotbugs/spotbugs/pull/2343))
    - Cleanup gitattributes switching text to auto.  For developers using windows, run 'git add . --renormalize' and see https://docs.github.com/en/get-started/getting-started-with-git/configuring-git-to-handle-line-endings if needed.
    - Rework spotless setup from plugin to build file plugin matching that of gradle plugin and thus allowing spotless to be updated to 6.22.0
    - Remove customized line endings for spotless so it uses git attributes as suggested by spotless
    - Add trimTrailingWhitespace for spotless
    - Fix deprecated usage of eclipse version from 4.13.0 to 4.13 per spotless requirements
- Bump spotbugs gradle plugin to 6.0.0-beta.3 demonstrating breaking changes for 6.0.0 in gradle/java.gradle build file ([#2582](https://github.com/spotbugs/spotbugs/pull/2582))
- Delete checked in j2ee jar and instead use servlet/ejb apis from jakarta (javax standard) ([#2585](https://github.com/spotbugs/spotbugs/pull/2585))
- Bump Eclipse from 4.14 to 4.29 (latest) ([#2589](https://github.com/spotbugs/spotbugs/pull/2589))
- Cleanup hamcrest imports / used library ([#2600](https://github.com/spotbugs/spotbugs/pull/2600))
- Migrate entirely to junit 5 ([#2605](https://github.com/spotbugs/spotbugs/pull/2605))
    - Some parts of codebase were junit 3
    - Delete the SpotbugsRule
    - Replace custom java determination on build with Junit 5 usage
    - Various 'public' methods in tests fixed to 'private'
    - Junit 5 styling applied throughout
    - Add missing code to the SpotBugsRunner and now use the Extension as replacement of SpotbugsRule

## 4.7.3 - 2022-10-15
### Fixed
- Fixed detector `DontUseFloatsAsLoopCounters` to prevent false positives. ([#2126](https://github.com/spotbugs/spotbugs/issues/2126))
- Fixed regression in `4.7.2` caused by ([#2141](https://github.com/spotbugs/spotbugs/pull/2141))
- improve compatibility with later version of jdk (>= 13). ([#2188](https://github.com/spotbugs/spotbugs/issues/2188))
- Fixed detector `UncallableMethodOfAnonymousClass` to not report unused methods of method-local enumerations and records ([#2120](https://github.com/spotbugs/spotbugs/issues/2120))
- Fixed detector `FindSqlInjection` to detect bug `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE SQL` with high priority in case of unsafe appends also in Java 11 and above ([#2183](https://github.com/spotbugs/spotbugs/issues/2183))
- Fixed detector `StringConcatenation` to detect bug `SBSC_USE_STRINGBUFFER_CONCATENATION` also in Java 11 and above ([#2182](https://github.com/spotbugs/spotbugs/issues/2182))
- Fixed `OpcodeStackDetector` to handle propagation of taints properly in case of string concatenation in Java 9 and above ([#2195](https://github.com/spotbugs/spotbugs/issues/2195))
- Bump up log4j2 binding to `2.19.0`
- Bump ObjectWeb ASM from 9.3 to 9.4 supporting JDK 20 ([#2200](https://github.com/spotbugs/spotbugs/pull/2200))
- Bump up commons-text to 1.10.0 ([#2197](https://github.com/spotbugs/spotbugs/pull/2197))
- Fixed debug detector `ViewCFG` to generate file names that are also valid on Windows ([#2209](https://github.com/spotbugs/spotbugs/issues/2209))

## 4.7.2 - 2022-09-02
### Fixed
- Bumped gson from 2.9.0 to 2.9.1 ([#2136](https://github.com/spotbugs/spotbugs/pull/2136))
- Bump up SLF4J API to `2.0.0`
- Bump up logback to `1.4.0`
- Bump up log4j2 binding to `2.18.0`
- Bump up Saxon-HE to `11.4` ([#2160](https://github.com/spotbugs/spotbugs/pull/2160))
- Fixed InvalidInputException in Eclipse while bug reporting ([#2134](https://github.com/spotbugs/spotbugs/issues/2134))
- Bug `SA_FIELD_SELF_ASSIGNMENT` is now reported from nested classes as well ([#2142](https://github.com/spotbugs/spotbugs/issues/2142))
- Avoid warning on use of security manager on Java 17 and newer. ([#1579](https://github.com/spotbugs/spotbugs/issues/1579))
- Fixed false positives `EI_EXPOSE_REP` thrown in case of fields initialized by the `of` or `copyOf` method of a `List`, `Map` or `Set` ([#1771](https://github.com/spotbugs/spotbugs/issues/1771))
- Fixed CFGBuilderException thrown when `dup_x2` is used to swap the reference and wide-value (double, long) in the stack ([#2146](https://github.com/spotbugs/spotbugs/pull/2146))

## 4.7.1 - 2022-06-26

### Fixed
- Fixed False positives for `RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE` on try-with-resources with interface references ([#1931](https://github.com/spotbugs/spotbugs/issues/1931))
- Fixed NullPointerException thrown by detector `FindPotentialSecurityCheckBasedOnUntrustedSource` on Kotlin files. ([#2041](https://github.com/spotbugs/spotbugs/issues/2041))
- Disabled detector `ThrowingExceptions` by default to avoid many false positives ([#2040](https://github.com/spotbugs/spotbugs/issues/2040))
- Fixed False positives for `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION` and `THROWS_METHOD_THROWS_CLAUSE_THROWABLE` on evaluating synthetic classes ([#2040](https://github.com/spotbugs/spotbugs/issues/2040))
- Fixed False positive for `SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA` on proper protection by using static lock for synchronized block, but inside an unsecured (synchronized and not static) method ([#2089](https://github.com/spotbugs/spotbugs/issues/2089))

## 4.7.0 - 2022-04-14
### Changed
- Updated documentation by adding parenthesis `()` to the negative odd check message ([#1995](https://github.com/spotbugs/spotbugs/issues/1995))
- Let the Plugin class implement AutoCloseable so we can release the .jar file ([#2024](https://github.com/spotbugs/spotbugs/issues/2024))

### Fixed
- Fixed reports to truncate existing files before writing new content ([#1950](https://github.com/spotbugs/spotbugs/issues/1950))
- Bumped Saxon-HE from 10.6 to 11.3 ([#1955](https://github.com/spotbugs/spotbugs/pull/1955), [#1999](https://github.com/spotbugs/spotbugs/pull/1999))
- Fixed traversal of nested archives governed by `-nested:true` ([#1930](https://github.com/spotbugs/spotbugs/pull/1930))
- Warnings of deprecated System::setSecurityManager calls on Java 17 ([#1983](https://github.com/spotbugs/spotbugs/pull/1983))
- Fixed false positive SSD bug for locking on java.lang.Class objects ([#1978](https://github.com/spotbugs/spotbugs/issues/1978))
- FindReturnRef throws an IllegalArgumentException unexpectedly ([#2019](https://github.com/spotbugs/spotbugs/issues/2019))
- Bump ObjectWeb ASM from 9.2 to 9.3 supporting JDK 19 ([#2004](https://github.com/spotbugs/spotbugs/pull/2004))

### Added
* New detector `ThrowingExceptions` and introduced new bug types:
  * `THROWS_METHOD_THROWS_RUNTIMEEXCEPTION` is reported in case of a method throwing RuntimeException, 
  * `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION` is reported when a method has Exception in its throws clause and 
  * `THROWS_METHOD_THROWS_CLAUSE_THROWABLE` is reported when a method has Throwable in its throws clause (See [SEI CERT ERR07-J](https://wiki.sei.cmu.edu/confluence/display/java/ERR07-J.+Do+not+throw+RuntimeException%2C+Exception%2C+or+Throwable))
* New rule `PERM_SUPER_NOT_CALLED_IN_GETPERMISSIONS` to warn for custom class loaders who do not call their superclasses' `getPermissions()` in their `getPermissions()` method. This rule based on the SEI CERT rule *SEC07-J Call the superclass's getPermissions() method when writing a custom class loader*. ([#SEC07-J](https://wiki.sei.cmu.edu/confluence/display/java/SEC07-J.+Call+the+superclass%27s+getPermissions%28%29+method+when+writing+a+custom+class+loader))
* New rule `USC_POTENTIAL_SECURITY_CHECK_BASED_ON_UNTRUSTED_SOURCE` to detect cases where a non-final method of a non-final class is called from public methods of public classes and then the same method is called on the same object inside a doPrivileged block. Since the called method may have been overridden to behave differently on the first and second invocations this is a possible security check based on an unreliable source. This rule is based on *SEC02-J. Do not base security checks on untrusted sources*. ([#SEC02-J](https://wiki.sei.cmu.edu/confluence/display/java/SEC02-J.+Do+not+base+security+checks+on+untrusted+sources))
* New detector `DontUseFloatsAsLoopCounters` to detect usage of floating-point variables as loop counters (`FL_FLOATS_AS_LOOP_COUNTERS`), according to SEI CERT rules [NUM09-J. Do not use floating-point variables as loop counters](https://wiki.sei.cmu.edu/confluence/display/java/NUM09-J.+Do+not+use+floating-point+variables+as+loop+counters)
* New test detector `ViewCFG` to visualize the control-flow graph for `SpotBugs` developers

## 4.6.0 - 2022-03-08
### Fixed
- Fixed spotbugs build with ecj compiler ([#1903](https://github.com/spotbugs/spotbugs/issues/1903))
- Moved tests from spotbugs project to spotbugs-tests project ([#1914](https://github.com/spotbugs/spotbugs/issues/1914))
- Fixed UI freezes in Eclipse on bug count decorations update ([#285](https://github.com/spotbugs/spotbugs/issues/285))
- Bumped log4j from 2.17.1 to 2.17.2 ([#1960](https://github.com/spotbugs/spotbugs/pull/1960))
- Bumped gson from 2.8.9 to 2.9.0 ([#1960](https://github.com/spotbugs/spotbugs/pull/1966))

### Added
* New detector `FindInstanceLockOnSharedStaticData` for new bug type `SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA`. This detector reports a bug if an instance level lock is used to modify a shared static data. (See [SEI CERT rule LCK06-J](https://wiki.sei.cmu.edu/confluence/display/java/LCK06-J.+Do+not+use+an+instance+lock+to+protect+shared+static+data))

## 4.5.3 - 2022-01-04
### Security
- Bumped log4j from 2.16.0 to 2.17.1 to address [CVE-2021-45105](https://nvd.nist.gov/vuln/detail/CVE-2021-45105) and [CVE-2021-44832](https://nvd.nist.gov/vuln/detail/CVE-2021-44832) ([#1885](https://github.com/spotbugs/spotbugs/pull/1885), [#1897](https://github.com/spotbugs/spotbugs/pull/1897))

### Fixed
- Remove duplicated logging frameworks from the Eclipse plugin distribution ([#1868](https://github.com/spotbugs/spotbugs/issues/1868))
- Corrected class name validation to no longer fail for Kotlin classes on class path containing special characters. ([#1883](https://github.com/spotbugs/spotbugs/issues/1883))

## 4.5.2 - 2021-12-13
### Security
- Bumped log4j from 2.14.1 to 2.16.0 to address CVE-2021-44228

### Fixed
- False negative about the rule RV_DONT_JUST_NULL_CHECK_READLINE ([#1821](https://github.com/spotbugs/spotbugs/issues/1821)[#1820](https://github.com/spotbugs/spotbugs/issues/1820)[#1819](https://github.com/spotbugs/spotbugs/issues/1819)[#1818](https://github.com/spotbugs/spotbugs/issues/1818))
- Updated RV_01_TO_INT to handle float and long checks ([#1518](https://github.com/spotbugs/spotbugs/issues/1518))

## 4.5.1 - 2021-12-08
### Fixed
- Ant task does not produce XML anymore ([#1827](https://github.com/spotbugs/spotbugs/issues/1827))
- Do not emit false positives of `MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR` and `MC_OVERRIDABLE_METHOD_CALL_IN_CLONE` for final classes ([#1812](https://github.com/spotbugs/spotbugs/issues/1812)).
- Reports cannot be created on Windows platform ([#1842](https://github.com/spotbugs/spotbugs/pull/1842))

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
* Remove unused methods and deprecated methods in `edu.umd.cs.findbugs.util.Util`
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
* edu.umd.cs.findbugs.classfile.ClassDescriptor#toDottedClassName() is deprecated and getDottedClassName() can be used instead.

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
* Missing java.nio.file.Files support in  OS_OPEN_STREAM ([Bugs#1399](https://sourceforge.net/p/findbugs/bugs/1399/))
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

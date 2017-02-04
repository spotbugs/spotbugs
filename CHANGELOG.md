# Changelog

This is the changelog for SpotBugs. This follows [Keep a Changelog v0.3](http://keepachangelog.com/en/0.3.0/).

## Unreleased

### Added

* Make TypeQualifierResolver recognize JetBrains NotNull annotations ([Patch #248](https://sourceforge.net/p/findbugs/patches/248/))


### Changed

* Support user preferences exported by the Export->Preferences wizard in Eclipse ([01b7df7](https://github.com/spotbugs/spotbugs/commit/01b7df7))
* No more dependency in annotations on BugRanker and Priorities ([2f9d672](https://github.com/findbugsproject/findbugs/commit/2f9d672), [725be6e](https://github.com/findbugsproject/findbugs/commit/725be6e))

### Deprecated

### Removed

* Java7 Support ([Issue #19](https://github.com/spotbugs/spotbugs/issues/19))

### Fixed

* Fix typo in description of VA_FORMAT_STRING_BAD_CONVERSION ([f5f62b6](https://github.com/spotbugs/spotbugs/commit/f5f62b6))
* Fix description for VA_FORMAT_STRING_USES_NEWLINE ([89b82fa](https://github.com/spotbugs/spotbugs/commit/89b82fa))
* StackOverflowError in ValueRangeAnalysisFactory ([Bug#1369](https://sourceforge.net/p/findbugs/bugs/1369/))
* Fix descriptions of UC_USELESS_CONDITION and UC_USELESS_CONDITION_TYPE ([e02535c](https://github.com/spotbugs/spotbugs/commit/e02535c))
* Command line "@" feature ([Bug#1375](https://sourceforge.net/p/findbugs/bugs/1375/))
* SOAPMessage.getSOAPHeader() can and does return null ([Bug#1368](https://sourceforge.net/p/findbugs/bugs/1368/))
* False positive in UC_USELESS_OBJECT  ([Bug#1373](https://sourceforge.net/p/findbugs/bugs/1373/))
* False positive in NP_LOAD_OF_KNOWN_NULL_VALUE  ([Bug#1372](https://sourceforge.net/p/findbugs/bugs/1372/))
* Missing java.nio.file.Files support in  OS_OPEN_STREAM ([Bugs#1399](https://sourceforge.net/p/findbugs/bugs/1399/)])
* False negative in GC_UNRELATED_TYPES  ([Bug#1387](https://sourceforge.net/p/findbugs/bugs/1387/))
* Japanese messages  ([Bug#1380](https://sourceforge.net/p/findbugs/bugs/1380/), [FindBugs#45](https://github.com/findbugsproject/findbugs/pull/45), [Bug#1417](https://sourceforge.net/p/findbugs/bugs/1417/))
* Description of NP_OPTIONAL_RETURN_NULL ([05680c6](https://github.com/findbugsproject/findbugs/commit/05680c6))
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


## FindBugs 3.0.1 or older

Check [changelog at SourceForge](http://findbugs.sourceforge.net/Changes.html).

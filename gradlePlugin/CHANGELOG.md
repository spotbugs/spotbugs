# Changelog

This is the changelog for SpotBugs Gradle Plugin. This follows [Keep a Changelog v1.0.0](http://keepachangelog.com/en/1.0.0/).

Currently the versioning policy of this project does not follow [Semantic Versioning](http://semver.org/).

## 1.6.0 - 2017-10-23

* Use SpotBugs 3.1.0

## 1.5 - 2017-10-14

* Use SpotBugs 3.1.0-RC7
* Make build failed when user uses unsupported Gradle version ([#357](https://github.com/spotbugs/spotbugs/issues/357))
* Make error message human readable ([#428](https://github.com/spotbugs/spotbugs/pull/428))
* Fix missing dependency on compile task ([#440](https://github.com/spotbugs/spotbugs/issues/440))

## 1.4 - 2017-09-25

* Use SpotBugs 3.1.0-RC6
* Fix "Cannot convert the provided notation to a File or URI: classesDirs" error ([#320](https://github.com/spotbugs/spotbugs/issues/320))
* Support working with Android Gradle Plugin 2.3 ([#256](https://github.com/spotbugs/spotbugs/issues/256))

## 1.3 - 2017-08-16 [YANKED]

* Use SpotBugs 3.1.0-RC5
* Stop using [single class directory](https://docs.gradle.org/4.0.2/release-notes.html#multiple-class-directories-for-a-single-source-set) to prepare for Gradle v5 ([#299](https://github.com/spotbugs/spotbugs/issues/299))
* Print 'SpotBugs' instead of 'FindBugs' ([#291](https://github.com/spotbugs/spotbugs/issues/291))

## 1.2 - 2017-07-21

* Use SpotBugs 3.1.0-RC4
* Fixed [#214](https://github.com/spotbugs/spotbugs/issues/214)

## 1.1 - 2017-06-10

* Use SpotBugs 3.1.0-RC2

## 1.0 - 2017-05-16

* First release which uses FindBugs 3.0.1

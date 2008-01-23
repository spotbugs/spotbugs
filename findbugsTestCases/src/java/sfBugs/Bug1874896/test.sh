#!/bin/sh

javac *.java
findbugs -textui -output out1.xml -xml:withMessages Foo.class
findbugs -textui -output out2.xml -xml:withMessages TestFoo.class
unionBugs -output out.xml out1.xml out2.xml

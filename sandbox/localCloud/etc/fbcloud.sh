#!/bin/sh
export JARS=$(find . -name \*.jar -exec echo -n ":" \; -exec echo -n {} \;)
java -cp $JARS edu.umd.cs.findbugs.flybush.local.LocalFindBugsCloud
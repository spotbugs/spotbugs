Installing
==========

This chapter explains how to install SpotBugs.

Extracting the Distribution
---------------------------

The easiest way to install SpotBugs is to download a binary distribution.
Binary distributions are available in `gzipped tar format <http://repo.maven.apache.org/maven2/com/github/spotbugs/spotbugs/3.1.0-RC6/spotbugs-3.1.0-RC6.tgz>`_ and `zip format <http://repo.maven.apache.org/maven2/com/github/spotbugs/spotbugs/3.1.0-RC6/spotbugs-3.1.0-RC6.zip>`_.
Once you have downloaded a binary distribution, extract it into a directory of your choice.

Extracting a gzipped tar format distribution::

    $ gunzip -c spotbugs-3.1.0-RC6.tgz | tar xvf -

Extracting a zip format distribution::

    C:\Software> unzip spotbugs-3.1.0-RC6.zip

Usually, extracting a binary distribution will create a directory ending in ``spotbugs-3.1.0-RC6``.
For example, if you extracted the binary distribution from the ``C:\Software directory``, then the SpotBugs software will be extracted into the directory ``C:\Software\spotbugs-3.1.0-RC6``.
This directory is the SpotBugs home directory.
We'll refer to it as ``$SPOTBUGS_HOME`` (or ``%SPOTBUGS_HOME%`` for Windows) throughout this manual.

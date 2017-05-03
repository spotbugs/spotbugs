Installing
==========

This chapter explains how to install SpotBugs.

Extracting the Distribution
---------------------------

The easiest way to install SpotBugs is to download a binary distribution.
Binary distributions are available in `gzipped tar format <https://github.com/spotbugs/spotbugs/archive/3.1.0_preview2.tar.gz>`_ and `zip format <https://github.com/spotbugs/spotbugs/archive/3.1.0_preview2.zip>`_.
Once you have downloaded a binary distribution, extract it into a directory of your choice.

Extracting a gzipped tar format distribution::

    $ gunzip -c spotbugs-3.1.0-dev-20161105-d07b50f.tar.gz | tar xvf -

Extracting a zip format distribution::

    C:\Software> unzip spotbugs-3.1.0-dev-20161105-d07b50f.zip

Usually, extracting a binary distribution will create a directory ending in ``spotbugs-3.1.0-dev-20161105-d07b50f``.
For example, if you extracted the binary distribution from the ``C:\Software directory``, then the SpotBugs software will be extracted into the directory ``C:\Software\findbugs-3.1.0-dev-20161105-d07b50f``.
This directory is the SpotBugs home directory.
We'll refer to it as ``$SPOTBUGS_HOME`` (or ``%SPOTBUGS_HOME%`` for Windows) throughout this manual.

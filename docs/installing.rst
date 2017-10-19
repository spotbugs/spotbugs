Installing
==========

This chapter explains how to install SpotBugs.

Extracting the Distribution
---------------------------

The easiest way to install SpotBugs is to download a binary distribution.
Binary distributions are available in :dist:`gzipped tar format <tgz>` and :dist:`zip format <zip>`.
Once you have downloaded a binary distribution, extract it into a directory of your choice.

Extracting a gzipped tar format distribution:

.. literalinclude:: generated/install-tgz.template.inc

Extracting a zip format distribution:

.. literalinclude:: generated/install-zip.template.inc

Usually, extracting a binary distribution will create a directory ending in spotbugs-|release|.
For example, if you extracted the binary distribution from the C:\\Software directory, then the SpotBugs software will be extracted into the directory C:\\Software\\spotbugs-|release|.
This directory is the SpotBugs home directory.
We'll refer to it as ``$SPOTBUGS_HOME`` (or ``%SPOTBUGS_HOME%`` for Windows) throughout this manual.

@echo off
:: Launch FindBugs GUI on a Windows system.
:: Adapted from scripts found at http://www.ericphelps.com/batch/

:: ----------------------------------------------------------------------
:: Set up default values
:: ----------------------------------------------------------------------
set appjar=findbugsGUI.jar
set jvmargs=-Xmx128m
set args=

goto loop

:: ----------------------------------------------------------------------
:: Process command-line arguments
:: ----------------------------------------------------------------------

:shift2
shift
:shift1
shift

:loop

if "%1"=="-gui" set appjar=findbugsGUI.jar
if "%1"=="-gui" goto shift1

if "%1"=="-textui" set appjar=findbugs.jar
if "%1"=="-textui" goto shift1

if "%1"=="-home" set FINDBUGS_HOME=%2
if "%1"=="-home" goto shift2

if "%1"=="-jvmArgs" set jvmargs=%2
if "%1"=="-jvmArgs" goto shift2

if "%1"=="-help" goto help

if "%1"=="" goto launch

set args=%args% %1
goto shift1

:: ----------------------------------------------------------------------
:: Launch FindBugs
:: ----------------------------------------------------------------------
:launch
:: Make sure FINDBUGS_HOME is set.
if "%FINDBUGS_HOME%"=="" goto homeNotSet

:: echo FINDBUGS_HOME is %FINDBUGS_HOME%
:: echo appjar is %appjar%
:: echo args is %args%
:: echo jvmargs is %jvmargs%
java %jvmargs% -jar %FINDBUGS_HOME%\lib\%appjar% %args%
goto end

:: ----------------------------------------------------------------------
:: Display usage information
:: ----------------------------------------------------------------------
:help
echo Usage: findbugs [options] 
echo    -home dir       Use dir as FINDBUGS_HOME
echo    -gui            Use the Graphical UI (default behavior)
echo    -textui         Use the Text UI
echo    -jvmArgs args   Pass args to JVM
echo    -help           Display this message
echo All other options are passed to the FindBugs application
goto end

:: ----------------------------------------------------------------------
:: Report that FINDBUGS_HOME is not set (and was not specified)
:: ----------------------------------------------------------------------
:homeNotSet
echo Please set FINDBUGS_HOME before running findbugs.bat
goto end

:end

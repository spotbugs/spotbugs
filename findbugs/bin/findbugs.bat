@echo off
:: Launch FindBugs GUI on a Windows system.
:: Adapted from scripts found at http://www.ericphelps.com/batch/
:: This will only work on Windows NT or later!

:: ----------------------------------------------------------------------
:: Set up default values
:: ----------------------------------------------------------------------
set appjar=findbugsGUI.jar
set jvmargs=-Xmx256m -Xss2m
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

:: Remove surrounding quotes from %1 and %2
set firstArg=%~1
set secondArg=%~2

if "%firstArg%"=="-gui" set appjar=findbugsGUI.jar
if "%firstArg%"=="-gui" goto shift1

if "%firstArg%"=="-textui" set appjar=findbugs.jar
if "%firstArg%"=="-textui" goto shift1

if "%firstArg%"=="-home" set FINDBUGS_HOME=%secondArg%
if "%firstArg%"=="-home" goto shift2

if "%firstArg%"=="-jvmArgs" set jvmargs=%secondArg%
if "%firstArg%"=="-jvmArgs" goto shift2

if "%firstArg%"=="-help" goto help

if "%firstArg%"=="" goto launch

set args=%args% "%firstArg%"
goto shift1

:: ----------------------------------------------------------------------
:: Launch FindBugs
:: ----------------------------------------------------------------------
:launch
:: Make sure FINDBUGS_HOME is set.
:: Note that this will fail miserably if the value of FINDBUGS_HOME
:: has quote characters in it.
if "%FINDBUGS_HOME%"=="" goto homeNotSet

:: echo FINDBUGS_HOME is %FINDBUGS_HOME%
:: echo appjar is %appjar%
:: echo args is %args%
:: echo jvmargs is %jvmargs%
java "-Dfindbugs.home=%FINDBUGS_HOME%" %jvmargs% -jar "%FINDBUGS_HOME%\lib\%appjar%" %args%
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

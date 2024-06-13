@echo off
setlocal enabledelayedexpansion

JAVA_ARGS="-server -Xss64M -Xms2G -Xmx8G -XX:NewSize=512M"
set LVL="COMPET"
set SOLS=-1
set TIME_LIMIT=-1

:: set the full path to the java archive of choco-solver
set CHOCO_JAR=choco-parsers-4.10.15-SNAPSHOT-light.jar


if "%#"=="0" (
    echo "%% No flatzinc file found"
    exit /b 1
)

set ARGS=""

set usage="\

Usage: fzn_choco.exe [<options>] [<file>]

    Parse and solve <file> using Choco.

OPTIONS:

    -h, --help
        Display this message.

    -a
        This causes the solver to search for, and output all solutions.
        When this option is not given the solver should search for, and output the first solution or the best known one.

    -f
        When invoked with this option the solver ignores any specified search strategy.

    -v
        When invoked with this option verbose solving is activated.

    -s
        When invoked with this option the solver outputs statistics for solving

    -p
        When invoked with this option the solver is free to use multiple threads and/or cores during search.
        The argument n specifies the number of cores that are available.  (The default is %NB_NODES%.)

    -t <n>
        Limit the resolution time of each problem instance to n ms.  (The default is %TIME_LIMIT%.)

    -jar <j>
        Override the jar file.  (The default is %CHOCO_JAR%.)

    --jargs <args>
        Override default java argument (The default is %JAVA_ARGS%.)

    --cp-profiler <id>,<port>
        Enable the cp-profiler with the given id and port.

EXAMPLES:

  Basic command to solve a fzn model with choco:
  fzn-choco -jar C:/path/to/choco-parsers-with-dep.jar alpha.fzn

  Additionnal arguments:
  $> fzn-choco.exe -tl 100 -a -f alpha.fzn

"

:parse_args
if "%~1"=="" goto end_parse_args

if /i "%~1"=="-h" goto display_help
if /i "%~1"=="--help" goto display_help

if /i "%~1"=="-a" (
    set ARGS=%ARGS%" -a"
)else
if /i "%~1"=="-f" (
    set ARGS=%ARGS%" -f"
)else
if /i "%~1"=="-p" (
    set ARGS=%ARGS%" -p %~2"
    shift
)else
if /i "%~1"=="-t" (
    set "TIME_LIMIT=%~2"
    shift
)else
if /i "%~1"=="-v" (
    set "LVL=INFO"
)else
if /i "%~1"=="-s" (
    set ARGS=$ARGS" -stasol"
)else
if /i "%~1"=="-n" (
    set "SOLS=%~2"
    shift
)else
if /i "%~1"=="-r" (
    set ARGS=%ARGS%" -seed %~2"
    shift
)else
if /i "%~1"=="-jar" (
    set "CHOCO_JAR=%~2"
    shift
)else
if /i "%~1"=="--cp-profiler" (
    set ARGS=%ARGS%" --cp-profiler %~2"
    shift
)else
if /i "%~1"=="--jargs" (
    set "JAVA_ARGS=%~2"
    shift
)else
(
    echo "%% Unknown option %~1"
    goto display_help
)
shift
goto parse_args

:display_help
echo %usage%
exit /b 2

:end_parse_args

set FILE="%~1"

set ARGS=$ARGS" -limit=[%TIME_LIMIT%,%SOLS%sols] -lvl %LVL%"

set CMD="java %JAVA_ARGS% -cp %CHOCO_JAR% org.chocosolver.parser.flatzinc.ChocoFZN \"%FILE%\" %ARGS%"

call %CMD%


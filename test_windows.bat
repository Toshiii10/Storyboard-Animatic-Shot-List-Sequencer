@echo off
setlocal
cd /d "%~dp0"
call build_windows.bat
if errorlevel 1 exit /b 1
if not exist build\test-classes mkdir build\test-classes
javac --release 17 -encoding UTF-8 -cp build/classes -d build/test-classes test/com/csis/BoardFlowTest.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "build/classes;build/test-classes" com.csis.BoardFlowTest

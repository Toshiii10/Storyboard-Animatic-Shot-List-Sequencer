@echo off
setlocal
cd /d "%~dp0web"
call mvn -B package
if errorlevel 1 (
    echo BoardFlow web build failed. Check that JDK 21+ and Maven are installed.
    exit /b 1
)
echo BoardFlow web build complete. Run run_web_windows.bat to start the studio.

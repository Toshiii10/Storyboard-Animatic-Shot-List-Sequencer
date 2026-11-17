@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
if not exist dist mkdir dist
> build\sources.txt (for /r src %%f in (*.java) do (
    set "source=%%f"
    echo "!source:\=/!"
))
echo Compiling BoardFlow...
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
if errorlevel 1 exit /b 1
jar cfm dist/Storyboard.jar manifest.mf -C build/classes .
if errorlevel 1 exit /b 1
echo Build complete: dist\Storyboard.jar

@echo off
setlocal enabledelayedexpansion

:: Force the terminal to switch to your exact project directory
cd /d "D:\2nd year and 3rd year\Main Files\tesda\TESDA JAVA\Storyboard & Animatic Shot-List Sequencer"

echo Cleaning old builds...
rmdir /S /Q build\classes 2>nul
rmdir /S /Q dist 2>nul
mkdir build\classes
mkdir dist

echo Finding Java source files...
type nul > sources.txt
for /R src %%f in (*.java) do (
    set "filepath=%%f"
    :: Swap backslashes with forward slashes so Java doesn't get confused
    set "filepath=!filepath:\=/!"
    echo "!filepath!" >> sources.txt
)

echo Compiling Java classes...
javac -d build/classes -cp "lib/*" @sources.txt

if %errorlevel% neq 0 (
    echo Compilation failed.
    del sources.txt
    pause
    exit /b %errorlevel%
)

echo Packaging JAR...
jar cvfm dist/Storyboard.jar manifest.mf -C build/classes .

echo Cleaning up temporary files...
del sources.txt

echo Build complete! Executable is in the dist folder.
pause
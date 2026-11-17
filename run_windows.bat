@echo off
setlocal
cd /d "%~dp0"
if not exist dist\Storyboard.jar call build_windows.bat
if errorlevel 1 exit /b 1
java -jar dist\Storyboard.jar

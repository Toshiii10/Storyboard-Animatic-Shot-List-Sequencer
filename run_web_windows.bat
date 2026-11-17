@echo off
setlocal
cd /d "%~dp0web"
if not exist "target\boardflow-web-1.0.0.jar" (
    call "%~dp0build_web_windows.bat"
    if errorlevel 1 exit /b 1
)
echo Starting BoardFlow. Once started, open http://localhost:8080 in your browser.
echo Keep this window open while using the studio. Press Ctrl+C to stop.
java -jar target\boardflow-web-1.0.0.jar
if errorlevel 1 pause

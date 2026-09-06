@echo off
if exist "%~dp0..\gradlew.bat" (
  call "%~dp0..\gradlew.bat" -p "%~dp0" %*
) else (
  call gradle -p "%~dp0" %*
)
exit /b %ERRORLEVEL%

@echo off
setlocal
echo Compilando y ejecutando Clase 1...
call "%~dp0mvnw.cmd" compile exec:exec "-DmainClass=com.graphics.clase1"
set "CIUDAD_EXIT=%ERRORLEVEL%"
endlocal & exit /b %CIUDAD_EXIT%

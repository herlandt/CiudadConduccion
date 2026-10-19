@echo off
setlocal
echo Compilando y ejecutando Clase 4...
call "%~dp0mvnw.cmd" compile exec:exec "-DmainClass=com.graphics.clase4"
set "CIUDAD_EXIT=%ERRORLEVEL%"
endlocal & exit /b %CIUDAD_EXIT%

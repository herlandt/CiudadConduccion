@echo off
setlocal
rem Lanzador local: usa Maven incluido o Maven instalado en PATH.
set "CIUDAD_MAVEN=%~dp0apache-maven-3.9.6\bin\mvn.cmd"
if exist "%CIUDAD_MAVEN%" goto ejecutar
where mvn.cmd >nul 2>&1
if errorlevel 1 goto sinMaven
set "CIUDAD_MAVEN=mvn.cmd"

:ejecutar
pushd "%~dp0"
if errorlevel 1 exit /b 1
call "%CIUDAD_MAVEN%" %*
set "CIUDAD_EXIT=%ERRORLEVEL%"
popd
endlocal & exit /b %CIUDAD_EXIT%

:sinMaven
echo [ERROR] No se encontro Maven local ni mvn.cmd en PATH.
echo Instala Maven o restaura apache-maven-3.9.6 en la carpeta del proyecto.
endlocal & exit /b 9009

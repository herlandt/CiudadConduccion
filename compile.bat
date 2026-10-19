
@echo off
setlocal
echo Compilando Ciudad y Conduccion...
call "%~dp0mvnw.cmd" compile
set "CIUDAD_EXIT=%ERRORLEVEL%"
if "%CIUDAD_EXIT%"=="0" echo [OK] Compilacion completada.
if not "%CIUDAD_EXIT%"=="0" echo [ERROR] No se pudo compilar el proyecto.
endlocal & exit /b %CIUDAD_EXIT%

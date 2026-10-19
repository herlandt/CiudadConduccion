@echo off
setlocal
set "CIUDAD_EXIT=0"

:menu
cls
echo ========================================================
echo   CIUDAD Y CONDUCCION EN OPENGL - Java 17 / LWJGL
echo ========================================================
echo.
echo   [1] Clase 1 - Ciudad, mapa 3D y renderizador
echo   [2] Clase 2 - Auto, conduccion y colisiones
echo   [3] Clase 3 - Iluminacion, farolas y faros
echo   [4] Clase 4 - Juego completo
echo   [5] Compilar proyecto
echo   [0] Salir
echo.
choice /C 123450 /N /M "Selecciona una opcion [1-5, 0]: "
if errorlevel 7 goto errorMenu
if errorlevel 6 goto salir
if errorlevel 5 goto compilar
if errorlevel 4 goto clase4
if errorlevel 3 goto clase3
if errorlevel 2 goto clase2
if errorlevel 1 goto clase1
goto salir

:clase1
call "%~dp0run_clase1.bat"
set "CIUDAD_EXIT=%ERRORLEVEL%"
goto resultado

:clase2
call "%~dp0run_clase2.bat"
set "CIUDAD_EXIT=%ERRORLEVEL%"
goto resultado

:clase3
call "%~dp0run_clase3.bat"
set "CIUDAD_EXIT=%ERRORLEVEL%"
goto resultado

:clase4
call "%~dp0run_clase4.bat"
set "CIUDAD_EXIT=%ERRORLEVEL%"
goto resultado

:compilar
call "%~dp0compile.bat"
set "CIUDAD_EXIT=%ERRORLEVEL%"
goto resultado

:resultado
echo.
if not "%CIUDAD_EXIT%"=="0" echo [ERROR] El comando termino con codigo %CIUDAD_EXIT%. Revisa el mensaje anterior.
pause
goto menu

:errorMenu
set "CIUDAD_EXIT=1"
echo [ERROR] No se pudo leer la opcion del menu.

:salir
endlocal & exit /b %CIUDAD_EXIT%

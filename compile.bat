@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
echo N | mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========== COMPILACION EXITOSA ==========
    dir target\galacticos-*.jar
) else (
    echo.
    echo ========== ERROR EN COMPILACION ==========
)
pause

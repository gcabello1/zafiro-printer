@echo off
REM ============================================================
REM  Actualiza el JAR del servicio PrinterService
REM  Ejecutar como Administrador
REM ============================================================

REM -- 1. Verificar JAVA_HOME --
IF "%JAVA_HOME%"=="" (
    echo [ERROR] JAVA_HOME no esta configurado.
    pause
    exit /b 1
)

SET JVM_DLL=%JAVA_HOME%\bin\server\jvm.dll
IF NOT EXIST "%JVM_DLL%" SET JVM_DLL=%JAVA_HOME%\jre\bin\server\jvm.dll
IF NOT EXIST "%JVM_DLL%" (
    echo [ERROR] jvm.dll no encontrado en JAVA_HOME: %JAVA_HOME%
    pause
    exit /b 1
)

SET PRUNSRV=%CD%\prunsrv64.exe
IF NOT EXIST "%PRUNSRV%" SET PRUNSRV=%CD%\prunsrv.exe
IF NOT EXIST "%PRUNSRV%" (
    echo [ERROR] No se encontro prunsrv64.exe ni prunsrv.exe
    pause
    exit /b 1
)

REM -- 2. Detener el servicio --
echo Deteniendo servicio...
"%PRUNSRV%" //SS//PrinterService
timeout /t 3 /nobreak >nul

REM -- 3. Actualizar configuracion --
echo Actualizando configuracion...
"%PRUNSRV%" //US//PrinterService ^
  --Jvm="%JVM_DLL%" ^
  --Classpath="%CD%\Printer-0.50.0.jar" ^
  --StartParams="-p;%CD%" ^
  ++JvmOptions=--add-opens=java.base/java.lang=ALL-UNNAMED ^
  ++JvmOptions=--add-opens=java.base/java.nio=ALL-UNNAMED ^
  ++JvmOptions=--add-opens=java.base/sun.nio.ch=ALL-UNNAMED

REM -- 4. Reiniciar el servicio --
echo Reiniciando servicio...
"%PRUNSRV%" //ES//PrinterService

IF %ERRORLEVEL% EQU 0 (
    echo [OK] Servicio actualizado y reiniciado correctamente.
) ELSE (
    echo [ERROR] Problema al reiniciar. Codigo: %ERRORLEVEL%
    echo         Iniciar manualmente: net start PrinterService
)
pause

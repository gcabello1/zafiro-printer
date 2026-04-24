@echo off
REM ============================================================
REM  Instala Printer como servicio Windows usando procrun
REM  Requiere: Java 21 (64-bit) + prunsrv64.exe en el mismo dir
REM  Ejecutar como Administrador
REM ============================================================

REM -- 1. Verificar que JAVA_HOME esté definido --
IF "%JAVA_HOME%"=="" (
    echo [ERROR] JAVA_HOME no esta configurado como variable de sistema.
    echo         Configuralo en: Panel de Control - Sistema - Variables de entorno
    echo         Ejemplo: C:\Program Files\Eclipse Adoptium\jdk-21.0.x.x-hotspot
    pause
    exit /b 1
)

REM -- 2. Localizar jvm.dll (Java 21 usa bin\server\jvm.dll) --
SET JVM_DLL=%JAVA_HOME%\bin\server\jvm.dll
IF NOT EXIST "%JVM_DLL%" (
    SET JVM_DLL=%JAVA_HOME%\jre\bin\server\jvm.dll
)
IF NOT EXIST "%JVM_DLL%" (
    echo [ERROR] No se encontro jvm.dll en:
    echo         %JAVA_HOME%\bin\server\jvm.dll
    echo         Verificar que JAVA_HOME apunta a un JDK 21 valido
    pause
    exit /b 1
)
echo [OK] JVM encontrado: %JVM_DLL%

REM -- 3. Usar prunsrv64.exe para JDK 64-bit (obligatorio con Java 21) --
SET PRUNSRV=%CD%\prunsrv64.exe
IF NOT EXIST "%PRUNSRV%" (
    SET PRUNSRV=%CD%\prunsrv.exe
)
IF NOT EXIST "%PRUNSRV%" (
    echo [ERROR] No se encontro prunsrv64.exe ni prunsrv.exe en %CD%
    echo         Descargar desde: https://commons.apache.org/proper/commons-daemon/procrun.html
    pause
    exit /b 1
)
echo [OK] Usando: %PRUNSRV%

REM -- 4. Crear directorio de logs si no existe --
IF NOT EXIST "%CD%\logs" mkdir "%CD%\logs"

REM -- 5. Instalar el servicio --
REM    NOTA: ++JvmOptions sin comillas, separados por ; o en entradas separadas
REM    Los --add-opens son obligatorios para Undertow/XNIO con Java 9+
"%PRUNSRV%" //IS//PrinterService ^
  --Description="Printer Service Zafiro" ^
  --Install="%PRUNSRV%" ^
  --Jvm="%JVM_DLL%" ^
  --Classpath="%CD%\Printer-0.50.0.jar" ^
  --StartMode=jvm ^
  --StartClass=py.com.zafiro.printer.Main ^
  --StartParams="-p;%CD%" ^
  --StopMode=jvm ^
  --StopClass=py.com.zafiro.printer.Main ^
  --StopMethod=stop ^
  --LogPath="%CD%\logs" ^
  --StdOutput=auto ^
  --StdError=auto ^
  --Startup=auto ^
  --JvmMs=128 ^
  --JvmMx=512 ^
  ++JvmOptions=--add-opens=java.base/java.lang=ALL-UNNAMED ^
  ++JvmOptions=--add-opens=java.base/java.nio=ALL-UNNAMED ^
  ++JvmOptions=--add-opens=java.base/sun.nio.ch=ALL-UNNAMED

IF %ERRORLEVEL% EQU 0 (
    echo [OK] Servicio PrinterService instalado correctamente.
    echo      Para iniciarlo: net start PrinterService
) ELSE (
    echo [ERROR] Fallo la instalacion del servicio. Codigo: %ERRORLEVEL%
)
pause

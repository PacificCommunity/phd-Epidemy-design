@echo off
set JAVA_HOME="C:\Program Files\java\jdk-23\"
set PATH=%JAVA_HOME%\bin;%PATH%

java -version

set APP_NAME=epidemy-design
set MAIN_JAR=epidemy-design.jar

set INPUT_DIR=out\artifacts\epidemy_design_jar
set OUTPUT_DIR=redist

set FX_HOME=E:\fabriceb\Devel\Java\lib\JavaFX\javafx-sdk-23
set FX_LIBS=%FX_HOME%\lib
set FX_JMODS=%FX_HOME%\jmods

set MODULES=jdk.jsobject,java.desktop,java.logging,java.prefs,javafx.fxml,javafx.graphics,javafx.swing,javafx.web
set ICON=.\package\windows\epidemy-design.ico

if exist %OUTPUT_DIR%\%APP_NAME% rmdir /s /q %OUTPUT_DIR%\%APP_NAME%
if not exist %OUTPUT_DIR% mkdir %OUTPUT_DIR%
jpackage.exe --type app-image --input %INPUT_DIR% --name %APP_NAME% --main-jar %MAIN_JAR% --module-path %FX_JMODS% --add-modules %MODULES% --dest %OUTPUT_DIR% --icon %ICON%


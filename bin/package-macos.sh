#!/bin/bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-23.0.jdk/Contents/Home/
export PATH=$PATH:$JAVA_HOME/bin

export APP_NAME=epidemy-design
export MAIN_JAR=epidemy-design.jar

export INPUT_DIR=./out/artifacts/epidemy_design_jar
export OUTPUT_DIR=redist

export FX_HOME=/Users/fabriceb/devel/lib/javafx/javafx-sdk-23
export FX_LIBS=$FX_HOME/lib
export FX_JMODS=$FX_HOME/jmods

export MODULES=jdk.jsobject,java.desktop,java.logging,java.prefs,javafx.fxml,javafx.graphics,javafx.swing,javafx.web
export ICON=./package/macosx/epidemy-design.icns

if [[ -d $OUTPUT_DIR/$APP_NAME ]]; then
	rm -rf "{$OUTPUT_DIR/$APP_NAME:?}"*
fi
mkdir -p $OUTPUT_DIR
jpackage --type dmg --input $INPUT_DIR --name $APP_NAME --main-jar $MAIN_JAR --module-path $FX_JMODS --add-modules $MODULES --dest $OUTPUT_DIR --icon $ICON

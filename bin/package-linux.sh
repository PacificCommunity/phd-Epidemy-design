#!/bin/bash
export PATH=$PATH:$JAVA_HOME/bin

export APP_NAME=epidemy-design
export MAIN_JAR=epidemy-design.jar.jar

export INPUT_DIR=./out/artifacts/epidemy_design_jar
export OUTPUT_DIR=redist

export FX_HOME=
export FX_LIBS=$FX_HOME/lib
export FX_JMODS=$FX_HOME/jmods

export MODULES=jdk.jsobject,java.desktop,java.logging,java.prefs,javafx.fxml,javafx.graphics,javafx.swing,javafx.web
export ICON=./package/linux/epidemy-design.png

if [[ -d $OUTPUT_DIR/$APP_NAME ]]; then
	rm -rf "{$OUTPUT_DIR/$APP_NAME:?}"*
fi
mkdir -p $OUTPUT_DIR
jpackage --type app-image --input $INPUT_DIR --name $APP_NAME --main-jar $MAIN_JAR --module-path $FX_JMODS --add-modules $MODULES --dest $OUTPUT_DIR --icon $ICON
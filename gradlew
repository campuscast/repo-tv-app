#!/bin/sh

APP_HOME=$(cd "${0%/*}" && pwd -P)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "ERROR: Missing $WRAPPER_JAR"
  echo "Add Gradle wrapper JAR or run with local 'gradle'."
  exit 1
fi

exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"

@echo off
set APP_HOME=%~dp0
set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo ERROR: Missing %WRAPPER_JAR%
  echo Add Gradle wrapper JAR or run with local gradle.
  exit /b 1
)

java -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*

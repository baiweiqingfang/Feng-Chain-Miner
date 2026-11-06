#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 1

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi

if [ ! -x "$JAVACMD" ] && [ "$JAVACMD" != "java" ]; then
    echo "ERROR: JAVA_HOME does not point to a valid Java installation: $JAVA_HOME" >&2
    exit 1
fi

exec "$JAVACMD" -Xmx64m -Xms64m \
    -Dorg.gradle.appname=gradlew \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "$@"


#!/bin/bash
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

echo "DIR '$DIR'"

# Flags requeridos para Java 21: Undertow/XNIO necesitan acceso a módulos internos del JDK
JVM_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
JVM_OPTS="$JVM_OPTS --add-opens java.base/java.nio=ALL-UNNAMED"
JVM_OPTS="$JVM_OPTS --add-opens java.base/sun.nio.ch=ALL-UNNAMED"

java $JVM_OPTS -jar "$DIR/Printer.jar" -p "$DIR"

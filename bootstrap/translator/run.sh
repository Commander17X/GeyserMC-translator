#!/usr/bin/env sh
cd "$(dirname "$0")"
if [ ! -f config.yml ]; then
  echo "config.yml not found. Copy config.yml.example to config.yml and edit it."
  exit 1
fi
if [ -f Geyser-Translator.jar ]; then
  JAR=Geyser-Translator.jar
elif [ -f build/libs/Geyser-Translator.jar ]; then
  JAR=build/libs/Geyser-Translator.jar
else
  echo "Geyser-Translator.jar not found. Build with: ./gradlew :translator:shadowJar"
  exit 1
fi
exec java -jar "$JAR" --nogui

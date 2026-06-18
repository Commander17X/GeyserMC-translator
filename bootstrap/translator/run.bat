@echo off
cd /d "%~dp0"
if not exist "config.yml" (
  echo config.yml not found. Copy config.yml.example to config.yml and edit it.
  pause
  exit /b 1
)
if not exist "Geyser-Translator.jar" (
  if exist "build\libs\Geyser-Translator.jar" (
    set JAR=build\libs\Geyser-Translator.jar
  ) else (
    echo Geyser-Translator.jar not found in this folder.
    echo Build with: gradlew :translator:shadowJar
    pause
    exit /b 1
  )
) else (
  set JAR=Geyser-Translator.jar
)
echo Starting Geyser Translator standalone node...
java -jar "%JAR%" --nogui
pause

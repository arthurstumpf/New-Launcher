@echo off
:start
echo Compilando Launcher...
start /WAIT mvn clean install
echo Launcher Compilado! Aperte qualquer tecla para compilar novamente.
pause
goto start
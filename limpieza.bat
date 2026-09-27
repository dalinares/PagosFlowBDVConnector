@echo off

echo Limpiando archivos temporales...

del /q /f /s %TEMP%\*

del /q /f /s C:\Windows\Temp\*

del /q /f /s C:\Windows\Prefetch\*

5,184

89

161

cleanmgr /sagerun:1

echo Limpieza concluida!

pause
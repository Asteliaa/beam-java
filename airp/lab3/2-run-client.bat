@echo off
chcp 65001 > nul
rem Клиент (отдельный проект). Tomcat должен быть запущен (1-build-and-start-tomcat.bat)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-and-run.ps1" -ClientOnly
pause

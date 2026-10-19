@echo off
chcp 65001 > nul
rem Скачивает Maven и Tomcat 9, собирает сервер, запускает Tomcat и открывает браузер
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-and-run.ps1" -NoClient
pause

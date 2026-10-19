# Лаба 3 (вариант 3, музыка Моцарта): установка и запуск одной командой.
# Запуск:  powershell -ExecutionPolicy Bypass -File setup-and-run.ps1
# Скачивает проект, Maven и Tomcat 9 в папку $Root, собирает, запускает, открывает браузер.
param(
    [string]$Root = "C:\lab3",
    [switch]$NoClient,     # не запускать окно клиента
    [switch]$ClientOnly,   # только собрать и запустить клиент (Tomcat уже запущен)
    [switch]$Stop          # остановить Tomcat
)
$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = "SilentlyContinue"

$TomcatVer = "9.0.98"
$MavenVer  = "3.9.9"
$RepoZip   = "https://github.com/Asteliaa/beam-java/archive/refs/heads/main.zip"

New-Item -ItemType Directory -Force $Root | Out-Null
Set-Location $Root

# 1. Java
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "Java не найдена, ставлю JDK 17 через winget..."
    winget install -e --id EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements
    $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
}
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    # java в PATH может быть заглушкой, поэтому спрашиваем путь у самой Java
    $props = cmd /c "java -XshowSettings:properties -version 2>&1"
    $line = $props | Where-Object { $_ -match "java\.home\s*=" } | Select-Object -First 1
    $env:JAVA_HOME = ($line -split "=", 2)[1].Trim()
}
if (-not (Test-Path "$env:JAVA_HOME\bin\javac.exe")) { throw "Нужен JDK (с javac), а найдена только JRE: $env:JAVA_HOME" }
Write-Host "JAVA_HOME = $env:JAVA_HOME"

# 2. Проект: если скрипт лежит в папке лабы (рядом lab_servlet), берём её, иначе скачиваем
if ($PSScriptRoot -and (Test-Path "$PSScriptRoot\lab_servlet\pom.xml")) {
    $Lab = $PSScriptRoot
} else {
    if (-not (Test-Path "$Root\src\airp\lab3")) {
        Write-Host "Скачиваю проект..."
        Invoke-WebRequest $RepoZip -OutFile "$Root\repo.zip"
        Expand-Archive "$Root\repo.zip" "$Root\tmp" -Force
        Move-Item (Get-ChildItem "$Root\tmp" | Select-Object -First 1).FullName "$Root\src"
        Remove-Item "$Root\tmp","$Root\repo.zip" -Recurse -Force
    }
    $Lab = "$Root\src\airp\lab3"
}
Write-Host "Проект: $Lab"

# 3. Maven
$Mvn = "$Root\tools\apache-maven-$MavenVer\bin\mvn.cmd"
if (-not (Test-Path $Mvn)) {
    Write-Host "Скачиваю Maven $MavenVer..."
    New-Item -ItemType Directory -Force "$Root\tools" | Out-Null
    Invoke-WebRequest "https://archive.apache.org/dist/maven/maven-3/$MavenVer/binaries/apache-maven-$MavenVer-bin.zip" -OutFile "$Root\tools\maven.zip"
    Expand-Archive "$Root\tools\maven.zip" "$Root\tools" -Force
    Remove-Item "$Root\tools\maven.zip"
}

# 4. Tomcat 9
$Cat = "$Root\tools\apache-tomcat-$TomcatVer"
if (-not (Test-Path "$Cat\bin\catalina.bat")) {
    Write-Host "Скачиваю Tomcat $TomcatVer..."
    Invoke-WebRequest "https://archive.apache.org/dist/tomcat/tomcat-9/v$TomcatVer/bin/apache-tomcat-$TomcatVer-windows-x64.zip" -OutFile "$Root\tools\tomcat.zip"
    Expand-Archive "$Root\tools\tomcat.zip" "$Root\tools" -Force
    Remove-Item "$Root\tools\tomcat.zip"
}
$env:CATALINA_HOME = $Cat

if ($Stop) {
    & "$Cat\bin\shutdown.bat"
    return
}

function Start-Client {
    Write-Host "Собираю и запускаю клиент (apacheclient)..."
    Push-Location "$Lab\apacheclient"
    & $Mvn -q package
    if ($LASTEXITCODE -ne 0) { throw "Ошибка сборки клиента" }
    Pop-Location
    Start-Process java -ArgumentList "-Dfile.encoding=UTF-8","-jar","$Lab\apacheclient\target\apacheclient-1.0.jar"
}

if ($ClientOnly) {
    Start-Client
    return
}

# 5. Порт 8080 должен быть свободен
if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
    throw "Порт 8080 занят. Закройте программу, которая его использует (например, старый Tomcat), и запустите скрипт снова."
}

# 6. Сборка сервера и деплой
Write-Host "Собираю сервер (lab_servlet)..."
Push-Location "$Lab\lab_servlet"
& $Mvn -q package
if ($LASTEXITCODE -ne 0) { throw "Ошибка сборки сервера" }
Pop-Location
Remove-Item "$Cat\webapps\lab_servlet*" -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item "$Lab\lab_servlet\target\lab_servlet.war" "$Cat\webapps\lab_servlet.war" -Force

# 7. Запуск Tomcat в отдельном окне
Write-Host "Запускаю Tomcat..."
Start-Process -FilePath "$Cat\bin\catalina.bat" -ArgumentList "run" -WorkingDirectory $Cat

$ok = $false
for ($i = 0; $i -lt 120; $i++) {
    Start-Sleep 2
    try { if ((Invoke-WebRequest "http://localhost:8080/lab_servlet/" -UseBasicParsing).StatusCode -eq 200) { $ok = $true; break } } catch {}
}
if (-not $ok) { throw "Сервер не поднялся. Смотрите окно Tomcat и $Cat\logs" }
Write-Host "Готово: http://localhost:8080/lab_servlet/  (кнопка 'Включить музыку')"
Start-Process "http://localhost:8080/lab_servlet/"

# 8. Клиент
if (-not $NoClient) { Start-Client }

Write-Host "Остановить Tomcat:  `$env:JAVA_HOME='$env:JAVA_HOME'; & '$Cat\bin\shutdown.bat'"

# Лабораторная 3 (вариант 3: музыка Моцарта): установка и запуск в VS Code

Сервлет `MusicServlet` возвращает клиенту музыкальный файл `music/mozart.wav`; браузер открывает его в проигрывателе и сразу начинает воспроизведение. Словарь (`MyServlet`) и рисунок (`ImageServlet`) тоже остались.

## 1. Что установить

| Что | Версия | Проверка в терминале VS Code |
|---|---|---|
| JDK | 17 или новее | `java -version` |
| Maven | 3.9 или новее | `mvn -version` |
| Apache Tomcat | **9.x** (не 10 и выше: там `jakarta.servlet`, код не заработает) | распаковать zip, например в `C:\tools\apache-tomcat-9.0.x` |
| VS Code + расширение **Extension Pack for Java** | | вкладка Extensions |

Если `mvn` не находится, добавьте папку `bin` Maven в `PATH` и перезапустите VS Code.

## 2. Открыть проект

`File → Open Folder…` → папка `airp/lab3`. Внутри два независимых проекта: `lab_servlet` (сервер) и `apacheclient` (клиент). Дальнейшие команды выполняйте в терминале VS Code (`Ctrl+` ` ``).

## 3. Собрать и запустить сервер

```powershell
cd lab_servlet
mvn package
copy target\lab_servlet.war C:\tools\apache-tomcat-9.0.x\webapps\
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"   # путь к вашему JDK
C:\tools\apache-tomcat-9.0.x\bin\catalina.bat run
```

Путь к Tomcat и JDK замените на свои. Tomcat работает, пока открыто окно терминала; остановка: `Ctrl+C`.

Через 5–10 секунд откройте http://localhost:8080/lab_servlet/ .

### Запуск Tomcat из VS Code кнопкой (необязательно)

Расширение **Community Server Connectors** (Red Hat): панель Servers → `+` → Download/Add server → Apache Tomcat 9 → правый клик по серверу → `Add Deployment` → выбрать `lab_servlet/target/lab_servlet.war` → `Start Server`.

## 4. Проверка

| Действие | Ожидаемо |
|---|---|
| «привет» → Перевести | `привет - hello` |
| `book` → Перевести обратно | `book - книга` |
| «мышь» | «Слово «мышь» не найдено» |
| «Показать рисунок» | картинка, адрес `.../ImageServlet` |
| **«Включить музыку»** | страница `.../MusicServlet`, сразу играет музыка |

Браузеры не воспроизводят звук без клика пользователя, поэтому музыка включается именно по кнопке, а не при загрузке `dictionary.html`.

## 5. Клиент (отдельный проект)

При запущенном Tomcat:

```powershell
cd apacheclient
mvn package
java -Dfile.encoding=UTF-8 -jar target\apacheclient-1.0.jar
```

Откроется окно «Словарь (клиент)».

## 6. Свой музыкальный файл

В репозитории лежит `mozart.wav` — **синтезированное** начало «Eine kleine Nachtmusik» (около 18 секунд). Для сдачи замените на настоящую запись:

1. Положите файл в `lab_servlet/src/main/webapp/music/`.
2. Для mp3: в `MusicServlet.java` поменяйте путь на `/music/mozart.mp3` и тип на `audio/mpeg`.
3. Пересоберите (`mvn package`), заново скопируйте WAR в `webapps` и перезапустите Tomcat.

## 7. Если не работает

| Симптом | Решение |
|---|---|
| 404 на `/lab_servlet/` | WAR не лежит в `webapps` или не развернулся: смотрите `logs/catalina.*.log` |
| 404 на `/MusicServlet` | старый WAR: пересоберите и скопируйте заново, перезапустите Tomcat |
| Порт 8080 занят | закройте другую программу на 8080 или смените порт в `conf/server.xml` (и в `ApacheClient.java`) |
| `mvn` не найден | Maven не в `PATH` |
| Клиент: «Connection refused» | Tomcat не запущен |
| `mvn package` падает: файл занят | остановите Tomcat/Jetty, который держит `target\lab_servlet\WEB-INF\lib` |
| Музыка не играет | проверьте, что звук включён и нажата кнопка; откройте `.../MusicServlet` напрямую |

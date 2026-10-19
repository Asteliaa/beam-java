# Лабораторная 1: как установить и запустить на ноутбуке (Windows)

Все файлы лабы уже в репозитории: код, `medicines.txt`, html-описания (`source_html`), PDF-описания (`source_pdf`), библиотеки PDFBox (`lib`). Ничего докачивать вручную не нужно, кроме Java.

## 1. Установить Java (один раз)

Откройте PowerShell и проверьте:

```powershell
javac -version
```

Если пишет «не найдено» или версия ниже 17, установите JDK 17:

```powershell
winget install -e --id EclipseAdoptium.Temurin.17.JDK
```

Закройте PowerShell и откройте новый, снова выполните `javac -version`.

## 2. Создать папку и склонировать репозиторий

```powershell
mkdir C:\labs
cd C:\labs
git clone -b lab1-setup-and-mcdonalds https://github.com/Asteliaa/beam-java.git
cd beam-java\airp\lab1
```

После принятия PR в `main` вместо этого достаточно `git clone https://github.com/KsushaKhadzhinova/beam-java.git` и `cd beam-java\airp\lab1`.

Если git не установлен: `winget install -e --id Git.Git`, затем новый PowerShell. Либо скачайте zip: https://github.com/Asteliaa/beam-java/archive/refs/heads/lab1-setup-and-mcdonalds.zip, распакуйте в `C:\labs` и откройте папку `beam-java-lab1-setup-and-mcdonalds\airp\lab1`.

## 3. Собрать и запустить (поиск лекарства)

**Запускать нужно из папки `airp\lab1`**: программа ищет `medicines.txt`, `source_html` и `source_pdf` рядом с местом запуска.

```powershell
javac -encoding UTF-8 -cp "lib/*" -d out src/web_res_lab/Web_res_lab.java src/web_res_lab/McDonalds.java
java -Dfile.encoding=UTF-8 -cp "out;lib/*" web_res_lab.Web_res_lab
```

Откроется окно «My Window».
1. Введите от 1 до 3 симптомов через запятую, например `головная боль, высокая температура`.
2. Нажмите **Search**: покажутся счёты по каждому лекарству, откроется html-описание найденного.
3. Нажмите **PDF**: PDF найденного лекарства показывается в отдельном окне программы (сначала нужен Search).
4. Нажмите **HTML**: строится `result.html` с таблицей результатов и открывается в браузере.

## 4. Дополнительное задание: чизбургер с сайта McDonald's

Программа заходит на сайт, читает html по URL, ищет теги, парсит цену и картинку чизбургера и записывает их в `cheeseburger.html`, который открывается в браузере.

```powershell
java -Dfile.encoding=UTF-8 -cp "out;lib/*" web_res_lab.McDonalds
```

По умолчанию читается `https://mcdonalds.by/menu`. Если у чизбургера своя страница, передайте её адрес:

```powershell
java -Dfile.encoding=UTF-8 -cp "out;lib/*" web_res_lab.McDonalds https://mcdonalds.by/адрес-страницы-чизбургера
```

В консоли будут строки «Цена: …» и «Картинка: …». Если консоль показывает «кракозябры», выполните перед запуском `chcp 65001`.

Как это устроено (для защиты):
- страница читается через `URL` и `URLConnection` в `ArrayList<String>` (как в основной лабе);
- ищется слово «Чизбургер» (без «Двойной»), берётся кусок html вокруг него;
- в нём регулярными выражениями находятся тег `<img ... src="...">` (картинка) и число с валютой (цена, `BYN`/`руб.`); если их там нет, берутся мета-теги `og:image`, `product:price:amount`, `itemprop="price"`;
- результат пишется в `cheeseburger.html`.

**Важно:** разметка реального сайта мной не проверялась (с моего компьютера `mcdonalds.by` не открывался). Логика проверена на тестовой странице. Если на вашем ноутбуке цена или картинка не нашлись, откройте страницу в браузере, нажмите F12 и посмотрите, как оформлены цена и картинка чизбургера: подстроить регулярные выражения нужно в методах `findPrice` и `findImage` файла `src/web_res_lab/McDonalds.java`.

## 5. Если не работает

| Симптом | Решение |
|---|---|
| `javac` не найден | JDK не установлен или не в PATH: шаг 1, затем новый PowerShell |
| Ошибка «package org.apache.pdfbox does not exist» | не указан `-cp "lib/*"`, или запуск не из `airp\lab1` |
| На Mac/Linux | в `-cp` вместо `;` использовать `:` |
| «Сначала выполните поиск» | кнопки PDF и HTML работают только после Search |
| «Не удалось открыть PDF» | нет папки `source_pdf` или PDF с именем лекарства; запускайте из `airp\lab1` |
| Нет окна / ошибка Desktop | нужна обычная Windows с экраном, не WSL |
| McDonalds: «Connection timed out» | сайт недоступен из вашей сети: попробуйте с другим интернетом или VPN |

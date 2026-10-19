package web_res_lab;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Дополнительное задание к лабораторной 1: зайти на сайт McDonald's (mcdonalds.by),
// найти чизбургер, получить теги страницы, распарсить цену и картинку
// и вывести их в html-файл cheeseburger.html.
//
// Как работает:
// 1. Страница читается по URL (URL -> URLConnection -> BufferedReader), как в основной лабе.
// 2. Строки страницы собираются в ArrayList<String> и склеиваются в один html-текст.
// 3. Ищется слово "Чизбургер" (но не "Двойной чизбургер" и т.п.), берётся кусок html вокруг него.
// 4. В этом куске регулярными выражениями находятся теги: <img ... src="..."> (картинка)
//    и число с валютой (цена, например 4.50 BYN). Если в куске нет, берутся мета-теги
//    страницы: og:image, product:price:amount, itemprop="price".
// 5. Результат записывается в cheeseburger.html: цена и картинка.
//
// Запуск (из папки airp/lab1):
//   java -cp "out;lib/*" web_res_lab.McDonalds [URL страницы чизбургера или меню]
// Без аргумента берётся https://mcdonalds.by/menu
public class McDonalds {

    static final String DEFAULT_URL = "https://mcdonalds.by/menu";

    // Чтение html-страницы по адресу
    static String download(String address) throws IOException {
        URLConnection connection = new URL(address).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");     // сайт может отдавать страницу только браузеру
        connection.setRequestProperty("Accept-Language", "ru");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        ArrayList<String> lines = new ArrayList<>();                    // буфер строк страницы
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return String.join("\n", lines);
    }

    // Позиция слова "Чизбургер" как отдельного блюда (без "Двойной", "Дабл" и т.п. перед ним)
    static int findCheeseburger(String html) {
        Matcher m = Pattern.compile("(?<![Дд]войной\\s)(?<![Дд]абл\\s)(?<![Бб]ольшой\\s)[Чч]избургер|[Cc]heeseburger").matcher(html);
        while (m.find()) {
            String before = html.substring(Math.max(0, m.start() - 12), m.start()).toLowerCase();
            if (before.contains("двойной") || before.contains("дабл") || before.contains("double")) {
                continue;
            }
            return m.start();
        }
        return -1;
    }

    // Первый тег <img>, src которого указывает на картинку; относительный адрес превращаем в полный
    static String findImage(String chunk, String base) {
        Matcher m = Pattern.compile("<img[^>]+?(?:data-src|src)\\s*=\\s*[\"']([^\"']+\\.(?:png|jpe?g|webp)[^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(chunk);
        if (m.find()) {
            return absolute(m.group(1), base);
        }
        m = Pattern.compile("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(chunk);
        return m.find() ? absolute(m.group(1), base) : null;
    }

    // Цена: число рядом с валютой (BYN, руб, р.) либо мета-тег цены
    static String findPrice(String chunk) {
        Matcher m = Pattern.compile("(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(?:&nbsp;|\\s)?(BYN|byn|Br|руб\\.?|р\\.|₽|&#8381;)").matcher(chunk);
        if (m.find()) {
            return m.group(1).replace(',', '.') + " " + m.group(2);
        }
        m = Pattern.compile("(?:product:price:amount|itemprop=[\"']price[\"'])[^>]*content=[\"']([\\d.,]+)[\"']", Pattern.CASE_INSENSITIVE).matcher(chunk);
        if (m.find()) {
            return m.group(1).replace(',', '.');
        }
        m = Pattern.compile("[\"']price[\"']\\s*:\\s*[\"']?(\\d{1,4}(?:[.,]\\d{1,2})?)").matcher(chunk);  // цена в json внутри страницы
        return m.find() ? m.group(1).replace(',', '.') : null;
    }

    static String absolute(String src, String base) {
        try {
            return new URL(new URL(base), src).toString();
        } catch (IOException e) {
            return src;
        }
    }

    // Страница меню содержит ссылку на страницу чизбургера: ищем её в тегах <a href>
    static String findProductLink(String html, String base) {
        Matcher m = Pattern.compile("<a[^>]+href\\s*=\\s*[\"']([^\"']*(?:cheeseburger|chizburger|cheese-burger)[^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
        while (m.find()) {
            if (!m.group(1).toLowerCase().contains("double") && !m.group(1).toLowerCase().contains("dabl")) {
                return absolute(m.group(1), base);
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        String address = args.length > 0 ? args[0] : DEFAULT_URL;
        System.out.println("Читаю страницу: " + address);
        String html = download(address);
        System.out.println("Получено символов: " + html.length());

        // Если это страница меню, переходим на страницу самого чизбургера (если есть ссылка)
        String link = findProductLink(html, address);
        if (link != null && !link.equals(address)) {
            System.out.println("Страница чизбургера: " + link);
            address = link;
            html = download(address);
        }

        // Берём кусок html вокруг слова "Чизбургер", в нём лежат нужные теги
        int pos = findCheeseburger(html);
        String chunk = pos < 0 ? html : html.substring(Math.max(0, pos - 1500), Math.min(html.length(), pos + 2500));
        String price = findPrice(chunk);
        String image = findImage(chunk, address);
        if (price == null) price = findPrice(html);
        if (image == null) image = findImage(html, address);
        System.out.println("Цена: " + price);
        System.out.println("Картинка: " + image);

        // Результат: html-страница с ценой и картинкой
        ArrayList<String> out = new ArrayList<>();
        out.add("<!DOCTYPE html>");
        out.add("<html><head><meta charset=\"UTF-8\"><title>Чизбургер</title></head>");
        out.add("<body bgcolor=\"#aaccff\">");
        out.add("<h1>Чизбургер</h1>");
        out.add("<h2>Цена: " + (price == null ? "не найдена" : price) + "</h2>");
        if (image != null) {
            out.add("<img src=\"" + image + "\" alt=\"Чизбургер\" width=\"400\">");
        } else {
            out.add("<p>Картинка не найдена</p>");
        }
        out.add("<p>Источник: <a href=\"" + address + "\">" + address + "</a></p>");
        out.add("</body></html>");
        File file = new File("cheeseburger.html");
        try (PrintWriter w = new PrintWriter(file, "UTF-8")) {
            for (String line : out) {
                w.println(line);
            }
        }
        System.out.println("Записано: " + file.getAbsolutePath());
        try {
            java.awt.Desktop.getDesktop().browse(file.getAbsoluteFile().toURI());   // открыть в браузере
        } catch (Exception e) {
            System.out.println("Откройте файл вручную: " + file.getAbsolutePath());
        }
    }
}

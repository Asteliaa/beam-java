import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Разбор html-страницы сайта McDonald's (mcdonalds.by): из тегов страницы
// достаются цена и картинка чизбургера.
public class CheeseburgerParser {

    // Чтение html-страницы по адресу: URL -> URLConnection -> BufferedReader -> ArrayList строк
    public static String download(String address) throws IOException {
        URLConnection connection = new URL(address).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");   // сайт может отдавать страницу только браузеру
        connection.setRequestProperty("Accept-Language", "ru");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return String.join("\n", lines);
    }

    // Позиция слова "Чизбургер" как отдельного блюда (не "Двойной чизбургер")
    public static int findCheeseburger(String html) {
        Matcher m = Pattern.compile("[Чч]избургер|[Cc]heeseburger").matcher(html);
        while (m.find()) {
            String before = html.substring(Math.max(0, m.start() - 12), m.start()).toLowerCase();
            if (before.contains("двойной") || before.contains("дабл") || before.contains("double")) {
                continue;
            }
            return m.start();
        }
        return -1;
    }

    // Первый тег <img> с картинкой; относительный адрес превращается в полный
    public static String findImage(String chunk, String base) {
        Matcher m = Pattern.compile("<img[^>]+?(?:data-src|src)\\s*=\\s*[\"']([^\"']+\\.(?:png|jpe?g|webp)[^\"']*)[\"']",
                Pattern.CASE_INSENSITIVE).matcher(chunk);
        if (m.find()) {
            return absolute(m.group(1), base);
        }
        m = Pattern.compile("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE).matcher(chunk);
        return m.find() ? absolute(m.group(1), base) : null;
    }

    // Цена: число рядом с валютой (BYN, руб, р.) либо мета-тег или поле price в json страницы
    public static String findPrice(String chunk) {
        Matcher m = Pattern.compile("(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(?:&nbsp;|\\s)?(BYN|byn|Br|руб\\.?|р\\.|₽|&#8381;)").matcher(chunk);
        if (m.find()) {
            return m.group(1).replace(',', '.') + " " + m.group(2);
        }
        m = Pattern.compile("(?:product:price:amount|itemprop=[\"']price[\"'])[^>]*content=[\"']([\\d.,]+)[\"']",
                Pattern.CASE_INSENSITIVE).matcher(chunk);
        if (m.find()) {
            return m.group(1).replace(',', '.');
        }
        m = Pattern.compile("[\"']price[\"']\\s*:\\s*[\"']?(\\d{1,4}(?:[.,]\\d{1,2})?)").matcher(chunk);
        return m.find() ? m.group(1).replace(',', '.') : null;
    }

    // Ссылка на страницу чизбургера в тегах <a href> страницы меню
    public static String findProductLink(String html, String base) {
        Matcher m = Pattern.compile("<a[^>]+href\\s*=\\s*[\"']([^\"']*(?:cheeseburger|chizburger|cheese-burger)[^\"']*)[\"']",
                Pattern.CASE_INSENSITIVE).matcher(html);
        while (m.find()) {
            String h = m.group(1).toLowerCase();
            if (!h.contains("double") && !h.contains("dabl")) {
                return absolute(m.group(1), base);
            }
        }
        return null;
    }

    static String absolute(String src, String base) {
        try {
            return new URL(new URL(base), src).toString();
        } catch (IOException e) {
            return src;
        }
    }

    // Результат разбора: {цена, картинка, адрес страницы, из которой всё взято}
    public static String[] parse(String html, String address) {
        String price = null;
        String image = null;
        int pos = findCheeseburger(html);
        // берём кусок html вокруг слова "Чизбургер": в нём лежат нужные теги
        String chunk = pos < 0 ? html : html.substring(Math.max(0, pos - 1500), Math.min(html.length(), pos + 2500));
        price = findPrice(chunk);
        image = findImage(chunk, address);
        if (price == null) price = findPrice(html);
        if (image == null) image = findImage(html, address);
        return new String[]{price, image, address};
    }

    // Полный путь: скачать страницу; если это меню - перейти на страницу чизбургера; разобрать теги
    public static String[] fetch(String address) throws IOException {
        String html = download(address);
        String link = findProductLink(html, address);
        if (link != null && !link.equals(address)) {
            address = link;
            html = download(address);
        }
        return parse(html, address);
    }
}

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Дополнительное задание: сервлет заходит на сайт McDonald's (mcdonalds.by) по url,
// парсит теги страницы и возвращает клиенту html с ценой и картинкой чизбургера.
// Адрес берётся из параметра url, например: /McDonaldsServlet?url=https://mcdonalds.by/menu
public class McDonaldsServlet extends HttpServlet {

    static final String DEFAULT_URL = "https://mcdonalds.by/menu";

    // Только сам домен mcdonalds.by / mcdonalds.com и их поддомены
    private static boolean isMcDonaldsHost(String host) {
        return host.equals("mcdonalds.by") || host.endsWith(".mcdonalds.by")
                || host.equals("mcdonalds.com") || host.endsWith(".mcdonalds.com");
    }

    // Экранирование текста для вставки в html
    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String address = request.getParameter("url");
        if (address == null || address.trim().isEmpty()) {
            address = DEFAULT_URL;
        }
        address = address.trim();

        String price = null;
        String image = null;
        String source = address;
        String error = null;
        try {
            // Сервлет ходит только на сайты McDonald's (чтобы его нельзя было использовать для чужих адресов)
            URL u = new URL(address);
            String host = u.getHost().toLowerCase();
            if (!(u.getProtocol().equals("http") || u.getProtocol().equals("https")) || !isMcDonaldsHost(host)) {
                error = "Разрешены только адреса сайта McDonald's (например, https://mcdonalds.by/menu)";
            } else {
                String[] r = CheeseburgerParser.fetch(address);
                price = r[0];
                image = r[1];
                source = r[2];
            }
        } catch (IOException e) {
            error = "Не удалось получить страницу: " + e.getMessage();
        }

        out.println("<html>");
        out.println("<head>");
        out.println("<title>Servlet McDonaldsServlet</title>");
        out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        out.println("</head>");
        out.println("<body bgcolor='#aaccff'>");
        out.println("<h1>Чизбургер</h1>");
        if (error != null) {
            out.println("<h2>" + esc(error) + "</h2>");
        } else {
            out.println("<h2>Цена: " + (price == null ? "не найдена" : esc(price)) + "</h2>");
            if (image != null) {
                out.println("<img src=\"" + esc(image) + "\" alt=\"Чизбургер\" width=\"400\"><br>");
            } else {
                out.println("<p>Картинка не найдена</p>");
            }
            out.println("<p>Источник: <a href=\"" + esc(source) + "\">" + esc(source) + "</a></p>");
        }
        out.println("<br><a href=\"dictionary.html\">Назад</a>");
        out.println("</body>");
        out.println("</html>");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}

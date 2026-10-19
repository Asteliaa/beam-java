import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Вариант 3: сервлет возвращает на сторону клиента музыкальный файл (Моцарт),
// браузер сам открывает его в проигрывателе и начинает воспроизведение
public class MusicServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Музыкальный файл находится в каталоге приложения
        InputStream in = getServletContext().getResourceAsStream("/music/mozart.wav");
        if (in == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Музыкальный файл не найден");
            return;
        }

        // Тип содержимого: браузер сам проигрывает аудио
        response.setContentType("audio/wav");
        response.setHeader("Content-Disposition", "inline; filename=\"mozart.wav\"");
        OutputStream out = response.getOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        in.close();
        out.close();
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

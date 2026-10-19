package web_res_lab;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

// Вариант 2: поиск названия лекарства по 1-3 симптомам.
//
// 1. Назначения буфера (3): а) выравнивание скорости медленного диска и быстрой
//    оперативной памяти; б) асинхронный ввод-вывод; в) кэширование данных.
//    В программе буфер - BufferedReader (readLines) и StringBuilder-подобный список строк.
// 2. Массив, ArrayList и List: массив (String[]) имеет фиксированный размер;
//    ArrayList - класс-коллекция, размер растёт сам; List - интерфейс (контракт),
//    который реализует ArrayList. Термин "список" - как в Прологе.
//    Здесь: String[] keywords (массив) и ArrayList<String> (список строк файла и строк html).
// 3. Куча и стек: локальные переменные (bestCount, keywords-ссылка) лежат в стеке потока;
//    объекты (ArrayList, String, окно) - в куче, ими управляет сборщик мусора.
// 4. Кэш - быстрая память процессора между ним и оперативной памятью.
// 5. URL - адрес ресурса (протокол, хост, порт, путь); здесь протокол file:.
// 6. Слушатель (Listener): ActionListener получает события кнопок.
// Список лекарств и симптомов находится в текстовом файле medicines.txt,
// описания лекарств (html-документы) находятся в папке source_html,
// pdf-описания - в папке source_pdf (кнопка PDF показывает их в отдельном окне, библиотека PDFBox из папки lib).
public class Web_res_lab extends Frame implements ActionListener {

    // Элементы окна: кнопки и поле для ввода симптомов / вывода результата
    Button exitButton = new Button("Exit");
    Button searchButton = new Button("Search");
    Button pdfButton = new Button("PDF");  // показывает PDF-описание найденного лекарства в окне
    Button htmlButton = new Button("HTML"); // строит html-страницу с результатами и открывает её
    String lastFound = null;               // название последнего найденного лекарства
    ArrayList<String> lastRows = new ArrayList<>(); // строки "название : счёт" последнего поиска
    TextArea textArea = new TextArea();

    public Web_res_lab() {
        super("My Window");  // заголовок окна
        // Дальше - построение окна: размер, цвет, положение кнопок и текстового поля
        setLayout(null);
        setBackground(new Color(150, 200, 100));
        setSize(450, 250);

        exitButton.setBounds(110, 190, 100, 20);
        exitButton.addActionListener(this);   // нажатие кнопки -> actionPerformed()
        add(exitButton);

        searchButton.setBounds(110, 165, 100, 20);
        searchButton.addActionListener(this);
        add(searchButton);

        pdfButton.setBounds(220, 165, 100, 20);
        pdfButton.addActionListener(this);
        add(pdfButton);

        htmlButton.setBounds(330, 165, 100, 20);
        htmlButton.addActionListener(this);
        add(htmlButton);

        textArea.setBounds(20, 50, 300, 100);
        add(textArea);

        setLocationRelativeTo(null);
        setVisible(true);  // показать окно

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {  // закрытие окна крестиком
                System.exit(0);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent ae) {  // сюда приходят нажатия обеих кнопок
        if (ae.getSource() == exitButton) {
            System.exit(0);
        } else if (ae.getSource() == htmlButton) {
            // Генерация html-страницы с результатами через ArrayList и открытие в браузере
            if (lastRows.isEmpty()) {
                textArea.append("\nСначала выполните поиск (Search)");
            } else {
                openInBrowser(writeResultPage());
            }
        } else if (ae.getSource() == pdfButton) {
            // PDF лежит в source_pdf под тем же именем, что и html-описание
            if (lastFound == null) {
                textArea.append("\nСначала выполните поиск (Search)");
            } else {
                showPdf(new File("source_pdf", lastFound + ".pdf"));
            }
        } else if (ae.getSource() == searchButton) {
            // Ключевые слова (симптомы) разделяются запятыми
            String[] keywords = textArea.getText().split(",");
            textArea.setText("");

            // Список лекарств читается из текстового файла
            File medicinesFile = new File("medicines.txt");
            ArrayList<String> medicines = readLines(medicinesFile);

            // Ищем лекарство с наибольшим числом совпавших симптомов
            String bestName = null;
            int bestCount = 0;
            lastRows = new ArrayList<>();  // новый поиск - очищаем прошлые результаты
            for (String line : medicines) {
                // Формат строки: Название | симптом1, симптом2, ...
                String[] parts = line.split("\\|");  // parts[0] - название, parts[1] - симптомы
                if (parts.length < 2) {
                    continue;
                }
                String name = parts[0].trim();
                int coincidenceCount = testSymptoms(parts[1], keywords);
                textArea.append("\n" + name + "  :" + coincidenceCount);  // счёт по каждому лекарству
                lastRows.add(name + "|" + coincidenceCount);  // запоминаем для html-страницы
                if (coincidenceCount > bestCount) {
                    bestCount = coincidenceCount;
                    bestName = name;
                }
            }

            lastFound = bestName;  // запоминаем результат для кнопки PDF
            if (bestName == null) {
                textArea.append("\nЛекарство не найдено");
            } else {
                textArea.append("\n\nЛекарство: " + bestName);
                openInBrowser(findHtml(bestName));  // открыть описание победителя
            }
        }
    }

    // Генерация html-страницы: каждая строка страницы - элемент ArrayList<String>,
    // затем список целиком записывается в файл result.html
    private File writeResultPage() {
        ArrayList<String> html = new ArrayList<>();   // тут собирается страница строка за строкой
        html.add("<!DOCTYPE html>");
        html.add("<html><head><meta charset=\"UTF-8\"><title>Результаты поиска</title></head>");
        html.add("<body bgcolor=\"#aaccff\">");
        html.add("<h1>Результаты поиска</h1>");
        html.add("<table border=\"1\" cellpadding=\"6\"><tr><th>Лекарство</th><th>Совпадений</th></tr>");
        for (String row : lastRows) {                 // по строке таблицы на каждое лекарство
            String[] p = row.split("\\|");
            html.add("<tr><td>" + p[0] + "</td><td>" + p[1] + "</td></tr>");
        }
        html.add("</table>");
        if (lastFound != null) {                      // ссылки на описание победителя
            html.add("<h2>Лекарство: " + lastFound + "</h2>");
            html.add("<p><a href=\"source_html/" + lastFound + ".html\">Описание (html)</a> | "
                    + "<a href=\"source_pdf/" + lastFound + ".pdf\">Описание (pdf)</a></p>");
        } else {
            html.add("<h2>Лекарство не найдено</h2>");
        }
        html.add("</body></html>");

        File out = new File("result.html");
        try (PrintWriter writer = new PrintWriter(out, "UTF-8")) {
            for (String line : html) {                // запись списка в файл
                writer.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return out;
    }
// Чтение строк файла через URL и потоковые классы
    private ArrayList<String> readLines(File file) {
        ArrayList<String> lines = new ArrayList<>();
        try {
            URL url = file.toURI().toURL();                  // файл -> URL вида file:/...
            URLConnection connection = url.openConnection(); // соединение с ресурсом
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.startsWith("#")) {  // пропускаем пустые строки и комментарии
                        lines.add(line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return lines;
    }

    // Число ключевых слов, найденных среди симптомов лекарства
    private int testSymptoms(String symptoms, String[] keywords) {
        int results = 0;
        String content = symptoms.toLowerCase();  // регистр не учитываем
        for (String keyword : keywords) {
            // +1, если симптом из ввода есть в списке симптомов лекарства
            if (!keyword.trim().isEmpty() && content.contains(keyword.trim().toLowerCase())) {
                results++;
            }
        }
        return results;
    }

    // Поиск html-описания среди всех файлов папки source_html
    // (как в методичке: listFiles() -> ArrayList<File>)
    private File findHtml(String name) {
        File[] all = new File("source_html").listFiles();   // список файлов папки (null, если папки нет)
        ArrayList<File> files = all == null ? new ArrayList<File>() : new ArrayList<>(Arrays.asList(all));
        for (File f : files) {
            if (f.getName().equals(name + ".html")) {
                return f;
            }
        }
        return new File("source_html", name + ".html");
    }

    // Открытие html-документа в программе по умолчанию (браузер)
    private void openInBrowser(File file) {
        try {
            // запуск браузера по умолчанию
            Desktop.getDesktop().browse(file.getAbsoluteFile().toURI());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Показ PDF в отдельном окне программы: первая страница рисуется библиотекой PDFBox
    private void showPdf(File pdf) {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
            java.awt.image.BufferedImage img =
                    new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0, 90);
            javax.swing.JFrame f = new javax.swing.JFrame(pdf.getName());
            f.add(new javax.swing.JScrollPane(new javax.swing.JLabel(new javax.swing.ImageIcon(img))));
            f.setSize(700, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        } catch (IOException e) {
            textArea.append("\nНе удалось открыть PDF: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Web_res_lab();  // точка входа: создаём окно
    }
}

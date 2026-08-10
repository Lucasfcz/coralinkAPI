package io.github.lucasfcz.coralink.sources;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HtmlDebugger {

    private static final String URL =
            """
               https://upe.br/noticias/
            """;

    public static void main(String[] args) throws Exception {
        debug(URL);
    }

    private static void debug(String url) throws IOException {

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get();

        Path folder = Path.of("debug");

        Files.createDirectories(folder);

        // HTML completo
        Files.writeString(folder.resolve("page.html"), doc.outerHtml());

        // Informações básicas
        System.out.println("==================================");
        System.out.println("Título:");
        System.out.println(doc.title());
        System.out.println();

        System.out.println("URL:");
        System.out.println(url);
        System.out.println();

        System.out.println("HTML salvo em:");
        System.out.println(folder.resolve("page.html").toAbsolutePath());
        System.out.println("==================================");

        inspect(doc, "article");
        inspect(doc, "div");
        inspect(doc, "section");
        inspect(doc, "li");
        inspect(doc, "a");
        inspect(doc, "h1");
        inspect(doc, "h2");
        inspect(doc, "h3");
        inspect(doc, "img");
    }

    private static void inspect(Document doc, String selector) throws IOException {

        var elements = doc.select(selector);

        System.out.printf("%-10s -> %d%n", selector, elements.size());

        if (elements.isEmpty()) {
            return;
        }

        Path folder = Path.of("debug", selector);

        Files.createDirectories(folder);

        int limit = Math.min(10, elements.size());

        for (int i = 0; i < limit; i++) {

            Element e = elements.get(i);

            Files.writeString(
                    folder.resolve(selector + "-" + (i + 1) + ".html"),
                    e.outerHtml()
            );
        }
    }
}
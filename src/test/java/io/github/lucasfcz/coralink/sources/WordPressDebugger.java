package io.github.lucasfcz.coralink.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WordPressDebugger {

    private static final String API_URL =
            """
               https://upe.br/wp-json/wp/v2/posts
            """;


    private static final int TIMEOUT_MILLIS = 15000;

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    public static void main(String[] args) throws Exception {
        debug(API_URL);
    }

    private static void debug(String endpoint) throws IOException {

        String json = Jsoup.connect(endpoint)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MILLIS)
                .userAgent("CoralinkBot/1.0 (+https://github.com/lucasfcz/coralink)")
                .execute()
                .body();

        Path folder = Path.of("debug", "wordpress");

        Files.createDirectories(folder);

        // JSON completo
        Files.writeString(folder.resolve("response.json"), json);

        JsonNode posts = OBJECT_MAPPER.readTree(json);

        System.out.println("==================================");
        System.out.println("WordPress Debugger");
        System.out.println("==================================");

        System.out.println("Endpoint:");
        System.out.println(endpoint);
        System.out.println();

        System.out.println("JSON salvo em:");
        System.out.println(
                folder.resolve("response.json").toAbsolutePath()
        );
        System.out.println();

        if (!posts.isArray()) {
            System.out.println("ERRO: resposta não é um array.");
            return;
        }

        System.out.println("Quantidade de posts:");
        System.out.println(posts.size());
        System.out.println();

        inspectPosts(posts, folder);

        System.out.println("==================================");
    }

    private static void inspectPosts(
            JsonNode posts,
            Path folder
    ) throws IOException {

        int limit = Math.min(10, posts.size());

        for (int i = 0; i < limit; i++) {

            JsonNode post = posts.get(i);

            System.out.println("----------------------------------");
            System.out.println("POST " + (i + 1));
            System.out.println("----------------------------------");

            String id = post.path("id").asText();

            String title = post
                    .path("title")
                    .path("rendered")
                    .asText();

            String slug = post
                    .path("slug")
                    .asText();

            String url = post
                    .path("link")
                    .asText();

            String date = post
                    .path("date")
                    .asText();

            String excerpt = post
                    .path("excerpt")
                    .path("rendered")
                    .asText();

            String content = post
                    .path("content")
                    .path("rendered")
                    .asText();

            String imageUrl = extractFeaturedImage(post);

            System.out.println("ID: " + id);
            System.out.println("Título: " + cleanHtml(title));
            System.out.println("Slug: " + slug);
            System.out.println("URL: " + url);
            System.out.println("Data: " + date);
            System.out.println("Imagem: " + imageUrl);

            System.out.println();
            System.out.println("Resumo:");
            System.out.println(cleanHtml(excerpt));

            System.out.println();
            System.out.println("Conteúdo:");
            System.out.println(cleanHtml(content));

            System.out.println();

            savePostDebug(
                    folder,
                    i + 1,
                    post
            );
        }
    }

    private static String extractFeaturedImage(JsonNode post) {

        JsonNode media = post
                .path("_embedded")
                .path("wp:featuredmedia");

        if (!media.isArray() || media.isEmpty()) {
            return null;
        }

        String sourceUrl = media
                .get(0)
                .path("source_url")
                .asText();

        return sourceUrl.isBlank()
                ? null
                : sourceUrl;
    }

    private static String cleanHtml(String html) {

        if (html == null || html.isBlank()) {
            return "";
        }

        return Jsoup.parse(html).text();
    }

    private static void savePostDebug(
            Path folder,
            int index,
            JsonNode post
    ) throws IOException {

        Files.writeString(
                folder.resolve("post-" + index + ".json"),
                post.toPrettyString()
        );
    }
}

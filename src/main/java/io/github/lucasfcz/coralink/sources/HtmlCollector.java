package io.github.lucasfcz.coralink.sources;

import io.github.lucasfcz.coralink.dto.NewsSummary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public abstract class HtmlCollector extends AbstractCollector {

    protected HtmlCollector(SourceTypeDetector detector) {
        super(detector);
    }

    protected abstract String pageUrl();
    protected abstract List<Element> articles(Document document);
    protected abstract NewsSummary mapArticle(Element element);

    @Override
    protected List<NewsSummary> collectHtml(){
        try {
            Document doc = Jsoup.connect(pageUrl()).get();

            return articles(doc)
                    .stream()
                    .map(this::mapArticle)
                    .filter(Objects::nonNull)
                    .toList();

        } catch(IOException e){
            throw new RuntimeException("HTML scraping failed", e);
        }
    }
}
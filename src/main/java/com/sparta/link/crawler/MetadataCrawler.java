package com.sparta.link.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class MetadataCrawler {

    public LinkMetadata crawl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            String title = getMetaContent(doc, "og:title");
            if (title.isBlank()) title = doc.title();

            String description = getMetaContent(doc, "og:description");
            String favicon = extractFavicon(doc, url);

            return new LinkMetadata(title, description, favicon);
        } catch (IOException e) {
            return new LinkMetadata("", "", "");
        }
    }

    private String getMetaContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=" + property + "]");
        if (meta != null) return meta.attr("content");
        return "";
    }

    private String extractFavicon(Document doc, String url) {
        Element icon = doc.selectFirst("link[rel~=icon]");
        if (icon != null) return icon.absUrl("href");
        // 기본 favicon 경로 시도
        try {
            URI uri = new URI(url);
            return uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";
        } catch (Exception e) {
            return "";
        }
    }

    public record LinkMetadata(String title, String description, String favicon) {}
}

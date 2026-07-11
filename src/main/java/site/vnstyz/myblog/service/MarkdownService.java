package site.vnstyz.myblog.service;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * Markdown 渲染服务，将 Markdown 源码转换为安全的 HTML。
 * 渲染后再经过 jsoup 白名单清洗，防止存储型 XSS。
 */
@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownService() {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder()
                .escapeHtml(true)
                .build();
        // 允许常见富文本标签，同时限制链接/图片协议，禁止脚本与事件属性
        this.safelist = Safelist.relaxed()
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https");
    }

    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String rawHtml = renderer.render(parser.parse(markdown));
        return Jsoup.clean(rawHtml, safelist);
    }
}

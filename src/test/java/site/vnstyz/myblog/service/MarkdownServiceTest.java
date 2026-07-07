package site.vnstyz.myblog.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownServiceTest {

    private final MarkdownService markdownService = new MarkdownService();

    @Test
    void renderHeadingAndParagraph() {
        String markdown = "# Hello\n\nThis is a paragraph.";
        String html = markdownService.renderToHtml(markdown);
        assertTrue(html.contains("<h1>Hello</h1>"));
        assertTrue(html.contains("<p>This is a paragraph.</p>"));
    }

    @Test
    void renderCodeBlock() {
        String markdown = "```java\nSystem.out.println(\"hi\");\n```";
        String html = markdownService.renderToHtml(markdown);
        assertTrue(html.contains("<pre>"));
        assertTrue(html.contains("<code>"));
    }

    @Test
    void escapeRawHtmlToPreventXss() {
        String markdown = "<script>alert('xss')</script>";
        String html = markdownService.renderToHtml(markdown);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void returnEmptyStringForNullOrBlank() {
        assertEquals("", markdownService.renderToHtml(null));
        assertEquals("", markdownService.renderToHtml("   "));
    }
}

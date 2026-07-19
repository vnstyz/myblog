package site.vnstyz.myblog.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Year;

/**
 * 全局模型属性：为所有页面（含错误页、后台页）统一注入站点级可配置变量，
 * 避免在各模板中硬编码邮箱、年份、站点名，便于统一维护与复用页脚片段。
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final String siteName;
    private final String siteEmail;

    public GlobalModelAdvice(
            @Value("${blog.site.name:MyBlog}") String siteName,
            @Value("${blog.site.email:johnsmithvnstzy@gmail.com}") String siteEmail) {
        this.siteName = siteName;
        this.siteEmail = siteEmail;
    }

    /** 站点名称，默认 MyBlog */
    @ModelAttribute("siteName")
    public String siteName() {
        return siteName;
    }

    /** 联系邮箱，可在 application.yml 的 blog.site.email 中配置 */
    @ModelAttribute("siteEmail")
    public String siteEmail() {
        return siteEmail;
    }

    /** 版权年份，自动取当前年份，避免每年手动改模板 */
    @ModelAttribute("copyrightYear")
    public int copyrightYear() {
        return Year.now().getValue();
    }
}

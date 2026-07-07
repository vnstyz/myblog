package site.vnstyz.myblog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ErrorController {

    @GetMapping("/error/{code}")
    public String handleError(@PathVariable String code) {
        switch (code) {
            case "404":
                return "error/404";
            case "500":
                return "error/500";
            default:
                return "error/default";
        }
    }
}
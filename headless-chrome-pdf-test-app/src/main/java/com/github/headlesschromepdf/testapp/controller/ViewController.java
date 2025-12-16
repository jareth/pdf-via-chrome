package com.github.headlesschromepdf.testapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving Thymeleaf UI pages.
 */
@Controller
public class ViewController {

    /**
     * Main page with HTML-to-PDF conversion form.
     *
     * @param model Spring MVC model
     * @return template name
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "Headless Chrome PDF Generator");
        return "index";
    }

    /**
     * Alternative mapping for UI.
     *
     * @param model Spring MVC model
     * @return template name
     */
    @GetMapping("/ui")
    public String ui(Model model) {
        return index(model);
    }
}

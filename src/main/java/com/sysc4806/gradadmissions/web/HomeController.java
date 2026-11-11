package com.sysc4806.gradadmissions.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the landing page and the landing page of each role section.
 * Feature pages (forms, lists, ...) belong to their own controllers and are
 * linked from the section pages once they are built.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/applicant")
    public String applicant() {
        return "applicant/index";
    }

    @GetMapping("/professor")
    public String professor() {
        return "professor/index";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin/index";
    }
}
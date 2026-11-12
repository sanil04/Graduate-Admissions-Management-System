package com.sysc4806.gradadmissions.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.sysc4806.gradadmissions.service.AdminApplicationService;

/** The admin's view of submitted applications. */
@Controller
@RequestMapping("/admin/applications")
public class AdminController {

    private final AdminApplicationService service;

    public AdminController(AdminApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("applications", service.submittedApplications());
        return "admin/applications";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("app", service.findSubmittedApplication(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "admin/application";
    }
}

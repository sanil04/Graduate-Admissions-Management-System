package com.sysc4806.gradadmissions.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.sysc4806.gradadmissions.model.Evaluation;
import com.sysc4806.gradadmissions.service.CurrentUserService;
import com.sysc4806.gradadmissions.service.ProfessorApplicationService;

/** The applications routed to the logged-in professor: list and detail. */
@Controller
@RequestMapping("/professor/applications")
public class ProfessorApplicationController {

    private final ProfessorApplicationService service;
    private final CurrentUserService currentUsers;

    public ProfessorApplicationController(ProfessorApplicationService service, CurrentUserService currentUsers) {
        this.service = service;
        this.currentUsers = currentUsers;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("evaluations", service.assignedTo(currentUsers.currentProfessor().getId()));
        return "professor/applications";
    }

    /** 404 when the application doesn't exist or wasn't routed to this professor. */
    @GetMapping("/{applicationId}")
    public String detail(@PathVariable Long applicationId, Model model) {
        Evaluation evaluation = service.findAssigned(applicationId, currentUsers.currentProfessor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("app", evaluation.getApplication());
        return "professor/application";
    }
}
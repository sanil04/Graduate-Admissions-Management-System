package com.sysc4806.gradadmissions.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sysc4806.gradadmissions.model.Evaluation;
import com.sysc4806.gradadmissions.model.Recommendation;
import com.sysc4806.gradadmissions.service.CurrentUserService;
import com.sysc4806.gradadmissions.service.ProfessorApplicationService;

/** The applications routed to the logged-in professor: list, detail and assessment. */
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
        model.addAttribute("today", LocalDate.now());
        return "professor/applications";
    }

    /** 404 when the application doesn't exist or wasn't routed to this professor. */
    @GetMapping("/{applicationId}")
    public String detail(@PathVariable Long applicationId, Model model) {
        Evaluation evaluation = service.findAssigned(applicationId, currentUsers.currentProfessor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("app", evaluation.getApplication());
        model.addAttribute("recommendations", Recommendation.values());
        model.addAttribute("today", LocalDate.now());
        return "professor/application";
    }

    /**
     * Submits the professor's assessment: one recommendation and optional comments. Rule violations
     * (nothing chosen, comments too long, already submitted, evaluation deadline passed) go back to the page as an error, with what
     * the professor typed. 404 when the application wasn't routed to this professor.
     */
    @PostMapping("/{applicationId}/assessment")
    public String submitAssessment(@PathVariable Long applicationId,
                                   @RequestParam(name = "recommendation", required = false) Recommendation recommendation,
                                   @RequestParam(name = "comments", required = false) String comments,
                                   RedirectAttributes redirect) {
        Long professorId = currentUsers.currentProfessor().getId();
        try {
            service.submitAssessment(applicationId, professorId, recommendation, comments, LocalDateTime.now());
            redirect.addFlashAttribute("message", "Your assessment was submitted.");
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            if (recommendation != null) {
                redirect.addFlashAttribute("draftRecommendation", recommendation);
            }
            if (comments != null) {
                redirect.addFlashAttribute("draftComments", comments);
            }
        }
        return "redirect:/professor/applications/" + applicationId;
    }
}

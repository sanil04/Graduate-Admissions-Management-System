package com.sysc4806.gradadmissions.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.Evaluation;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.service.AdminApplicationService;
import com.sysc4806.gradadmissions.service.NotificationService;

/** The admin's view of submitted applications: list, detail, routing to professors and screening out. */
@Controller
@RequestMapping("/admin/applications")
public class AdminController {

    private final AdminApplicationService service;
    private final NotificationService notifications;

    public AdminController(AdminApplicationService service, NotificationService notifications) {
        this.service = service;
        this.notifications = notifications;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("applications", service.submittedApplications());
        model.addAttribute("notificationStatus", notifications.status());
        return "admin/applications";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Application application = service.findSubmittedApplication(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Evaluation> evaluations = service.evaluationsFor(id);
        Set<Long> routedIds = evaluations.stream()
                .map(evaluation -> evaluation.getProfessor().getId())
                .collect(Collectors.toSet());
        Set<Long> preferredIds = application.getPreferredProfessors().stream()
                .map(Professor::getId)
                .collect(Collectors.toSet());

        model.addAttribute("app", application);
        model.addAttribute("evaluations", evaluations);
        // Professors it can still be routed to; the applicant's preferred ones are pre-selected.
        model.addAttribute("routableProfessors", service.allProfessors().stream()
                .filter(professor -> !routedIds.contains(professor.getId()))
                .toList());
        model.addAttribute("preferredIds", preferredIds);
        return "admin/application";
    }

    @PostMapping("/{id}/route")
    public String route(@PathVariable Long id,
                        @RequestParam(name = "professorIds", required = false) List<Long> professorIds,
                        RedirectAttributes redirect) {
        List<Evaluation> created;
        try {
            created = service.route(id, professorIds, LocalDateTime.now());
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/applications/" + id;
        }
        NotificationService.Result emails = notifications.notifyRouted(created, LocalDateTime.now());
        redirect.addFlashAttribute("message", "Routed to " + created.stream()
                .map(evaluation -> evaluation.getProfessor().getName())
                .collect(Collectors.joining(", ")) + ". " + describe(emails));
        return "redirect:/admin/applications/" + id;
    }

    private static String describe(NotificationService.Result emails) {
        return emails.mailConfigured()
                ? "Emails: %d sent, %d failed, %d skipped.".formatted(emails.sent(), emails.failed(), emails.skipped())
                : "No mail server is configured, so no email was sent.";
    }

    @PostMapping("/{id}/screen-out")
    public String screenOut(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.screenOut(id);
            redirect.addFlashAttribute("message", "The application was screened out.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/applications/" + id;
    }
}

package com.sysc4806.gradadmissions.web;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.service.ApplicationService;
import com.sysc4806.gradadmissions.web.dto.ApplicationFormDto;

/** The applicant's "submit an application" flow. */
@Controller
@RequestMapping("/apply")
public class ApplicationController {

    /** One entry of the professor dropdown, as JSON. */
    public record ProfessorOption(Long id, String name) {}

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("form", new ApplicationFormDto());
        return formView(model, null);
    }

    /** Feeds the professor dropdown when the applicant picks a research field. */
    @GetMapping("/professors")
    @ResponseBody
    public List<ProfessorOption> professors(@RequestParam Long fieldId) {
        return service.professorsForField(fieldId).stream()
                .map(p -> new ProfessorOption(p.getId(), p.getName()))
                .toList();
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") ApplicationFormDto form, BindingResult result,
                         Model model) {
        if (!result.hasErrors()) {
            try {
                Application application = service.submit(form.getPersonalInfo(), form.getDesiredFieldId(),
                        form.getProfessorIds(), LocalDateTime.now());
                return "redirect:/apply/confirmation/" + application.getId();
            } catch (IllegalArgumentException | IllegalStateException e) {
                result.reject("application.rejected", e.getMessage());
            }
        }
        return formView(model, form.getDesiredFieldId());
    }

    @GetMapping("/confirmation/{id}")
    public String confirmation(@PathVariable Long id, Model model) {
        Application application = service.findApplication(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("application", application);
        return "applicant/confirmation";
    }

    /** Fills in the dropdowns; professors are only listed once a field is chosen. */
    private String formView(Model model, Long selectedFieldId) {
        model.addAttribute("fields", service.researchFields());
        model.addAttribute("professors",
                selectedFieldId == null ? List.of() : service.professorsForField(selectedFieldId));
        return "applicant/apply";
    }
}
package com.sysc4806.gradadmissions.web;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.PersonalInfo;
import com.sysc4806.gradadmissions.service.ApplicationService;
import com.sysc4806.gradadmissions.service.CurrentUserService;
import com.sysc4806.gradadmissions.web.dto.ApplicationFormDto;

/** The logged-in applicant's "submit an application" flow. */
@Controller
@RequestMapping("/apply")
public class ApplicationController {

    /** One entry of the professor dropdown, as JSON. */
    public record ProfessorOption(Long id, String name) {}

    private final ApplicationService service;
    private final CurrentUserService currentUsers;

    public ApplicationController(ApplicationService service, CurrentUserService currentUsers) {
        this.service = service;
        this.currentUsers = currentUsers;
    }

    /**
     * The form starts from the logged-in applicant's account: name and email filled in. On a POST
     * the request's values are bound onto it, except the email (see {@link #initBinder}).
     */
    @ModelAttribute("form")
    public ApplicationFormDto form() {
        ApplicationFormDto form = new ApplicationFormDto();
        form.setPersonalInfo(personalInfoFor(currentUsers.currentApplicant()));
        return form;
    }

    /** The email is the account's, so a changed or forged email in the request is ignored. */
    @InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("personalInfo.email");
    }

    @GetMapping
    public String showForm(Model model) {
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
                Application application = service.submit(currentUsers.currentApplicant(), form.getPersonalInfo(),
                        form.getDesiredFieldId(), form.getProfessorIds(), LocalDateTime.now());
                return "redirect:/apply/confirmation/" + application.getId();
            } catch (IllegalArgumentException | IllegalStateException e) {
                result.reject("application.rejected", e.getMessage());
            }
        }
        return formView(model, form.getDesiredFieldId());
    }

    /** 404 unless it is the logged-in applicant's own application. */
    @GetMapping("/confirmation/{id}")
    public String confirmation(@PathVariable Long id, Model model) {
        Application application = service.findApplicationOf(id, currentUsers.currentApplicant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Not "application": that name is reserved in Thymeleaf (the servlet context) and would be null.
        model.addAttribute("app", application);
        return "applicant/confirmation";
    }

    /**
     * The account's email, and its name split at the first space ("Ada King Lovelace" gives first
     * name "Ada", last name "King Lovelace"). The applicant can correct the name on the form.
     */
    static PersonalInfo personalInfoFor(Applicant applicant) {
        String name = applicant.getName().trim();
        int space = name.indexOf(' ');
        PersonalInfo info = new PersonalInfo();
        info.setFirstName(space < 0 ? name : name.substring(0, space));
        info.setLastName(space < 0 ? "" : name.substring(space + 1).trim());
        info.setEmail(applicant.getEmail());
        return info;
    }

    /** Fills in the dropdowns; professors are only listed once a field is chosen. */
    private String formView(Model model, Long selectedFieldId) {
        model.addAttribute("fields", service.researchFields());
        model.addAttribute("professors",
                selectedFieldId == null ? List.of() : service.professorsForField(selectedFieldId));
        return "applicant/apply";
    }
}

package com.sysc4806.gradadmissions.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
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
import com.sysc4806.gradadmissions.model.ApplicationStatus;
import com.sysc4806.gradadmissions.model.PersonalInfo;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.service.ApplicationService;
import com.sysc4806.gradadmissions.service.CurrentUserService;
import com.sysc4806.gradadmissions.web.dto.ApplicationFormDto;
import com.sysc4806.gradadmissions.web.dto.ApplicationSubmit;

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
    public String showForm(@ModelAttribute("form") ApplicationFormDto form, Model model) {
        Applicant applicant = currentUsers.currentApplicant();
        Long selectedFieldId = null;
        Optional<Application> existing = service.findActiveCycleApplicationForApplicant(applicant);
        if (existing.isPresent() && existing.get().getStatus() == ApplicationStatus.DRAFT) {
            Application app = existing.get();
            // A copy, so the saved draft is never changed by the form; the email stays the account's.
            PersonalInfo saved = app.getPersonalInfo();
            form.setPersonalInfo(new PersonalInfo(saved.getFirstName(), saved.getLastName(),
                    applicant.getEmail(), saved.getPhoneNumber(), saved.getCitizenship()));
            if (app.getDesiredField() != null) {
                selectedFieldId = app.getDesiredField().getId();
                form.setDesiredFieldId(selectedFieldId);
            }
            form.setProfessorIds(app.getPreferredProfessors().stream()
                    .map(Professor::getId)
                    .toList());
        }
        return formView(model, selectedFieldId);
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
    public String submit(@Validated({Default.class, ApplicationSubmit.class}) @ModelAttribute("form") ApplicationFormDto form,
                         BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                Applicant applicant = currentUsers.currentApplicant();
                Application application = service.submit(applicant, form.getPersonalInfo(),
                        form.getDesiredFieldId(), form.getProfessorIds(), LocalDateTime.now());
                return "redirect:/apply/confirmation/" + application.getId();
            } catch (IllegalArgumentException | IllegalStateException e) {
                result.reject("application.rejected", e.getMessage());
            }
        }
        return formView(model, form.getDesiredFieldId());
    }

    /** Saves a draft; field and professors are optional. */
    @PostMapping("/draft")
    public String saveDraft(@Valid @ModelAttribute("form") ApplicationFormDto form,
                            BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                Applicant applicant = currentUsers.currentApplicant();
                service.saveDraft(applicant, form.getPersonalInfo(), form.getDesiredFieldId(),
                        form.getProfessorIds());
                model.addAttribute("draftSaved", true);
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
        if (!application.getApplicant().getId().equals(currentUsers.currentApplicant().getId())) {
            throw new AccessDeniedException("You can only view your own application");
        }
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
        model.addAttribute("existingApplication", service.findActiveCycleApplicationForApplicant(
                currentUsers.currentApplicant()).orElse(null));
        model.addAttribute("fields", service.researchFields());
        model.addAttribute("professors",
                selectedFieldId == null ? List.of() : service.professorsForField(selectedFieldId));
        return "applicant/apply";
    }
}

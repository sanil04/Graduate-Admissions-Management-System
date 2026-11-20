package com.sysc4806.gradadmissions.web;

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
import org.springframework.web.server.ResponseStatusException;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.service.ProfessorService;
import com.sysc4806.gradadmissions.web.dto.ProfessorFormDto;

/** Professor profiles: list, view and edit. */
@Controller
@RequestMapping("/professor")
public class ProfessorController {

    private final ProfessorService service;

    public ProfessorController(ProfessorService service) {
        this.service = service;
    }

    @GetMapping("/profiles")
    public String list(Model model) {
        model.addAttribute("professors", service.findAll());
        return "professor/list";
    }

    @GetMapping("/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        model.addAttribute("professor", professor(id));
        return "professor/profile";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Professor professor = professor(id);
        ProfessorFormDto form = new ProfessorFormDto();
        form.setName(professor.getName());
        form.setEmail(professor.getEmail());
        professor.getResearchFields().stream().map(ResearchField::getId).forEach(form.getResearchFieldIds()::add);
        model.addAttribute("form", form);
        return formView(id, model);
    }

    @PostMapping("/{id}/edit")
    public String updateProfile(@PathVariable Long id, @Valid @ModelAttribute("form") ProfessorFormDto form,
                                BindingResult result, Model model) {
        professor(id);
        if (!result.hasErrors()) {
            try {
                service.update(id, form);
                return "redirect:/professor/" + id;
            } catch (ProfessorService.NoResearchFieldsException e) {
                result.rejectValue("researchFieldIds", "researchFieldIds.empty", e.getMessage());
            } catch (IllegalArgumentException e) {
                result.rejectValue("email", "email.taken", e.getMessage());
            }
        }
        return formView(id, model);
    }

    private Professor professor(Long id) {
        return service.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String formView(Long id, Model model) {
        model.addAttribute("professorId", id);
        model.addAttribute("allResearchFields", service.researchFields());
        return "professor/form";
    }
}
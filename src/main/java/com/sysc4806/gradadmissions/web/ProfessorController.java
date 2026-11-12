package com.sysc4806.gradadmissions.web;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.service.ProfessorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/professor")
public class ProfessorController {

    @Autowired
    private ProfessorService professorService;

    @Autowired
    private ResearchFieldRepository researchFieldRepository;

    @GetMapping("/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        Professor professor = professorService.findById(id);
        model.addAttribute("professor", professor);
        return "professor/profile";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Professor professor = professorService.findById(id);
        model.addAttribute("professor", professor);
        model.addAttribute("allResearchFields", researchFieldRepository.findAll());
        return "professor/form";
    }

    @PostMapping("/{id}/edit")
    public String updateProfile(@PathVariable Long id,
                                @Valid @ModelAttribute("professor") Professor professor,
                                BindingResult result,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("allResearchFields", researchFieldRepository.findAll());
            return "professor/form";
        }
        professor.setId(id);
        professorService.save(professor);
        return "redirect:/professor/" + id;
    }
}
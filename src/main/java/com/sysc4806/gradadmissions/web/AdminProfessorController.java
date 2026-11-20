package com.sysc4806.gradadmissions.web;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.service.ProfessorService;
import com.sysc4806.gradadmissions.web.dto.AdminProfessorFormDto;

/** Admin creates professor accounts. */
@Controller
@RequestMapping("/admin/professors")
public class AdminProfessorController {

	private final ProfessorService service;

	public AdminProfessorController(ProfessorService service) {
		this.service = service;
	}

	@GetMapping("/new")
	public String showForm(Model model) {
		model.addAttribute("form", new AdminProfessorFormDto());
		return formView(model);
	}

	@PostMapping("/new")
	public String create(@Valid @ModelAttribute("form") AdminProfessorFormDto form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				Professor created = service.create(form);
				redirect.addFlashAttribute("message", "Created professor " + created.getName());
				return "redirect:/admin";
			} catch (ProfessorService.NoResearchFieldsException e) {
				result.rejectValue("researchFieldIds", "researchFieldIds.empty", e.getMessage());
			} catch (IllegalArgumentException e) {
				result.rejectValue("email", "email.taken", e.getMessage());
			}
		}
		return formView(model);
	}

	private String formView(Model model) {
		model.addAttribute("allResearchFields", service.researchFields());
		return "admin/professors/new";
	}
}

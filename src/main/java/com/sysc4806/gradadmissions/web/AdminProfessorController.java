package com.sysc4806.gradadmissions.web;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
			BindingResult result, Model model) {
		if (!result.hasErrors()) {
			try {
				service.create(form);
				return "redirect:/admin";
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
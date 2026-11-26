package com.sysc4806.gradadmissions.web;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sysc4806.gradadmissions.service.RegistrationService;
import com.sysc4806.gradadmissions.web.dto.RegistrationFormDto;

/** Applicant registration. Open to everyone (see SecurityConfig); afterwards the applicant logs in. */
@Controller
@RequestMapping("/register")
public class RegistrationController {

    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("form", new RegistrationFormDto());
        return "register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("form") RegistrationFormDto form, BindingResult result) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            service.register(form);
        } catch (RegistrationService.EmailTakenException e) {
            result.rejectValue("email", "email.taken", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}

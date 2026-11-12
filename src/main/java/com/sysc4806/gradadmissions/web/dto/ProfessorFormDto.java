package com.sysc4806.gradadmissions.web.dto;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** What the professor profile form binds to. */
@Getter
@Setter
public class ProfessorFormDto {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    private Set<Long> researchFieldIds = new LinkedHashSet<>();
}

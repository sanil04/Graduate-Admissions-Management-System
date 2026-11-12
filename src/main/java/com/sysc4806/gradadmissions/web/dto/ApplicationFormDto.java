package com.sysc4806.gradadmissions.web.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.PersonalInfo;

/** What the application form binds to. */
@Getter
@Setter
public class ApplicationFormDto {

    @Valid
    @NotNull
    private PersonalInfo personalInfo = new PersonalInfo();

    @NotNull(message = "Choose a research field")
    private Long desiredFieldId;

    @NotEmpty(message = "Choose at least one professor")
    @Size(max = Application.MAX_PREFERRED_PROFESSORS, message = "Choose at most 3 professors")
    private List<Long> professorIds = new ArrayList<>();
}
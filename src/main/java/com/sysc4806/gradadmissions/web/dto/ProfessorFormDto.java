package com.sysc4806.gradadmissions.web.dto;

import com.sysc4806.gradadmissions.model.ResearchField;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

public class ProfessorFormDto {
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    private Set<ResearchField> researchFields = new HashSet<>();

    // Constructors, Getters, and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<ResearchField> getResearchFields() { return researchFields; }
    public void setResearchFields(Set<ResearchField> researchFields) { this.researchFields = researchFields; }
}
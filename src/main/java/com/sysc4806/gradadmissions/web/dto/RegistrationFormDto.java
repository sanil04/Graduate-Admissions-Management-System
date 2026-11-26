package com.sysc4806.gradadmissions.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** What the applicant registration form binds to. */
@Getter
@Setter
public class RegistrationFormDto {

    public static final int MIN_PASSWORD_LENGTH = 8;

    /** BCrypt only uses the first 72 bytes of a password. */
    public static final int MAX_PASSWORD_LENGTH = 72;

    @NotBlank(message = "First name cannot be blank")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    private String lastName;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Password cannot be blank")
    @Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH,
            message = "Password must be between 8 and 72 characters")
    private String password;

    private String confirmPassword;

    /** Trimmed before validation, so " ada@example.com " is accepted. Passwords are never trimmed. */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}

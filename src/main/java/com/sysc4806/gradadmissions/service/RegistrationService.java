package com.sysc4806.gradadmissions.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.repository.ApplicantRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;
import com.sysc4806.gradadmissions.web.dto.RegistrationFormDto;

/** Applicants create their own account (FR-01). Professors and admins don't register themselves. */
@Service
public class RegistrationService {

    /** Message shown when the email already belongs to an account. */
    public static final String EMAIL_TAKEN_MESSAGE = "An account with that email address already exists";

    /** Thrown when the email already belongs to an account of any role. */
    public static class EmailTakenException extends IllegalArgumentException {
        public EmailTakenException() {
            super(EMAIL_TAKEN_MESSAGE);
        }
    }

    private final UserRepository users;
    private final ApplicantRepository applicants;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository users, ApplicantRepository applicants,
                               PasswordEncoder passwordEncoder) {
        this.users = users;
        this.applicants = applicants;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates an applicant account that can log in right away. The email is stored trimmed and in
     * lower case.
     *
     * @throws EmailTakenException if any account already uses the email, ignoring case
     */
    @Transactional
    public Applicant register(RegistrationFormDto form) {
        String email = normalizeEmail(form.getEmail());
        // Emails are unique across all roles.
        if (users.existsByEmailIgnoreCase(email)) {
            throw new EmailTakenException();
        }
        Applicant applicant = new Applicant(form.getFirstName().trim() + " " + form.getLastName().trim(), email);
        applicant.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        try {
            return applicants.saveAndFlush(applicant);
        } catch (DataIntegrityViolationException e) {
            // Someone registered the same email between the check and the insert.
            throw new EmailTakenException();
        }
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

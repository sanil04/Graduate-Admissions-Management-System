package com.sysc4806.gradadmissions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.repository.ApplicantRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;
import com.sysc4806.gradadmissions.web.dto.RegistrationFormDto;

/** Runs against the real database, so the case-insensitive email check and the unique constraint are real. */
@DataJpaTest
@Import({RegistrationService.class, RegistrationServiceTest.Encoder.class})
class RegistrationServiceTest {

    @TestConfiguration
    static class Encoder {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Autowired private TestEntityManager em;
    @Autowired private RegistrationService service;
    @Autowired private UserRepository users;
    @Autowired private ApplicantRepository applicants;
    @Autowired private PasswordEncoder passwordEncoder;

    private static RegistrationFormDto form(String firstName, String lastName, String email) {
        RegistrationFormDto form = new RegistrationFormDto();
        form.setFirstName(firstName);
        form.setLastName(lastName);
        form.setEmail(email);
        form.setPassword("secret123");
        form.setConfirmPassword("secret123");
        return form;
    }

    @Test
    void registerCreatesAnApplicantWithAHashedPassword() {
        Applicant saved = service.register(form("Grace", "Hopper", "grace@example.com"));
        em.flush();
        em.clear();

        Applicant loaded = applicants.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Grace Hopper");
        assertThat(loaded.getEmail()).isEqualTo("grace@example.com");
        assertThat(loaded.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", loaded.getPasswordHash())).isTrue();
        assertThat(users.findByEmail("grace@example.com")).get().isInstanceOf(Applicant.class);
    }

    @Test
    void registerStoresTheEmailInLowerCaseAndTrimsTheName() {
        Applicant saved = service.register(form("  Grace ", " Hopper  ", " Grace.Hopper@Example.COM "));

        assertThat(saved.getEmail()).isEqualTo("grace.hopper@example.com");
        assertThat(saved.getName()).isEqualTo("Grace Hopper");
    }

    @Test
    void registerRejectsAnEmailAnApplicantAlreadyUsesIgnoringCase() {
        em.persist(new Applicant("Ada Lovelace", "ada@example.com"));
        long before = applicants.count();

        assertThatThrownBy(() -> service.register(form("Ada", "Again", "ADA@Example.com")))
                .isInstanceOf(RegistrationService.EmailTakenException.class)
                .hasMessage(RegistrationService.EMAIL_TAKEN_MESSAGE);
        assertThat(applicants.count()).isEqualTo(before);
    }

    @Test
    void registerRejectsAProfessorsEmail() {
        // Emails are unique across all roles, so an applicant can't take a professor's login.
        em.persist(new Professor("Alice Chen", "achen@carleton.ca"));

        assertThatThrownBy(() -> service.register(form("Alice", "Chen", "achen@carleton.ca")))
                .isInstanceOf(RegistrationService.EmailTakenException.class);
    }
}

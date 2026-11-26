package com.sysc4806.gradadmissions.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.sysc4806.gradadmissions.config.SecurityConfig;
import com.sysc4806.gradadmissions.service.RegistrationService;

/** Runs logged out: registering is the one form open to everyone. */
@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
class RegistrationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RegistrationService service;

    private MockHttpServletRequestBuilder registration(String email, String password, String confirmPassword) {
        return post("/register").with(csrf())
                .param("firstName", "Grace")
                .param("lastName", "Hopper")
                .param("email", email)
                .param("password", password)
                .param("confirmPassword", confirmPassword);
    }

    @Test
    void theFormOpensWithoutLoggingIn() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(containsString("name=\"firstName\"")))
                .andExpect(content().string(containsString("name=\"confirmPassword\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void aValidRegistrationCreatesTheAccountAndSendsTheApplicantToLogIn() throws Exception {
        mockMvc.perform(registration("grace@example.com", "secret123", "secret123"))
                .andExpect(redirectedUrl("/login?registered"));

        verify(service).register(argThat(form -> form.getEmail().equals("grace@example.com")
                && form.getPassword().equals("secret123")));
    }

    @Test
    void theEmailIsTrimmedBeforeItIsValidated() throws Exception {
        mockMvc.perform(registration(" grace@example.com ", "secret123", "secret123"))
                .andExpect(redirectedUrl("/login?registered"));

        verify(service).register(argThat(form -> form.getEmail().equals("grace@example.com")));
    }

    @Test
    void invalidFieldsRedisplayTheFormWithoutCreatingAnAccount() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("firstName", "")
                        .param("lastName", " ")
                        .param("email", "not-an-email")
                        .param("password", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "firstName", "lastName", "email", "password"));

        verify(service, never()).register(any());
    }

    @Test
    void mismatchedPasswordsAreRejected() throws Exception {
        mockMvc.perform(registration("grace@example.com", "secret123", "secret999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "confirmPassword"))
                .andExpect(content().string(containsString("Passwords do not match")));

        verify(service, never()).register(any());
    }

    @Test
    void theTypedPasswordIsNotSentBackInThePage() throws Exception {
        mockMvc.perform(registration("grace@example.com", "secret123", "secret999"))
                .andExpect(content().string(not(containsString("secret123"))))
                .andExpect(content().string(not(containsString("secret999"))));
    }

    @Test
    void aTakenEmailIsShownOnTheEmailField() throws Exception {
        when(service.register(any())).thenThrow(new RegistrationService.EmailTakenException());

        mockMvc.perform(registration("grace@example.com", "secret123", "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "email"))
                .andExpect(content().string(containsString(RegistrationService.EMAIL_TAKEN_MESSAGE)));
    }

    @Test
    void registeringWithoutTheCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/register").param("email", "grace@example.com"))
                .andExpect(status().isForbidden());

        verify(service, never()).register(any());
    }
}

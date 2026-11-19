package com.sysc4806.gradadmissions.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.sysc4806.gradadmissions.config.SecurityConfig;
import com.sysc4806.gradadmissions.service.AdminApplicationService;
import com.sysc4806.gradadmissions.service.ApplicationService;
import com.sysc4806.gradadmissions.service.CurrentUserService;
import com.sysc4806.gradadmissions.service.ProfessorService;

// ApplicationController, AdminController and ProfessorController are included so the link check
// can follow the section pages' links to /apply, /admin/applications and /professor/profiles.
@WebMvcTest({HomeController.class, LoginController.class, ApplicationController.class,
        AdminController.class, ProfessorController.class})
@Import(SecurityConfig.class)
class HomeControllerTest {

    /** Each role and the section it may open. */
    private static final Map<String, String> SECTIONS =
            Map.of("APPLICANT", "applicant", "PROFESSOR", "professor", "ADMIN", "admin");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ApplicationService applicationService;
    @MockitoBean private AdminApplicationService adminApplicationService;
    @MockitoBean private ProfessorService professorService;
    @MockitoBean private CurrentUserService currentUserService;

    private static RequestPostProcessor as(String role) {
        return user(role.toLowerCase() + "@example.com").roles(role);
    }

    @Test
    void anonymousHomePageAsksToLogInAndLinksToNoSection() throws Exception {
        mockMvc.perform(get("/").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(not(containsString("href=\"/applicant\""))))
                .andExpect(content().string(not(containsString("href=\"/professor\""))))
                .andExpect(content().string(not(containsString("href=\"/admin\""))))
                .andExpect(content().string(not(containsString("Log out"))));
    }

    @Test
    void homePageLinksOnlyToTheSectionOfTheUsersRole() throws Exception {
        for (Map.Entry<String, String> entry : SECTIONS.entrySet()) {
            String html = mockMvc.perform(get("/").with(as(entry.getKey())))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            for (String section : SECTIONS.values()) {
                boolean linked = html.contains("href=\"/" + section + "\"");
                if (section.equals(entry.getValue()) != linked) {
                    throw new AssertionError(entry.getKey() + " home page link to /" + section + ": " + linked);
                }
            }
        }
    }

    @Test
    void navShowsWhoIsLoggedInAndALogoutButton() throws Exception {
        mockMvc.perform(get("/").with(as("PROFESSOR")))
                .andExpect(content().string(containsString("professor@example.com (Professor)")))
                .andExpect(content().string(matchesPattern("(?s).*<form[^>]*action=\"/logout\"[^>]*method=\"post\".*")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Log out")))
                .andExpect(content().string(not(containsString("href=\"/login\""))));
    }

    @Test
    void homePageUsesSharedHeaderAndNav() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("class=\"site-header\"")))
                .andExpect(content().string(containsString("aria-label=\"Main navigation\"")))
                .andExpect(content().string(containsString("class=\"site-footer\"")));
    }

    @Test
    void sectionPagesRenderWithSharedLayoutAndActiveNav() throws Exception {
        for (Map.Entry<String, String> entry : SECTIONS.entrySet()) {
            String section = entry.getValue();
            mockMvc.perform(get("/" + section).with(as(entry.getKey())))
                    .andExpect(status().isOk())
                    .andExpect(view().name(section + "/index"))
                    .andExpect(content().string(containsString("class=\"site-header\"")))
                    .andExpect(content().string(matchesPattern(
                            "(?s).*<a[^>]*href=\"/" + section + "\"[^>]*class=\"active\".*|(?s).*<a[^>]*class=\"active\"[^>]*href=\"/" + section + "\".*")));
        }
    }

    @Test
    void pagesOnlyLinkToPagesThatExist() throws Exception {
        // Nav and cards must not point at unbuilt features, nor at pages the user may not open.
        checkLinks(anonymous(), List.of("/", "/login"));
        for (Map.Entry<String, String> entry : SECTIONS.entrySet()) {
            checkLinks(as(entry.getKey()), List.of("/", "/" + entry.getValue()));
        }
    }

    private void checkLinks(RequestPostProcessor who, List<String> pages) throws Exception {
        for (String page : pages) {
            String html = mockMvc.perform(get(page).with(who)).andReturn().getResponse().getContentAsString();
            Matcher m = Pattern.compile("href=\"(/[^\"]*)\"").matcher(html);
            while (m.find()) {
                String link = m.group(1);
                if (link.startsWith("/css/")) continue;
                mockMvc.perform(get(link).with(who)).andExpect(status().isOk());
            }
        }
    }
}

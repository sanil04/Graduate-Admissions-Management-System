package com.sysc4806.gradadmissions.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.config.SecurityConfig;
import com.sysc4806.gradadmissions.service.AdminApplicationService;
import com.sysc4806.gradadmissions.service.ApplicationService;
import com.sysc4806.gradadmissions.service.ProfessorService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// ApplicationController and AdminController are included so the link check can follow the
// section pages' links to /apply and /admin/applications.
// ProfessorController is included for the Professor page's link to /professor/profiles.
@WebMvcTest({HomeController.class, ApplicationController.class, AdminController.class, ProfessorController.class, AdminProfessorController.class})
@Import(SecurityConfig.class)
@WithMockUser(roles = {"ADMIN", "PROFESSOR", "APPLICANT"})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private AdminApplicationService adminApplicationService;

    @MockitoBean
    private ProfessorService professorService;

    @Test
    void homePageLinksToAllThreeSections() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("href=\"/applicant\"")))
                .andExpect(content().string(containsString("href=\"/professor\"")))
                .andExpect(content().string(containsString("href=\"/admin\"")));
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
        for (String section : new String[]{"applicant", "professor", "admin"}) {
            mockMvc.perform(get("/" + section))
                    .andExpect(status().isOk())
                    .andExpect(view().name(section + "/index"))
                    .andExpect(content().string(containsString("class=\"site-header\"")))
                    .andExpect(content().string(matchesPattern(
                            "(?s).*<a[^>]*href=\"/" + section + "\"[^>]*class=\"active\".*|(?s).*<a[^>]*class=\"active\"[^>]*href=\"/" + section + "\".*")));
        }
    }

    @Test
    void pagesOnlyLinkToPagesThatExist() throws Exception {
        // Nav and cards must not point at unbuilt features (e.g. forms or lists).
        for (String path : new String[]{"/", "/applicant", "/professor", "/admin"}) {
            String html = mockMvc.perform(get(path)).andReturn().getResponse().getContentAsString();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("href=\"(/[^\"]*)\"").matcher(html);
            while (m.find()) {
                String link = m.group(1);
                if (link.startsWith("/css/")) continue;
                mockMvc.perform(get(link)).andExpect(status().isOk());
            }
        }
    }
}
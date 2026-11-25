package com.sysc4806.gradadmissions.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.config.SecurityConfig;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.service.ProfessorService;
import com.sysc4806.gradadmissions.web.dto.AdminProfessorFormDto;

@WebMvcTest(AdminProfessorController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class AdminProfessorControllerTest {

	@Autowired private MockMvc mockMvc;
	@MockitoBean private ProfessorService service;

	@Test
	void formListsResearchFields() throws Exception {
		ResearchField ai = new ResearchField("AI");
		ReflectionTestUtils.setField(ai, "id", 1L);
		when(service.researchFields()).thenReturn(List.of(ai));

		mockMvc.perform(get("/admin/professors/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/professors/new"))
				.andExpect(content().string(containsString("AI")))
				.andExpect(content().string(containsString("temporaryPassword")));
	}

	@Test
	void validCreateRedirectsToAdminHome() throws Exception {
		when(service.create(any(AdminProfessorFormDto.class))).thenReturn(new Professor("Dr. New", "new@carleton.ca"));

		mockMvc.perform(post("/admin/professors/new").with(csrf())
						.param("name", "Dr. New")
						.param("email", "new@carleton.ca")
						.param("temporaryPassword", "temp123")
						.param("researchFieldIds", "1"))
				.andExpect(redirectedUrl("/admin"))
				.andExpect(flash().attribute("message", "Created professor Dr. New"));

		verify(service).create(any(AdminProfessorFormDto.class));
	}

	@Test
	void invalidCreateRedisplaysTheFormWithoutCallingTheService() throws Exception {
		when(service.researchFields()).thenReturn(List.of());

		mockMvc.perform(post("/admin/professors/new").with(csrf())
						.param("name", "")
						.param("email", "not-an-email")
						.param("temporaryPassword", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/professors/new"))
				.andExpect(model().attributeHasFieldErrors("form", "name", "email", "temporaryPassword",
						"researchFieldIds"));

		verify(service, never()).create(any());
	}

	@Test
	void duplicateEmailIsShownOnTheForm() throws Exception {
		when(service.researchFields()).thenReturn(List.of());
		when(service.create(any(AdminProfessorFormDto.class)))
				.thenThrow(new IllegalArgumentException("That email address belongs to another account"));

		mockMvc.perform(post("/admin/professors/new").with(csrf())
						.param("name", "Dr. New")
						.param("email", "taken@carleton.ca")
						.param("temporaryPassword", "temp123")
						.param("researchFieldIds", "1"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/professors/new"))
				.andExpect(model().attributeHasFieldErrors("form", "email"))
				.andExpect(content().string(containsString("That email address belongs to another account")));
	}

	@Test
	void emptyResearchFieldsErrorIsShownOnTheResearchFieldsControl() throws Exception {
		when(service.researchFields()).thenReturn(List.of());
		when(service.create(any(AdminProfessorFormDto.class)))
				.thenThrow(new ProfessorService.NoResearchFieldsException());

		mockMvc.perform(post("/admin/professors/new").with(csrf())
						.param("name", "Dr. New")
						.param("email", "new@carleton.ca")
						.param("temporaryPassword", "temp123")
						.param("researchFieldIds", "999"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/professors/new"))
				.andExpect(model().attributeHasFieldErrors("form", "researchFieldIds"))
				.andExpect(content().string(containsString("Select at least one research field")));
	}
}
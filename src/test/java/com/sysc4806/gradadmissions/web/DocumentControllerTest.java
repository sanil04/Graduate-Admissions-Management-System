package com.sysc4806.gradadmissions.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sysc4806.gradadmissions.config.SecurityConfig;
import com.sysc4806.gradadmissions.model.*;
import com.sysc4806.gradadmissions.service.CurrentUserService;
import com.sysc4806.gradadmissions.service.DocumentService;

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "APPLICANT")
class DocumentControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private DocumentService documents;
    @MockitoBean private CurrentUserService currentUsers;
    private final Applicant applicant = new Applicant("Ada", "ada@example.com");
    private final MockMultipartFile pdf = new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.7".getBytes());

    @BeforeEach
    void setUp() { when(currentUsers.currentApplicant()).thenReturn(applicant); }

    @Test
    void showsTheUploadFormAndPreviouslyUploadedFiles() throws Exception {
        Application application = new Application();
        ReflectionTestUtils.setField(application, "id", 5L);
        Document document = new Document(application, DocumentType.CV);
        document.replace("cv.pdf", pdf.getBytes(), LocalDateTime.now());
        when(documents.ownedApplication(5L, applicant)).thenReturn(application);
        when(documents.uploadedDocuments(5L, applicant)).thenReturn(List.of(document));
        mvc.perform(get("/apply/5/documents")).andExpect(status().isOk())
                .andExpect(content().string(containsString("cv.pdf")))
                .andExpect(content().string(containsString("multipart/form-data")))
                .andExpect(content().string(containsString("/apply/5/documents")));
    }

    @Test
    void theUploadFormIsHiddenAfterSubmission() throws Exception {
        Application application = new Application();
        ReflectionTestUtils.setField(application, "id", 5L);
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        when(documents.ownedApplication(5L, applicant)).thenReturn(application);
        mvc.perform(get("/apply/5/documents")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Documents can only be changed while the application is a draft")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("multipart/form-data"))));
    }

    @Test
    void lockedApplicationsAndConcurrentUploadsShowFriendlyErrors() throws Exception {
        doThrow(new IllegalStateException("Documents can only be changed while the application is a draft"))
                .when(documents).upload(anyLong(), any(), any(), any(), any());
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "CV").with(csrf()))
                .andExpect(redirectedUrl("/apply/5/documents"))
                .andExpect(flash().attribute("error", "Documents can only be changed while the application is a draft"));
        doThrow(new DataIntegrityViolationException("duplicate document"))
                .when(documents).upload(anyLong(), any(), any(), any(), any());
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "CV").with(csrf()))
                .andExpect(redirectedUrl("/apply/5/documents"))
                .andExpect(flash().attribute("error", "Another upload finished at the same time. Please try again."));
    }

    @Test
    void uploadsAndRedirectsWithAConfirmation() throws Exception {
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "CV").with(csrf()))
                .andExpect(redirectedUrl("/apply/5/documents"))
                .andExpect(flash().attribute("message", "Document uploaded."));
        verify(documents).upload(eq(5L), eq(applicant), eq(DocumentType.CV), any(), any());
    }

    @Test
    void validationAndReadFailuresAreShownWithoutLosingTheApplication() throws Exception {
        when(documents.upload(anyLong(), any(), any(), any(), any())).thenThrow(new IllegalArgumentException("The file must be a PDF"));
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "CV").with(csrf()))
                .andExpect(flash().attribute("error", "The file must be a PDF"));
        doThrow(new IOException("internal path")).when(documents).upload(anyLong(), any(), any(), any(), any());
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "CV").with(csrf()))
                .andExpect(flash().attribute("error", "The file could not be read. Please try again."));
    }

    @Test
    void ownershipFailuresRemainForbidden() throws Exception {
        when(documents.upload(anyLong(), any(), any(), any(), any())).thenThrow(new AccessDeniedException("Not your application"));
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "CV").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void csrfAndDocumentTypeAreRequired() throws Exception {
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "CV")).andExpect(status().isForbidden());
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "UNKNOWN").with(csrf())).andExpect(status().isBadRequest());
        verifyNoInteractions(documents);
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    void professorsCannotUpload() throws Exception {
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "CV").with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(documents);
    }

    @Test
    void uploadAfterTheApplicationDeadlineShowsTheError() throws Exception {
        when(documents.upload(anyLong(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("The application deadline has passed"));
        mvc.perform(multipart("/apply/5/documents").file(pdf).param("type", "CV").with(csrf()))
                .andExpect(redirectedUrl("/apply/5/documents"))
                .andExpect(flash().attribute("error", "The application deadline has passed"));
    }
}

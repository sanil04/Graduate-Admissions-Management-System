package com.sysc4806.gradadmissions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.config.DataSeeder;
import com.sysc4806.gradadmissions.model.*;
import com.sysc4806.gradadmissions.repository.*;
import com.sysc4806.gradadmissions.service.DocumentService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentAccessTest {
    private static final byte[] PDF = "%PDF-1.7\nexample".getBytes(StandardCharsets.US_ASCII);
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private AdmissionCycleRepository cycles;
    @Autowired private ResearchFieldRepository fields;
    @Autowired private ApplicationRepository applications;
    @Autowired private EvaluationRepository evaluations;
    @Autowired private DocumentService documents;
    @Autowired private EntityManager em;
    private Applicant owner;
    private Applicant other;
    private Application application;
    private Long documentId;

    @BeforeEach
    void setUp() throws Exception {
        owner = users.save(new Applicant("Document Owner", "document-owner@example.com"));
        other = users.save(new Applicant("Other Applicant", "other-document-applicant@example.com"));
        application = new Application(owner, cycles.findByActiveTrue().orElseThrow(),
                new PersonalInfo("Document", "Owner", owner.getEmail(), null, null));
        application.setDesiredField(fields.findByName("AI").orElseThrow());
        // A preferred professor still needs an assignment to read the documents.
        Professor brian = (Professor) users.findByEmail("bokonkwo@gradadmissions.local").orElseThrow();
        application.addPreferredProfessor(brian);
        LocalDateTime now = LocalDateTime.of(2026, 11, 25, 12, 0);
        application.submit(now);
        applications.save(application);
        Professor alice = (Professor) users.findByEmail("achen@gradadmissions.local").orElseThrow();
        evaluations.save(new Evaluation(application, alice, now));
        documentId = documents.upload(application.getId(), owner, DocumentType.CV,
                new MockMultipartFile("file", "cv.pdf", "application/pdf", PDF), now).getId();
        em.flush();
        em.clear();
    }

    @Test
    void owningApplicantReceivesThePdfAsAnUncachedAttachment() throws Exception {
        mvc.perform(get("/documents/" + documentId).with(user(owner.getEmail()).roles("APPLICANT")))
                .andExpect(status().isOk()).andExpect(content().bytes(PDF))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("cv.pdf")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().longValue("Content-Length", PDF.length));
    }

    @Test
    void adminsAndAssignedProfessorsCanDownload() throws Exception {
        mvc.perform(get("/documents/" + documentId).with(user(DataSeeder.ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().bytes(PDF));
        mvc.perform(get("/documents/" + documentId).with(user("achen@gradadmissions.local").roles("PROFESSOR")))
                .andExpect(status().isOk()).andExpect(content().bytes(PDF));
    }

    @Test
    void otherApplicantsAndUnassignedProfessorsGetForbidden() throws Exception {
        mvc.perform(get("/documents/" + documentId).with(user(other.getEmail()).roles("APPLICANT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/documents/" + documentId).with(user("bokonkwo@gradadmissions.local").roles("PROFESSOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousVisitorsMustLogInAndMissingDocumentsReturnNotFound() throws Exception {
        mvc.perform(get("/documents/" + documentId)).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/documents/" + Long.MAX_VALUE).with(user(DataSeeder.ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void allThreeApplicationPagesShowTheDownloadLink() throws Exception {
        List<String> paths = List.of("/apply/confirmation/" + application.getId(),
                "/admin/applications/" + application.getId(), "/professor/applications/" + application.getId());
        List<String> emails = List.of(owner.getEmail(), DataSeeder.ADMIN_EMAIL, "achen@gradadmissions.local");
        List<String> roles = List.of("APPLICANT", "ADMIN", "PROFESSOR");
        for (int i = 0; i < paths.size(); i++) {
            mvc.perform(get(paths.get(i)).with(user(emails.get(i)).roles(roles.get(i))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("href=\"/documents/" + documentId + "\"")));
        }
    }

    @Test
    void anotherApplicantCannotSeeTheConfirmationPageOrDocumentNames() throws Exception {
        mvc.perform(get("/apply/confirmation/" + application.getId()).with(user(other.getEmail()).roles("APPLICANT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/apply/" + application.getId() + "/documents").with(user(other.getEmail()).roles("APPLICANT")))
                .andExpect(status().isForbidden());
    }
}

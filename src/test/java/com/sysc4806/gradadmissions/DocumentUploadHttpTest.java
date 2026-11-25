package com.sysc4806.gradadmissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.eq;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.PersonalInfo;
import com.sysc4806.gradadmissions.model.DocumentType;
import com.sysc4806.gradadmissions.repository.AdmissionCycleRepository;
import com.sysc4806.gradadmissions.repository.ApplicationRepository;
import com.sysc4806.gradadmissions.repository.DocumentRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
        "spring.datasource.url=jdbc:h2:mem:document-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class DocumentUploadHttpTest {
    @LocalServerPort private int port;
    @Autowired private UserRepository users;
    @Autowired private ApplicationRepository applications;
    @Autowired private AdmissionCycleRepository cycles;
    @MockitoSpyBean private DocumentRepository documents;
    @Autowired private PasswordEncoder passwords;
    private HttpClient client;
    private Long applicationId;
    private String uploadToken;

    @BeforeEach
    void logInWithARealSession() throws Exception {
        Applicant owner = new Applicant("HTTP upload test", "http-" + UUID.randomUUID() + "@example.com");
        owner.setPasswordHash(passwords.encode("upload-test-password"));
        users.save(owner);
        applicationId = applications.save(new Application(owner, cycles.findByActiveTrue().orElseThrow(),
                new PersonalInfo("HTTP", "Upload", owner.getEmail(), null, null))).getId();
        client = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(10)).build();
        String loginPage = client.send(HttpRequest.newBuilder(uri("/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        String form = "email=" + encode(owner.getEmail()) + "&password=upload-test-password&_csrf=" + encode(csrf(loginPage));
        var login = client.send(HttpRequest.newBuilder(uri("/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(login.headers().firstValue("Location").orElseThrow()).doesNotContain("error");
        var page = client.send(HttpRequest.newBuilder(uri("/apply/" + applicationId + "/documents")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(page.statusCode()).isEqualTo(200);
        uploadToken = csrf(page.body());
    }

    @ParameterizedTest
    @ValueSource(ints = {5 * 1024 * 1024 + 1, 7 * 1024 * 1024})
    void oversizedPdfsGetAFriendlySizeErrorThroughTheRealSecurityFilters(int size) throws Exception {
        var response = upload(size, true);
        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("Choose a PDF no larger than 5 MB");
        assertThat(documents.findByApplicationIdOrderByTypeAsc(applicationId)).isEmpty();
    }

    @Test
    void aValidMultipartUploadWithAFormCsrfTokenIsSaved() throws Exception {
        var response = upload(1024, true);
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(documents.findByApplicationIdOrderByTypeAsc(applicationId)).hasSize(1);
    }

    @Test
    void simultaneousFirstUploadsKeepOneFileAndBothReturnWithoutAServerError() throws Exception {
        CyclicBarrier bothRead = new CyclicBarrier(2);
        doAnswer(invocation -> {
            var result = documents.findByApplicationIdOrderByTypeAsc(applicationId).stream()
                    .filter(document -> document.getType() == DocumentType.CV).findFirst();
            assertThat(result).isEmpty();
            bothRead.await(10, TimeUnit.SECONDS);
            return result;
        }).when(documents).findByApplicationIdAndType(eq(applicationId), eq(DocumentType.CV));

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> upload(1024, true));
            var second = pool.submit(() -> upload(2048, true));
            assertThat(first.get(20, TimeUnit.SECONDS).statusCode()).isEqualTo(302);
            assertThat(second.get(20, TimeUnit.SECONDS).statusCode()).isEqualTo(302);
        }
        assertThat(documents.findByApplicationIdOrderByTypeAsc(applicationId)).singleElement()
                .satisfies(document -> assertThat(document.getSize()).isIn(1024L, 2048L));
    }

    @Test
    void multipartUploadsStillRequireCsrf() throws Exception {
        assertThat(upload(1024, false).statusCode()).isEqualTo(403);
        assertThat(documents.findByApplicationIdOrderByTypeAsc(applicationId)).isEmpty();
    }

    private HttpResponse<String> upload(int size, boolean includeCsrf) throws Exception {
        String boundary = "UploadBoundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        if (includeCsrf) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"_csrf\"\r\n\r\n"
                    + uploadToken + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"type\"\r\n\r\nCV\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n%PDF-").getBytes(StandardCharsets.UTF_8));
        body.write(new byte[size - 5]);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(HttpRequest.newBuilder(uri("/apply/" + applicationId + "/documents"))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String csrf(String html) {
        var token = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(token.find()).as("The real form has a CSRF token").isTrue();
        return token.group(1);
    }
}

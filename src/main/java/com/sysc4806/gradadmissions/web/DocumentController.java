package com.sysc4806.gradadmissions.web;

import java.io.IOException;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.DocumentType;
import com.sysc4806.gradadmissions.model.Document;
import com.sysc4806.gradadmissions.service.CurrentUserService;
import com.sysc4806.gradadmissions.service.DocumentService;

@Controller
public class DocumentController {

    private final DocumentService documents;
    private final CurrentUserService currentUsers;

    public DocumentController(DocumentService documents, CurrentUserService currentUsers) {
        this.documents = documents;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/apply/{id}/documents")
    public String uploads(@PathVariable Long id, Model model) {
        Applicant applicant = currentUsers.currentApplicant();
        model.addAttribute("app", documents.ownedApplication(id, applicant));
        model.addAttribute("documents", documents.uploadedDocuments(id, applicant));
        model.addAttribute("types", DocumentType.values());
        return "applicant/documents";
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Document document = documents.download(id, currentUsers.currentUser()
                .orElseThrow(() -> new AccessDeniedException("Log in to download documents")));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .contentLength(document.getSize()).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(document.getBytes());
    }

    @PostMapping("/apply/{id}/documents")
    public String upload(@PathVariable Long id, @RequestParam DocumentType type,
                         @RequestParam MultipartFile file, RedirectAttributes redirect) {
        try {
            documents.upload(id, currentUsers.currentApplicant(), type, file, LocalDateTime.now());
            redirect.addFlashAttribute("message", "Document uploaded.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "Another upload finished at the same time. Please try again.");
        } catch (IOException e) {
            redirect.addFlashAttribute("error", "The file could not be read. Please try again.");
        }
        return "redirect:/apply/" + id + "/documents";
    }

}

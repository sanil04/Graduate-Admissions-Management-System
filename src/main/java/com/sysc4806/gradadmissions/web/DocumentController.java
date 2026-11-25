package com.sysc4806.gradadmissions.web;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.DocumentType;
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

    @PostMapping("/apply/{id}/documents")
    public String upload(@PathVariable Long id, @RequestParam DocumentType type,
                         @RequestParam MultipartFile file, RedirectAttributes redirect) {
        try {
            documents.upload(id, currentUsers.currentApplicant(), type, file, LocalDateTime.now());
            redirect.addFlashAttribute("message", "Document uploaded.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            redirect.addFlashAttribute("error", "The file could not be read. Please try again.");
        }
        return "redirect:/apply/" + id + "/documents";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE)
    @ResponseBody
    public String uploadTooLarge() {
        return "Choose a PDF no larger than 5 MB.";
    }
}

package com.sysc4806.gradadmissions.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import com.sysc4806.gradadmissions.model.Application;
import com.sysc4806.gradadmissions.model.Evaluation;
import com.sysc4806.gradadmissions.repository.EvaluationRepository;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * Emails professors when an application is routed to them, through a circuit breaker. Never throws,
 * so routing always stands. Without {@code spring.mail.host} there is no mail sender and emails are only logged.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public record Result(int sent, int failed, int skipped, boolean mailConfigured) {
    }

    public record Status(boolean mailConfigured, CircuitBreaker.State state) {
    }

    private final MailSender mailSender;
    private final CircuitBreaker circuitBreaker;
    private final EvaluationRepository evaluations;
    private final String from;
    private final String baseUrl;

    public NotificationService(ObjectProvider<MailSender> mailSender, CircuitBreaker circuitBreaker,
                               EvaluationRepository evaluations,
                               @Value("${notifications.from}") String from,
                               @Value("${notifications.base-url}") String baseUrl) {
        this.mailSender = mailSender.getIfAvailable();
        this.circuitBreaker = circuitBreaker;
        this.evaluations = evaluations;
        this.from = from;
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    public Result notifyRouted(List<Evaluation> routed, LocalDateTime now) {
        if (mailSender == null) {
            routed.forEach(evaluation -> log.info("Email not sent (no mail server):\n{}", message(evaluation).getText()));
            return new Result(0, 0, 0, false);
        }
        int sent = 0;
        int failed = 0;
        int skipped = 0;
        for (Evaluation evaluation : routed) {
            SimpleMailMessage message = message(evaluation);
            try {
                circuitBreaker.executeRunnable(() -> mailSender.send(message));
                // Update only the timestamp so a review submitted meanwhile isn't overwritten.
                evaluations.markNotified(evaluation.getId(), now);
                evaluation.markNotified(now);
                sent++;
            } catch (CallNotPermittedException e) {
                skipped++;
            } catch (RuntimeException e) {
                log.warn("Email to {} failed: {}", evaluation.getProfessor().getEmail(), e.getMessage());
                failed++;
            }
        }
        return new Result(sent, failed, skipped, true);
    }

    public Status status() {
        return new Status(mailSender != null, circuitBreaker.getState());
    }

    SimpleMailMessage message(Evaluation evaluation) {
        Application application = evaluation.getApplication();
        String applicant = application.getPersonalInfo().getFirstName() + " " + application.getPersonalInfo().getLastName();
        String field = application.getDesiredField() == null ? "-" : application.getDesiredField().getName();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(evaluation.getProfessor().getEmail());
        message.setSubject("New graduate application to review: " + applicant);
        message.setText("""
                Hello %s,

                You have been sent %s's application to review (research field: %s).
                Enter your assessment by %s at %s/professor/applications
                """.formatted(evaluation.getProfessor().getName(), applicant, field,
                application.getCycle().getEvaluationDeadline(), baseUrl));
        return message;
    }
}

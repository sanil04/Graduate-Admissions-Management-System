package com.sysc4806.gradadmissions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import com.sysc4806.gradadmissions.config.NotificationConfig;
import com.sysc4806.gradadmissions.model.*;
import com.sysc4806.gradadmissions.repository.EvaluationRepository;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/** A real breaker that opens after 2 failed sends and waits 100 ms, with a mocked mail server. */
class NotificationServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 10, 0);
	private static final Duration WAIT = Duration.ofMillis(100);

	private MailSender mailSender;
	private EvaluationRepository evaluations;
	private CircuitBreaker circuitBreaker;
	private NotificationService service;
	private Application application;

	@BeforeEach
	void setUp() {
		mailSender = mock(MailSender.class);
		evaluations = mock(EvaluationRepository.class);
		circuitBreaker = NotificationConfig.circuitBreaker(2, WAIT);
		service = newService(mailSender);
		AdmissionCycle cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
		application = new Application(new Applicant("Ada Lovelace", "ada@example.com"), cycle,
				new PersonalInfo("Ada", "Lovelace", "ada@example.com", null, null));
		application.setDesiredField(new ResearchField("AI"));
	}

	@SuppressWarnings("unchecked")
	private NotificationService newService(MailSender sender) {
		ObjectProvider<MailSender> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(sender);
		return new NotificationService(provider, circuitBreaker, evaluations,
				"no-reply@gradadmissions.local", "https://example.azurewebsites.net/");
	}

	private Evaluation routedTo(String name) {
		return new Evaluation(application, new Professor(name, name.toLowerCase().replace(' ', '.') + "@carleton.ca"), NOW);
	}

	private void mailServerDown() {
		doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));
	}

	@Test
	void eachProfessorGetsAnEmailAndOnlyTheTimestampIsSaved() {
		Evaluation alice = routedTo("Alice Chen");

		NotificationService.Result result = service.notifyRouted(List.of(alice, routedTo("Brian Okonkwo")), NOW);

		assertThat(result).isEqualTo(new NotificationService.Result(2, 0, 0, true));
		ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender, times(2)).send(sent.capture());
		SimpleMailMessage first = sent.getAllValues().get(0);
		assertThat(first.getTo()).containsExactly("alice.chen@carleton.ca");
		assertThat(first.getSubject()).contains("Ada Lovelace");
		assertThat(first.getText()).contains("Hello Alice Chen", "research field: AI", "2027-03-01",
				"https://example.azurewebsites.net/professor/applications");
		assertThat(alice.getNotifiedAt()).isEqualTo(NOW);
		verify(evaluations, times(2)).markNotified(any(), eq(NOW));
		verify(evaluations, never()).save(any());
	}

	@Test
	void failuresOpenTheBreakerAndLaterEmailsAreSkippedWithoutCallingTheMailServer() {
		mailServerDown();

		NotificationService.Result result = service.notifyRouted(
				List.of(routedTo("Alice Chen"), routedTo("Brian Okonkwo"), routedTo("Carla Rossi")), NOW);

		assertThat(result).isEqualTo(new NotificationService.Result(0, 2, 1, true));
		verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
		verify(evaluations, never()).markNotified(any(), any());
		assertThat(service.status().state()).isEqualTo(CircuitBreaker.State.OPEN);
	}

	@Test
	void afterTheWaitOneTrialEmailClosesTheBreakerIfTheMailServerIsBack() throws InterruptedException {
		mailServerDown();
		service.notifyRouted(List.of(routedTo("Alice Chen"), routedTo("Brian Okonkwo")), NOW);

		Thread.sleep(WAIT.toMillis() * 2);
		doNothing().when(mailSender).send(any(SimpleMailMessage.class));
		NotificationService.Result result = service.notifyRouted(List.of(routedTo("Carla Rossi")), NOW);

		assertThat(result.sent()).isEqualTo(1);
		assertThat(service.status().state()).isEqualTo(CircuitBreaker.State.CLOSED);
	}

	@Test
	void withoutAMailServerEmailsAreOnlyLogged() {
		NotificationService withoutMail = newService(null);

		assertThat(withoutMail.notifyRouted(List.of(routedTo("Alice Chen")), NOW))
				.isEqualTo(new NotificationService.Result(0, 0, 0, false));
		assertThat(withoutMail.status().mailConfigured()).isFalse();
	}
}

package com.sysc4806.gradadmissions.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One professor's review of one application. The admin creates it when routing the application
 * to the professor; {@link #recommendation} stays empty until the professor submits an assessment.
 * Submitting an assessment never changes the application's status: only the admin does that.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(
		name = "uk_evaluation_application_professor", columnNames = {"application_id", "professor_id"}))
public class Evaluation {

	public static final int MAX_COMMENTS_LENGTH = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "application_id")
	private Application application;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "professor_id")
	private Professor professor;

	@NotNull
	@Column(nullable = false)
	private LocalDateTime assignedAt;

	/** Null until the professor submits their assessment. */
	@Enumerated(EnumType.STRING)
	private Recommendation recommendation;

	@Size(max = MAX_COMMENTS_LENGTH)
	@Column(length = MAX_COMMENTS_LENGTH)
	private String comments;

	private LocalDateTime submittedAt;

	/** When the professor was emailed; null if they weren't. */
	private LocalDateTime notifiedAt;

	public Evaluation(Application application, Professor professor, LocalDateTime assignedAt) {
		this.application = application;
		this.professor = professor;
		this.assignedAt = assignedAt;
	}

	public boolean isSubmitted() {
		return recommendation != null;
	}

	public void markNotified(LocalDateTime now) {
		this.notifiedAt = now;
	}

	/** Records the professor's assessment. It can be submitted only once. */
	public void submitAssessment(Recommendation recommendation, String comments, LocalDateTime now) {
		if (isSubmitted()) {
			throw new IllegalStateException("This assessment has already been submitted");
		}
		if (recommendation == null) {
			throw new IllegalArgumentException("Choose a recommendation");
		}
		String trimmed = comments == null || comments.isBlank() ? null : comments.strip();
		if (trimmed != null && trimmed.length() > MAX_COMMENTS_LENGTH) {
			throw new IllegalArgumentException("Comments can be at most " + MAX_COMMENTS_LENGTH + " characters");
		}
		this.recommendation = recommendation;
		this.comments = trimmed;
		this.submittedAt = now;
	}
}

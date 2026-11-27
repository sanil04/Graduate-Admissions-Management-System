package com.sysc4806.gradadmissions.model;

import java.time.LocalDate;
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

	/**
	 * Optimistic-lock counter. Two requests that both read the evaluation as unsubmitted can't both
	 * save it: the second update finds the version changed and fails. The column default lets
	 * {@code ddl-auto=update} add it to a table that already has rows.
	 */
	@Version
	@Column(nullable = false, columnDefinition = "bigint default 0")
	private long version;

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

	/** Still pending on a date after the cycle's evaluation deadline (the deadline date itself is not late). */
	public boolean isOverdue(LocalDate today) {
		return !isSubmitted() && !application.getCycle().isEvaluationOpenOn(today);
	}

	/**
	 * Records the professor's assessment. It can be submitted only once, and not after the cycle's
	 * evaluation deadline (it is allowed throughout the deadline's date).
	 */
	public void submitAssessment(Recommendation recommendation, String comments, LocalDateTime now) {
		if (isSubmitted()) {
			throw new IllegalStateException("This assessment has already been submitted");
		}
		application.getCycle().requireEvaluationOpen(now.toLocalDate());
		if (recommendation == null) {
			throw new IllegalArgumentException("Choose a recommendation");
		}
		// Browsers submit each line break as CRLF but count it as one character in maxlength,
		// so count (and store) a line break as one character here too.
		String normalized = comments == null ? null : comments.replace("\r\n", "\n").replace('\r', '\n');
		String trimmed = normalized == null || normalized.isBlank() ? null : normalized.strip();
		if (trimmed != null && trimmed.length() > MAX_COMMENTS_LENGTH) {
			throw new IllegalArgumentException("Comments can be at most " + MAX_COMMENTS_LENGTH + " characters");
		}
		this.recommendation = recommendation;
		this.comments = trimmed;
		this.submittedAt = now;
	}
}

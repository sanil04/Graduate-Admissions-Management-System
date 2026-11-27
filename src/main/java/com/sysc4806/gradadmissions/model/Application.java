package com.sysc4806.gradadmissions.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
public class Application {

	/** Team design assumption: the project spec only says "a list of profs". */
	public static final int MAX_PREFERRED_PROFESSORS = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(optional = false)
	private Applicant applicant;

	@NotNull
	@ManyToOne(optional = false)
	private AdmissionCycle cycle;

	@Setter
	@Valid
	@NotNull
	@Embedded
	private PersonalInfo personalInfo;

	@Setter
	@ManyToOne
	private ResearchField desiredField;

	@Size(max = MAX_PREFERRED_PROFESSORS)
	@ManyToMany
	@JoinTable(name = "application_preferred_professors",
			joinColumns = @JoinColumn(name = "application_id"),
			inverseJoinColumns = @JoinColumn(name = "professor_id"))
	private Set<Professor> preferredProfessors = new LinkedHashSet<>();

	@OneToMany(mappedBy = "application")
	private List<Document> documents = new ArrayList<>();

	@Setter
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ApplicationStatus status = ApplicationStatus.DRAFT;

	private LocalDateTime submittedAt;

	public Application(Applicant applicant, AdmissionCycle cycle, PersonalInfo personalInfo) {
		this.applicant = applicant;
		this.cycle = cycle;
		this.personalInfo = personalInfo;
	}

	/** Read-only so the limit can't be bypassed; use add/removePreferredProfessor. */
	public Set<Professor> getPreferredProfessors() {
		return Collections.unmodifiableSet(preferredProfessors);
	}

	public void addPreferredProfessor(Professor professor) {
		if (!preferredProfessors.contains(professor)
				&& preferredProfessors.size() >= MAX_PREFERRED_PROFESSORS) {
			throw new IllegalStateException("At most " + MAX_PREFERRED_PROFESSORS + " professors");
		}
		preferredProfessors.add(professor);
	}

	public void removePreferredProfessor(Professor professor) {
		preferredProfessors.remove(professor);
	}

	/** A draft needs a desired field and at least one professor to be submitted. */
	public void submit(LocalDateTime now) {
		if (status != ApplicationStatus.DRAFT) {
			throw new IllegalStateException("Only a draft can be submitted");
		}
		if (desiredField == null || preferredProfessors.isEmpty()) {
			throw new IllegalStateException("A desired field and at least one professor are required");
		}
		// Submissions are allowed throughout the deadline date.
		cycle.requireApplicationOpen(now.toLocalDate());
		status = ApplicationStatus.SUBMITTED;
		submittedAt = now;
	}

	public boolean isDocumentsEditable() {
		return status == ApplicationStatus.DRAFT;
	}

	/** Whether the admin can still route this application to (more) professors. */
	public boolean isRoutable() {
		return status == ApplicationStatus.SUBMITTED || status == ApplicationStatus.UNDER_REVIEW;
	}

	/**
	 * The admin routed the application to professors. It can be routed again while under review,
	 * to add professors.
	 */
	public void startReview() {
		if (!isRoutable()) {
			throw new IllegalStateException("Only a submitted application can be routed to professors");
		}
		status = ApplicationStatus.UNDER_REVIEW;
	}

	/** Whether the admin can still screen this application out. */
	public boolean isScreenable() {
		return status == ApplicationStatus.SUBMITTED;
	}

	/** The admin decided the application isn't good enough to send to professors. */
	public void screenOut() {
		if (!isScreenable()) {
			throw new IllegalStateException("Only a submitted application that hasn't been routed can be screened out");
		}
		status = ApplicationStatus.SCREENED_OUT;
	}
}

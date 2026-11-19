package com.sysc4806.gradadmissions.config;

import java.time.LocalDate;
import java.util.function.Supplier;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Admin;
import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.AdmissionCycle;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.model.User;
import com.sysc4806.gradadmissions.repository.AdmissionCycleRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;

/**
 * Creates the demo data on startup if it is missing. Safe to run on every start, including
 * against the production database. The demo logins are listed in the README for the TA.
 */
@Component
public class DataSeeder implements ApplicationRunner {

	public static final String ADMIN_EMAIL = "admin@gradadmissions.local";
	public static final String ADMIN_PASSWORD = "admin123";
	public static final String PROFESSOR_PASSWORD = "prof123";
	public static final String APPLICANT_EMAIL = "applicant@gradadmissions.local";
	public static final String APPLICANT_PASSWORD = "applicant123";

	private final ResearchFieldRepository researchFieldRepository;
	private final UserRepository userRepository;
	private final AdmissionCycleRepository admissionCycleRepository;
	private final PasswordEncoder passwordEncoder;

	public DataSeeder(ResearchFieldRepository researchFieldRepository,
			UserRepository userRepository,
			AdmissionCycleRepository admissionCycleRepository,
			PasswordEncoder passwordEncoder) {
		this.researchFieldRepository = researchFieldRepository;
		this.userRepository = userRepository;
		this.admissionCycleRepository = admissionCycleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		ResearchField ai = seedField("AI");
		ResearchField softwareEngineering = seedField("Software Engineering");
		ResearchField networks = seedField("Networks");
		ResearchField security = seedField("Security");

		seedUser(ADMIN_EMAIL, ADMIN_PASSWORD, () -> new Admin("System Admin", ADMIN_EMAIL));

		seedProfessor("Alice Chen", "achen@gradadmissions.local", ai, softwareEngineering);
		seedProfessor("Brian Okonkwo", "bokonkwo@gradadmissions.local", networks, security);
		seedProfessor("Carla Rossi", "crossi@gradadmissions.local", ai, security);

		// Until applicant registration exists (#37), this is the account used to apply.
		seedUser(APPLICANT_EMAIL, APPLICANT_PASSWORD, () -> new Applicant("Demo Applicant", APPLICANT_EMAIL));

		// Applications can only be submitted into an active cycle.
		if (admissionCycleRepository.findByActiveTrue().isEmpty()) {
			AdmissionCycle cycle = new AdmissionCycle("Fall 2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 1));
			cycle.setActive(true);
			admissionCycleRepository.save(cycle);
		}
	}

	private ResearchField seedField(String name) {
		return researchFieldRepository.findByName(name)
				.orElseGet(() -> researchFieldRepository.save(new ResearchField(name)));
	}

	private void seedProfessor(String name, String email, ResearchField... fields) {
		seedUser(email, PROFESSOR_PASSWORD, () -> {
			Professor professor = new Professor(name, email);
			for (ResearchField field : fields) {
				professor.getResearchFields().add(field);
			}
			return professor;
		});
	}

	/**
	 * Creates the user if no one has this email. Users seeded before passwords existed (already in
	 * the production database) have no password yet, so they get the demo password here too.
	 * A password that is already set is never overwritten.
	 */
	private void seedUser(String email, String password, Supplier<? extends User> newUser) {
		User user = userRepository.findByEmail(email).orElseGet(newUser);
		if (user.getPasswordHash() == null) {
			user.setPasswordHash(passwordEncoder.encode(password));
		}
		userRepository.save(user);
	}
}

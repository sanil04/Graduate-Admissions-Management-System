package com.sysc4806.gradadmissions.config;

import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sysc4806.gradadmissions.model.Admin;
import com.sysc4806.gradadmissions.model.AdmissionCycle;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.repository.AdminRepository;
import com.sysc4806.gradadmissions.repository.AdmissionCycleRepository;
import com.sysc4806.gradadmissions.repository.ProfessorRepository;
import com.sysc4806.gradadmissions.repository.ResearchFieldRepository;
import com.sysc4806.gradadmissions.repository.UserRepository;

@Component
public class DataSeeder implements ApplicationRunner {

	private final ResearchFieldRepository researchFieldRepository;
	private final AdminRepository adminRepository;
	private final ProfessorRepository professorRepository;
	private final UserRepository userRepository;
	private final AdmissionCycleRepository admissionCycleRepository;

	public DataSeeder(ResearchFieldRepository researchFieldRepository,
			AdminRepository adminRepository,
			ProfessorRepository professorRepository,
			UserRepository userRepository,
			AdmissionCycleRepository admissionCycleRepository) {
		this.researchFieldRepository = researchFieldRepository;
		this.adminRepository = adminRepository;
		this.professorRepository = professorRepository;
		this.userRepository = userRepository;
		this.admissionCycleRepository = admissionCycleRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		ResearchField ai = seedField("AI");
		ResearchField softwareEngineering = seedField("Software Engineering");
		ResearchField networks = seedField("Networks");
		ResearchField security = seedField("Security");

		seedAdmin("System Admin", "admin@gradadmissions.local");

		seedProfessor("Alice Chen", "achen@gradadmissions.local", ai, softwareEngineering);
		seedProfessor("Brian Okonkwo", "bokonkwo@gradadmissions.local", networks, security);
		seedProfessor("Carla Rossi", "crossi@gradadmissions.local", ai, security);

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

	private void seedAdmin(String name, String email) {
		if (userRepository.findByEmail(email).isEmpty()) {
			adminRepository.save(new Admin(name, email));
		}
	}

	private void seedProfessor(String name, String email, ResearchField... fields) {
		if (userRepository.findByEmail(email).isPresent()) {
			return;
		}
		Professor professor = new Professor(name, email);
		for (ResearchField field : fields) {
			professor.getResearchFields().add(field);
		}
		professorRepository.save(professor);
	}
}

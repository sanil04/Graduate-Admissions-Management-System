package com.sysc4806.gradadmissions.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.sysc4806.gradadmissions.model.Admin;
import com.sysc4806.gradadmissions.model.Applicant;
import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.User;

@DataJpaTest
class UserRepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private UserRepository users;

	@Autowired
	private ApplicantRepository applicants;

	@Autowired
	private ProfessorRepository professors;

	@Autowired
	private AdminRepository admins;

	@Test
	void findByEmailReturnsTheRightSubclass() {
		applicants.save(new Applicant("Ada Applicant", "ada@example.com"));
		professors.save(new Professor("Pat Professor", "pat@carleton.ca"));
		admins.save(new Admin("Sam Admin", "sam@carleton.ca"));
		em.flush();
		em.clear();

		assertThat(users.findAll()).hasSize(3);
		assertThat(users.findByEmail("ada@example.com")).get().isInstanceOf(Applicant.class);
		assertThat(users.findByEmail("pat@carleton.ca")).get().isInstanceOf(Professor.class);
		assertThat(users.findByEmail("sam@carleton.ca")).get().isInstanceOf(Admin.class);
		assertThat(users.findByEmail("nobody@example.com")).isEmpty();
	}

	@Test
	void subclassRepositoriesOnlySeeTheirOwnRole() {
		applicants.save(new Applicant("Ada Applicant", "ada@example.com"));
		professors.save(new Professor("Pat Professor", "pat@carleton.ca"));
		em.flush();
		em.clear();

		assertThat(applicants.findAll()).extracting(User::getEmail).containsExactly("ada@example.com");
		assertThat(professors.findAll()).extracting(User::getEmail).containsExactly("pat@carleton.ca");
		assertThat(admins.findAll()).isEmpty();
		assertThat(professors.findByEmail("ada@example.com")).isEmpty();
	}

	@Test
	void emailMustBeUniqueAcrossRoles() {
		applicants.saveAndFlush(new Applicant("Ada Applicant", "same@example.com"));

		assertThatThrownBy(() -> professors.saveAndFlush(new Professor("Pat", "same@example.com")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}

package com.sysc4806.gradadmissions.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.sysc4806.gradadmissions.model.Professor;
import com.sysc4806.gradadmissions.model.ResearchField;
import com.sysc4806.gradadmissions.model.User;

@DataJpaTest
class ProfessorRepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private ProfessorRepository professors;

	@Autowired
	private ResearchFieldRepository fields;

	@Test
	void researchFieldsArePersistedThroughJoinTable() {
		ResearchField ml = fields.save(new ResearchField("Machine Learning"));
		ResearchField networks = fields.save(new ResearchField("Networks"));
		Professor prof = new Professor("Pat Professor", "pat@carleton.ca");
		prof.addResearchField(ml);
		prof.addResearchField(networks);
		Long id = professors.save(prof).getId();
		em.flush();
		em.clear();

		Professor reloaded = professors.findById(id).orElseThrow();
		assertThat(reloaded.getResearchFields()).containsExactlyInAnyOrder(ml, networks);
	}

	@Test
	void removingAFieldUpdatesTheJoinTable() {
		ResearchField ml = fields.save(new ResearchField("Machine Learning"));
		Professor prof = new Professor("Pat Professor", "pat@carleton.ca");
		prof.addResearchField(ml);
		Long id = professors.saveAndFlush(prof).getId();

		prof.removeResearchField(ml);
		em.flush();
		em.clear();

		assertThat(professors.findById(id).orElseThrow().getResearchFields()).isEmpty();
		assertThat(fields.findById(ml.getId())).isPresent();
	}

	@Test
	void findByResearchFieldsContaining() {
		ResearchField ml = fields.save(new ResearchField("Machine Learning"));
		ResearchField networks = fields.save(new ResearchField("Networks"));
		Professor a = new Professor("Alice", "alice@carleton.ca");
		a.addResearchField(ml);
		Professor b = new Professor("Bob", "bob@carleton.ca");
		b.addResearchField(ml);
		b.addResearchField(networks);
		Professor c = new Professor("Carol", "carol@carleton.ca");
		c.addResearchField(networks);
		professors.saveAll(java.util.List.of(a, b, c));
		em.flush();
		em.clear();

		assertThat(professors.findByResearchFieldsContaining(ml))
				.extracting(User::getName).containsExactlyInAnyOrder("Alice", "Bob");
		assertThat(professors.findAllByOrderByNameAsc())
				.extracting(User::getName).containsExactly("Alice", "Bob", "Carol");
	}
}

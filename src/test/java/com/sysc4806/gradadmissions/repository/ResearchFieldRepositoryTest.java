package com.sysc4806.gradadmissions.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.sysc4806.gradadmissions.model.ResearchField;

@DataJpaTest
class ResearchFieldRepositoryTest {

	@Autowired
	private ResearchFieldRepository fields;

	@Test
	void findByNameIgnoresCase() {
		fields.save(new ResearchField("Machine Learning"));

		assertThat(fields.findByNameIgnoreCase("machine learning")).isPresent();
		assertThat(fields.findByNameIgnoreCase("Robotics")).isEmpty();
	}

	@Test
	void listsFieldsAlphabetically() {
		fields.save(new ResearchField("Networks"));
		fields.save(new ResearchField("Databases"));
		fields.save(new ResearchField("Machine Learning"));

		assertThat(fields.findAllByOrderByNameAsc()).extracting(ResearchField::getName)
				.containsExactly("Databases", "Machine Learning", "Networks");
	}

	@Test
	void namesAreUnique() {
		fields.saveAndFlush(new ResearchField("Networks"));

		assertThatThrownBy(() -> fields.saveAndFlush(new ResearchField("Networks")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}

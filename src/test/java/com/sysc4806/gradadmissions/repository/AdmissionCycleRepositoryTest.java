package com.sysc4806.gradadmissions.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.sysc4806.gradadmissions.model.AdmissionCycle;

@DataJpaTest
class AdmissionCycleRepositoryTest {

	private static final LocalDate DEADLINE = LocalDate.of(2027, 1, 15);

	@Autowired
	private AdmissionCycleRepository cycles;

	@Test
	void findsTheActiveCycle() {
		cycles.save(new AdmissionCycle("Fall 2026", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 1)));
		AdmissionCycle current = new AdmissionCycle("Fall 2027", DEADLINE, LocalDate.of(2027, 3, 1));
		current.setActive(true);
		cycles.save(current);

		assertThat(cycles.findFirstByActiveTrue()).get()
				.extracting(AdmissionCycle::getName).isEqualTo("Fall 2027");
	}

	@Test
	void noActiveCycle() {
		cycles.save(new AdmissionCycle("Fall 2026", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 1)));

		assertThat(cycles.findFirstByActiveTrue()).isEmpty();
	}

	@Test
	void acceptsApplicationsUntilTheEndOfTheDeadlineDay() {
		AdmissionCycle cycle = new AdmissionCycle("Fall 2027", DEADLINE, LocalDate.of(2027, 3, 1));
		cycle.setActive(true);

		assertThat(cycle.isAcceptingApplications(DEADLINE.minusDays(1))).isTrue();
		assertThat(cycle.isAcceptingApplications(DEADLINE)).isTrue();
		assertThat(cycle.isAcceptingApplications(DEADLINE.plusDays(1))).isFalse();

		cycle.setActive(false);
		assertThat(cycle.isAcceptingApplications(DEADLINE.minusDays(1))).isFalse();
	}
}

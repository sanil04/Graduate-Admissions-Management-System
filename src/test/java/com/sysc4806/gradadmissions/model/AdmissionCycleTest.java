package com.sysc4806.gradadmissions.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** The deadline rules of {@link AdmissionCycle}, without a database. Deadlines include their whole date. */
class AdmissionCycleTest {

    private static final LocalDate APPLICATION_DEADLINE = LocalDate.of(2027, 1, 15);
    private static final LocalDate EVALUATION_DEADLINE = LocalDate.of(2027, 3, 1);

    private final AdmissionCycle cycle = new AdmissionCycle("Fall 2027", APPLICATION_DEADLINE, EVALUATION_DEADLINE);

    @Test
    void applicationsAreOpenUpToAndIncludingTheDeadlineDate() {
        assertThat(cycle.isApplicationOpenOn(APPLICATION_DEADLINE.minusDays(1))).isTrue();
        assertThat(cycle.isApplicationOpenOn(APPLICATION_DEADLINE)).isTrue();
        assertThat(cycle.isApplicationOpenOn(APPLICATION_DEADLINE.plusDays(1))).isFalse();
    }

    @Test
    void evaluationsAreOpenUpToAndIncludingTheDeadlineDate() {
        assertThat(cycle.isEvaluationOpenOn(EVALUATION_DEADLINE.minusDays(1))).isTrue();
        assertThat(cycle.isEvaluationOpenOn(EVALUATION_DEADLINE)).isTrue();
        assertThat(cycle.isEvaluationOpenOn(EVALUATION_DEADLINE.plusDays(1))).isFalse();
    }

    @Test
    void theTwoDeadlinesAreIndependent() {
        LocalDate between = APPLICATION_DEADLINE.plusDays(10);

        assertThat(cycle.isApplicationOpenOn(between)).isFalse();
        assertThat(cycle.isEvaluationOpenOn(between)).isTrue();
    }

    @Test
    void requireApplicationOpenExplainsWhenTheDeadlineHasPassed() {
        assertThatCode(() -> cycle.requireApplicationOpen(APPLICATION_DEADLINE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> cycle.requireApplicationOpen(APPLICATION_DEADLINE.plusDays(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The application deadline has passed");
    }

    @Test
    void requireEvaluationOpenExplainsWhenTheDeadlineHasPassed() {
        assertThatCode(() -> cycle.requireEvaluationOpen(EVALUATION_DEADLINE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> cycle.requireEvaluationOpen(EVALUATION_DEADLINE.plusDays(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The evaluation deadline has passed");
    }
}

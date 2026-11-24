package com.sysc4806.gradadmissions.model;

/** A professor's assessment of an application, worded as in the project description. */
public enum Recommendation {

	DO_NOT_RECOMMEND("Don't recommend for admission"),
	RECOMMEND_NO_SUPERVISION("Recommend, but not interested in supervision"),
	RECOMMEND_NO_FUNDING("Recommend, but no funding"),
	RECOMMEND_WITH_FUNDING("Recommend, and yes to funding");

	private final String label;

	Recommendation(String label) {
		this.label = label;
	}

	/** Text to show on pages. */
	public String getLabel() {
		return label;
	}
}

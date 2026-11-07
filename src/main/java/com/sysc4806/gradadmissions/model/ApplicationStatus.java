package com.sysc4806.gradadmissions.model;

/**
 * Lifecycle of an {@link Application}. Only the admin moves an application past SUBMITTED.
 *
 * <pre>
 * DRAFT -&gt; SUBMITTED -&gt; SCREENED_OUT
 *                     -&gt; UNDER_REVIEW -&gt; ADMITTED
 *                                      -&gt; REJECTED
 * </pre>
 */
public enum ApplicationStatus {
	DRAFT,
	SUBMITTED,
	SCREENED_OUT,
	UNDER_REVIEW,
	ADMITTED,
	REJECTED
}

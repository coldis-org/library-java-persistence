package org.coldis.library.persistence.settings;

/**
 * PostgreSQL {@code plan_cache_mode} values, for {@link QuerySettings#planCacheMode()}.
 */
public enum PlanCacheMode {

	/** Leaves {@code plan_cache_mode} untouched (the session value applies). */
	UNCHANGED(null),

	/** PostgreSQL default: may switch a prepared statement to a generic plan after five executions. */
	AUTO("auto"),

	/**
	 * Plans every execution with the actual parameter values (plan-time partition pruning, estimates
	 * for skewed values), at the cost of planning each execution.
	 */
	FORCE_CUSTOM_PLAN("force_custom_plan"),

	/** Always uses the generic plan, ignoring the parameter values. */
	FORCE_GENERIC_PLAN("force_generic_plan");

	/** PostgreSQL value ({@code null} for {@link #UNCHANGED}). */
	private final String value;

	/**
	 * @param value PostgreSQL value.
	 */
	PlanCacheMode(final String value) {
		this.value = value;
	}

	/**
	 * @return The PostgreSQL value, or {@code null} for {@link #UNCHANGED}.
	 */
	public String getValue() {
		return this.value;
	}

}

package org.coldis.library.persistence.settings;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Settings {@link QuerySettings} may change: PostgreSQL parameters a regular user can set per
 * transaction that shape how queries are planned and executed. Anything else (connection, logging,
 * security, search path, custom placeholders) is rejected, so the annotation cannot be used to change
 * what a query reads or who it runs as.
 */
public final class QuerySettingNames {

	/** Planner method switches ({@code enable_nestloop}, {@code enable_seqscan}, ...). */
	private static final Pattern PLANNER_SWITCH = Pattern.compile("enable_[a-z_]+");

	/** Other allowed settings. */
	private static final Set<String> ALLOWED = Set.of(
			// Memory.
			"work_mem", "hash_mem_multiplier", "maintenance_work_mem",
			// Plan caching.
			"plan_cache_mode",
			// Planner costs and estimates.
			"random_page_cost", "seq_page_cost", "cpu_tuple_cost", "cpu_index_tuple_cost", "cpu_operator_cost", "effective_cache_size",
			"effective_io_concurrency", "cursor_tuple_fraction", "recursive_worktable_factor",
			// Join planning.
			"from_collapse_limit", "join_collapse_limit", "geqo", "geqo_threshold", "constraint_exclusion",
			// Parallel query.
			"max_parallel_workers_per_gather", "parallel_setup_cost", "parallel_tuple_cost", "min_parallel_table_scan_size",
			"min_parallel_index_scan_size", "parallel_leader_participation",
			// JIT.
			"jit", "jit_above_cost", "jit_inline_above_cost", "jit_optimize_above_cost",
			// Timeouts.
			"statement_timeout", "lock_timeout");

	/**
	 * No instances.
	 */
	private QuerySettingNames() {
	}

	/**
	 * Checks whether a (lower case) setting name may be changed by {@link QuerySettings}.
	 *
	 * @param  name Setting name.
	 * @return      Whether the name is allowed.
	 */
	public static boolean isAllowed(
			final String name) {
		return QuerySettingNames.ALLOWED.contains(name) || QuerySettingNames.PLANNER_SWITCH.matcher(name).matches();
	}

}

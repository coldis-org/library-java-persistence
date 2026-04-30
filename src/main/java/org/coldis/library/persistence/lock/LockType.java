package org.coldis.library.persistence.lock;

/**
 * Mechanism used by {@link LockServiceComponent} to acquire transaction-scoped locks on string
 * keys.
 *
 * <p>Both variants share the same API and {@link org.coldis.library.persistence.LockBehavior}
 * semantics — pick based on the cost/correctness trade-off below.
 */
public enum LockType {

	/**
	 * PostgreSQL advisory locks via {@code pg_advisory_xact_lock} / {@code pg_try_advisory_xact_lock}.
	 *
	 * <p><b>Pros:</b> pure in-memory in Postgres' lock manager, O(1) acquisition, no heap or WAL
	 * I/O, single-round-trip batches. Best choice for high-frequency locking on a bounded set of
	 * subsystem labels.
	 *
	 * <p><b>Cons:</b> the (namespace, key) pair is hashed to a 64-bit id, so unrelated keys can
	 * theoretically collide. The collision probability is negligible for typical key cardinalities,
	 * but the consequences differ by {@link org.coldis.library.persistence.LockBehavior}:
	 * <ul>
	 *   <li>{@code WAIT_AND_LOCK} — extra wait on the unrelated holder; self-corrects.</li>
	 *   <li>{@code LOCK_FAIL_FAST} — false failure surfaced as an exception; loud and recoverable.</li>
	 *   <li><b>{@code LOCK_SKIP} — silent skip of work that should have run; no signal, no retry.</b>
	 *       Prefer {@link #TABLE} when correctness under {@code LOCK_SKIP} matters.</li>
	 * </ul>
	 *
	 * <p><b>Observability:</b> shows up in {@code pg_locks} with {@code locktype='advisory'} and the
	 * numeric id only — you cannot recover the original string key from the lock view.
	 */
	ADVISORY,

	/**
	 * Row-level locks on a dedicated {@code lock_key} table. Each acquired lock is an INSERT into
	 * {@code lock_key} that is DELETEd via a {@code beforeCommit} hook of the surrounding
	 * transaction, so the table stays empty in steady state.
	 *
	 * <p><b>Pros:</b> compares strings literally — no hash collisions. Lock state is visible in
	 * normal SQL views (a row exists in {@code lock_key} for every currently-held lock) and in
	 * {@code pg_locks} as standard row-level locks. Use when {@code LOCK_SKIP} correctness matters
	 * or when you need to audit lock activity.
	 *
	 * <p><b>Cons:</b> every acquisition costs a real INSERT (heap + index + WAL) and a DELETE,
	 * versus advisory's pure-memory operation. Non-blocking modes ({@code LOCK_SKIP},
	 * {@code LOCK_FAIL_FAST}) are implemented via {@code SET LOCAL lock_timeout} and translate the
	 * resulting {@code 55P03} (lock_not_available) error into the matching response — practically
	 * equivalent to {@code NOWAIT}, but not literally instantaneous.
	 */
	TABLE

}

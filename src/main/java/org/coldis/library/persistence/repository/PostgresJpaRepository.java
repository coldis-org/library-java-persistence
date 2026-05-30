package org.coldis.library.persistence.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.persistence.LockBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * JPA repository with Postgres row-locking helpers.
 *
 * <p>Postgres' {@code SELECT … FOR UPDATE} only supports {@code NOWAIT} and {@code SKIP LOCKED} on
 * the locking clause — there is no native "wait up to N" timeout. So the bounded-wait variant
 * ({@link #findByIdForUpdateWait(Object, Duration)}) sets the transaction-local {@code lock_timeout}
 * GUC and then takes a plain {@code FOR UPDATE}; the others map straight to the JPA lock-timeout hint
 * ({@code 0} → {@code NOWAIT}, {@code -2} → {@code SKIP LOCKED}).
 *
 * <p>The lock variants require an active transaction (the caller's): a {@code FOR UPDATE} outside a
 * transaction has no one to hold the lock, and {@code set_config(…, is_local := true)} only affects
 * the current transaction. {@link #findByIdForRead(Object)} takes no lock and detaches its result,
 * so it is safe to call without a transaction (and won't leak a dirty entity into a later flush
 * under open-session-in-view).
 *
 * @param <T> Entity type.
 * @param <I> Identifier type.
 */
@NoRepositoryBean
public interface PostgresJpaRepository<T, I> extends JpaRepository<T, I> {

	/**
	 * Reads an entity by id with no lock and returns it <em>detached</em>. Use for read-only access:
	 * the detach guarantees the read has no side effects (no accidental {@code UPDATE} when an
	 * accessor mutates a mapped field, e.g. an expiration timestamp), even under open-session-in-view
	 * where the entity would otherwise stay managed for the whole request.
	 *
	 * @param  id Identifier.
	 * @return    The detached entity, if present.
	 */
	Optional<T> findByIdForRead(I id);

	/**
	 * Locks the row with {@code FOR UPDATE}, waiting indefinitely for any concurrent holder.
	 *
	 * @param  id Identifier.
	 * @return    The locked entity, if present.
	 */
	Optional<T> findByIdForUpdateWait(I id);

	/**
	 * Locks the row with {@code FOR UPDATE}, waiting at most {@code timeout} for a concurrent holder
	 * before failing (Postgres {@code lock_timeout}). On expiry the underlying
	 * {@code LockTimeoutException} surfaces as a Spring {@code PessimisticLockingFailureException}.
	 *
	 * @param  id      Identifier.
	 * @param  timeout Maximum time to wait for the lock.
	 * @return         The locked entity, if present.
	 */
	Optional<T> findByIdForUpdateWait(I id, Duration timeout);

	/**
	 * Locks the row with {@code FOR UPDATE SKIP LOCKED}: returns empty if the row is currently locked
	 * by another transaction instead of waiting.
	 *
	 * @param  id Identifier.
	 * @return    The locked entity, or empty if present-but-locked (or absent).
	 */
	Optional<T> findByIdForUpdateSkip(I id);

	/**
	 * Locks the row with {@code FOR UPDATE NOWAIT}: fails immediately (Spring
	 * {@code PessimisticLockingFailureException}) if the row is currently locked by another
	 * transaction.
	 *
	 * @param  id Identifier.
	 * @return    The locked entity, if present.
	 */
	Optional<T> findByIdForUpdateFail(I id);

	/**
	 * Takes a shared lock with {@code FOR SHARE} ({@code PESSIMISTIC_READ}): blocks concurrent
	 * writers but allows other shared readers.
	 *
	 * @param  id Identifier.
	 * @return    The shared-locked entity, if present.
	 */
	Optional<T> findByIdForShare(I id);

	/**
	 * Sets the transaction-local Postgres {@code lock_timeout} (applies to subsequent {@code FOR
	 * UPDATE} statements in this transaction).
	 *
	 * @param timeout Maximum time a lock acquisition should wait.
	 */
	void setLockTimeout(Duration timeout);

	/**
	 * Sets the transaction-local Postgres {@code statement_timeout} (bounds total execution time of
	 * subsequent statements in this transaction).
	 *
	 * @param timeout Maximum statement execution time.
	 */
	void setStatementTimeout(Duration timeout);

	/**
	 * Detaches an entity from the persistence context (so later changes are not flushed).
	 *
	 * @param entity Entity to detach.
	 */
	void detach(T entity);

	/**
	 * Re-reads an entity's state from the database, overwriting in-memory changes.
	 *
	 * @param entity Entity to refresh.
	 */
	void refresh(T entity);

	/**
	 * Atomically claims a time-based lease on a row — the building block for distributed single-flight
	 * (one worker per key runs an expensive operation while others back off). In one statement, sets
	 * the row's {@code leaseAttribute} to {@code now + lease} <em>iff</em> no live lease is held (the
	 * attribute is null or already in the past). Unlike a {@code FOR UPDATE} lock, the claim commits
	 * immediately and holds no connection or row lock while the caller does its (slow) work — at the
	 * cost of the lease being released by TTL rather than automatically: a crashed holder's lease
	 * frees once {@code now} passes the stored value, when the next claim succeeds.
	 *
	 * <p>{@code leaseAttribute} is whitelisted against the entity metamodel before use, so it is not a
	 * query-injection vector; it must name a persistent temporal attribute (e.g. a {@code LocalDateTime}
	 * column). Requires a single-attribute id.
	 *
	 * @param  id             Identifier.
	 * @param  leaseAttribute Name of the entity's lease (timestamp) attribute.
	 * @param  lease          How long to hold the lease.
	 * @return                {@code true} if the lease was claimed; {@code false} if a live lease is held.
	 */
	boolean claimLease(I id, String leaseAttribute, Duration lease);

	/**
	 * Releases a lease taken with {@link #claimLease} by clearing the row's {@code leaseAttribute}
	 * (idempotent). {@code leaseAttribute} is whitelisted against the entity metamodel.
	 *
	 * @param id             Identifier.
	 * @param leaseAttribute Name of the entity's lease (timestamp) attribute.
	 */
	void releaseLease(I id, String leaseAttribute);

	/**
	 * Bounded poll-to-claim: tries {@link #claimLease(Object, String, Duration)} immediately, then
	 * retries every {@code pollInterval} until it succeeds or {@code maxWait} elapses. Each attempt is
	 * its own short transaction and the waits hold no transaction or connection, so a waiter ties up
	 * no DB resources while another worker holds the lease.
	 *
	 * <p>MUST be called outside an ambient transaction (otherwise the inherited transaction would stay
	 * open across the sleeps). Returns as soon as the holder releases or its lease expires (the actual
	 * duration of the in-progress work), capped by {@code maxWait}.
	 *
	 * @param  id             Identifier.
	 * @param  leaseAttribute Name of the entity's lease (timestamp) attribute.
	 * @param  lease          How long to hold the lease once claimed.
	 * @param  pollInterval   How long to wait between claim attempts.
	 * @param  maxWait        Maximum total time to keep trying.
	 * @return                {@code true} if the lease was claimed within {@code maxWait}; {@code false} otherwise.
	 */
	default boolean claimLease(
			final I id,
			final String leaseAttribute,
			final Duration lease,
			final Duration pollInterval,
			final Duration maxWait) {
		final LocalDateTime deadline = DateTimeHelper.getCurrentLocalDateTime().plus(maxWait);
		boolean claimed = this.claimLease(id, leaseAttribute, lease);
		while (!claimed && DateTimeHelper.getCurrentLocalDateTime().isBefore(deadline)) {
			try {
				Thread.sleep(pollInterval.toMillis());
			}
			catch (final InterruptedException exception) {
				Thread.currentThread().interrupt();
				break;
			}
			claimed = this.claimLease(id, leaseAttribute, lease);
		}
		return claimed;
	}

	/**
	 * Reads the row by id with the given {@link LockBehavior}. Dispatches to the matching
	 * {@code findByIdFor…} method; for {@link LockBehavior#WAIT_AND_LOCK} a non-null {@code timeout}
	 * bounds the wait (otherwise it waits indefinitely).
	 *
	 * @param  id       Identifier.
	 * @param  behavior Lock behavior.
	 * @param  timeout  Bounded wait for {@code WAIT_AND_LOCK}; ignored by the other behaviors and may
	 *                      be {@code null}.
	 * @return          The entity, per the chosen behavior.
	 */
	default Optional<T> findById(
			final I id,
			final LockBehavior behavior,
			final Duration timeout) {
		final Optional<T> result;
		switch (behavior) {
			case NO_LOCK:
				result = this.findByIdForRead(id);
				break;
			case LOCK_FAIL_FAST:
				result = this.findByIdForUpdateFail(id);
				break;
			case LOCK_SKIP:
				result = this.findByIdForUpdateSkip(id);
				break;
			case WAIT_AND_LOCK:
			default:
				result = (timeout == null) ? this.findByIdForUpdateWait(id) : this.findByIdForUpdateWait(id, timeout);
				break;
		}
		return result;
	}

	/**
	 * Find-or-create under the row lock, race-safe — <em>optimistic</em> (find first). Shorthand for
	 * {@link #findByIdForUpdateOrCreate(Object, Runnable, boolean)} with {@code insertFirst = false}.
	 *
	 * @param  id              Identifier.
	 * @param  idempotentInsert Idempotent ({@code ON CONFLICT DO NOTHING}) insert for {@code id}.
	 * @return                 The existing or freshly created, locked entity.
	 */
	default T findByIdForUpdateOrCreate(
			final I id,
			final Runnable idempotentInsert) {
		return this.findByIdForUpdateOrCreate(id, idempotentInsert, false);
	}

	/**
	 * Find-or-create under the row lock, race-safe, with a choice of strategy.
	 *
	 * <p>The {@code idempotentInsert} MUST be an {@code INSERT … ON CONFLICT DO NOTHING} so it never
	 * throws — a throwing insert would abort the transaction on the create race and make the re-read
	 * impossible on Postgres. The insert stays with the caller because a generic {@code ON CONFLICT}
	 * cannot be built without entity-specific column knowledge.
	 *
	 * @param  id              Identifier.
	 * @param  idempotentInsert Idempotent ({@code ON CONFLICT DO NOTHING}) insert for {@code id}.
	 * @param  insertFirst     Strategy. {@code false} (optimistic, find first): lock the row, and only
	 *                             run the insert + re-lock on a miss — a single statement when the row
	 *                             already exists, so best for mostly-existing rows. {@code true}
	 *                             (pessimistic, insert first): run the idempotent insert first to
	 *                             guarantee the row, then take a single lock — best for mostly-absent
	 *                             rows, at the cost of always issuing the insert.
	 * @return                 The existing or freshly created, locked entity.
	 */
	default T findByIdForUpdateOrCreate(
			final I id,
			final Runnable idempotentInsert,
			final boolean insertFirst) {
		T entity;
		if (insertFirst) {
			idempotentInsert.run();
			entity = this.findByIdForUpdateWait(id).orElse(null);
		}
		else {
			entity = this.findByIdForUpdateWait(id).orElse(null);
			if (entity == null) {
				idempotentInsert.run();
				entity = this.findByIdForUpdateWait(id).orElse(null);
			}
		}
		if (entity == null) {
			throw new IllegalStateException("Row absent after idempotent insert for id: " + id);
		}
		return entity;
	}

}

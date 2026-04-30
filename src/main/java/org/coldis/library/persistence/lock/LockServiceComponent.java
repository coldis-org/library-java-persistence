package org.coldis.library.persistence.lock;

import java.util.Collection;

import org.coldis.library.exception.BusinessException;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.persistence.LockBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Acquires transaction-scoped locks for arbitrary string keys via either Postgres advisory locks
 * ({@link LockType#ADVISORY}, default) or row locks on a dedicated {@code lock_key} table
 * ({@link LockType#TABLE}). Used to serialize concurrent writers on application-defined keys
 * without locking actual domain table rows.
 *
 * <p>Locks are released automatically when the surrounding transaction commits or rolls back.
 * Callers must invoke this component from inside an active transaction.
 *
 * <p>Acquisition is deadlock-free across batches: keys within a single call are sorted before
 * locking, so two concurrent batches with overlapping keys always acquire them in the same order.
 *
 * <p>Acquisition mode mirrors {@link LockBehavior}: blocking wait, non-blocking skip, or
 * non-blocking fail-fast. See {@link LockType} for the trade-offs between the two
 * implementations — most importantly, {@code LOCK_SKIP} on {@code ADVISORY} can silently skip
 * work on hash collisions; prefer {@link LockType#TABLE} when that risk is unacceptable.
 */
@Service
public class LockServiceComponent {

	/** Default lock namespace. Use a subsystem-specific namespace to prevent cross-subsystem hash collisions. */
	public static final int DEFAULT_NAMESPACE = 0;

	/** Message code raised when a {@code LOCK_FAIL_FAST} acquisition cannot grab a lock. */
	public static final String LOCK_NOT_ACQUIRED_CODE = "lock.notacquired";

	/** Repository for {@link LockKey} rows and Postgres advisory primitives — owns the raw JDBC. */
	@Autowired
	private LockKeyRepository repository;

	// ---------------------------------------------------------------------------------------
	// Always-blocking convenience overloads — default to ADVISORY + WAIT_AND_LOCK.
	// ---------------------------------------------------------------------------------------

	/**
	 * Acquires advisory locks for every given key under the default namespace; blocks until held.
	 */
	public void lockKeysBlocking(
			final Collection<String> keys) throws BusinessException {
		this.lockKeys(LockBehavior.WAIT_AND_LOCK, LockType.ADVISORY, LockServiceComponent.DEFAULT_NAMESPACE, keys);
	}

	/**
	 * Acquires advisory locks for every given key under the given namespace label; blocks until
	 * held. The namespace string is hashed to a 32-bit int via {@link String#hashCode()} (which
	 * is spec-stable across JVMs).
	 */
	public void lockKeysBlocking(
			final String namespace,
			final Collection<String> keys) throws BusinessException {
		this.lockKeys(LockBehavior.WAIT_AND_LOCK, LockType.ADVISORY, namespace, keys);
	}

	/**
	 * Acquires advisory locks for every given key under the given numeric namespace; blocks
	 * until held.
	 */
	public void lockKeysBlocking(
			final int namespace,
			final Collection<String> keys) throws BusinessException {
		this.lockKeys(LockBehavior.WAIT_AND_LOCK, LockType.ADVISORY, namespace, keys);
	}

	// ---------------------------------------------------------------------------------------
	// Deprecated aliases — same name as behavior-aware overloads but different return type
	// (void vs boolean) made the call-site intent ambiguous. Use lockKeysBlocking(...) for the
	// always-blocking convenience forms.
	// ---------------------------------------------------------------------------------------

	/** @deprecated Use {@link #lockKeysBlocking(Collection)}. */
	@Deprecated
	public void lockKeys(
			final Collection<String> keys) throws BusinessException {
		this.lockKeysBlocking(keys);
	}

	/** @deprecated Use {@link #lockKeysBlocking(String, Collection)}. */
	@Deprecated
	public void lockKeys(
			final String namespace,
			final Collection<String> keys) throws BusinessException {
		this.lockKeysBlocking(namespace, keys);
	}

	/** @deprecated Use {@link #lockKeysBlocking(int, Collection)}. */
	@Deprecated
	public void lockKeys(
			final int namespace,
			final Collection<String> keys) throws BusinessException {
		this.lockKeysBlocking(namespace, keys);
	}

	// ---------------------------------------------------------------------------------------
	// LockBehavior-aware overloads — default to ADVISORY.
	// ---------------------------------------------------------------------------------------

	public boolean lockKeys(
			final LockBehavior behavior,
			final String namespace,
			final Collection<String> keys) throws BusinessException {
		return this.lockKeys(behavior, LockType.ADVISORY, namespace, keys);
	}

	public boolean lockKeys(
			final LockBehavior behavior,
			final int namespace,
			final Collection<String> keys) throws BusinessException {
		return this.lockKeys(behavior, LockType.ADVISORY, namespace, keys);
	}

	// ---------------------------------------------------------------------------------------
	// Full API: behavior + type + namespace + keys.
	// ---------------------------------------------------------------------------------------

	/**
	 * Acquires transaction-scoped locks following the given {@link LockBehavior} and
	 * {@link LockType}.
	 *
	 * @param  behavior          How to react to contention. {@code WAIT_AND_LOCK} /
	 *                           {@code NO_LOCK} / {@code null} block until acquired.
	 *                           {@code LOCK_SKIP} returns {@code false} if any lock could not be
	 *                           acquired immediately. {@code LOCK_FAIL_FAST} throws
	 *                           {@link BusinessException} (code {@link #LOCK_NOT_ACQUIRED_CODE})
	 *                           if any lock could not be acquired immediately.
	 * @param  type              Mechanism to use; see {@link LockType}.
	 * @param  namespace         Lock namespace label.
	 * @param  keys              Application-defined keys to lock.
	 * @return                   {@code true} if all locks were acquired; {@code false} only when
	 *                           {@code behavior == LOCK_SKIP} and at least one was held by another
	 *                           transaction.
	 * @throws BusinessException Only when {@code behavior == LOCK_FAIL_FAST} and at least one
	 *                           lock could not be acquired.
	 */
	public boolean lockKeys(
			final LockBehavior behavior,
			final LockType type,
			final String namespace,
			final Collection<String> keys) throws BusinessException {
		final boolean acquired;
		if (LockType.TABLE.equals(type)) {
			acquired = this.acquireTable(behavior, namespace, keys);
		}
		else {
			acquired = this.acquireAdvisory(behavior, namespace == null ? LockServiceComponent.DEFAULT_NAMESPACE : namespace.hashCode(), keys);
		}
		if (!acquired && LockBehavior.LOCK_FAIL_FAST.equals(behavior)) {
			throw new BusinessException(new SimpleMessage(LockServiceComponent.LOCK_NOT_ACQUIRED_CODE));
		}
		return acquired;
	}

	/**
	 * Acquires transaction-scoped locks following the given {@link LockBehavior} and
	 * {@link LockType}, with a numeric advisory namespace (used only when
	 * {@code type == ADVISORY}; for {@code TABLE} the integer is rendered as text).
	 */
	public boolean lockKeys(
			final LockBehavior behavior,
			final LockType type,
			final int namespace,
			final Collection<String> keys) throws BusinessException {
		final boolean acquired;
		if (LockType.TABLE.equals(type)) {
			acquired = this.acquireTable(behavior, Integer.toString(namespace), keys);
		}
		else {
			acquired = this.acquireAdvisory(behavior, namespace, keys);
		}
		if (!acquired && LockBehavior.LOCK_FAIL_FAST.equals(behavior)) {
			throw new BusinessException(new SimpleMessage(LockServiceComponent.LOCK_NOT_ACQUIRED_CODE));
		}
		return acquired;
	}

	// ---------------------------------------------------------------------------------------
	// Mode-specific orchestration — delegates the actual SQL to LockKeyRepository.
	// ---------------------------------------------------------------------------------------

	private boolean acquireAdvisory(
			final LockBehavior behavior,
			final int namespace,
			final Collection<String> keys) {
		boolean acquired = true;
		if ((keys != null) && !keys.isEmpty()) {
			if ((behavior == null) || LockBehavior.WAIT_AND_LOCK.equals(behavior) || LockBehavior.NO_LOCK.equals(behavior)) {
				this.repository.acquireAdvisoryBlocking(namespace, keys);
			}
			else {
				acquired = this.repository.acquireAdvisoryTry(namespace, keys);
			}
		}
		return acquired;
	}

	private boolean acquireTable(
			final LockBehavior behavior,
			final String namespace,
			final Collection<String> keys) {
		boolean acquired = true;
		if ((keys != null) && !keys.isEmpty()) {
			final String prefix = (namespace == null ? "" : namespace) + ":";
			final String[] ids = keys.stream()
					.map(k -> prefix + k)
					.sorted()
					.toArray(String[]::new);
			final boolean nonBlocking = LockBehavior.LOCK_SKIP.equals(behavior) || LockBehavior.LOCK_FAIL_FAST.equals(behavior);
			acquired = this.repository.acquireTableLock(ids, nonBlocking);
			if (acquired) {
				this.registerBeforeCommitDelete(ids);
			}
		}
		return acquired;
	}

	/**
	 * Registers a {@code beforeCommit} hook on the current transaction that DELETEs every
	 * inserted lock row before the transaction commits, so the {@code lock_key} table stays
	 * empty in steady state. If the transaction rolls back instead, the original INSERT is also
	 * rolled back — no row persists.
	 */
	private void registerBeforeCommitDelete(
			final String[] ids) {
		final LockKeyRepository repo = this.repository;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

			@Override
			public void beforeCommit(
					final boolean readOnly) {
				repo.releaseTableLock(ids);
			}

		});
	}

}

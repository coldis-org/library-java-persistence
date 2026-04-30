package org.coldis.library.persistence.lock;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import org.coldis.library.exception.BusinessException;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.persistence.LockBehavior;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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

	/** Postgres SQLState for "lock not available" — emitted when {@code lock_timeout} fires. */
	private static final String SQLSTATE_LOCK_NOT_AVAILABLE = "55P03";

	/** Effectively-zero wait used to make TABLE-mode INSERT non-blocking. */
	private static final String NON_BLOCKING_LOCK_TIMEOUT = "1ms";

	/** Entity manager. */
	@PersistenceContext
	private EntityManager entityManager;

	// ---------------------------------------------------------------------------------------
	// Convenience overloads — default to ADVISORY + WAIT_AND_LOCK.
	// ---------------------------------------------------------------------------------------

	/**
	 * Acquires advisory locks for every given key under the default namespace; blocks until held.
	 */
	public void lockKeys(
			final Collection<String> keys) throws BusinessException {
		this.lockKeys(LockBehavior.WAIT_AND_LOCK, LockType.ADVISORY, LockServiceComponent.DEFAULT_NAMESPACE, keys);
	}

	/**
	 * Acquires advisory locks for every given key under the given namespace label; blocks until
	 * held. The namespace string is hashed to a 32-bit int via {@link String#hashCode()} (which
	 * is spec-stable across JVMs).
	 */
	public void lockKeys(
			final String namespace,
			final Collection<String> keys) throws BusinessException {
		this.lockKeys(LockBehavior.WAIT_AND_LOCK, LockType.ADVISORY, namespace, keys);
	}

	/**
	 * Acquires advisory locks for every given key under the given numeric namespace; blocks
	 * until held.
	 */
	public void lockKeys(
			final int namespace,
			final Collection<String> keys) throws BusinessException {
		this.lockKeys(LockBehavior.WAIT_AND_LOCK, LockType.ADVISORY, namespace, keys);
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
	// ADVISORY mode.
	// ---------------------------------------------------------------------------------------

	private boolean acquireAdvisory(
			final LockBehavior behavior,
			final int namespace,
			final Collection<String> keys) {
		boolean acquired = true;
		if ((keys != null) && !keys.isEmpty()) {
			if ((behavior == null) || LockBehavior.WAIT_AND_LOCK.equals(behavior) || LockBehavior.NO_LOCK.equals(behavior)) {
				this.advisoryAcquireBlocking(namespace, keys);
			}
			else {
				acquired = this.advisoryAcquireTry(namespace, keys);
			}
		}
		return acquired;
	}

	private void advisoryAcquireBlocking(
			final int namespace,
			final Collection<String> keys) {
		final String[] keyArray = keys.toArray(new String[0]);
		this.entityManager.unwrap(Session.class).doWork(connection -> {
			final Array sqlArray = connection.createArrayOf("text", keyArray);
			try (PreparedStatement statement = connection.prepareStatement(
					"SELECT pg_advisory_xact_lock(?, hashtext(k)) "
							+ "FROM unnest(?) AS k "
							+ "ORDER BY hashtext(k)")) {
				statement.setInt(1, namespace);
				statement.setArray(2, sqlArray);
				statement.execute();
			}
			finally {
				sqlArray.free();
			}
		});
	}

	private boolean advisoryAcquireTry(
			final int namespace,
			final Collection<String> keys) {
		final String[] keyArray = keys.toArray(new String[0]);
		final boolean[] allAcquired = { true };
		this.entityManager.unwrap(Session.class).doWork(connection -> {
			final Array sqlArray = connection.createArrayOf("text", keyArray);
			try (PreparedStatement statement = connection.prepareStatement(
					"SELECT pg_try_advisory_xact_lock(?, hashtext(k)) "
							+ "FROM unnest(?) AS k "
							+ "ORDER BY hashtext(k)")) {
				statement.setInt(1, namespace);
				statement.setArray(2, sqlArray);
				try (ResultSet rs = statement.executeQuery()) {
					while (rs.next()) {
						if (!rs.getBoolean(1)) {
							allAcquired[0] = false;
						}
					}
				}
			}
			finally {
				sqlArray.free();
			}
		});
		return allAcquired[0];
	}

	// ---------------------------------------------------------------------------------------
	// TABLE mode.
	// ---------------------------------------------------------------------------------------

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
			acquired = this.tableInsertLockRows(ids, nonBlocking);
			if (acquired) {
				LockServiceComponent.registerBeforeCommitDelete(this.entityManager, ids);
			}
		}
		return acquired;
	}

	/**
	 * Inserts a lock row for every id in deterministic (sorted) order.
	 *
	 * <p>For non-blocking modes, the INSERT is wrapped in a savepoint and {@code lock_timeout}
	 * is set to {@value #NON_BLOCKING_LOCK_TIMEOUT}. If the timeout fires, Postgres marks the
	 * statement aborted with SQLState {@code 55P03} and the entire transaction goes into the
	 * "current transaction is aborted" state — the savepoint lets us {@code ROLLBACK TO} back
	 * out of the failed statement so the caller's transaction can continue normally.
	 * {@code lock_timeout} is reset to {@code 0} (no timeout) before returning so the rest of
	 * the surrounding transaction is unaffected.
	 */
	private boolean tableInsertLockRows(
			final String[] ids,
			final boolean nonBlocking) {
		final boolean[] acquired = { true };
		this.entityManager.unwrap(Session.class).doWork(connection -> {
			if (nonBlocking) {
				// SET LOCAL is issued before the savepoint so its reset (below) is unaffected
				// by the savepoint rollback.
				LockServiceComponent.executeStatement(connection, "SET LOCAL lock_timeout = '" + LockServiceComponent.NON_BLOCKING_LOCK_TIMEOUT + "'");
				LockServiceComponent.executeStatement(connection, "SAVEPOINT lock_attempt");
			}
			boolean inserted = false;
			SQLException toThrow = null;
			try (PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO lock_key (id) "
							+ "SELECT k FROM unnest(?) AS k ORDER BY k "
							+ "ON CONFLICT (id) DO NOTHING")) {
				final Array sqlArray = connection.createArrayOf("text", ids);
				try {
					statement.setArray(1, sqlArray);
					statement.executeUpdate();
					inserted = true;
				}
				finally {
					sqlArray.free();
				}
			}
			catch (final SQLException exception) {
				if (nonBlocking && LockServiceComponent.SQLSTATE_LOCK_NOT_AVAILABLE.equals(exception.getSQLState())) {
					acquired[0] = false;
				}
				else {
					toThrow = exception;
				}
			}
			if (nonBlocking) {
				if (!inserted) {
					// Either the lock_timeout fired or some other failure happened — either way
					// the tx is poisoned at the savepoint level; rolling back to the savepoint
					// clears it.
					LockServiceComponent.executeStatement(connection, "ROLLBACK TO SAVEPOINT lock_attempt");
				}
				LockServiceComponent.executeStatement(connection, "RELEASE SAVEPOINT lock_attempt");
				LockServiceComponent.executeStatement(connection, "SET LOCAL lock_timeout = '0'");
			}
			if (toThrow != null) {
				throw toThrow;
			}
		});
		return acquired[0];
	}

	private static void executeStatement(
			final java.sql.Connection connection,
			final String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	/**
	 * Registers a {@code beforeCommit} hook on the current transaction that DELETEs every
	 * inserted lock row before the transaction commits, so the {@code lock_key} table stays
	 * empty in steady state. If the transaction rolls back instead, the original INSERT is also
	 * rolled back — no row persists.
	 */
	private static void registerBeforeCommitDelete(
			final EntityManager entityManager,
			final String[] ids) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

			@Override
			public void beforeCommit(
					final boolean readOnly) {
				entityManager.unwrap(Session.class).doWork(connection -> {
					try (PreparedStatement statement = connection.prepareStatement("DELETE FROM lock_key WHERE id = ANY(?)")) {
						final Array sqlArray = connection.createArrayOf("text", ids);
						try {
							statement.setArray(1, sqlArray);
							statement.executeUpdate();
						}
						finally {
							sqlArray.free();
						}
					}
				});
			}

		});
	}

}

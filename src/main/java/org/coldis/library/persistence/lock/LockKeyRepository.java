package org.coldis.library.persistence.lock;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository for {@link LockKey} rows and Postgres advisory-lock primitives. Owns all raw JDBC
 * used by {@link LockServiceComponent}, so the service component stays focused on orchestration
 * and Spring transaction coordination.
 *
 * <p>All operations execute on the surrounding transaction's connection (acquired via the JPA
 * {@link EntityManager}). Callers must invoke from inside an active transaction.
 */
@Repository
public class LockKeyRepository {

	/** Postgres SQLState for "lock not available" — emitted when {@code lock_timeout} fires. */
	private static final String SQLSTATE_LOCK_NOT_AVAILABLE = "55P03";

	/** Effectively-zero wait used to make table-mode INSERT non-blocking. */
	private static final String NON_BLOCKING_LOCK_TIMEOUT = "1ms";

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Acquires Postgres advisory locks for every key in deterministic hash order, blocking until
	 * each is held. Used by {@link LockType#ADVISORY} blocking modes.
	 *
	 * @param namespace 32-bit namespace passed as the first argument to {@code pg_advisory_xact_lock}.
	 * @param keys      Application-defined keys to lock.
	 */
	public void acquireAdvisoryBlocking(
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

	/**
	 * Attempts to acquire Postgres advisory locks for every key, non-blocking. Returns
	 * {@code true} only if every key was acquired. Used by {@link LockType#ADVISORY}
	 * non-blocking modes ({@code LOCK_SKIP} / {@code LOCK_FAIL_FAST}).
	 */
	public boolean acquireAdvisoryTry(
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

	/**
	 * Inserts lock rows for every id in deterministic (sorted) order. Used by
	 * {@link LockType#TABLE} acquisition.
	 *
	 * <p>For non-blocking modes, the INSERT is wrapped in a savepoint and {@code lock_timeout}
	 * is set to {@value #NON_BLOCKING_LOCK_TIMEOUT}. If the timeout fires, Postgres marks the
	 * statement aborted with SQLState {@code 55P03} and the entire transaction goes into the
	 * "current transaction is aborted" state — the savepoint lets us {@code ROLLBACK TO} back
	 * out of the failed statement so the caller's transaction can continue normally.
	 * {@code lock_timeout} is reset to {@code 0} (no timeout) before returning so the rest of
	 * the surrounding transaction is unaffected.
	 *
	 * @param  ids         Lock-key ids to insert (caller has already prefixed namespace + sorted).
	 * @param  nonBlocking {@code true} to use the lock_timeout + savepoint path.
	 * @return             {@code true} when all rows were inserted (or already existed under the
	 *                     same transaction), {@code false} only when {@code nonBlocking} and
	 *                     another transaction holds an uncommitted conflicting row.
	 */
	public boolean acquireTableLock(
			final String[] ids,
			final boolean nonBlocking) {
		final boolean[] acquired = { true };
		this.entityManager.unwrap(Session.class).doWork(connection -> {
			if (nonBlocking) {
				// SET LOCAL is issued before the savepoint so its reset (below) is unaffected
				// by the savepoint rollback.
				LockKeyRepository.executeStatement(connection, "SET LOCAL lock_timeout = '" + LockKeyRepository.NON_BLOCKING_LOCK_TIMEOUT + "'");
				LockKeyRepository.executeStatement(connection, "SAVEPOINT lock_attempt");
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
				if (nonBlocking && LockKeyRepository.SQLSTATE_LOCK_NOT_AVAILABLE.equals(exception.getSQLState())) {
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
					LockKeyRepository.executeStatement(connection, "ROLLBACK TO SAVEPOINT lock_attempt");
				}
				LockKeyRepository.executeStatement(connection, "RELEASE SAVEPOINT lock_attempt");
				LockKeyRepository.executeStatement(connection, "SET LOCAL lock_timeout = '0'");
			}
			if (toThrow != null) {
				throw toThrow;
			}
		});
		return acquired[0];
	}

	/**
	 * Deletes lock rows by id. Used by {@link LockServiceComponent}'s {@code beforeCommit} hook
	 * so the table never retains a row past commit.
	 */
	public void releaseTableLock(
			final String[] ids) {
		this.entityManager.unwrap(Session.class).doWork(connection -> {
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

	private static void executeStatement(
			final Connection connection,
			final String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

}

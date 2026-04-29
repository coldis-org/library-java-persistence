package org.coldis.library.persistence.lock;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.Collection;

import org.hibernate.Session;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Acquires PostgreSQL transaction-scoped advisory locks for arbitrary string keys. Used to
 * serialize concurrent writers on application-defined keys without locking actual table rows.
 *
 * <p>Locks are released automatically when the surrounding transaction commits or rolls back.
 * Callers must invoke this component from inside an active transaction.
 *
 * <p>Acquisition is deadlock-free across batches: keys within a single call are sorted by hash
 * before locking, so two concurrent batches with overlapping keys always acquire them in the same
 * order.
 */
@Service
public class AdvisoryLockServiceComponent {

	/** Default lock namespace. Use a subsystem-specific namespace to prevent cross-subsystem hash collisions. */
	public static final int DEFAULT_NAMESPACE = 0;

	/** Entity manager. */
	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Acquires transaction-scoped advisory locks for every given key under the default namespace.
	 *
	 * @param keys Application-defined keys to lock.
	 */
	public void lockKeys(
			final Collection<String> keys) {
		this.lockKeys(AdvisoryLockServiceComponent.DEFAULT_NAMESPACE, keys);
	}

	/**
	 * Acquires transaction-scoped advisory locks for every given key under the given namespace.
	 * Different namespaces never collide, even on identical keys.
	 *
	 * @param namespace Lock namespace.
	 * @param keys      Application-defined keys to lock.
	 */
	public void lockKeys(
			final int namespace,
			final Collection<String> keys) {
		if ((keys == null) || keys.isEmpty()) {
			return;
		}
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

}

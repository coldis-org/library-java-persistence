package org.coldis.library.test.persistence.settings;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.coldis.library.persistence.settings.PlanCacheMode;
import org.coldis.library.persistence.settings.QuerySettings;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Reports, around {@link QuerySettings} calls on the primary and secondary datasources, when the
 * transaction takes its pooled connection and what the JPA queries of the transaction see (only when
 * the secondary datasource is configured).
 */
@Component
@ConditionalOnProperty(name = "org.coldis.configuration.persistence.datasources.secondary.url")
public class QuerySettingsConnectionServiceComponent {

	/** Key of the pool's active connections before the annotated call. */
	public static final String ACTIVE_BEFORE = "active_before";

	/** Key of the pool's active connections inside the annotated call. */
	public static final String ACTIVE_INSIDE = "active_inside";

	/** Key of the pool's active connections after the annotated call, in the same transaction. */
	public static final String ACTIVE_AFTER = "active_after";

	/** Key of work_mem as seen by a JPA query. */
	public static final String WORK_MEM = "work_mem";

	/** Key of plan_cache_mode as seen by a JPA query. */
	public static final String PLAN_CACHE_MODE = "plan_cache_mode";

	/** Key of the read-only flag as seen by a JPA query. */
	public static final String READ_ONLY = "transaction_read_only";

	/** Key of the isolation level as seen by a JPA query. */
	public static final String ISOLATION = "transaction_isolation";

	/** Key of the backend process id serving the JPA query. */
	public static final String BACKEND_PID = "backend_pid";

	/** Primary pool. */
	@Autowired
	private DataSource dataSource;

	/** Secondary pool. */
	@Autowired
	@Qualifier(value = "secondaryDataSource")
	private DataSource secondaryDataSource;

	/** Primary entity manager. */
	@PersistenceContext
	private EntityManager entityManager;

	/** Secondary entity manager. */
	@PersistenceContext(unitName = "secondary")
	private EntityManager secondaryEntityManager;

	/**
	 * Gets the connections a pool has handed out.
	 *
	 * @param  pool Pool.
	 * @return      The active connections.
	 */
	public static int getActiveConnections(
			final DataSource pool) {
		return ((HikariDataSource) pool).getHikariPoolMXBean().getActiveConnections();
	}

	/**
	 * Reads, with a native JPA query (Hibernate's own connection handling), the settings the
	 * transaction's statements run with.
	 *
	 * @param  probedEntityManager Entity manager of the probed unit.
	 * @param  pool                Pool of the probed unit.
	 * @return                     Values by key, plus {@link #ACTIVE_INSIDE}.
	 */
	private static Map<String, String> readTransactionSettings(
			final EntityManager probedEntityManager,
			final DataSource pool) {
		final Object[] row = (Object[]) probedEntityManager
				.createNativeQuery("SELECT current_setting('work_mem'), current_setting('plan_cache_mode'), current_setting('transaction_read_only'),"
						+ " current_setting('transaction_isolation'), pg_backend_pid()::text")
				.getSingleResult();
		final Map<String, String> values = new LinkedHashMap<>();
		values.put(QuerySettingsConnectionServiceComponent.WORK_MEM, (String) row[0]);
		values.put(QuerySettingsConnectionServiceComponent.PLAN_CACHE_MODE, (String) row[1]);
		values.put(QuerySettingsConnectionServiceComponent.READ_ONLY, (String) row[2]);
		values.put(QuerySettingsConnectionServiceComponent.ISOLATION, (String) row[3]);
		values.put(QuerySettingsConnectionServiceComponent.BACKEND_PID, (String) row[4]);
		values.put(QuerySettingsConnectionServiceComponent.ACTIVE_INSIDE,
				Integer.toString(QuerySettingsConnectionServiceComponent.getActiveConnections(pool)));
		return values;
	}

	/**
	 * Borrows pooled connections outside any transaction, holding each, until it gets the one with the
	 * given backend process id (at most the pool's maximum size), and reads the settings every borrowed
	 * connection runs with.
	 *
	 * @param  pool       Pool.
	 * @param  backendPid Backend process id of the connection to find.
	 * @return            work_mem by backend process id of every borrowed connection.
	 */
	private static Map<String, String> readSettingsOfNextBorrowers(
			final DataSource pool,
			final String backendPid) {
		final Map<String, String> workMemByBackendPid = new LinkedHashMap<>();
		final List<Connection> borrowedConnections = new ArrayList<>();
		final int maximumPoolSize = ((HikariDataSource) pool).getMaximumPoolSize();
		try {
			while (!workMemByBackendPid.containsKey(backendPid) && (borrowedConnections.size() < maximumPoolSize)) {
				final Connection connection = pool.getConnection();
				borrowedConnections.add(connection);
				try (Statement statement = connection.createStatement();
						ResultSet resultSet = statement.executeQuery("SELECT pg_backend_pid()::text, current_setting('work_mem')")) {
					resultSet.next();
					workMemByBackendPid.put(resultSet.getString(1), resultSet.getString(2));
				}
			}
		}
		catch (final SQLException exception) {
			throw new IllegalStateException(exception);
		}
		finally {
			for (final Connection borrowedConnection : borrowedConnections) {
				try {
					borrowedConnection.close();
				}
				catch (final SQLException exception) {
					throw new IllegalStateException(exception);
				}
			}
		}
		return workMemByBackendPid;
	}

	/**
	 * Gets a pool's session work_mem through a connection of its own, outside any transaction.
	 *
	 * @param  pool Pool.
	 * @return      work_mem.
	 */
	public static String getPoolWorkMem(
			final DataSource pool) {
		return new JdbcTemplate(pool).queryForObject("SELECT current_setting('work_mem')", String.class);
	}

	/**
	 * Asserts that the connection with the given backend process id, and every pooled connection
	 * borrowed before it, runs with the session work_mem outside a transaction.
	 *
	 * @param pool           Pool.
	 * @param backendPid     Backend process id of the connection the settings were applied on.
	 * @param sessionWorkMem Session work_mem.
	 */
	public static void assertNextBorrowersSeeDefaults(
			final DataSource pool,
			final String backendPid,
			final String sessionWorkMem) {
		final Map<String, String> workMemByBackendPid = QuerySettingsConnectionServiceComponent.readSettingsOfNextBorrowers(pool, backendPid);
		Assertions.assertTrue(workMemByBackendPid.containsKey(backendPid), workMemByBackendPid::toString);
		workMemByBackendPid.values().forEach(workMem -> Assertions.assertEquals(sessionWorkMem, workMem, workMemByBackendPid::toString));
	}

	/**
	 * Annotated, joining the caller's primary transaction: reads what a JPA query sees.
	 *
	 * @return Values by key.
	 */
	@QuerySettings(
			workMem = "61MB",
			planCacheMode = PlanCacheMode.FORCE_CUSTOM_PLAN
	)
	public Map<String, String> readPrimarySettings() {
		return QuerySettingsConnectionServiceComponent.readTransactionSettings(this.entityManager, this.dataSource);
	}

	/**
	 * Primary transaction that does other work first and then, optionally, calls the annotated method.
	 *
	 * @param  callAnnotatedMethod Whether to call the annotated method.
	 * @return                     Values by key, with the active connections before, inside and after.
	 */
	@Transactional
	public Map<String, String> primaryTransaction(
			final boolean callAnnotatedMethod) {
		final Map<String, String> values = new LinkedHashMap<>();
		values.put(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE,
				Integer.toString(QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource)));
		if (callAnnotatedMethod) {
			values.putAll(this.readPrimarySettings());
		}
		values.put(QuerySettingsConnectionServiceComponent.ACTIVE_AFTER,
				Integer.toString(QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource)));
		return values;
	}

	/**
	 * Read-only, serializable primary transaction calling the annotated method.
	 *
	 * @return Values by key, with the active connections before, inside and after.
	 */
	@Transactional(
			readOnly = true,
			isolation = Isolation.SERIALIZABLE
	)
	public Map<String, String> readOnlySerializableTransaction() {
		final Map<String, String> values = new LinkedHashMap<>();
		values.put(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE,
				Integer.toString(QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource)));
		values.putAll(this.readPrimarySettings());
		values.put(QuerySettingsConnectionServiceComponent.ACTIVE_AFTER,
				Integer.toString(QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource)));
		return values;
	}

	/**
	 * Read-only, repeatable read primary transaction on the annotated method itself.
	 *
	 * @return Values by key.
	 */
	@Transactional(
			readOnly = true,
			isolation = Isolation.REPEATABLE_READ
	)
	@QuerySettings(workMem = "62MB")
	public Map<String, String> readOnlyRepeatableReadAnnotatedTransaction() {
		return QuerySettingsConnectionServiceComponent.readTransactionSettings(this.entityManager, this.dataSource);
	}

	/**
	 * Annotated, opening its own read-only primary transaction.
	 *
	 * @return Values by key.
	 */
	@QuerySettings(
			workMem = "63MB",
			startTransaction = true
	)
	public Map<String, String> readPrimarySettingsInStartedTransaction() {
		return QuerySettingsConnectionServiceComponent.readTransactionSettings(this.entityManager, this.dataSource);
	}

	/**
	 * Annotated, joining the caller's secondary transaction: reads what a JPA query sees.
	 *
	 * @return Values by key.
	 */
	@QuerySettings(
			workMem = "74MB",
			datasource = "secondary"
	)
	public Map<String, String> readSecondarySettings() {
		return QuerySettingsConnectionServiceComponent.readTransactionSettings(this.secondaryEntityManager, this.secondaryDataSource);
	}

	/**
	 * Secondary transaction that does other work first and then, optionally, calls the annotated
	 * method.
	 *
	 * @param  callAnnotatedMethod Whether to call the annotated method.
	 * @return                     Values by key, with the active connections before, inside and after.
	 */
	@Transactional(transactionManager = "secondaryTransactionManager")
	public Map<String, String> secondaryTransaction(
			final boolean callAnnotatedMethod) {
		final Map<String, String> values = new LinkedHashMap<>();
		values.put(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE,
				Integer.toString(QuerySettingsConnectionServiceComponent.getActiveConnections(this.secondaryDataSource)));
		if (callAnnotatedMethod) {
			values.putAll(this.readSecondarySettings());
		}
		values.put(QuerySettingsConnectionServiceComponent.ACTIVE_AFTER,
				Integer.toString(QuerySettingsConnectionServiceComponent.getActiveConnections(this.secondaryDataSource)));
		return values;
	}

}

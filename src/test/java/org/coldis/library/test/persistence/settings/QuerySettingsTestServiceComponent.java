package org.coldis.library.test.persistence.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.coldis.library.persistence.settings.PlanCacheMode;
import org.coldis.library.persistence.settings.QuerySetting;
import org.coldis.library.persistence.settings.QuerySettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Methods annotated with {@link QuerySettings}, reporting what the database sees while they run.
 */
@Component
public class QuerySettingsTestServiceComponent {

	/** Settings read by the probes. */
	public static final List<String> PROBED_SETTINGS = List.of("work_mem", "plan_cache_mode", "enable_nestloop");

	/** Key of the backend process id in the probe results. */
	public static final String BACKEND_PID = "backend_pid";

	/** Key of the read-only flag in the probe results. */
	public static final String READ_ONLY = "read_only";

	/** Primary datasource. */
	@Autowired
	private DataSource dataSource;

	/**
	 * Reads the probed settings and the backend process id on the connection bound to the current
	 * transaction (or a fresh one outside a transaction).
	 *
	 * @param  probedDataSource Datasource to read from.
	 * @return                  Values by setting name, plus {@link #BACKEND_PID}.
	 */
	public static Map<String, String> readSettings(
			final DataSource probedDataSource) {
		final Map<String, String> values = new LinkedHashMap<>();
		final Connection connection = DataSourceUtils.getConnection(probedDataSource);
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT current_setting('work_mem'), current_setting('plan_cache_mode'), current_setting('enable_nestloop'), pg_backend_pid()::text")) {
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				for (int index = 0; index < QuerySettingsTestServiceComponent.PROBED_SETTINGS.size(); index++) {
					values.put(QuerySettingsTestServiceComponent.PROBED_SETTINGS.get(index), resultSet.getString(index + 1));
				}
				values.put(QuerySettingsTestServiceComponent.BACKEND_PID, resultSet.getString(4));
			}
		}
		catch (final SQLException exception) {
			throw new IllegalStateException(exception);
		}
		finally {
			DataSourceUtils.releaseConnection(connection, probedDataSource);
		}
		return values;
	}

	/**
	 * Reads the probed settings on the primary datasource.
	 *
	 * @return Values by setting name.
	 */
	public Map<String, String> readSettings() {
		return QuerySettingsTestServiceComponent.readSettings(this.dataSource);
	}

	/**
	 * Reads the settings in a transaction, without query settings.
	 *
	 * @return Values by setting name.
	 */
	@Transactional
	public Map<String, String> readSettingsInPlainTransaction() {
		return this.readSettings();
	}

	/**
	 * Reads the settings with all three kinds of attributes applied.
	 *
	 * @return Values by setting name.
	 */
	@Transactional
	@QuerySettings(
			workMem = "77MB",
			planCacheMode = PlanCacheMode.FORCE_CUSTOM_PLAN,
			settings = { @QuerySetting(name = "enable_nestloop", value = "off") }
	)
	public Map<String, String> readSettingsWithQuerySettings() {
		return this.readSettings();
	}

	/**
	 * Inner scope.
	 *
	 * @return Values by setting name.
	 */
	@QuerySettings(workMem = "22MB")
	public Map<String, String> readSettingsInInnerScope() {
		return this.readSettings();
	}

	/**
	 * Outer scope calling the inner one: records work_mem before, inside and after the inner call.
	 *
	 * @return work_mem values ("outer-before", "inner", "outer-after").
	 */
	@Transactional
	@QuerySettings(workMem = "11MB")
	public Map<String, String> readSettingsInNestedScopes() {
		final Map<String, String> values = new LinkedHashMap<>();
		values.put("outer-before", this.readSettings().get("work_mem"));
		values.put("inner", this.readSettingsInInnerScope().get("work_mem"));
		values.put("outer-after", this.readSettings().get("work_mem"));
		return values;
	}

	/**
	 * Throws once the settings are in effect.
	 *
	 * @return Never returns.
	 */
	@QuerySettings(workMem = "33MB")
	public Map<String, String> failWithQuerySettings() {
		throw new IllegalArgumentException("Failure inside work_mem " + this.readSettings().get("work_mem"));
	}

	/**
	 * Calls a failing annotated method, catches its error and reads the settings afterwards, in the
	 * same transaction.
	 *
	 * @return Values ("failure" message, "after" work_mem).
	 */
	@Transactional
	public Map<String, String> readSettingsAfterFailure() {
		final Map<String, String> values = new LinkedHashMap<>();
		try {
			this.failWithQuerySettings();
		}
		catch (final IllegalArgumentException exception) {
			values.put("failure", exception.getMessage());
		}
		values.put("after", this.readSettings().get("work_mem"));
		return values;
	}

	/**
	 * Fails with an SQL error once the settings are in effect, which aborts the transaction (so the
	 * values cannot be restored; the rollback discards them).
	 *
	 * @return Never returns.
	 */
	@Transactional
	@QuerySettings(workMem = "34MB")
	public Map<String, String> failWithSqlError() {
		final Connection connection = DataSourceUtils.getConnection(this.dataSource);
		try (PreparedStatement statement = connection.prepareStatement("SELECT 1 / 0")) {
			statement.executeQuery();
		}
		catch (final SQLException exception) {
			throw new IllegalStateException("SQL error inside the query settings scope.", exception);
		}
		finally {
			DataSourceUtils.releaseConnection(connection, this.dataSource);
		}
		return Map.of();
	}

	/**
	 * Annotated, without a transaction.
	 *
	 * @return Values by setting name.
	 */
	@QuerySettings(workMem = "44MB")
	public Map<String, String> readSettingsWithoutTransaction() {
		return this.readSettings();
	}

	/**
	 * Annotated, with the caller's transaction suspended.
	 *
	 * @return Values by setting name.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@QuerySettings(workMem = "45MB")
	public Map<String, String> readSettingsWithSuspendedTransaction() {
		return this.readSettings();
	}

	/**
	 * Annotated, opening its own transaction.
	 *
	 * @return Values by setting name, plus {@link #READ_ONLY}.
	 */
	@QuerySettings(
			workMem = "55MB",
			startTransaction = true
	)
	public Map<String, String> readSettingsInStartedTransaction() {
		final Map<String, String> values = this.readSettings();
		values.put(QuerySettingsTestServiceComponent.READ_ONLY, Boolean.toString(TransactionSynchronizationManager.isCurrentTransactionReadOnly()));
		return values;
	}

	/**
	 * Throws an unchecked exception inside a transaction it opens or joins.
	 */
	@QuerySettings(
			workMem = "57MB",
			startTransaction = true
	)
	public void failUncheckedInStartedTransaction() {
		throw new IllegalArgumentException("Unchecked failure.");
	}

	/**
	 * Throws a checked exception inside a transaction it opens or joins.
	 *
	 * @throws Exception Always.
	 */
	@QuerySettings(
			workMem = "58MB",
			startTransaction = true
	)
	public void failCheckedInStartedTransaction() throws Exception {
		throw new Exception("Checked failure.");
	}

	/**
	 * Annotated with a placeholder value.
	 *
	 * @return Values by setting name.
	 */
	@Transactional
	@QuerySettings(workMem = "${org.coldis.test.query-settings.work-mem:12MB}")
	public Map<String, String> readSettingsWithPlaceholder() {
		return this.readSettings();
	}

	/**
	 * Runs the same prepared statement repeatedly (past the driver's server-side prepare threshold) and
	 * returns how PostgreSQL planned its executions.
	 *
	 * @param  marker Unique marker identifying the statement.
	 * @return        "generic_plans" and "custom_plans" of the statement.
	 */
	private Map<String, Long> countPlans(
			final String marker) {
		final Map<String, Long> counts = new LinkedHashMap<>();
		final Connection connection = DataSourceUtils.getConnection(this.dataSource);
		try {
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT count(*) FROM generate_series(1, 100) AS value WHERE value = ? /* " + marker + " */")) {
				for (int execution = 0; execution < 12; execution++) {
					statement.setInt(1, execution);
					try (ResultSet resultSet = statement.executeQuery()) {
						resultSet.next();
					}
				}
			}
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT generic_plans, custom_plans FROM pg_prepared_statements WHERE statement LIKE ?")) {
				statement.setString(1, "%" + marker + "%");
				try (ResultSet resultSet = statement.executeQuery()) {
					resultSet.next();
					counts.put("generic_plans", resultSet.getLong(1));
					counts.put("custom_plans", resultSet.getLong(2));
				}
			}
		}
		catch (final SQLException exception) {
			throw new IllegalStateException(exception);
		}
		finally {
			DataSourceUtils.releaseConnection(connection, this.dataSource);
		}
		return counts;
	}

	/**
	 * Counts plans with custom plans forced.
	 *
	 * @param  marker Unique marker identifying the statement.
	 * @return        Plan counts.
	 */
	@Transactional
	@QuerySettings(planCacheMode = PlanCacheMode.FORCE_CUSTOM_PLAN)
	public Map<String, Long> countPlansWithCustomPlansForced(
			final String marker) {
		return this.countPlans(marker);
	}

	/**
	 * Counts plans with generic plans forced.
	 *
	 * @param  marker Unique marker identifying the statement.
	 * @return        Plan counts.
	 */
	@Transactional
	@QuerySettings(planCacheMode = PlanCacheMode.FORCE_GENERIC_PLAN)
	public Map<String, Long> countPlansWithGenericPlansForced(
			final String marker) {
		return this.countPlans(marker);
	}

}

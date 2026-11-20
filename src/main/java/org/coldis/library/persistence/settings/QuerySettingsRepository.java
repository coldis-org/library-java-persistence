package org.coldis.library.persistence.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the SQL used by {@link QuerySettingsServiceComponent}, which keeps the transaction and scope
 * orchestration. Statements run on the connection given by the caller (the connection of the
 * transaction active for the datasource the settings target), with names and values as bound
 * parameters.
 */
public class QuerySettingsRepository {

	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(QuerySettingsRepository.class);

	/** Reads the current values (one row per name). */
	private static final String READ_SETTINGS = "SELECT setting.name, current_setting(setting.name) FROM unnest(?::text[]) AS setting(name)";

	/** Sets transaction-local values (one row per name). */
	private static final String WRITE_SETTINGS = "SELECT set_config(setting.name, setting.value, true) FROM unnest(?::text[], ?::text[]) AS setting(name, value)";

	/**
	 * Reads the current values of settings.
	 *
	 * @param  connection   Connection of the active transaction.
	 * @param  names        Setting names.
	 * @return              Values by name.
	 * @throws SQLException If a setting does not exist or the statement fails.
	 */
	public Map<String, String> read(
			final Connection connection,
			final Set<String> names) throws SQLException {
		final Map<String, String> values = new LinkedHashMap<>();
		try (PreparedStatement statement = connection.prepareStatement(QuerySettingsRepository.READ_SETTINGS)) {
			statement.setArray(1, connection.createArrayOf("text", names.toArray(new String[] {})));
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					values.put(resultSet.getString(1), resultSet.getString(2));
				}
			}
		}
		return values;
	}

	/**
	 * Sets transaction-local values ({@code set_config(name, value, true)}).
	 *
	 * @param  connection   Connection of the active transaction.
	 * @param  values       Values by name.
	 * @throws SQLException If a value is invalid or the statement fails.
	 */
	public void write(
			final Connection connection,
			final Map<String, String> values) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(QuerySettingsRepository.WRITE_SETTINGS)) {
			statement.setArray(1, connection.createArrayOf("text", values.keySet().toArray(new String[] {})));
			statement.setArray(2, connection.createArrayOf("text", values.values().toArray(new String[] {})));
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					QuerySettingsRepository.LOGGER.debug("Query setting applied: {}.", resultSet.getString(1));
				}
			}
		}
	}

}

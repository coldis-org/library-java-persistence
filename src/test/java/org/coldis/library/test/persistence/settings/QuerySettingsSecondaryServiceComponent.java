package org.coldis.library.test.persistence.settings;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.coldis.library.persistence.settings.QuerySettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settings targeting the secondary datasource (only when it is configured).
 */
@Component
@ConditionalOnProperty(name = "org.coldis.configuration.persistence.datasources.secondary.url")
public class QuerySettingsSecondaryServiceComponent {

	/** Primary datasource. */
	@Autowired
	private DataSource dataSource;

	/** Secondary datasource. */
	@Autowired
	@Qualifier(value = "secondaryDataSource")
	private DataSource secondaryDataSource;

	/**
	 * Reads work_mem on both datasources inside a secondary transaction with secondary settings.
	 *
	 * @return work_mem by datasource ("primary", "secondary").
	 */
	@Transactional(transactionManager = "secondaryTransactionManager")
	@QuerySettings(
			workMem = "71MB",
			datasource = "secondary"
	)
	public Map<String, String> readSettingsOnSecondary() {
		final Map<String, String> values = new LinkedHashMap<>();
		values.put("primary", QuerySettingsTestServiceComponent.readSettings(this.dataSource).get("work_mem"));
		values.put("secondary", QuerySettingsTestServiceComponent.readSettings(this.secondaryDataSource).get("work_mem"));
		return values;
	}

	/**
	 * Secondary settings inside a primary transaction: no secondary transaction to apply them to.
	 *
	 * @return Never returns.
	 */
	@Transactional
	@QuerySettings(
			workMem = "72MB",
			datasource = "secondary"
	)
	public Map<String, String> readSettingsOnSecondaryInPrimaryTransaction() {
		return QuerySettingsTestServiceComponent.readSettings(this.secondaryDataSource);
	}

}

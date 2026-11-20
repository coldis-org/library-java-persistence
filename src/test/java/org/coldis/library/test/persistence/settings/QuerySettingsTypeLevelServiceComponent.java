package org.coldis.library.test.persistence.settings;

import java.util.Map;

import javax.sql.DataSource;

import org.coldis.library.persistence.settings.QuerySettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Annotated at type level, with one method overriding it.
 */
@Component
@QuerySettings(workMem = "66MB")
public class QuerySettingsTypeLevelServiceComponent {

	/** Primary datasource. */
	@Autowired
	private DataSource dataSource;

	/**
	 * Uses the type-level settings.
	 *
	 * @return Values by setting name.
	 */
	@Transactional
	public Map<String, String> readSettingsWithTypeSettings() {
		return QuerySettingsTestServiceComponent.readSettings(this.dataSource);
	}

	/**
	 * Uses its own settings instead of the type-level ones.
	 *
	 * @return Values by setting name.
	 */
	@Transactional
	@QuerySettings(workMem = "88MB")
	public Map<String, String> readSettingsWithMethodSettings() {
		return QuerySettingsTestServiceComponent.readSettings(this.dataSource);
	}

}

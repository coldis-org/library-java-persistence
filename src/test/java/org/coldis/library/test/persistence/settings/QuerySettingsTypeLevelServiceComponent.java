package org.coldis.library.test.persistence.settings;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.coldis.library.persistence.settings.QuerySettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

/**
 * Annotated at type level, with one method overriding it. Its {@code @PostConstruct} method and
 * {@code toString} run outside transactions, so the type-level settings must not cover them.
 */
@Component
@QuerySettings(workMem = "66MB")
public class QuerySettingsTypeLevelServiceComponent {

	/** Primary datasource. */
	@Autowired
	private DataSource dataSource;

	/** Whether the initialization callback ran. */
	private boolean initialized;

	/**
	 * Initialization callback (outside any transaction).
	 */
	@PostConstruct
	public void initialize() {
		this.initialized = true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "QuerySettingsTypeLevelServiceComponent[initialized=" + this.initialized + "]";
	}

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

	/**
	 * Calls another method of the class with the same type-level settings, recording work_mem inside
	 * and after the inner call.
	 *
	 * @return work_mem values ("inner", "outer-after").
	 */
	@Transactional
	public Map<String, String> readSettingsAroundOwnCall() {
		final Map<String, String> values = new LinkedHashMap<>();
		values.put("inner", this.readSettingsWithTypeSettings().get("work_mem"));
		values.put("outer-after", QuerySettingsTestServiceComponent.readSettings(this.dataSource).get("work_mem"));
		return values;
	}

}

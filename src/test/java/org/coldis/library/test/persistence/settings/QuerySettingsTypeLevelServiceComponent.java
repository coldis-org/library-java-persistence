package org.coldis.library.test.persistence.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.coldis.library.persistence.settings.QuerySettings;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Annotated at type level, with one method overriding it. Its framework callbacks (injection,
 * container and lifecycle callbacks, {@code Object} methods) run outside transactions, so the
 * type-level settings must not cover them: the context would not start otherwise.
 */
@Component
@QuerySettings(workMem = "66MB")
public class QuerySettingsTypeLevelServiceComponent implements BeanNameAware, InitializingBean {

	/** Primary datasource. */
	private DataSource dataSource;

	/** Callbacks that ran, in order. */
	private final List<String> callbacks = new ArrayList<>();

	/**
	 * Setter injection (outside any transaction).
	 *
	 * @param dataSource Primary datasource.
	 */
	@Autowired
	public void setDataSource(
			final DataSource dataSource) {
		this.dataSource = dataSource;
		this.callbacks.add("setDataSource");
	}

	/**
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	@Override
	public void setBeanName(
			final String name) {
		this.callbacks.add("setBeanName");
	}

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
		this.callbacks.add("afterPropertiesSet");
	}

	/**
	 * Initialization callback (outside any transaction).
	 */
	@PostConstruct
	public void initialize() {
		this.callbacks.add("initialize");
	}

	/**
	 * Destruction callback (outside any transaction).
	 */
	@PreDestroy
	public void release() {
		this.callbacks.add("release");
	}

	/**
	 * @return Callbacks that ran, in order.
	 */
	@QuerySettings(
			workMem = "1MB",
			startTransaction = true
	)
	public List<String> getCallbacks() {
		return new ArrayList<>(this.callbacks);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.dataSource);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(
			final Object other) {
		return (other instanceof final QuerySettingsTypeLevelServiceComponent component) && Objects.equals(this.dataSource, component.dataSource);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "QuerySettingsTypeLevelServiceComponent" + this.callbacks;
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

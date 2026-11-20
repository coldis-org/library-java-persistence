package org.coldis.library.persistence.settings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A validated {@link QuerySettings} annotation, with placeholders resolved and names in lower case.
 */
final class ResolvedQuerySettings {

	/** Datasource name (empty for the primary). */
	private final String datasource;

	/** Whether to open a transaction when none is active. */
	private final boolean startTransaction;

	/** Setting values by name, in declaration order. */
	private final Map<String, String> values;

	/**
	 * @param datasource       Datasource name (empty for the primary).
	 * @param startTransaction Whether to open a transaction when none is active.
	 * @param values           Setting values by name.
	 */
	ResolvedQuerySettings(
			final String datasource,
			final boolean startTransaction,
			final Map<String, String> values) {
		this.datasource = datasource;
		this.startTransaction = startTransaction;
		this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
	}

	/**
	 * @return Datasource name (empty for the primary).
	 */
	String getDatasource() {
		return this.datasource;
	}

	/**
	 * @return Whether to open a transaction when none is active.
	 */
	boolean isStartTransaction() {
		return this.startTransaction;
	}

	/**
	 * @return Setting values by name, in declaration order.
	 */
	Map<String, String> getValues() {
		return this.values;
	}

}

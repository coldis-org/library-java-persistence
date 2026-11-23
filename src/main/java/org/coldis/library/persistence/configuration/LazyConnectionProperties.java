package org.coldis.library.persistence.configuration;

/**
 * Lazy connection acquisition settings of one secondary datasource, bound from
 * {@code org.coldis.configuration.persistence.datasources.<name>.lazy-connection.*}.
 */
public class LazyConnectionProperties {

	/**
	 * Whether the unit's transactions take a pooled connection only at their first statement. Unset, the
	 * unit follows {@code org.coldis.configuration.persistence.lazy-connection.enabled}.
	 */
	private Boolean enabled;

	/**
	 * Gets whether lazy connection acquisition is enabled (null when unset).
	 *
	 * @return Whether lazy connection acquisition is enabled.
	 */
	public Boolean getEnabled() {
		return this.enabled;
	}

	/**
	 * Sets whether lazy connection acquisition is enabled.
	 *
	 * @param enabled Whether lazy connection acquisition is enabled.
	 */
	public void setEnabled(
			final Boolean enabled) {
		this.enabled = enabled;
	}

}

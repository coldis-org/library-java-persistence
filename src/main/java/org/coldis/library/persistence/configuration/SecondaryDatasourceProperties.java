package org.coldis.library.persistence.configuration;

/**
 * One secondary datasource entry, bound from
 * {@code org.coldis.configuration.persistence.datasources.<name>.*}. The map key is the datasource
 * name and drives the bean names ({@code <name>DataSource}, {@code <name>EntityManagerFactory},
 * {@code <name>TransactionManager}) and the {@link DatasourceUnit @DatasourceUnit} value that binds
 * entities and repositories to the unit. Pool tuning is bound separately from the
 * {@code <name>.hikari.*} sub-tree onto the Hikari datasource.
 */
public class SecondaryDatasourceProperties {

	/** JDBC url. */
	private String url;

	/** JDBC username. */
	private String username;

	/** JDBC password. */
	private String password;

	/** JDBC driver class name (optional; inferred from the url when absent). */
	private String driverClassName;

	public String getUrl() {
		return this.url;
	}

	public void setUrl(
			final String url) {
		this.url = url;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(
			final String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(
			final String password) {
		this.password = password;
	}

	public String getDriverClassName() {
		return this.driverClassName;
	}

	public void setDriverClassName(
			final String driverClassName) {
		this.driverClassName = driverClassName;
	}

}

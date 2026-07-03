package org.coldis.library.persistence.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * One secondary datasource entry, bound from
 * {@code org.coldis.configuration.persistence.datasources.<name>.*}. The map key is the datasource
 * name and drives the bean names ({@code <name>DataSource}, {@code <name>EntityManagerFactory},
 * {@code <name>TransactionManager}). Pool tuning is bound separately from the {@code <name>.hikari.*}
 * sub-tree onto the Hikari datasource.
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

	/** Entity packages mapped by this unit. */
	private List<String> entityPackages = new ArrayList<>();

	/** Repository packages bound to this unit (and excluded from the primary scan). */
	private List<String> repositoryPackages = new ArrayList<>();

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

	public List<String> getEntityPackages() {
		return this.entityPackages;
	}

	public void setEntityPackages(
			final List<String> entityPackages) {
		this.entityPackages = entityPackages;
	}

	public List<String> getRepositoryPackages() {
		return this.repositoryPackages;
	}

	public void setRepositoryPackages(
			final List<String> repositoryPackages) {
		this.repositoryPackages = repositoryPackages;
	}

}

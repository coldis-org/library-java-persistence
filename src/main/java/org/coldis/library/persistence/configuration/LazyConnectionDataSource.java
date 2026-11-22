package org.coldis.library.persistence.configuration;

import javax.sql.DataSource;

import org.springframework.core.InfrastructureProxy;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * Data source a JPA entity manager factory uses so that a transaction takes a pooled connection only
 * when it runs its first statement, not when it begins.
 *
 * <p>
 * It is Spring's {@link LazyConnectionDataSourceProxy}: the connection handed out is a proxy that
 * records auto-commit, isolation and read-only, and applies them to the pooled connection when the
 * first statement needs one. A transaction that never runs a statement never touches the pool;
 * commit and rollback on it are no-ops.
 * </p>
 *
 * <p>
 * It is also an {@link InfrastructureProxy} over the pooled data source. Spring binds the
 * transaction's connection under the transaction manager's data source, which for JPA is the entity
 * manager factory's (this proxy); unwrapping makes that key the pooled data source again, so JDBC code
 * using the pooled data source bean ({@code JdbcTemplate}, {@code DataSourceUtils}) keeps joining the
 * JPA transaction exactly as before.
 * </p>
 *
 * <p>
 * The pooled data source stays the application's {@code DataSource} bean: only the entity manager
 * factory sees this proxy, so pool metrics, pool names and code that unwraps the pool are unchanged.
 * </p>
 */
public class LazyConnectionDataSource extends LazyConnectionDataSourceProxy implements InfrastructureProxy {

	/**
	 * Wraps a pooled data source. The default auto-commit and isolation are read from the first
	 * connection taken through the proxy, not here, so building it does not touch the pool.
	 *
	 * @param targetDataSource Pooled data source.
	 */
	public LazyConnectionDataSource(
			final DataSource targetDataSource) {
		super();
		this.setTargetDataSource(targetDataSource);
	}

	/**
	 * @see org.springframework.core.InfrastructureProxy#getWrappedObject()
	 */
	@Override
	public Object getWrappedObject() {
		return this.obtainTargetDataSource();
	}

}

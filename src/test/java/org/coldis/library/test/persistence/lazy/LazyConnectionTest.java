package org.coldis.library.test.persistence.lazy;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.coldis.library.persistence.configuration.LazyConnectionDataSource;
import org.coldis.library.test.SpringTestHelper;
import org.coldis.library.test.StartTestWithContainerExtension;
import org.coldis.library.test.StopTestWithContainerExtension;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.TestWithContainer;
import org.coldis.library.test.persistence.TestApplication;
import org.coldis.library.test.persistence.model.TestEntity;
import org.coldis.library.test.persistence.model.TestEntityRepository;
import org.hibernate.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceUnwrapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.GenericContainer;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

/**
 * Lazy connection acquisition on the primary unit: a transaction takes a pooled connection at its
 * first statement, gives it back at commit or rollback, and keeps the transaction semantics it had
 * when the connection was taken at begin.
 */
@TestWithContainer
@ExtendWith(StartTestWithContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class
)
@ExtendWith(StopTestWithContainerExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LazyConnectionTest extends SpringTestHelper {

	/** Postgres container. */
	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	/** Artemis container. */
	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	/** Primary data source bean (the pool). */
	@Autowired
	private DataSource dataSource;

	/** Primary entity manager factory. */
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	/** Repository. */
	@Autowired
	private TestEntityRepository repository;

	/** Entity manager. */
	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Gets the primary pool.
	 *
	 * @return The primary pool.
	 */
	private HikariDataSource getPool() {
		return (HikariDataSource) this.dataSource;
	}

	/**
	 * Gets the connections the primary pool has handed out.
	 *
	 * @return The active connections.
	 */
	private int getActiveConnections() {
		return this.getPool().getHikariPoolMXBean().getActiveConnections();
	}

	/**
	 * Runs a statement on the current transaction's connection.
	 *
	 * @param  query Query returning one value.
	 * @return       The value.
	 */
	private Object querySingleResult(
			final String query) {
		return this.entityManager.createNativeQuery(query).getSingleResult();
	}

	/**
	 * Counts the persisted test entities through a connection of its own.
	 *
	 * @return The count.
	 */
	private long countEntities() {
		return this.repository.count();
	}

	/**
	 * A transaction that runs no statement.
	 *
	 * @return Active connections inside the transaction.
	 */
	@Transactional
	public int transactionWithoutStatement() {
		Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
		return this.getActiveConnections();
	}

	/**
	 * Reads a gauge inside a transaction that runs no statement.
	 *
	 * @param  gauge Gauge.
	 * @return       The gauge value inside the transaction.
	 */
	@Transactional
	public double gaugeInTransactionWithoutStatement(
			final Gauge gauge) {
		Assertions.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
		return gauge.value();
	}

	/**
	 * A transaction that runs one statement.
	 *
	 * @return Active connections before and after the statement.
	 */
	@Transactional
	public int[] transactionWithStatement() {
		final int before = this.getActiveConnections();
		this.querySingleResult("SELECT 1");
		final int after = this.getActiveConnections();
		return new int[] { before, after };
	}

	/**
	 * A transaction that writes and then fails.
	 *
	 * @return Never returns.
	 */
	@Transactional
	public int failingTransactionWithStatement() {
		this.repository.saveAndFlush(new TestEntity());
		Assertions.assertEquals(1, this.getActiveConnections());
		throw new IllegalStateException("Fails after writing.");
	}

	/**
	 * Inner participant that optionally writes and then fails.
	 *
	 * @param write Whether to write before failing.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void failingParticipant(
			final boolean write) {
		if (write) {
			this.repository.saveAndFlush(new TestEntity());
		}
		throw new IllegalStateException("Inner participant fails.");
	}

	/**
	 * Outer transaction that optionally writes, calls a failing participant and swallows its failure.
	 *
	 * @param write Whether the outer and the inner transactions write.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void outerSwallowingParticipantFailure(
			final boolean write) {
		if (write) {
			this.repository.saveAndFlush(new TestEntity());
		}
		try {
			this.failingParticipant(write);
		}
		catch (final IllegalStateException exception) {
			Assertions.assertEquals("Inner participant fails.", exception.getMessage());
		}
	}

	/**
	 * Read-only transaction.
	 *
	 * @return Active connections before the statement, then the database's read-only setting.
	 */
	@Transactional(readOnly = true)
	public Object[] readOnlyTransaction() {
		final int before = this.getActiveConnections();
		return new Object[] { before, this.querySingleResult("SELECT current_setting('transaction_read_only')") };
	}

	/**
	 * Serializable transaction.
	 *
	 * @return Active connections before the statement, then the database's isolation setting.
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE)
	public Object[] serializableTransaction() {
		final int before = this.getActiveConnections();
		return new Object[] { before, this.querySingleResult("SELECT current_setting('transaction_isolation')") };
	}

	/**
	 * Read-only, repeatable read transaction.
	 *
	 * @return Active connections before the statement, then the database's read-only and isolation
	 *         settings.
	 */
	@Transactional(
			readOnly = true,
			isolation = Isolation.REPEATABLE_READ
	)
	public Object[] readOnlyRepeatableReadTransaction() {
		final int before = this.getActiveConnections();
		return new Object[] { before, this.querySingleResult("SELECT current_setting('transaction_read_only')"),
				this.querySingleResult("SELECT current_setting('transaction_isolation')") };
	}

	/**
	 * Default transaction.
	 *
	 * @return The database's read-only and isolation settings.
	 */
	@Transactional
	public Object[] defaultTransaction() {
		return new Object[] { this.querySingleResult("SELECT current_setting('transaction_read_only')"),
				this.querySingleResult("SELECT current_setting('transaction_isolation')") };
	}

	/**
	 * Writes through the pooled data source bean inside a JPA transaction, then fails.
	 */
	@Transactional
	public void jdbcWriteInJpaTransactionThenFail() {
		this.repository.saveAndFlush(new TestEntity());
		new JdbcTemplate(this.dataSource).update("INSERT INTO lazy_connection_probe (id) VALUES (1)");
		throw new IllegalStateException("Fails after writing through JPA and JDBC.");
	}

	/**
	 * The entity manager factory sees the lazy proxy, while the data source bean stays the pool: the
	 * pool unwraps from the proxy (as Boot's pool metrics unwrap it) and its Micrometer gauge reads no
	 * active connection inside a transaction that runs no statement.
	 */
	@Test
	@DisplayName("the data source bean stays the pool and its metrics see no connection in a transaction without SQL")
	public void testPoolStaysTheDataSourceBean() {
		final DataSource factoryDataSource = ((EntityManagerFactoryInfo) this.entityManagerFactory).getDataSource();
		Assertions.assertInstanceOf(LazyConnectionDataSource.class, factoryDataSource);
		Assertions.assertInstanceOf(HikariDataSource.class, this.dataSource);
		Assertions.assertSame(this.dataSource, DataSourceUnwrapper.unwrap(factoryDataSource, HikariConfigMXBean.class, HikariDataSource.class));
		Assertions.assertTrue(this.getPool().getPoolName().startsWith("HikariPool-"));
		final SimpleMeterRegistry registry = new SimpleMeterRegistry();
		this.getPool().setMetricRegistry(registry);
		final Gauge activeGauge = registry.find("hikaricp.connections.active").tag("pool", this.getPool().getPoolName()).gauge();
		Assertions.assertNotNull(activeGauge);
		Assertions.assertEquals(0D, this.gaugeInTransactionWithoutStatement(activeGauge));
	}

	/**
	 * A transaction that runs no statement takes no pooled connection.
	 */
	@Test
	@DisplayName("a transaction that runs no statement takes no pooled connection")
	public void testNoConnectionWithoutStatement() {
		Assertions.assertEquals(0, this.getActiveConnections());
		Assertions.assertEquals(0, this.transactionWithoutStatement());
		Assertions.assertEquals(0, this.getActiveConnections());
	}

	/**
	 * A transaction takes a connection at its first statement and gives it back at commit and at
	 * rollback; the rolled-back write is not persisted.
	 */
	@Test
	@DisplayName("the connection is taken at the first statement and released at commit and rollback")
	public void testConnectionTakenAtFirstStatementAndReleasedAtEnd() {
		final int[] activeConnections = this.transactionWithStatement();
		Assertions.assertEquals(0, activeConnections[0]);
		Assertions.assertEquals(1, activeConnections[1]);
		Assertions.assertEquals(0, this.getActiveConnections());
		final long countBefore = this.countEntities();
		Assertions.assertThrows(IllegalStateException.class, this::failingTransactionWithStatement);
		Assertions.assertEquals(0, this.getActiveConnections());
		Assertions.assertEquals(countBefore, this.countEntities());
	}

	/**
	 * A failing inner participant whose failure the caller swallows still makes the outer commit throw
	 * {@link UnexpectedRollbackException}, with or without statements, and nothing is written.
	 */
	@Test
	@DisplayName("a swallowed participant failure still fails the outer commit with UnexpectedRollbackException")
	public void testSwallowedParticipantFailureRollsBackOuterTransaction() {
		final long countBefore = this.countEntities();
		Assertions.assertThrows(UnexpectedRollbackException.class, () -> this.outerSwallowingParticipantFailure(false));
		Assertions.assertEquals(0, this.getActiveConnections());
		Assertions.assertThrows(UnexpectedRollbackException.class, () -> this.outerSwallowingParticipantFailure(true));
		Assertions.assertEquals(0, this.getActiveConnections());
		Assertions.assertEquals(countBefore, this.countEntities());
	}

	/**
	 * Read-only and isolation still defer the connection, and are applied to it when it is taken; the
	 * connection goes back to the pool with the defaults restored.
	 */
	@Test
	@DisplayName("read-only and isolation stay lazy and are applied to the fetched connection")
	public void testReadOnlyAndIsolationAppliedToLazyConnection() {
		final Object[] readOnly = this.readOnlyTransaction();
		Assertions.assertEquals(0, readOnly[0]);
		Assertions.assertEquals("on", readOnly[1]);
		final Object[] serializable = this.serializableTransaction();
		Assertions.assertEquals(0, serializable[0]);
		Assertions.assertEquals("serializable", serializable[1]);
		final Object[] readOnlyRepeatableRead = this.readOnlyRepeatableReadTransaction();
		Assertions.assertEquals(0, readOnlyRepeatableRead[0]);
		Assertions.assertEquals("on", readOnlyRepeatableRead[1]);
		Assertions.assertEquals("repeatable read", readOnlyRepeatableRead[2]);
		final int maximumPoolSize = this.getPool().getMaximumPoolSize();
		for (int attempt = 0; attempt < maximumPoolSize; attempt++) {
			final Object[] defaults = this.defaultTransaction();
			Assertions.assertEquals("off", defaults[0]);
			Assertions.assertEquals("read committed", defaults[1]);
		}
		Assertions.assertEquals(0, this.getActiveConnections());
	}

	/**
	 * Counts the rows of a probe table through a pooled connection of its own.
	 *
	 * @param  table Table.
	 * @return       The count.
	 */
	private long countRows(
			final String table) {
		return new JdbcTemplate(this.dataSource).queryForObject("SELECT count(*) FROM " + table, Long.class);
	}

	/**
	 * Outside a transaction, writes keep auto-committing: through a connection of the factory's data
	 * source, and through a Hibernate session's connection, each write is visible to another
	 * connection without a commit. Repository writes stay refused outside a transaction, as before.
	 *
	 * @throws Exception If the test fails.
	 */
	@Test
	@DisplayName("writes outside a transaction keep auto-committing")
	public void testNonTransactionalWriteAutoCommits() throws Exception {
		Assertions.assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS lazy_connection_auto_commit (id BIGINT)");
		final DataSource factoryDataSource = ((EntityManagerFactoryInfo) this.entityManagerFactory).getDataSource();
		try (Connection connection = factoryDataSource.getConnection(); Statement statement = connection.createStatement()) {
			Assertions.assertTrue(connection.getAutoCommit());
			statement.execute("INSERT INTO lazy_connection_auto_commit (id) VALUES (1)");
			Assertions.assertEquals(1, this.countRows("lazy_connection_auto_commit"));
		}
		final EntityManager sessionEntityManager = this.entityManagerFactory.createEntityManager();
		try {
			sessionEntityManager.unwrap(Session.class).doWork(connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.execute("INSERT INTO lazy_connection_auto_commit (id) VALUES (2)");
				}
				Assertions.assertEquals(2, this.countRows("lazy_connection_auto_commit"));
			});
		}
		finally {
			sessionEntityManager.close();
		}
		Assertions.assertThrows(InvalidDataAccessApiUsageException.class, () -> this.repository.save(new TestEntity()));
		jdbcTemplate.execute("DROP TABLE lazy_connection_auto_commit");
		Assertions.assertEquals(0, this.getActiveConnections());
	}

	/**
	 * JDBC code on the pooled data source bean still joins the JPA transaction: its write rolls back
	 * with the transaction.
	 */
	@Test
	@DisplayName("JDBC on the pool bean still joins the JPA transaction")
	public void testJdbcOnPoolJoinsJpaTransaction() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS lazy_connection_probe (id BIGINT)");
		final long countBefore = this.countEntities();
		Assertions.assertThrows(IllegalStateException.class, this::jdbcWriteInJpaTransactionThenFail);
		Assertions.assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM lazy_connection_probe", Long.class));
		Assertions.assertEquals(countBefore, this.countEntities());
		jdbcTemplate.execute("DROP TABLE lazy_connection_probe");
		Assertions.assertEquals(0, this.getActiveConnections());
	}

}

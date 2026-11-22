package org.coldis.library.test.persistence.secondary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.coldis.library.persistence.configuration.LazyConnectionDataSource;
import org.coldis.library.test.SpringTestHelper;
import org.coldis.library.test.StartTestWithContainerExtension;
import org.coldis.library.test.StopTestWithContainerExtension;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.TestWithContainer;
import org.coldis.library.test.persistence.TestApplication;
import org.coldis.library.test.persistence.model.TestEntity;
import org.coldis.library.test.persistence.model.TestEntityService;
import org.coldis.library.test.persistence.secondary.model.TestSecondaryEntity;
import org.coldis.library.test.persistence.tertiary.TestTertiaryEntityRepository;
import org.coldis.library.test.persistence.tertiary.TestTertiaryEntityService;
import org.coldis.library.test.persistence.tertiary.model.TestTertiaryEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

/**
 * Multi-datasource integration test against <strong>three</strong> real PostgreSQL instances: the
 * explicit primary plus two property-driven secondaries ({@code secondary}, {@code tertiary}),
 * proving N secondaries. Unit membership is annotation-driven ({@code @DatasourceUnit} on the
 * entities and repositories; the properties carry only connection settings). It asserts routing and
 * isolation (a unit's entities exist ONLY in that unit's database) and that the primary keeps working
 * alongside them. The tertiary unit opts out of lazy connection acquisition, so it also proves the
 * per-datasource flag.
 */
@TestWithContainer
@ExtendWith(StartTestWithContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = {
				"test.properties",
				"org.coldis.configuration.persistence.explicit-primary=true",
				"org.coldis.configuration.persistence.datasources.secondary.url=jdbc:postgresql://localhost:${POSTGRES_CONTAINER_SECONDARY_5432}/test",
				"org.coldis.configuration.persistence.datasources.secondary.username=test",
				"org.coldis.configuration.persistence.datasources.secondary.password=test",
				"org.coldis.configuration.persistence.datasources.tertiary.lazy-connection.enabled=false",
				"org.coldis.configuration.persistence.datasources.tertiary.url=jdbc:postgresql://localhost:${POSTGRES_CONTAINER_TERTIARY_5432}/test",
				"org.coldis.configuration.persistence.datasources.tertiary.username=test",
				"org.coldis.configuration.persistence.datasources.tertiary.password=test" },
		classes = TestApplication.class)
@ExtendWith(StopTestWithContainerExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SecondaryDatasourceTest extends SpringTestHelper {

	/** Primary Postgres container. */
	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	/** Secondary Postgres container (a genuinely separate database). */
	public static GenericContainer<?> POSTGRES_CONTAINER_SECONDARY = TestHelper.createPostgresContainer();

	/** Tertiary Postgres container (a second, independent secondary database). */
	public static GenericContainer<?> POSTGRES_CONTAINER_TERTIARY = TestHelper.createPostgresContainer();

	/** Artemis container. */
	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	/** Secondary-datasource service (commits through the secondary transaction manager). */
	@Autowired
	private TestSecondaryEntityService testSecondaryEntityService;

	/** Secondary-datasource repository. */
	@Autowired
	private TestSecondaryEntityRepository testSecondaryEntityRepository;

	/** Tertiary-datasource service (commits through the tertiary transaction manager). */
	@Autowired
	private TestTertiaryEntityService testTertiaryEntityService;

	/** Tertiary-datasource repository. */
	@Autowired
	private TestTertiaryEntityRepository testTertiaryEntityRepository;

	/** Primary-datasource service. */
	@Autowired
	private TestEntityService testEntityService;

	/** Primary datasource (the {@code @Primary} bean → first container). */
	@Autowired
	private DataSource dataSource;

	/** Secondary datasource (registered by name from properties). */
	@Autowired
	@Qualifier(value = "secondaryDataSource")
	private DataSource secondaryDataSource;

	/** Tertiary datasource (registered by name from properties). */
	@Autowired
	@Qualifier(value = "tertiaryDataSource")
	private DataSource tertiaryDataSource;

	/** Primary entity manager factory (the explicit primary). */
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	/** Secondary entity manager factory. */
	@Autowired
	@Qualifier(value = "secondaryEntityManagerFactory")
	private EntityManagerFactory secondaryEntityManagerFactory;

	/** Tertiary entity manager factory. */
	@Autowired
	@Qualifier(value = "tertiaryEntityManagerFactory")
	private EntityManagerFactory tertiaryEntityManagerFactory;

	/** Secondary entity manager. */
	@PersistenceContext(unitName = "secondary")
	private EntityManager secondaryEntityManager;

	/**
	 * Gets the connections a pool has handed out.
	 *
	 * @param  pool Pool data source.
	 * @return      The active connections.
	 */
	private int getActiveConnections(
			final DataSource pool) {
		return ((HikariDataSource) pool).getHikariPoolMXBean().getActiveConnections();
	}

	/**
	 * Secondary transaction that runs no statement, then one.
	 *
	 * @return Secondary active connections before and after the statement.
	 */
	@Transactional(transactionManager = "secondaryTransactionManager")
	public int[] secondaryTransactionWithStatement() {
		final int before = this.getActiveConnections(this.secondaryDataSource);
		this.secondaryEntityManager.createNativeQuery("SELECT 1").getSingleResult();
		final int after = this.getActiveConnections(this.secondaryDataSource);
		return new int[] { before, after };
	}

	/**
	 * Secondary participant that optionally writes and then fails.
	 *
	 * @param write Whether to write before failing.
	 */
	@Transactional(
			transactionManager = "secondaryTransactionManager",
			propagation = Propagation.REQUIRED
	)
	public void secondaryFailingParticipant(
			final boolean write) {
		if (write) {
			final TestSecondaryEntity entity = new TestSecondaryEntity();
			entity.setName("rolled-back");
			this.testSecondaryEntityRepository.saveAndFlush(entity);
		}
		throw new IllegalStateException("Inner participant fails.");
	}

	/**
	 * Secondary outer transaction that calls a failing participant and swallows its failure.
	 *
	 * @param write Whether the inner participant writes.
	 */
	@Transactional(
			transactionManager = "secondaryTransactionManager",
			propagation = Propagation.REQUIRED
	)
	public void secondaryOuterSwallowingParticipantFailure(
			final boolean write) {
		try {
			this.secondaryFailingParticipant(write);
		}
		catch (final IllegalStateException exception) {
			Assertions.assertEquals("Inner participant fails.", exception.getMessage());
		}
	}

	/**
	 * Tertiary transaction that runs no statement.
	 *
	 * @return Tertiary active connections inside the transaction.
	 */
	@Transactional(transactionManager = "tertiaryTransactionManager")
	public int tertiaryTransactionWithoutStatement() {
		return this.getActiveConnections(this.tertiaryDataSource);
	}

	/**
	 * The explicit primary and the secondary unit take connections lazily, through the pool beans;
	 * the tertiary unit, opted out, keeps its factory on the pool and takes the connection at begin.
	 */
	@Test
	@DisplayName("secondary units are lazy unless opted out per datasource")
	public void testLazyConnectionPerDatasource() {
		Assertions.assertInstanceOf(LazyConnectionDataSource.class, ((EntityManagerFactoryInfo) this.entityManagerFactory).getDataSource());
		Assertions.assertInstanceOf(LazyConnectionDataSource.class, ((EntityManagerFactoryInfo) this.secondaryEntityManagerFactory).getDataSource());
		Assertions.assertSame(this.tertiaryDataSource, ((EntityManagerFactoryInfo) this.tertiaryEntityManagerFactory).getDataSource());
		Assertions.assertInstanceOf(HikariDataSource.class, this.secondaryDataSource);
		Assertions.assertEquals(1, this.tertiaryTransactionWithoutStatement());
		Assertions.assertEquals(0, this.getActiveConnections(this.tertiaryDataSource));
	}

	/**
	 * A secondary transaction takes its connection at the first statement and gives it back at commit;
	 * a swallowed participant failure still makes the outer commit throw
	 * {@link UnexpectedRollbackException}, and nothing is written.
	 */
	@Test
	@DisplayName("a lazy secondary unit keeps its transaction semantics")
	public void testSecondaryLazyConnectionKeepsTransactionSemantics() {
		final int[] activeConnections = this.secondaryTransactionWithStatement();
		Assertions.assertEquals(0, activeConnections[0]);
		Assertions.assertEquals(1, activeConnections[1]);
		Assertions.assertEquals(0, this.getActiveConnections(this.secondaryDataSource));
		final long countBefore = this.testSecondaryEntityRepository.count();
		Assertions.assertThrows(UnexpectedRollbackException.class, () -> this.secondaryOuterSwallowingParticipantFailure(false));
		Assertions.assertThrows(UnexpectedRollbackException.class, () -> this.secondaryOuterSwallowingParticipantFailure(true));
		Assertions.assertEquals(0, this.getActiveConnections(this.secondaryDataSource));
		Assertions.assertEquals(countBefore, this.testSecondaryEntityRepository.count());
	}

	/**
	 * The three datasources are genuinely distinct beans pointing at different databases.
	 */
	@Test
	public void testDatasourcesAreDistinct() {
		Assertions.assertNotSame(this.dataSource, this.secondaryDataSource);
		Assertions.assertNotSame(this.dataSource, this.tertiaryDataSource);
		Assertions.assertNotSame(this.secondaryDataSource, this.tertiaryDataSource);
	}

	/**
	 * A row written through the secondary unit is readable through the secondary unit, and the
	 * secondary entity's table exists <strong>only</strong> in the secondary database — the annotated
	 * entity is not even mapped by the primary or tertiary units.
	 */
	@Test
	public void testSecondaryRoutingAndIsolation() throws Exception {
		final TestSecondaryEntity entity = new TestSecondaryEntity();
		entity.setName("secondary-only");
		final TestSecondaryEntity saved = this.testSecondaryEntityService.save(entity);
		Assertions.assertNotNull(saved.getId());
		Assertions.assertEquals("secondary-only", this.testSecondaryEntityRepository.findById(saved.getId()).orElseThrow().getName());
		Assertions.assertEquals(1, this.testSecondaryEntityRepository.count());
		Assertions.assertFalse(this.tableExists(this.dataSource, "test_secondary_entity"));
		Assertions.assertFalse(this.tableExists(this.tertiaryDataSource, "test_secondary_entity"));
	}

	/**
	 * A row written through the tertiary unit lands only in the tertiary database — proving a second,
	 * independent secondary datasource (N &gt; 1) with its own annotation-selected entities.
	 */
	@Test
	public void testTertiaryRoutingAndIsolation() throws Exception {
		final TestTertiaryEntity entity = new TestTertiaryEntity();
		entity.setName("tertiary-only");
		final TestTertiaryEntity saved = this.testTertiaryEntityService.save(entity);
		Assertions.assertNotNull(saved.getId());
		Assertions.assertEquals("tertiary-only", this.testTertiaryEntityRepository.findById(saved.getId()).orElseThrow().getName());
		Assertions.assertEquals(1, this.testTertiaryEntityRepository.count());
		Assertions.assertFalse(this.tableExists(this.dataSource, "test_tertiary_entity"));
		Assertions.assertFalse(this.tableExists(this.secondaryDataSource, "test_tertiary_entity"));
	}

	/**
	 * The primary unit still works while secondary units are configured, and primary entities do not
	 * leak into the secondary schemas.
	 */
	@Test
	public void testPrimaryUnitStillWorks() throws Exception {
		TestEntity entity = new TestEntity();
		entity = this.testEntityService.save(entity);
		Assertions.assertNotNull(entity.getId());
		Assertions.assertNotNull(entity.getCreatedAt());
		Assertions.assertTrue(this.tableExists(this.dataSource, "test_entity"));
		Assertions.assertFalse(this.tableExists(this.secondaryDataSource, "test_entity"));
		Assertions.assertFalse(this.tableExists(this.tertiaryDataSource, "test_entity"));
	}

	/**
	 * Checks whether a table exists in the given datasource's database.
	 *
	 * @param  dataSource Datasource to query.
	 * @param  table      Table name.
	 * @return            Whether the table exists.
	 * @throws Exception  If the query fails.
	 */
	private boolean tableExists(
			final DataSource dataSource,
			final String table) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM information_schema.tables WHERE table_name = ?")) {
			statement.setString(1, table);
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return (resultSet.getLong(1) > 0);
			}
		}
	}

}

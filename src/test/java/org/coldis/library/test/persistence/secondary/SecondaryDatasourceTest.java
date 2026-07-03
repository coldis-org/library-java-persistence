package org.coldis.library.test.persistence.secondary;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;

/**
 * Multi-datasource integration test against <strong>three</strong> real PostgreSQL instances: the
 * explicit primary plus two property-driven secondaries ({@code secondary}, {@code tertiary}),
 * proving N secondaries. It asserts routing and isolation (a row written through one unit lands ONLY
 * in that unit's database) and that the primary keeps working alongside them.
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
				"org.coldis.configuration.persistence.datasources.secondary.entity-packages=org.coldis.library.test.persistence.secondary.model",
				"org.coldis.configuration.persistence.datasources.secondary.repository-packages=org.coldis.library.test.persistence.secondary",
				"org.coldis.configuration.persistence.datasources.tertiary.url=jdbc:postgresql://localhost:${POSTGRES_CONTAINER_TERTIARY_5432}/test",
				"org.coldis.configuration.persistence.datasources.tertiary.username=test",
				"org.coldis.configuration.persistence.datasources.tertiary.password=test",
				"org.coldis.configuration.persistence.datasources.tertiary.entity-packages=org.coldis.library.test.persistence.tertiary.model",
				"org.coldis.configuration.persistence.datasources.tertiary.repository-packages=org.coldis.library.test.persistence.tertiary" },
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
	 * A row written through the secondary unit is readable through the secondary unit and is
	 * <strong>absent</strong> from the primary database.
	 */
	@Test
	public void testSecondaryRoutingAndIsolation() throws Exception {
		final TestSecondaryEntity entity = new TestSecondaryEntity();
		entity.setName("secondary-only");
		final TestSecondaryEntity saved = this.testSecondaryEntityService.save(entity);
		Assertions.assertNotNull(saved.getId());
		Assertions.assertEquals("secondary-only", this.testSecondaryEntityRepository.findById(saved.getId()).orElseThrow().getName());
		Assertions.assertEquals(1, this.testSecondaryEntityRepository.count());
		Assertions.assertEquals(0, this.count(this.dataSource, "test_secondary_entity"));
	}

	/**
	 * A row written through the tertiary unit lands only in the tertiary database — proving a second,
	 * independent secondary datasource (N &gt; 1).
	 */
	@Test
	public void testTertiaryRoutingAndIsolation() throws Exception {
		final TestTertiaryEntity entity = new TestTertiaryEntity();
		entity.setName("tertiary-only");
		final TestTertiaryEntity saved = this.testTertiaryEntityService.save(entity);
		Assertions.assertNotNull(saved.getId());
		Assertions.assertEquals("tertiary-only", this.testTertiaryEntityRepository.findById(saved.getId()).orElseThrow().getName());
		Assertions.assertEquals(1, this.testTertiaryEntityRepository.count());
		Assertions.assertEquals(0, this.count(this.dataSource, "test_tertiary_entity"));
	}

	/**
	 * The primary unit still works while secondary units are configured.
	 */
	@Test
	public void testPrimaryUnitStillWorks() {
		TestEntity entity = new TestEntity();
		entity = this.testEntityService.save(entity);
		Assertions.assertNotNull(entity.getId());
		Assertions.assertNotNull(entity.getCreatedAt());
	}

	/**
	 * Counts rows of a table through the given datasource.
	 *
	 * @param  dataSource Datasource to query.
	 * @param  table      Table name.
	 * @return            Row count.
	 * @throws Exception  If the query fails.
	 */
	private long count(
			final DataSource dataSource,
			final String table) throws Exception {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM " + table)) {
			resultSet.next();
			return resultSet.getLong(1);
		}
	}

}

package org.coldis.library.test.persistence.settings;

import java.util.Map;

import javax.sql.DataSource;

import org.coldis.library.persistence.settings.QuerySettings;
import org.coldis.library.test.SpringTestHelper;
import org.coldis.library.test.StartTestWithContainerExtension;
import org.coldis.library.test.StopTestWithContainerExtension;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.TestWithContainer;
import org.coldis.library.test.persistence.TestApplication;
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
import org.testcontainers.containers.GenericContainer;

import jakarta.persistence.EntityManagerFactory;

/**
 * Tests {@link QuerySettings} with lazy connection acquisition turned off
 * ({@code org.coldis.configuration.persistence.lazy-connection.enabled=false}), on the primary and
 * secondary units: transactions take their connection at begin, as before lazy acquisition, and the
 * settings behave as they do with it. The flag is read when the entity manager factories are built, so
 * it needs a context of its own.
 */
@TestWithContainer
@ExtendWith(StartTestWithContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = {
				"test.properties",
				"org.coldis.configuration.persistence.explicit-primary=true",
				"org.coldis.configuration.persistence.lazy-connection.enabled=false",
				"org.coldis.configuration.persistence.datasources.secondary.url=jdbc:postgresql://localhost:${POSTGRES_CONTAINER_SECONDARY_5432}/test",
				"org.coldis.configuration.persistence.datasources.secondary.username=test",
				"org.coldis.configuration.persistence.datasources.secondary.password=test",
				"org.coldis.configuration.persistence.datasources.tertiary.url=jdbc:postgresql://localhost:${POSTGRES_CONTAINER_SECONDARY_5432}/test",
				"org.coldis.configuration.persistence.datasources.tertiary.username=test",
				"org.coldis.configuration.persistence.datasources.tertiary.password=test" },
		classes = TestApplication.class
)
@ExtendWith(StopTestWithContainerExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QuerySettingsEagerConnectionTest extends SpringTestHelper {

	/** Primary Postgres container. */
	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	/** Secondary Postgres container (also used by the tertiary unit the test entities require). */
	public static GenericContainer<?> POSTGRES_CONTAINER_SECONDARY = TestHelper.createPostgresContainer();

	/** Artemis container. */
	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	/** Primary datasource. */
	@Autowired
	private DataSource dataSource;

	/** Secondary datasource. */
	@Autowired
	@Qualifier(value = "secondaryDataSource")
	private DataSource secondaryDataSource;

	/** Primary entity manager factory. */
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	/** Secondary entity manager factory. */
	@Autowired
	@Qualifier(value = "secondaryEntityManagerFactory")
	private EntityManagerFactory secondaryEntityManagerFactory;

	/** Connection lifecycle probes. */
	@Autowired
	private QuerySettingsConnectionServiceComponent connectionServiceComponent;


	/**
	 * Both units' entity manager factories use the pools directly.
	 */
	@Test
	@DisplayName("both units take connections at begin in this context")
	public void testUnitsAreEager() {
		Assertions.assertSame(this.dataSource, ((EntityManagerFactoryInfo) this.entityManagerFactory).getDataSource());
		Assertions.assertSame(this.secondaryDataSource, ((EntityManagerFactoryInfo) this.secondaryEntityManagerFactory).getDataSource());
	}

	/**
	 * The transaction holds its connection from begin; the settings reach the JPA query on it, and the
	 * next borrower of the connection sees the session values.
	 */
	@Test
	@DisplayName("eager primary unit: the settings apply on the transaction's connection, and nothing leaks")
	public void testEagerPrimaryConnection() {
		final String sessionWorkMem = QuerySettingsConnectionServiceComponent.getPoolWorkMem(this.dataSource);
		Assertions.assertNotEquals("61MB", sessionWorkMem);
		final Map<String, String> withoutCall = this.connectionServiceComponent.primaryTransaction(false);
		Assertions.assertEquals("1", withoutCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		final Map<String, String> withCall = this.connectionServiceComponent.primaryTransaction(true);
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_INSIDE));
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_AFTER));
		Assertions.assertEquals("61MB", withCall.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("force_custom_plan", withCall.get(QuerySettingsConnectionServiceComponent.PLAN_CACHE_MODE));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
		QuerySettingsConnectionServiceComponent.assertNextBorrowersSeeDefaults(this.dataSource, withCall.get(QuerySettingsConnectionServiceComponent.BACKEND_PID),
				sessionWorkMem);
	}

	/**
	 * The same on the secondary unit.
	 */
	@Test
	@DisplayName("eager secondary unit: the settings apply on the transaction's connection, and nothing leaks")
	public void testEagerSecondaryConnection() {
		final String sessionWorkMem = QuerySettingsConnectionServiceComponent.getPoolWorkMem(this.secondaryDataSource);
		Assertions.assertNotEquals("74MB", sessionWorkMem);
		final Map<String, String> withCall = this.connectionServiceComponent.secondaryTransaction(true);
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_INSIDE));
		Assertions.assertEquals("74MB", withCall.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.secondaryDataSource));
		QuerySettingsConnectionServiceComponent.assertNextBorrowersSeeDefaults(this.secondaryDataSource,
				withCall.get(QuerySettingsConnectionServiceComponent.BACKEND_PID), sessionWorkMem);
	}

	/**
	 * Read-only and isolation apply together with the settings.
	 */
	@Test
	@DisplayName("eager unit: read-only and isolation apply together with the settings")
	public void testEagerReadOnlyAndIsolation() {
		final Map<String, String> serializable = this.connectionServiceComponent.readOnlySerializableTransaction();
		Assertions.assertEquals("1", serializable.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("61MB", serializable.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("on", serializable.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals("serializable", serializable.get(QuerySettingsConnectionServiceComponent.ISOLATION));
		final Map<String, String> repeatableRead = this.connectionServiceComponent.readOnlyRepeatableReadAnnotatedTransaction();
		Assertions.assertEquals("62MB", repeatableRead.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("on", repeatableRead.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals("repeatable read", repeatableRead.get(QuerySettingsConnectionServiceComponent.ISOLATION));
		final Map<String, String> next = this.connectionServiceComponent.primaryTransaction(true);
		Assertions.assertEquals("off", next.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals("read committed", next.get(QuerySettingsConnectionServiceComponent.ISOLATION));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
	}

	/**
	 * Outside a transaction the call is rejected; with startTransaction it runs in its own read-only
	 * transaction.
	 */
	@Test
	@DisplayName("eager unit: outside a transaction the call is rejected, or wrapped with startTransaction")
	public void testEagerWithoutTransaction() {
		final IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> this.connectionServiceComponent.readPrimarySettings());
		Assertions.assertTrue(failure.getMessage().contains("needs a transaction active"), failure::getMessage);
		final Map<String, String> started = this.connectionServiceComponent.readPrimarySettingsInStartedTransaction();
		Assertions.assertEquals("63MB", started.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("on", started.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
	}

}

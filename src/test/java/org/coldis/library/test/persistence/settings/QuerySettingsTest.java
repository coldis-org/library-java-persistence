package org.coldis.library.test.persistence.settings;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.coldis.library.persistence.configuration.LazyConnectionDataSource;
import org.coldis.library.persistence.settings.QuerySettings;
import org.coldis.library.persistence.settings.QuerySettingsRepository;
import org.coldis.library.persistence.settings.QuerySettingsServiceComponent;
import org.coldis.library.persistence.settings.QuerySettingsWoven;
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
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;

import jakarta.persistence.EntityManagerFactory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.StubMethod;

/**
 * Tests {@link QuerySettings} against real PostgreSQL databases: the primary and secondary units take
 * connections lazily (the default), and the tertiary unit opts out
 * ({@code org.coldis.configuration.persistence.datasources.<name>.lazy-connection.enabled=false}, the
 * per-unit form of {@code org.coldis.configuration.persistence.lazy-connection.enabled}), so its
 * transactions hold their connection from begin, as without lazy acquisition.
 *
 * <p>
 * The context is closed after the class, as in every test class here: each class starts and stops
 * its own containers, and the woven aspect supports one live context per JVM. The tertiary unit
 * shares the secondary container (its own pool) to save a container.
 * </p>
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
				"org.coldis.configuration.persistence.datasources.tertiary.url=jdbc:postgresql://localhost:${POSTGRES_CONTAINER_SECONDARY_5432}/test",
				"org.coldis.configuration.persistence.datasources.tertiary.username=test",
				"org.coldis.configuration.persistence.datasources.tertiary.password=test" },
		classes = TestApplication.class
)
@ExtendWith(StopTestWithContainerExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QuerySettingsTest extends SpringTestHelper {

	/** Primary Postgres container. */
	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	/** Secondary Postgres container (also used by the tertiary unit the test entities require). */
	public static GenericContainer<?> POSTGRES_CONTAINER_SECONDARY = TestHelper.createPostgresContainer();

	/** Artemis container. */
	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	/** Annotated methods. */
	@Autowired
	private QuerySettingsTestServiceComponent testServiceComponent;

	/** Type-level annotated component. */
	@Autowired
	private QuerySettingsTypeLevelServiceComponent typeLevelServiceComponent;

	/** Secondary datasource settings. */
	@Autowired
	private QuerySettingsSecondaryServiceComponent secondaryServiceComponent;

	/** Annotated repository. */
	@Autowired
	private QuerySettingsTestRepository repository;

	/** Repository annotated at type level. */
	@Autowired
	private QuerySettingsTypeLevelTestRepository typeLevelRepository;

	/** Repository of the secondary unit. */
	@Autowired
	private QuerySettingsSecondaryTestRepository secondaryRepository;

	/** Repository inheriting the type annotation of its parent interface. */
	@Autowired
	private QuerySettingsInheritedTestRepository inheritedRepository;

	/** Primary transaction manager. */
	@Autowired
	private PlatformTransactionManager transactionManager;

	/** Primary datasource. */
	@Autowired
	private DataSource dataSource;

	/** Secondary datasource. */
	@Autowired
	@Qualifier(value = "secondaryDataSource")
	private DataSource secondaryDataSource;

	/** Tertiary datasource. */
	@Autowired
	@Qualifier(value = "tertiaryDataSource")
	private DataSource tertiaryDataSource;

	/** Primary entity manager factory. */
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

	/** Connection lifecycle probes. */
	@Autowired
	private QuerySettingsConnectionServiceComponent connectionServiceComponent;

	/**
	 * Asserts a call fails because no transaction is active for its datasource (possibly translated by
	 * a repository proxy).
	 */
	private static void assertFailsWithoutTransaction(
			final Executable call) {
		final Throwable failure = Assertions.assertThrows(RuntimeException.class, call);
		Throwable cause = failure;
		while ((cause != null) && !((cause instanceof IllegalStateException) && cause.getMessage().contains("needs a transaction active"))) {
			cause = cause.getCause();
		}
		Assertions.assertNotNull(cause, () -> "Unexpected failure: " + failure);
	}

	/**
	 * Gets the session (default) work_mem.
	 */
	private String getSessionWorkMem() {
		return this.testServiceComponent.readSettingsInPlainTransaction().get("work_mem");
	}

	/**
	 * The settings are in effect while the annotated method runs.
	 */
	@Test
	@DisplayName("settings are in effect while the method runs")
	public void testSettingsAppliedDuringMethod() {
		final Map<String, String> values = this.testServiceComponent.readSettingsWithQuerySettings();
		Assertions.assertEquals("77MB", values.get("work_mem"));
		Assertions.assertEquals("force_custom_plan", values.get("plan_cache_mode"));
		Assertions.assertEquals("off", values.get("enable_nestloop"));
	}

	/**
	 * The next transaction on the same pooled connection sees the session values again.
	 */
	@Test
	@DisplayName("settings do not reach the next transaction on the same connection")
	public void testSettingsNotLeakedToNextTransaction() {
		final Map<String, String> before = this.testServiceComponent.readSettingsInPlainTransaction();
		final Map<String, String> during = this.testServiceComponent.readSettingsWithQuerySettings();
		final Map<String, String> after = this.testServiceComponent.readSettingsInPlainTransaction();
		Assertions.assertNotEquals(before.get("work_mem"), during.get("work_mem"));
		Assertions.assertEquals(during.get(QuerySettingsTestServiceComponent.BACKEND_PID), after.get(QuerySettingsTestServiceComponent.BACKEND_PID),
				"The pool should hand the same connection back to the same thread.");
		for (final String setting : QuerySettingsTestServiceComponent.PROBED_SETTINGS) {
			Assertions.assertEquals(before.get(setting), after.get(setting), setting);
		}
	}

	/**
	 * Nested annotated calls each get their own scope and restore the enclosing one.
	 */
	@Test
	@DisplayName("nested calls each get their own scope")
	public void testNestedScopes() {
		final Map<String, String> values = this.testServiceComponent.readSettingsInNestedScopes();
		Assertions.assertEquals("11MB", values.get("outer-before"));
		Assertions.assertEquals("22MB", values.get("inner"));
		Assertions.assertEquals("11MB", values.get("outer-after"));
	}

	/**
	 * A method that throws still restores the previous values in the ongoing transaction.
	 */
	@Test
	@DisplayName("settings are restored in the same transaction after the method throws")
	public void testRestoredAfterException() {
		final String sessionWorkMem = this.getSessionWorkMem();
		final Map<String, String> values = this.testServiceComponent.readSettingsAfterFailure();
		Assertions.assertEquals("Failure inside work_mem 33MB", values.get("failure"));
		Assertions.assertEquals(sessionWorkMem, values.get("after"));
	}

	/**
	 * An SQL error aborts the transaction, so the values cannot be restored in it: the rollback
	 * discards them, and the next transaction on the same pooled connection sees the session values.
	 */
	@Test
	@DisplayName("settings do not leak after an SQL error aborts the transaction")
	public void testNotLeakedAfterSqlError() {
		final Map<String, String> before = this.testServiceComponent.readSettingsInPlainTransaction();
		final IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> this.testServiceComponent.failWithSqlError());
		Assertions.assertTrue(failure.getMessage().startsWith("SQL error inside"), failure::getMessage);
		Assertions.assertEquals(1, failure.getSuppressed().length, "The failed restore is kept as suppressed.");
		final Map<String, String> after = this.testServiceComponent.readSettingsInPlainTransaction();
		Assertions.assertEquals(before.get(QuerySettingsTestServiceComponent.BACKEND_PID), after.get(QuerySettingsTestServiceComponent.BACKEND_PID),
				"The pool should hand the same connection back to the same thread.");
		Assertions.assertEquals(before.get("work_mem"), after.get("work_mem"));
	}

	/**
	 * Without a transaction (none started, or suspended) the call fails instead of applying nothing.
	 */
	@Test
	@DisplayName("calls without a transaction on the datasource fail")
	public void testFailsWithoutTransaction() {
		QuerySettingsTest.assertFailsWithoutTransaction(() -> this.testServiceComponent.readSettingsWithoutTransaction());
		new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> QuerySettingsTest
				.assertFailsWithoutTransaction(() -> this.testServiceComponent.readSettingsWithSuspendedTransaction()));
	}

	/**
	 * {@code startTransaction} opens a read-only transaction to apply the settings in.
	 */
	@Test
	@DisplayName("startTransaction opens a read-only transaction")
	public void testStartTransaction() {
		final Map<String, String> values = this.testServiceComponent.readSettingsInStartedTransaction();
		Assertions.assertEquals("55MB", values.get("work_mem"));
		Assertions.assertEquals("true", values.get(QuerySettingsTestServiceComponent.READ_ONLY));
	}

	/**
	 * A type annotation applies to its methods, and a method annotation replaces it.
	 */
	@Test
	@DisplayName("method annotation replaces the type annotation")
	public void testTypeAndMethodLevel() {
		Assertions.assertEquals("66MB", this.typeLevelServiceComponent.readSettingsWithTypeSettings().get("work_mem"));
		Assertions.assertEquals("88MB", this.typeLevelServiceComponent.readSettingsWithMethodSettings().get("work_mem"));
	}

	/**
	 * Values resolve placeholders.
	 */
	@Test
	@DisplayName("values resolve placeholders")
	public void testPlaceholder() {
		Assertions.assertEquals("12MB", this.testServiceComponent.readSettingsWithPlaceholder().get("work_mem"));
	}

	/**
	 * Annotated repository interface methods (JDK proxies) get the settings, scoped to the method.
	 */
	@Test
	@DisplayName("repository interface methods get the settings")
	public void testRepositoryMethod() {
		final String sessionWorkMem = this.getSessionWorkMem();
		new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> {
			Assertions.assertEquals("99MB,force_custom_plan", this.repository.findWorkMemAndPlanCacheMode());
			Assertions.assertEquals(sessionWorkMem, this.repository.findWorkMem());
		});
		Assertions.assertEquals("97MB", this.repository.findWorkMemInOwnTransaction());
		QuerySettingsTest.assertFailsWithoutTransaction(() -> this.repository.findWorkMemAndPlanCacheMode());
	}

	/**
	 * Every pool, primary and secondary, starts its connections with JIT off.
	 */
	@Test
	@DisplayName("every pool starts its connections with JIT off")
	public void testJitOffByDefault() {
		for (final DataSource pool : List.of(this.dataSource, this.secondaryDataSource, this.tertiaryDataSource)) {
			Assertions.assertEquals("off", new JdbcTemplate(pool).queryForObject("SELECT current_setting('jit')", String.class));
		}
	}

	/**
	 * Settings target the named datasource's transaction, and fail when that datasource has none.
	 */
	@Test
	@DisplayName("settings target the named datasource, or the repository's own")
	public void testSecondaryDatasource() {
		final String sessionWorkMem = this.getSessionWorkMem();
		final Map<String, String> values = this.secondaryServiceComponent.readSettingsOnSecondary();
		Assertions.assertEquals("71MB", values.get("secondary"));
		Assertions.assertEquals(sessionWorkMem, values.get("primary"));
		QuerySettingsTest.assertFailsWithoutTransaction(() -> this.secondaryServiceComponent.readSettingsOnSecondaryInPrimaryTransaction());
		Assertions.assertEquals("73MB", this.secondaryRepository.findWorkMem());
	}

	/**
	 * plan_cache_mode is in effect when the driver executes its server-side prepared statement past the
	 * prepare threshold.
	 */
	@Test
	@DisplayName("plan_cache_mode is in effect past the prepare threshold")
	public void testPlanCacheMode() {
		final Map<String, Long> custom = this.testServiceComponent.countPlansWithCustomPlansForced("query-settings-custom-plan-probe");
		Assertions.assertTrue(custom.get("custom_plans") > 0, custom::toString);
		Assertions.assertEquals(0L, custom.get("generic_plans"), custom::toString);
		final Map<String, Long> generic = this.testServiceComponent.countPlansWithGenericPlansForced("query-settings-generic-plan-probe");
		Assertions.assertTrue(generic.get("generic_plans") > 0, generic::toString);
		Assertions.assertEquals(0L, generic.get("custom_plans"), generic::toString);
	}

	/**
	 * {@code startTransaction} joins an active transaction (same connection, not made read-only).
	 */
	@Test
	@DisplayName("startTransaction joins an active transaction")
	public void testStartTransactionJoinsActiveTransaction() {
		new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> {
			final String callerPid = this.testServiceComponent.readSettings().get(QuerySettingsTestServiceComponent.BACKEND_PID);
			final Map<String, String> values = this.testServiceComponent.readSettingsInStartedTransaction();
			Assertions.assertEquals("55MB", values.get("work_mem"));
			Assertions.assertEquals(callerPid, values.get(QuerySettingsTestServiceComponent.BACKEND_PID));
			Assertions.assertEquals("false", values.get(QuerySettingsTestServiceComponent.READ_ONLY));
		});
	}

	/**
	 * A transaction joined by {@code startTransaction} is marked rollback-only on an unchecked
	 * exception and left alone on a checked one, as {@code @Transactional} does.
	 */
	@Test
	@DisplayName("startTransaction completes like @Transactional on failure")
	public void testStartTransactionFailure() {
		new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> Assertions.assertEquals("Checked failure.",
				Assertions.assertThrows(Exception.class, () -> this.testServiceComponent.failCheckedInStartedTransaction()).getMessage()));
		Assertions.assertThrows(UnexpectedRollbackException.class,
				() -> new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> Assertions
						.assertThrows(IllegalArgumentException.class, () -> this.testServiceComponent.failUncheckedInStartedTransaction())));
		Assertions.assertThrows(IllegalArgumentException.class, () -> this.testServiceComponent.failUncheckedInStartedTransaction());
		Assertions.assertEquals("Checked failure.",
				Assertions.assertThrows(Exception.class, () -> this.testServiceComponent.failCheckedInStartedTransaction()).getMessage());
	}

	/**
	 * A repository annotated at type level gets the settings on its methods.
	 */
	@Test
	@DisplayName("type-level annotation on a repository interface (or its parent) applies to its methods")
	public void testTypeLevelRepository() {
		Assertions.assertEquals("96MB", this.typeLevelRepository.findWorkMem());
		Assertions.assertEquals("95MB", this.inheritedRepository.findWorkMem());
	}

	/**
	 * A class annotated at type level calling its own methods keeps the settings, and its
	 * initialization callback and toString run outside the settings (no transaction needed).
	 */
	@Test
	@DisplayName("type-level class: own calls keep the settings, callbacks are not covered")
	public void testTypeLevelOwnCallsAndCallbacks() {
		final Map<String, String> values = this.typeLevelServiceComponent.readSettingsAroundOwnCall();
		Assertions.assertEquals("66MB", values.get("inner"));
		Assertions.assertEquals("66MB", values.get("outer-after"));
		Assertions.assertEquals("66MB", this.typeLevelServiceComponent.setupWithTypeSettings().get("work_mem"));
		Assertions.assertTrue(this.typeLevelServiceComponent.isRunning());
		Assertions.assertEquals(List.of("setDataSource", "setBeanName", "initialize", "afterPropertiesSet", "start"), this.typeLevelServiceComponent.getCallbacks());
		Assertions.assertTrue(this.typeLevelServiceComponent.toString().startsWith("QuerySettingsTypeLevelServiceComponent"));
		Assertions.assertTrue(this.typeLevelServiceComponent.equals(this.typeLevelServiceComponent));
		Assertions.assertEquals(this.typeLevelServiceComponent.hashCode(), this.typeLevelServiceComponent.hashCode());
		this.typeLevelServiceComponent.release();
		this.typeLevelServiceComponent.afterPropertiesSet();
		this.typeLevelServiceComponent.setBeanName("again");
		Assertions.assertEquals(List.of("setDataSource", "setBeanName", "initialize", "afterPropertiesSet", "start", "release", "afterPropertiesSet", "setBeanName"),
				this.typeLevelServiceComponent.getCallbacks());
	}

	/**
	 * Creates a component over a bean factory holding only the given bean classes and a primary
	 * transaction manager, and runs its start-up check.
	 */
	private void checkBeans(
			final Class<?>... beanClasses) {
		final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("transactionManager", new DataSourceTransactionManager(this.dataSource));
		for (final Class<?> beanClass : beanClasses) {
			beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
		}
		new QuerySettingsServiceComponent(beanFactory, new QuerySettingsRepository()).afterSingletonsInstantiated();
	}

	/**
	 * Asserts the start-up check rejects a bean class with the given message part.
	 */
	private void assertRejected(
			final String messagePart,
			final Class<?> beanClass) {
		final IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> this.checkBeans(beanClass));
		Assertions.assertTrue(exception.getMessage().contains(messagePart), exception::getMessage);
	}

	/**
	 * The start-up check accepts woven classes and repositories and rejects invalid annotations.
	 */
	@Test
	@DisplayName("start-up check rejects invalid or unwoven annotations")
	public void testStartupCheck() throws Exception {
		Assertions.assertTrue(QuerySettingsTestServiceComponent.class.getMethod("readSettingsWithQuerySettings").isAnnotationPresent(QuerySettingsWoven.class));
		Assertions.assertTrue(QuerySettingsTypeLevelServiceComponent.class.isAnnotationPresent(QuerySettingsWoven.class));
		this.checkBeans(QuerySettingsTestServiceComponent.class, QuerySettingsTypeLevelServiceComponent.class, QuerySettingsTestRepository.class,
				QuerySettingsTypeLevelTestRepository.class, QuerySettingsInheritedTestRepository.class);
		this.assertRejected("'search_path', which is not an allowed query setting", QuerySettingsFixtures.DisallowedSetting.class);
		this.assertRejected("'work_mem' more than once", QuerySettingsFixtures.DuplicateSetting.class);
		this.assertRejected("declares no setting", QuerySettingsFixtures.NoSetting.class);
		this.assertRejected("targets the missing datasource", QuerySettingsFixtures.UnknownDatasource.class);
		this.assertRejected("only Spring Data repository interfaces are supported", QuerySettingsFixtures.AnnotatedInterfaceImplementation.class);
		this.assertRejected("but the repository belongs to the secondary datasource", QuerySettingsFixtures.MismatchedDatasourceRepository.class);
		final Class<?> unwovenClass = new ByteBuddy().subclass(Object.class).name("org.coldis.library.test.persistence.settings.UnwovenQuerySettings")
				.defineMethod("run", void.class, Visibility.PUBLIC).intercept(StubMethod.INSTANCE)
				.annotateMethod(QuerySettingsFixtures.NoSetting.class.getMethod("run").getAnnotation(QuerySettings.class)).make()
				.load(this.getClass().getClassLoader()).getLoaded();
		this.assertRejected("is not woven", unwovenClass);
	}


	/**
	 * The primary and secondary factories use the lazy proxy; the opted-out tertiary one uses its pool.
	 */
	@Test
	@DisplayName("the primary and secondary units take connections lazily, the tertiary one at begin")
	public void testUnitsAreLazyOrEager() {
		Assertions.assertInstanceOf(LazyConnectionDataSource.class, ((EntityManagerFactoryInfo) this.entityManagerFactory).getDataSource());
		Assertions.assertInstanceOf(LazyConnectionDataSource.class, ((EntityManagerFactoryInfo) this.secondaryEntityManagerFactory).getDataSource());
		Assertions.assertSame(this.tertiaryDataSource, ((EntityManagerFactoryInfo) this.tertiaryEntityManagerFactory).getDataSource());
	}

	/**
	 * A transaction takes no connection at begin for a later annotated call: the call takes it, its
	 * settings reach the JPA query on that connection, and the next borrower of the connection sees the
	 * session values. A transaction that never makes the call takes no connection.
	 */
	@Test
	@DisplayName("lazy primary unit: the annotated call takes the connection, and nothing leaks")
	public void testLazyPrimaryConnection() {
		final String sessionWorkMem = QuerySettingsConnectionServiceComponent.getPoolWorkMem(this.dataSource);
		Assertions.assertNotEquals("61MB", sessionWorkMem);
		final Map<String, String> withoutCall = this.connectionServiceComponent.primaryTransaction(false);
		Assertions.assertEquals("0", withoutCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("0", withoutCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_AFTER));
		final Map<String, String> withCall = this.connectionServiceComponent.primaryTransaction(true);
		Assertions.assertEquals("0", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_INSIDE));
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_AFTER));
		Assertions.assertEquals("61MB", withCall.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("force_custom_plan", withCall.get(QuerySettingsConnectionServiceComponent.PLAN_CACHE_MODE));
		Assertions.assertEquals("off", withCall.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals("read committed", withCall.get(QuerySettingsConnectionServiceComponent.ISOLATION));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
		QuerySettingsConnectionServiceComponent.assertNextBorrowersSeeDefaults(this.dataSource, withCall.get(QuerySettingsConnectionServiceComponent.BACKEND_PID), sessionWorkMem);
	}

	/**
	 * The same on the secondary unit.
	 */
	@Test
	@DisplayName("lazy secondary unit: the annotated call takes the connection, and nothing leaks")
	public void testLazySecondaryConnection() {
		final String sessionWorkMem = QuerySettingsConnectionServiceComponent.getPoolWorkMem(this.secondaryDataSource);
		Assertions.assertNotEquals("74MB", sessionWorkMem);
		final Map<String, String> withoutCall = this.connectionServiceComponent.secondaryTransaction(false);
		Assertions.assertEquals("0", withoutCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("0", withoutCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_AFTER));
		final Map<String, String> withCall = this.connectionServiceComponent.secondaryTransaction(true);
		Assertions.assertEquals("0", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_INSIDE));
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_AFTER));
		Assertions.assertEquals("74MB", withCall.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.secondaryDataSource));
		QuerySettingsConnectionServiceComponent.assertNextBorrowersSeeDefaults(this.secondaryDataSource, withCall.get(QuerySettingsConnectionServiceComponent.BACKEND_PID),
				sessionWorkMem);
	}

	/**
	 * Read-only and isolation, recorded on the lazy connection, are applied together with the settings,
	 * whether the transaction is the caller's or the annotated method's own, and do not reach the next
	 * transaction.
	 */
	@Test
	@DisplayName("lazy unit: read-only and isolation apply together with the settings")
	public void testLazyReadOnlyAndIsolation() {
		final Map<String, String> serializable = this.connectionServiceComponent.readOnlySerializableTransaction();
		Assertions.assertEquals("0", serializable.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("1", serializable.get(QuerySettingsConnectionServiceComponent.ACTIVE_INSIDE));
		Assertions.assertEquals("61MB", serializable.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("on", serializable.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals("serializable", serializable.get(QuerySettingsConnectionServiceComponent.ISOLATION));
		final Map<String, String> repeatableRead = this.connectionServiceComponent.readOnlyRepeatableReadAnnotatedTransaction();
		Assertions.assertEquals("1", repeatableRead.get(QuerySettingsConnectionServiceComponent.ACTIVE_INSIDE));
		Assertions.assertEquals("62MB", repeatableRead.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("on", repeatableRead.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals("repeatable read", repeatableRead.get(QuerySettingsConnectionServiceComponent.ISOLATION));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
		final Map<String, String> next = this.connectionServiceComponent.primaryTransaction(true);
		Assertions.assertEquals(repeatableRead.get(QuerySettingsConnectionServiceComponent.BACKEND_PID), next.get(QuerySettingsConnectionServiceComponent.BACKEND_PID),
				"The pool should hand the same connection back to the same thread.");
		Assertions.assertEquals("off", next.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals("read committed", next.get(QuerySettingsConnectionServiceComponent.ISOLATION));
	}

	/**
	 * Outside a transaction the call is rejected before any connection is taken; with
	 * startTransaction it runs in its own read-only transaction, which gives the connection back with
	 * the session values.
	 */
	@Test
	@DisplayName("lazy unit: outside a transaction the call is rejected, or wrapped with startTransaction")
	public void testLazyWithoutTransaction() {
		final String sessionWorkMem = QuerySettingsConnectionServiceComponent.getPoolWorkMem(this.dataSource);
		QuerySettingsTest.assertFailsWithoutTransaction(() -> this.connectionServiceComponent.readPrimarySettings());
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
		final Map<String, String> started = this.connectionServiceComponent.readPrimarySettingsInStartedTransaction();
		Assertions.assertEquals("1", started.get(QuerySettingsConnectionServiceComponent.ACTIVE_INSIDE));
		Assertions.assertEquals("63MB", started.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("on", started.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
		QuerySettingsConnectionServiceComponent.assertNextBorrowersSeeDefaults(this.dataSource, started.get(QuerySettingsConnectionServiceComponent.BACKEND_PID), sessionWorkMem);
	}

	/**
	 * A repository advice takes the connection at the repository call, not at the caller's
	 * transaction begin.
	 */
	@Test
	@DisplayName("lazy unit: an annotated repository call takes the connection at the call")
	public void testLazyRepositoryConnection() {
		new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> {
			Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
			Assertions.assertEquals("99MB,force_custom_plan", this.repository.findWorkMemAndPlanCacheMode());
			Assertions.assertEquals(1, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
		});
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.dataSource));
		Assertions.assertEquals("73MB", this.secondaryRepository.findWorkMem());
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.secondaryDataSource));
	}

	/**
	 * Without lazy acquisition the transaction holds its connection from begin; the settings reach the
	 * JPA query on it, and the next borrower of the connection sees the session values.
	 */
	@Test
	@DisplayName("eager unit: the settings apply on the transaction's connection, and nothing leaks")
	public void testEagerConnection() {
		final String sessionWorkMem = QuerySettingsConnectionServiceComponent.getPoolWorkMem(this.tertiaryDataSource);
		Assertions.assertNotEquals("75MB", sessionWorkMem);
		final Map<String, String> withoutCall = this.connectionServiceComponent.tertiaryTransaction(false);
		Assertions.assertEquals("1", withoutCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		final Map<String, String> withCall = this.connectionServiceComponent.tertiaryTransaction(true);
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_INSIDE));
		Assertions.assertEquals("1", withCall.get(QuerySettingsConnectionServiceComponent.ACTIVE_AFTER));
		Assertions.assertEquals("75MB", withCall.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("force_custom_plan", withCall.get(QuerySettingsConnectionServiceComponent.PLAN_CACHE_MODE));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.tertiaryDataSource));
		QuerySettingsConnectionServiceComponent.assertNextBorrowersSeeDefaults(this.tertiaryDataSource,
				withCall.get(QuerySettingsConnectionServiceComponent.BACKEND_PID), sessionWorkMem);
	}

	/**
	 * Without lazy acquisition read-only and isolation apply together with the settings, and do not
	 * reach the next transaction.
	 */
	@Test
	@DisplayName("eager unit: read-only and isolation apply together with the settings")
	public void testEagerReadOnlyAndIsolation() {
		final Map<String, String> serializable = this.connectionServiceComponent.tertiaryReadOnlySerializableTransaction();
		Assertions.assertEquals("1", serializable.get(QuerySettingsConnectionServiceComponent.ACTIVE_BEFORE));
		Assertions.assertEquals("75MB", serializable.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("on", serializable.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals("serializable", serializable.get(QuerySettingsConnectionServiceComponent.ISOLATION));
		final Map<String, String> next = this.connectionServiceComponent.tertiaryTransaction(true);
		Assertions.assertEquals("off", next.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals("read committed", next.get(QuerySettingsConnectionServiceComponent.ISOLATION));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.tertiaryDataSource));
	}

	/**
	 * Without lazy acquisition the call is rejected outside a transaction, or wrapped in its own
	 * read-only transaction with startTransaction.
	 */
	@Test
	@DisplayName("eager unit: outside a transaction the call is rejected, or wrapped with startTransaction")
	public void testEagerWithoutTransaction() {
		QuerySettingsTest.assertFailsWithoutTransaction(() -> this.connectionServiceComponent.readTertiarySettings());
		final Map<String, String> started = this.connectionServiceComponent.readTertiarySettingsInStartedTransaction();
		Assertions.assertEquals("76MB", started.get(QuerySettingsConnectionServiceComponent.WORK_MEM));
		Assertions.assertEquals("on", started.get(QuerySettingsConnectionServiceComponent.READ_ONLY));
		Assertions.assertEquals(0, QuerySettingsConnectionServiceComponent.getActiveConnections(this.tertiaryDataSource));
	}

}

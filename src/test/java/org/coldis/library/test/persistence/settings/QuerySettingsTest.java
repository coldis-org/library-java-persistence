package org.coldis.library.test.persistence.settings;

import java.util.Map;

import javax.sql.DataSource;

import org.coldis.library.persistence.settings.QuerySettings;
import org.coldis.library.persistence.settings.QuerySettingsServiceComponent;
import org.coldis.library.persistence.settings.QuerySettingsWoven;
import org.coldis.library.test.SpringTestHelper;
import org.coldis.library.test.StartTestWithContainerExtension;
import org.coldis.library.test.StopTestWithContainerExtension;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.TestWithContainer;
import org.coldis.library.test.persistence.TestApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.StubMethod;

/**
 * Tests {@link QuerySettings} against real PostgreSQL databases (primary and secondary units).
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

	/** Primary transaction manager. */
	@Autowired
	private PlatformTransactionManager transactionManager;

	/** Primary datasource. */
	@Autowired
	private DataSource dataSource;

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
	public void testFailsWithoutTransaction() {
		QuerySettingsTest.assertFailsWithoutTransaction(() -> this.testServiceComponent.readSettingsWithoutTransaction());
		new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> QuerySettingsTest
				.assertFailsWithoutTransaction(() -> this.testServiceComponent.readSettingsWithSuspendedTransaction()));
	}

	/**
	 * {@code startTransaction} opens a read-only transaction to apply the settings in.
	 */
	@Test
	public void testStartTransaction() {
		final Map<String, String> values = this.testServiceComponent.readSettingsInStartedTransaction();
		Assertions.assertEquals("55MB", values.get("work_mem"));
		Assertions.assertEquals("true", values.get(QuerySettingsTestServiceComponent.READ_ONLY));
	}

	/**
	 * A type annotation applies to its methods, and a method annotation replaces it.
	 */
	@Test
	public void testTypeAndMethodLevel() {
		Assertions.assertEquals("66MB", this.typeLevelServiceComponent.readSettingsWithTypeSettings().get("work_mem"));
		Assertions.assertEquals("88MB", this.typeLevelServiceComponent.readSettingsWithMethodSettings().get("work_mem"));
	}

	/**
	 * Values resolve placeholders.
	 */
	@Test
	public void testPlaceholder() {
		Assertions.assertEquals("12MB", this.testServiceComponent.readSettingsWithPlaceholder().get("work_mem"));
	}

	/**
	 * Annotated repository interface methods (JDK proxies) get the settings, scoped to the method.
	 */
	@Test
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
	 * Settings target the named datasource's transaction, and fail when that datasource has none.
	 */
	@Test
	public void testSecondaryDatasource() {
		final String sessionWorkMem = this.getSessionWorkMem();
		final Map<String, String> values = this.secondaryServiceComponent.readSettingsOnSecondary();
		Assertions.assertEquals("71MB", values.get("secondary"));
		Assertions.assertEquals(sessionWorkMem, values.get("primary"));
		QuerySettingsTest.assertFailsWithoutTransaction(() -> this.secondaryServiceComponent.readSettingsOnSecondaryInPrimaryTransaction());
	}

	/**
	 * plan_cache_mode is in effect when the driver executes its server-side prepared statement past the
	 * prepare threshold.
	 */
	@Test
	public void testPlanCacheMode() {
		final Map<String, Long> custom = this.testServiceComponent.countPlansWithCustomPlansForced("query-settings-custom-plan-probe");
		Assertions.assertTrue(custom.get("custom_plans") > 0, custom::toString);
		Assertions.assertEquals(0L, custom.get("generic_plans"), custom::toString);
		final Map<String, Long> generic = this.testServiceComponent.countPlansWithGenericPlansForced("query-settings-generic-plan-probe");
		Assertions.assertTrue(generic.get("generic_plans") > 0, generic::toString);
		Assertions.assertEquals(0L, generic.get("custom_plans"), generic::toString);
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
		new QuerySettingsServiceComponent(beanFactory).afterSingletonsInstantiated();
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
	public void testStartupCheck() throws Exception {
		Assertions.assertTrue(QuerySettingsTestServiceComponent.class.getMethod("readSettingsWithQuerySettings").isAnnotationPresent(QuerySettingsWoven.class));
		Assertions.assertTrue(QuerySettingsTypeLevelServiceComponent.class.isAnnotationPresent(QuerySettingsWoven.class));
		this.checkBeans(QuerySettingsTestServiceComponent.class, QuerySettingsTypeLevelServiceComponent.class, QuerySettingsTestRepository.class);
		this.assertRejected("'search_path', which is not an allowed query setting", QuerySettingsFixtures.DisallowedSetting.class);
		this.assertRejected("'work_mem' more than once", QuerySettingsFixtures.DuplicateSetting.class);
		this.assertRejected("declares no setting", QuerySettingsFixtures.NoSetting.class);
		this.assertRejected("targets the missing datasource", QuerySettingsFixtures.UnknownDatasource.class);
		this.assertRejected("only Spring Data repository interfaces are supported", QuerySettingsFixtures.AnnotatedInterfaceImplementation.class);
		final Class<?> unwovenClass = new ByteBuddy().subclass(Object.class).name("org.coldis.library.test.persistence.settings.UnwovenQuerySettings")
				.defineMethod("run", void.class, Visibility.PUBLIC).intercept(StubMethod.INSTANCE)
				.annotateMethod(QuerySettingsFixtures.NoSetting.class.getMethod("run").getAnnotation(QuerySettings.class)).make()
				.load(this.getClass().getClassLoader()).getLoaded();
		this.assertRejected("is not woven", unwovenClass);
	}

}

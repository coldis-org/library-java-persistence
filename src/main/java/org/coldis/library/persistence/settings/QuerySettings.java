package org.coldis.library.persistence.settings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies PostgreSQL settings ({@code work_mem}, {@code plan_cache_mode}, planner {@code enable_*}
 * switches, ...) to the statements a method runs, for the duration of that method, inside its
 * transaction.
 *
 * <pre>
 * &#64;QuerySettings(
 * 		workMem = "256MB",
 * 		planCacheMode = PlanCacheMode.FORCE_CUSTOM_PLAN,
 * 		settings = { &#64;QuerySetting(name = "enable_nestloop", value = "off") })
 * List&lt;Order&gt; findPending(...);
 * </pre>
 *
 * <h2>Where it applies</h2>
 * <ul>
 * <li><strong>Spring Data repository interfaces</strong> (on the interface or its methods): applied
 * by an advice on the repository proxy, after the repository's own transaction interceptor. Works
 * without weaving.</li>
 * <li><strong>Classes</strong> (services, components, ...; on the class or its methods): applied by
 * the AspectJ aspect {@link QuerySettingsAspect}, ordered after Spring's AspectJ transaction aspect,
 * so a method that is both {@code @Transactional} and {@code @QuerySettings} gets the settings inside
 * its own transaction. The consuming module must weave this library's aspects (add
 * {@code org.coldis.library:persistence} to the {@code aspectj-maven-plugin}
 * {@code aspectLibraries}); a bean whose annotated methods were not woven fails the application
 * start-up.</li>
 * </ul>
 * A method annotation replaces (does not merge with) the type annotation. On a class, the type
 * annotation covers the public instance methods the class declares, except {@code toString},
 * {@code equals}, {@code hashCode} and {@code @Autowired}, {@code @PostConstruct} and
 * {@code @PreDestroy} methods; annotate other methods individually. Annotations on interfaces
 * that are not Spring Data repositories are not applied (and fail the start-up).
 *
 * <h2>Semantics</h2>
 * <ul>
 * <li>Before the method, on the connection of the transaction active for {@link #datasource()}, the
 * current values are read ({@code current_setting}) and the new ones applied with
 * {@code set_config(name, value, true)} (transaction-local, bound parameters).</li>
 * <li>After the method, whether it returned or threw, the previous values are restored (also
 * transaction-local). Nested annotated calls therefore each get their own scope, and nothing outlives
 * the transaction: the pooled connection goes back to the pool with its session values.</li>
 * <li>No transaction active for the datasource (no {@code @Transactional}, {@code NOT_SUPPORTED},
 * a transaction on another datasource): the call fails with an {@link IllegalStateException}
 * instead of silently applying nothing, unless {@link #startTransaction()} is set.</li>
 * <li>Setting names are checked against {@link QuerySettingNames}; an unknown name, an unknown
 * datasource or an unwoven annotated bean fails the start-up (or the first call, for types that are
 * not beans).</li>
 * </ul>
 *
 * <h2>Limits</h2>
 * Only statements executed while the method runs see the settings: JPA writes flushed at commit,
 * lazy loading and result streams consumed after the method returns run with the session values.
 */
@Documented
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface QuerySettings {

	/**
	 * @return {@code work_mem} (for example {@code 256MB}); empty leaves it unchanged.
	 */
	String workMem() default "";

	/**
	 * @return {@code plan_cache_mode}; {@link PlanCacheMode#UNCHANGED} leaves it unchanged.
	 */
	PlanCacheMode planCacheMode() default PlanCacheMode.UNCHANGED;

	/**
	 * @return Further settings. A name may appear only once across all attributes.
	 */
	QuerySetting[] settings() default {};

	/**
	 * @return Datasource the settings apply to: empty for the primary one, or a secondary datasource
	 *         name ({@code org.coldis.configuration.persistence.datasources.<name>}).
	 */
	String datasource() default "";

	/**
	 * @return Whether to open a read-only transaction on {@link #datasource()} around the method when
	 *         none is active (an active one is joined). When false (the default), a call without an
	 *         active transaction fails.
	 */
	boolean startTransaction() default false;

}

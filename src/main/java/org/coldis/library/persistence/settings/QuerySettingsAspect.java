package org.coldis.library.persistence.settings;

import java.lang.reflect.Method;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Applies {@link QuerySettings} on classes (and their methods), woven at compile time.
 *
 * <p>
 * Precedence: Spring's AspectJ transaction aspects run first (outermost), so the settings are applied
 * inside the transaction a {@code @Transactional} method opens. With proxy-mode transactions the
 * transaction proxy wraps the woven method anyway.
 * </p>
 *
 * <p>
 * {@code QuerySettingsWovenMarker} (shipped in the same aspect library) marks what is woven with
 * {@link QuerySettingsWoven}, which the start-up check of {@link QuerySettingsServiceComponent}
 * requires on every annotated bean class.
 * </p>
 */
@Aspect
@DeclarePrecedence("org.springframework.transaction.aspectj.*, org.coldis.library.persistence.settings.QuerySettingsAspect")
public class QuerySettingsAspect {

	/** Component applying the settings (set by the persistence auto-configuration). */
	private volatile QuerySettingsServiceComponent querySettingsServiceComponent;

	/**
	 * Sets the component applying the settings. A JVM may run several application contexts (a test
	 * suite does), and the aspect is one instance per JVM: the latest context wins, and a closing
	 * context clears its own component.
	 *
	 * @param querySettingsServiceComponent Component applying the settings.
	 */
	public void setQuerySettingsServiceComponent(
			final QuerySettingsServiceComponent querySettingsServiceComponent) {
		this.querySettingsServiceComponent = querySettingsServiceComponent;
	}

	/**
	 * Clears the component applying the settings, if it is still the given one (its context closed).
	 *
	 * @param querySettingsServiceComponent Component of the closing context.
	 */
	public void clearQuerySettingsServiceComponent(
			final QuerySettingsServiceComponent querySettingsServiceComponent) {
		if (this.querySettingsServiceComponent == querySettingsServiceComponent) {
			this.querySettingsServiceComponent = null;
		}
	}

	/**
	 * Methods annotated themselves.
	 */
	@Pointcut("execution(* *(..)) && @annotation(org.coldis.library.persistence.settings.QuerySettings)")
	public void annotatedMethod() {
	}

	/**
	 * Business methods of an annotated type: public instance methods it declares, except the
	 * {@code Object} methods and injection and lifecycle callbacks (which run outside transactions).
	 */
	@Pointcut("execution(public !static * *(..)) && @within(org.coldis.library.persistence.settings.QuerySettings)"
			+ " && !execution(String toString()) && !execution(boolean equals(Object)) && !execution(int hashCode())"
			+ " && !@annotation(org.springframework.beans.factory.annotation.Autowired) && !@annotation(jakarta.annotation.PostConstruct)"
			+ " && !@annotation(jakarta.annotation.PreDestroy)")
	public void annotatedTypeMethod() {
	}

	/**
	 * Applies the settings around the method.
	 *
	 * @param  joinPoint Join point.
	 * @return           The method result.
	 * @throws Throwable Whatever the method throws, or {@link IllegalStateException} when the
	 *                       settings cannot be applied.
	 */
	@Around("annotatedMethod() || annotatedTypeMethod()")
	public Object applySettings(
			final ProceedingJoinPoint joinPoint) throws Throwable {
		final QuerySettingsServiceComponent component = this.querySettingsServiceComponent;
		if (component == null) {
			throw new IllegalStateException("@QuerySettings method called without an application context to apply it ("
					+ joinPoint.getSignature().toLongString() + ").");
		}
		final Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
		final Optional<ResolvedQuerySettings> settings = component.resolve(method, method.getDeclaringClass());
		final Object result;
		if (settings.isPresent()) {
			result = component.execute(settings.get(), joinPoint::proceed);
		}
		else {
			result = joinPoint.proceed();
		}
		return result;
	}

}

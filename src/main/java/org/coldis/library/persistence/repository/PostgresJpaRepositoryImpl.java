package org.coldis.library.persistence.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.coldis.library.helper.DateTimeHelper;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;

/**
 * Default {@link PostgresJpaRepository} implementation, used as the Spring Data repository base
 * class (see {@code @EnableJpaRepositories(repositoryBaseClass = …)}). Extends
 * {@link SimpleJpaRepository} so every repository keeps the standard CRUD behavior and additionally
 * gets the Postgres locking helpers.
 *
 * <p>The lock-taking methods are explicitly {@code @Transactional} (read-write): {@link
 * SimpleJpaRepository} is annotated {@code @Transactional(readOnly = true)} at the class level, and
 * Postgres refuses {@code SELECT … FOR UPDATE/SHARE} inside a read-only transaction. Propagation is
 * the default {@code REQUIRED}, so they join the caller's transaction — which is what makes the lock
 * meaningful (it is held until the caller commits), and what callers should rely on.
 *
 * @param <T> Entity type.
 * @param <I> Identifier type.
 */
public class PostgresJpaRepositoryImpl<T, I> extends SimpleJpaRepository<T, I> implements PostgresJpaRepository<T, I> {

	/** JPA hint key for the lock timeout (also understood by Hibernate's special values below). */
	private static final String LOCK_TIMEOUT_HINT = "jakarta.persistence.lock.timeout";

	/** Hibernate special lock-timeout value: {@code FOR UPDATE NOWAIT}. */
	private static final int NO_WAIT = 0;

	/** Hibernate special lock-timeout value: {@code FOR UPDATE SKIP LOCKED}. */
	private static final int SKIP_LOCKED = -2;

	/** Entity manager. */
	private final EntityManager entityManager;

	/** Entity information (entity name and id attribute, used to build the generic lease queries). */
	private final JpaEntityInformation<T, ?> entityInformation;

	/** Entity type. */
	private final Class<T> domainClass;

	/**
	 * Default constructor (matches the signature Spring Data uses to instantiate the base class).
	 *
	 * @param entityInformation Entity information.
	 * @param entityManager     Entity manager.
	 */
	public PostgresJpaRepositoryImpl(
			final JpaEntityInformation<T, ?> entityInformation,
			final EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.entityManager = entityManager;
		this.entityInformation = entityInformation;
		this.domainClass = entityInformation.getJavaType();
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#findByIdForRead(java.lang.Object)
	 */
	@Override
	public Optional<T> findByIdForRead(
			final I id) {
		final T entity = this.entityManager.find(this.domainClass, id);
		if (entity != null) {
			this.entityManager.detach(entity);
		}
		return Optional.ofNullable(entity);
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#findByIdForUpdateWait(java.lang.Object)
	 */
	@Override
	@Transactional
	public Optional<T> findByIdForUpdateWait(
			final I id) {
		return Optional.ofNullable(this.entityManager.find(this.domainClass, id, LockModeType.PESSIMISTIC_WRITE));
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#findByIdForUpdateWait(java.lang.Object,
	 *      java.time.Duration)
	 */
	@Override
	@Transactional
	public Optional<T> findByIdForUpdateWait(
			final I id,
			final Duration timeout) {
		this.setLockTimeout(timeout);
		return Optional.ofNullable(this.entityManager.find(this.domainClass, id, LockModeType.PESSIMISTIC_WRITE));
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#findByIdForUpdateSkip(java.lang.Object)
	 */
	@Override
	@Transactional
	public Optional<T> findByIdForUpdateSkip(
			final I id) {
		return Optional.ofNullable(
				this.entityManager.find(this.domainClass, id, LockModeType.PESSIMISTIC_WRITE, Map.of(LOCK_TIMEOUT_HINT, SKIP_LOCKED)));
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#findByIdForUpdateFail(java.lang.Object)
	 */
	@Override
	@Transactional
	public Optional<T> findByIdForUpdateFail(
			final I id) {
		return Optional.ofNullable(
				this.entityManager.find(this.domainClass, id, LockModeType.PESSIMISTIC_WRITE, Map.of(LOCK_TIMEOUT_HINT, NO_WAIT)));
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#findByIdForShare(java.lang.Object)
	 */
	@Override
	@Transactional
	public Optional<T> findByIdForShare(
			final I id) {
		return Optional.ofNullable(this.entityManager.find(this.domainClass, id, LockModeType.PESSIMISTIC_READ));
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#setLockTimeout(java.time.Duration)
	 */
	@Override
	@Transactional
	public void setLockTimeout(
			final Duration timeout) {
		this.setConfig("lock_timeout", timeout);
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#setStatementTimeout(java.time.Duration)
	 */
	@Override
	@Transactional
	public void setStatementTimeout(
			final Duration timeout) {
		this.setConfig("statement_timeout", timeout);
	}

	/**
	 * Sets a transaction-local Postgres GUC to a millisecond duration value.
	 *
	 * <p>The {@code set_config} query is run with {@link FlushModeType#COMMIT} so it does not trigger
	 * a pre-execution auto-flush: otherwise a dirty entity left in the (open-session-in-view) context
	 * would be flushed as an {@code UPDATE} before the timeout is even applied, which can block on a
	 * row lock and time out under load.
	 *
	 * @param name    GUC name.
	 * @param timeout Duration (truncated to milliseconds, floored at 0).
	 */
	private void setConfig(
			final String name,
			final Duration timeout) {
		this.entityManager.createNativeQuery("SELECT set_config(:name, :value, true)")
				.setParameter("name", name)
				.setParameter("value", Long.toString(Math.max(0L, timeout.toMillis())))
				.setFlushMode(FlushModeType.COMMIT)
				.getSingleResult();
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#claimLease(java.lang.Object,
	 *      java.lang.String, java.time.Duration)
	 */
	@Override
	@Transactional
	public boolean claimLease(
			final I id,
			final String leaseAttribute,
			final Duration lease) {
		final String lease0 = this.validatedAttribute(leaseAttribute);
		final LocalDateTime now = DateTimeHelper.getCurrentLocalDateTime();
		final String update = "UPDATE " + this.entityInformation.getEntityName() + " entity SET entity." + lease0 + " = :until WHERE entity."
				+ this.entityInformation.getIdAttribute().getName() + " = :id AND (entity." + lease0 + " IS NULL OR entity." + lease0 + " < :now)";
		return this.entityManager.createQuery(update)
				.setParameter("until", now.plus(lease))
				.setParameter("now", now)
				.setParameter("id", id)
				.executeUpdate() == 1;
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#releaseLease(java.lang.Object,
	 *      java.lang.String)
	 */
	@Override
	@Transactional
	public void releaseLease(
			final I id,
			final String leaseAttribute) {
		final String lease0 = this.validatedAttribute(leaseAttribute);
		final String update = "UPDATE " + this.entityInformation.getEntityName() + " entity SET entity." + lease0 + " = NULL WHERE entity."
				+ this.entityInformation.getIdAttribute().getName() + " = :id";
		this.entityManager.createQuery(update).setParameter("id", id).executeUpdate();
	}

	/**
	 * Whitelists a lease attribute name before it is interpolated into JPQL: returns the canonical
	 * name only if it is a real persistent attribute of this entity, otherwise throws
	 * {@link IllegalArgumentException}. This is what keeps the lease helpers from being a JPQL/SQL
	 * injection vector — an attacker-supplied or mistyped name never reaches the query.
	 *
	 * @param  attribute Candidate attribute name.
	 * @return           The canonical persistent attribute name.
	 */
	private String validatedAttribute(
			final String attribute) {
		return this.entityManager.getMetamodel().entity(this.domainClass).getAttribute(attribute).getName();
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#detach(java.lang.Object)
	 */
	@Override
	public void detach(
			final T entity) {
		this.entityManager.detach(entity);
	}

	/**
	 * @see org.coldis.library.persistence.repository.PostgresJpaRepository#refresh(java.lang.Object)
	 */
	@Override
	@Transactional
	public void refresh(
			final T entity) {
		this.entityManager.refresh(entity);
	}

}

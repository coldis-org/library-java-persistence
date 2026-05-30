package org.coldis.library.test.persistence.repository;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.coldis.library.persistence.LockBehavior;
import org.coldis.library.persistence.keyvalue.KeyValue;
import org.coldis.library.persistence.keyvalue.KeyValueRepository;
import org.coldis.library.test.SpringTestHelper;
import org.coldis.library.test.StartTestWithContainerExtension;
import org.coldis.library.test.StopTestWithContainerExtension;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.TestWithContainer;
import org.coldis.library.test.persistence.TestApplication;
import org.coldis.library.test.persistence.keyvalue.TestValue;
import org.coldis.library.test.persistence.model.TestEntity;
import org.coldis.library.test.persistence.model.TestEntityRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Tests the Postgres locking helpers added by {@link org.coldis.library.persistence.repository.PostgresJpaRepository}.
 */
@TestWithContainer
@ExtendWith(StartTestWithContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class
)
@ExtendWith(StopTestWithContainerExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PostgresJpaRepositoryTest extends SpringTestHelper {

	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	/** How long the contention holder keeps the row lock — must outlast the probes below. */
	private static final long HOLD_MS = 2500L;

	@Autowired
	private TestEntityRepository repository;

	@Autowired
	private KeyValueRepository<TestValue> keyValueRepository;

	@PersistenceContext
	private EntityManager entityManager;

	/** Id of a row saved fresh before each test. */
	private Long entityId;

	@BeforeEach
	public void beforeEach() {
		this.entityId = this.createEntity();
	}

	/**
	 * Saves a fresh entity and returns its id.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long createEntity() {
		return this.repository.save(new TestEntity()).getId();
	}

	/**
	 * @return Whether the entity returned by {@code findByIdForRead} is still managed by the context.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean isManagedAfterForRead(
			final Long id) {
		final TestEntity entity = this.repository.findByIdForRead(id).orElseThrow();
		return this.entityManager.contains(entity);
	}

	/**
	 * @return Whether the entity returned by plain {@code findById} is managed by the context.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean isManagedAfterFindById(
			final Long id) {
		final TestEntity entity = this.repository.findById(id).orElseThrow();
		return this.entityManager.contains(entity);
	}

	@Test
	@DisplayName("findByIdForRead returns a detached entity; plain findById returns a managed one")
	public void testFindByIdForReadReturnsDetached() {
		Assertions.assertFalse(this.isManagedAfterForRead(this.entityId), "findByIdForRead must detach its result");
		Assertions.assertTrue(this.isManagedAfterFindById(this.entityId), "plain findById returns a managed entity");
	}

	/**
	 * Holds the row lock ({@code FOR UPDATE}) for {@link #HOLD_MS}, signalling once acquired.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void lockAndHold(
			final Long id,
			final CountDownLatch acquired,
			final long holdMs) throws InterruptedException {
		this.repository.findByIdForUpdateWait(id);
		acquired.countDown();
		Thread.sleep(holdMs);
	}

	/**
	 * Starts a daemon thread holding the row lock; returns once the lock is held.
	 */
	private Thread startHolder() throws InterruptedException {
		final CountDownLatch acquired = new CountDownLatch(1);
		final Thread holder = new Thread(() -> {
			try {
				this.lockAndHold(this.entityId, acquired, HOLD_MS);
			}
			catch (final InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
		});
		holder.setDaemon(true);
		holder.start();
		Assertions.assertTrue(acquired.await(5, TimeUnit.SECONDS), "holder failed to acquire the lock");
		return holder;
	}

	@Test
	@DisplayName("findByIdForUpdateFail throws immediately when the row is locked")
	public void testFindByIdForUpdateFailThrowsWhenLocked() throws InterruptedException {
		final Thread holder = this.startHolder();
		Assertions.assertThrows(PessimisticLockingFailureException.class, () -> this.repository.findByIdForUpdateFail(this.entityId));
		holder.join();
	}

	@Test
	@DisplayName("findByIdForUpdateSkip returns empty when the row is locked")
	public void testFindByIdForUpdateSkipReturnsEmptyWhenLocked() throws InterruptedException {
		final Thread holder = this.startHolder();
		Assertions.assertTrue(this.repository.findByIdForUpdateSkip(this.entityId).isEmpty());
		holder.join();
	}

	@Test
	@DisplayName("findByIdForUpdateWait(timeout) fails fast, well before the holder releases")
	public void testFindByIdForUpdateWaitTimesOut() throws InterruptedException {
		final Thread holder = this.startHolder();
		final long start = System.currentTimeMillis();
		Assertions.assertThrows(PessimisticLockingFailureException.class,
				() -> this.repository.findByIdForUpdateWait(this.entityId, Duration.ofMillis(200)));
		final long elapsed = System.currentTimeMillis() - start;
		Assertions.assertTrue(elapsed < HOLD_MS, "bounded wait should fail before the holder releases (elapsed=" + elapsed + "ms)");
		holder.join();
	}

	@Test
	@DisplayName("findById(NO_LOCK) returns the row without taking a lock")
	public void testDispatchNoLock() {
		Assertions.assertTrue(this.repository.findById(this.entityId, LockBehavior.NO_LOCK, null).isPresent());
	}

	@Test
	@DisplayName("findById(WAIT_AND_LOCK) returns the row when uncontended")
	public void testDispatchWaitAndLock() {
		Assertions.assertTrue(this.repository.findById(this.entityId, LockBehavior.WAIT_AND_LOCK, Duration.ofSeconds(1)).isPresent());
	}

	@Test
	@DisplayName("findByIdForShare returns the row when uncontended")
	public void testFindByIdForShare() {
		Assertions.assertTrue(this.repository.findByIdForShare(this.entityId).isPresent());
	}

	/**
	 * Runs find-or-create against the key/value table with the given strategy, recording whether the
	 * idempotent insert callback ran.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String findOrCreate(
			final String key,
			final AtomicBoolean insertInvoked,
			final boolean insertFirst) {
		final KeyValue<TestValue> result = this.keyValueRepository.findByIdForUpdateOrCreate(key, () -> {
			insertInvoked.set(true);
			this.keyValueRepository.insertIfAbsent(key);
		}, insertFirst);
		return (result == null) ? null : result.getKey();
	}

	@Test
	@DisplayName("findByIdForUpdateOrCreate optimistic (find first) inserts only on a miss")
	public void testFindOrCreateOptimistic() {
		final String key = "post-jpa-optimistic";
		final AtomicBoolean firstInsert = new AtomicBoolean(false);
		Assertions.assertEquals(key, this.findOrCreate(key, firstInsert, false));
		Assertions.assertTrue(firstInsert.get(), "absent row should trigger the idempotent insert");
		final AtomicBoolean secondInsert = new AtomicBoolean(false);
		Assertions.assertEquals(key, this.findOrCreate(key, secondInsert, false));
		Assertions.assertFalse(secondInsert.get(), "present row should not trigger the insert (find first)");
	}

	@Test
	@DisplayName("findByIdForUpdateOrCreate pessimistic (insert first) always issues the idempotent insert")
	public void testFindOrCreatePessimistic() {
		final String key = "post-jpa-pessimistic";
		final AtomicBoolean firstInsert = new AtomicBoolean(false);
		Assertions.assertEquals(key, this.findOrCreate(key, firstInsert, true));
		Assertions.assertTrue(firstInsert.get(), "absent row should be created by the insert-first strategy");
		final AtomicBoolean secondInsert = new AtomicBoolean(false);
		Assertions.assertEquals(key, this.findOrCreate(key, secondInsert, true));
		Assertions.assertTrue(secondInsert.get(), "insert-first always runs the idempotent insert (ON CONFLICT no-op)");
	}

	@Test
	@DisplayName("claimLease grants once, blocks a live second claim, and frees on release")
	public void testClaimLeaseAndRelease() {
		Assertions.assertTrue(this.repository.claimLease(this.entityId, "leasedUntil", Duration.ofMinutes(1)),
				"first claim should win");
		Assertions.assertFalse(this.repository.claimLease(this.entityId, "leasedUntil", Duration.ofMinutes(1)),
				"second claim should fail while the lease is live");
		this.repository.releaseLease(this.entityId, "leasedUntil");
		Assertions.assertTrue(this.repository.claimLease(this.entityId, "leasedUntil", Duration.ofMinutes(1)),
				"claim should win again after release");
	}

	@Test
	@DisplayName("claimLease whitelists the lease attribute against the metamodel (no injection)")
	public void testClaimLeaseRejectsUnknownAttribute() {
		// The metamodel lookup rejects a non-attribute name before any query is built; the repository's
		// persistence-exception translation surfaces it as InvalidDataAccessApiUsageException.
		Assertions.assertThrows(InvalidDataAccessApiUsageException.class,
				() -> this.repository.claimLease(this.entityId, "leasedUntil = NULL OR 1=1", Duration.ofMinutes(1)));
	}

	@Test
	@DisplayName("polling claimLease acquires once the holder's lease expires")
	public void testClaimLeasePollingAcquiresAfterExpiry() {
		// Hold a 1s lease, then poll with a longer one: the poll should win once the short lease lapses.
		Assertions.assertTrue(this.repository.claimLease(this.entityId, "leasedUntil", Duration.ofSeconds(1)));
		Assertions.assertTrue(
				this.repository.claimLease(this.entityId, "leasedUntil", Duration.ofMinutes(1), Duration.ofMillis(200), Duration.ofSeconds(5)),
				"poll should acquire after the 1s lease expires");
	}

	@Test
	@DisplayName("polling claimLease gives up after maxWait when the lease stays held")
	public void testClaimLeasePollingTimesOut() {
		Assertions.assertTrue(this.repository.claimLease(this.entityId, "leasedUntil", Duration.ofMinutes(1)));
		final long start = System.currentTimeMillis();
		Assertions.assertFalse(
				this.repository.claimLease(this.entityId, "leasedUntil", Duration.ofMinutes(1), Duration.ofMillis(200), Duration.ofSeconds(1)),
				"poll should give up while the lease stays live");
		Assertions.assertTrue(System.currentTimeMillis() - start >= 1000L, "should have polled until maxWait");
	}

}

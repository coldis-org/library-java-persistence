package org.coldis.library.persistence.keyvalue;

import java.util.List;

import org.coldis.library.exception.BusinessException;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.model.Typable;
import org.coldis.library.persistence.LockBehavior;
import org.coldis.library.persistence.lock.LockServiceComponent;
import org.coldis.library.persistence.lock.LockType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Key/value service.
 */
@Service
public class KeyValueServiceComponent {

	/**
	 * Delete queue.
	 */
	static final String DELETE_QUEUE = "key-value/delete";

	/** Advisory-lock namespace for key/value entries. */
	private static final String LOCK_NAMESPACE = "key-value";

	/**
	 * Repository.
	 */
	@Autowired
	private KeyValueRepository<Typable> repository;

	/**
	 * Lock service, used to serialize concurrent {@link #lock} callers per key.
	 */
	@Autowired
	private LockServiceComponent lockService;

	/**
	 * Gets the repository.
	 *
	 * @return The repository.
	 */
	public KeyValueRepository<Typable> getRepository() {
		return this.repository;
	}

	/**
	 * Finds a key entry.
	 *
	 * @param  key               The key.
	 * @param  lock              If the object should be clocked.
	 * @return                   The entry.
	 * @throws BusinessException If the entry is not found.
	 */
	public KeyValue<Typable> findById(
			final String key,
			final LockBehavior lock,
			final Boolean ignoreNotFound) throws BusinessException {
		// Tries to find the keyValue.
		final KeyValue<Typable> keyValue = (LockBehavior.WAIT_AND_LOCK.equals(lock) ? this.repository.findByIdForUpdate(key).orElse(null)
				: LockBehavior.LOCK_FAIL_FAST.equals(lock) ? this.repository.findByIdForUpdateFailFast(key).orElse(null)
						: LockBehavior.LOCK_SKIP.equals(lock) ? this.repository.findByIdForUpdateSkipLocked(key).orElse(null)
								: this.repository.findById(key).orElse(null));
		// If no keyValue is found.
		if (!ignoreNotFound && (keyValue == null)) {
			// Throws a not found exception.
			throw new BusinessException(new SimpleMessage("keyValue.notfound"));
		}
		// If the keyValue is found, returns it.
		return keyValue;
	}

	/**
	 * Finds a key entry.
	 *
	 * @param  key               The key.
	 * @return                   The entry.
	 * @throws BusinessException If the entry is not found.
	 */
	@Transactional(
			propagation = Propagation.NOT_SUPPORTED,
			readOnly = true
	)
	public KeyValue<Typable> findById(
			final String key) throws BusinessException {
		return this.findById(key, LockBehavior.NO_LOCK, false);
	}

	/**
	 * Finds a key entry.
	 *
	 * @param  key               The key.
	 * @return                   The entry.
	 * @throws BusinessException If the entry is not found.
	 */
	@Transactional(
			propagation = Propagation.NOT_SUPPORTED,
			readOnly = true
	)
	public List<KeyValue<Typable>> findByKeyStart(
			final String keyStart) throws BusinessException {
		return this.repository.findByKeyStartsWith(keyStart);
	}

	/**
	 * Creates a key entry.
	 *
	 * @param  key   The key.
	 * @param  value Value.
	 * @return       The created entry.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public KeyValue<Typable> create(
			final String key,
			final Typable value) {
		return this.repository.save(new KeyValue<>(key, value));
	}

	/**
	 * Creates a key entry.
	 *
	 * @param  key               The key.
	 * @param  value             Value.
	 * @return                   The created entry.
	 * @throws BusinessException If the key value cannot be updated.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public KeyValue<Typable> update(
			final String key,
			final Typable value) throws BusinessException {
		final KeyValue<Typable> keyValue = this.findById(key, LockBehavior.WAIT_AND_LOCK, false);
		keyValue.setValue(value);
		return this.repository.save(keyValue);
	}

	/**
	 * Deletes a key entry.
	 *
	 * @param key The key.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void delete(
			final String key) {
		if (this.repository.existsById(key)) {
			this.repository.deleteById(key);
		}
	}

	/**
	 * Locks a key, creating the row if it doesn't exist. Race-free against concurrent first-time
	 * inserts via {@link LockServiceComponent} on the cold (create) path; existing rows are
	 * acquired directly via {@code findByIdForUpdate} with the requested {@link LockBehavior}.
	 *
	 * @param  key               Key.
	 * @param  lock              Lock behavior.
	 * @return                   The locked entry, or {@code null} when the requested behavior
	 *                           could not acquire the row lock (e.g. {@code LOCK_SKIP} contended).
	 * @throws BusinessException If a {@code LOCK_FAIL_FAST} acquisition fails.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public KeyValue<Typable> lock(
			final String key,
			final LockBehavior lock) throws BusinessException {
		// Hot path: row already exists and is acquirable under the requested behavior.
		// findByIdForUpdate handles row-level SKIP / NOWAIT semantics directly — no lock service
		// needed for this case.
		KeyValue<Typable> entry = this.findById(key, lock, true);
		if (entry == null) {
			// Cold path: row doesn't exist (or was SKIP-contended). Take a key-scoped lock
			// via LockServiceComponent (TABLE mode for collision-free SKIP semantics) to
			// serialize the create with any other concurrent first-time creators, then
			// idempotent INSERT + re-find.
			final boolean acquired = this.lockService.lockKeys(lock, LockType.TABLE, KeyValueServiceComponent.LOCK_NAMESPACE, List.of(key));
			if (acquired) {
				this.repository.insertIfAbsent(key);
				entry = this.findById(key, lock, true);
			}
		}
		return entry;
	}

	/**
	 * @deprecated The {@code cleanAfterLock} flag conflates "lock a key" with "ensure no
	 *             materialized row survives the transaction." For a transient transaction-scoped
	 *             lock with no row materialized at all, use {@link LockServiceComponent#lockKeys}
	 *             directly. For a lock that creates and keeps the row, use
	 *             {@link #lock(String, LockBehavior)}.
	 */
	@Deprecated
	@Transactional(propagation = Propagation.REQUIRED)
	public KeyValue<Typable> lock(
			final String key,
			final LockBehavior lock,
			final Boolean cleanAfterLock) throws BusinessException {
		final KeyValue<Typable> entry = this.lock(key, lock);
		if ((cleanAfterLock != null) && cleanAfterLock && (entry != null)) {
			this.repository.delete(entry);
		}
		return entry;
	}

}

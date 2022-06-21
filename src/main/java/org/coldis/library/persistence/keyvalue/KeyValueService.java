package org.coldis.library.persistence.keyvalue;

import java.util.Optional;

import org.coldis.library.exception.BusinessException;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.model.Typable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Key/value service.
 */
@Service
public class KeyValueService {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(KeyValueService.class);

	/**
	 * Repository.
	 */
	@Autowired
	private KeyValueRepository<Typable> repository;

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
			final Boolean lock) throws BusinessException {
		// Tries to find the keyValue.
		final KeyValue<Typable> keyValue = (lock ? this.repository.findByIdForUpdate(key).orElse(null) : this.repository.findById(key).orElse(null));
		// If no keyValue is found.
		if (keyValue == null) {
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
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public KeyValue<Typable> findById(
			final String key) throws BusinessException {
		return this.findById(key, false);
	}

	/**
	 * Creates a key entry.
	 *
	 * @param  key   The key.
	 * @param  value Value.
	 * @return       The created entry.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
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
		final KeyValue<Typable> keyValue = this.findById(key, true);
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
	 * Locks a key.
	 *
	 * @param  key Key.
	 * @return     Returns the locked object.
	 */
	@Transactional(
			propagation = Propagation.REQUIRED,
			noRollbackFor = DataIntegrityViolationException.class
	)
	public Optional<KeyValue<Typable>> lock(
			final String key) {
		// Tries to lock the entry.
		Optional<KeyValue<Typable>> entry = this.repository.findByIdForUpdate(key);
		// If there is no entry.
		if (entry.isEmpty()) {
			// Tries creating the entry.
			try {
				this.create(key, null);
			}
			catch (final Exception exception) {
				KeyValueService.LOGGER.warn("Could not create key: " + exception.getLocalizedMessage());
				KeyValueService.LOGGER.debug("Could not create key.", exception);
			}
			// Locks the entry.
			entry = this.repository.findByIdForUpdate(key);
		}
		// Returns the object.
		return entry;
	}

}

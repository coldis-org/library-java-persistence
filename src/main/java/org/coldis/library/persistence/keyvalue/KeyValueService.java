package org.coldis.library.persistence.keyvalue;

import java.util.Optional;

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
	 * Creates a key entry.
	 *
	 * @param  key The key.
	 * @return     The created entry.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public KeyValue<Typable> create(
			final String key) {
		return this.repository.save(new KeyValue<>(key, null));
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
				this.create(key);
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

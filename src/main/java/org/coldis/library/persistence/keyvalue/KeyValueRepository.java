package org.coldis.library.persistence.keyvalue;

import java.util.List;
import java.util.Optional;

import org.coldis.library.model.Typable;
import org.coldis.library.persistence.repository.PostgresJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Key/value repository.
 *
 * @param <ValueType> Value type.
 */
@Repository
@ConditionalOnProperty(
		name = "org.coldis.configuration.persistence-keyvalue-enabled",
		havingValue = "true",
		matchIfMissing = true
)
public interface KeyValueRepository<ValueType extends Typable> extends PostgresJpaRepository<KeyValue<ValueType>, String> {

	/**
	 * Finds a key/value.
	 *
	 * @param  key The key for the value.
	 * @return     A key/value.
	 */
	@Override
	Optional<KeyValue<ValueType>> findById(
			String key);

	/**
	 * Finds a key/value for update.
	 *
	 * @param      key The key for the value.
	 * @return         A key/value for update.
	 * @deprecated     Use {@link #findByIdForUpdateWait(Object)}.
	 */
	@Deprecated
	default Optional<KeyValue<ValueType>> findByIdForUpdate(
			final String key) {
		return this.findByIdForUpdateWait(key);
	}

	/**
	 * Finds a key/value for update, skipping if locked.
	 *
	 * @param      key The key for the value.
	 * @return         A key/value for update, or empty if locked.
	 * @deprecated     Use {@link #findByIdForUpdateSkip(Object)}.
	 */
	@Deprecated
	default Optional<KeyValue<ValueType>> findByIdForUpdateSkipLocked(
			final String key) {
		return this.findByIdForUpdateSkip(key);
	}

	/**
	 * Finds a key/value for update, failing fast if locked.
	 *
	 * @param      key The key for the value.
	 * @return         A key/value for update.
	 * @deprecated     Use {@link #findByIdForUpdateFail(Object)}.
	 */
	@Deprecated
	default Optional<KeyValue<ValueType>> findByIdForUpdateFailFast(
			final String key) {
		return this.findByIdForUpdateFail(key);
	}

	/**
	 * Finds values for key starting with.
	 *
	 * @param  key Key.
	 * @return     Values for key starting with.
	 */
	List<KeyValue<ValueType>> findByKeyStartsWith(
			String key);

	/**
	 * Idempotent INSERT — creates the row with a null value if it doesn't exist; does nothing
	 * if a row with this key already exists. Returns the number of rows actually inserted
	 * (0 or 1). Used by {@code lock(...)} to make the create-or-find race architecturally
	 * impossible without a separate lock primitive.
	 */
	@Modifying
	@Query(
			value = "INSERT INTO key_value (key, created_at, updated_at) VALUES (:key, NOW(), NOW()) ON CONFLICT (key) DO NOTHING",
			nativeQuery = true
	)
	int insertIfAbsent(
			@Param("key")
			String key);

}

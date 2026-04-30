package org.coldis.library.persistence.keyvalue;

import java.util.List;
import java.util.Optional;

import org.coldis.library.model.Typable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

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
public interface KeyValueRepository<ValueType extends Typable> extends JpaRepository<KeyValue<ValueType>, String> {

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
	 * @param  key The key for the value.
	 * @return     A key/value for update.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT keyValue FROM KeyValue keyValue WHERE keyValue.key = :key")
	Optional<KeyValue<ValueType>> findByIdForUpdate(
			@Param("key")
			String key);

	/**
	 * Finds a key/value for update.
	 *
	 * @param  key The key for the value.
	 * @return     A key/value for update.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(
			value = { @QueryHint(
					name = "jakarta.persistence.lock.timeout",
					value = "-2"
			) }
	)
	@Query("SELECT keyValue FROM KeyValue keyValue WHERE keyValue.key = :key")
	Optional<KeyValue<ValueType>> findByIdForUpdateSkipLocked(
			@Param("key")
			String key);

	/**
	 * Finds a key/value for update.
	 *
	 * @param  key The key for the value.
	 * @return     A key/value for update.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(
			value = { @QueryHint(
					name = "jakarta.persistence.lock.timeout",
					value = "0"
			) }
	)
	@Query("SELECT keyValue FROM KeyValue keyValue WHERE keyValue.key = :key")
	Optional<KeyValue<ValueType>> findByIdForUpdateFailFast(
			@Param("key")
			String key);

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

package org.coldis.library.persistence.keyvalue;

import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.coldis.library.model.Typable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
	 * Finds values for key starting with.
	 *
	 * @param  key Key.
	 * @return     Values for key starting with.
	 */
	List<KeyValue<ValueType>> findByKeyStartsWith(
			String key);

}

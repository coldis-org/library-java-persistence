package org.coldis.library.persistence.lock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Internal repository for {@link LockKey} rows. Used by {@link LockServiceComponent} to acquire
 * and release {@link LockType#TABLE} locks via INSERT-then-DELETE in the same transaction.
 */
@Repository
public interface LockKeyRepository extends JpaRepository<LockKey, String> {

	/**
	 * Inserts a lock row, silently no-ops if it already exists. Returns the number of rows
	 * actually inserted (0 or 1) so the caller can detect when an existing committed row
	 * blocked acquisition.
	 */
	@Modifying
	@Query(
			value = "INSERT INTO lock_key (id) VALUES (:id) ON CONFLICT (id) DO NOTHING",
			nativeQuery = true
	)
	int insertIfAbsent(
			@Param("id")
			String id);

	/**
	 * Deletes a lock row by id. Used during the {@code beforeCommit} hook of the lock-holder's
	 * transaction so the row never persists past commit.
	 */
	@Modifying
	@Query(
			value = "DELETE FROM lock_key WHERE id = :id",
			nativeQuery = true
	)
	int deleteRow(
			@Param("id")
			String id);

}

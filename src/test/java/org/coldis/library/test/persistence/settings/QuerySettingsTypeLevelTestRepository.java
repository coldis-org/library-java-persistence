package org.coldis.library.test.persistence.settings;

import org.coldis.library.persistence.settings.QuerySettings;
import org.coldis.library.test.persistence.model.TestEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Repository annotated at type level: the settings apply to all its methods.
 */
@org.springframework.stereotype.Repository
@QuerySettings(
		workMem = "96MB",
		startTransaction = true
)
public interface QuerySettingsTypeLevelTestRepository extends Repository<TestEntity, Long> {

	/**
	 * Reads work_mem.
	 *
	 * @return work_mem.
	 */
	@Query(
			value = "SELECT current_setting('work_mem')",
			nativeQuery = true
	)
	String findWorkMem();

}

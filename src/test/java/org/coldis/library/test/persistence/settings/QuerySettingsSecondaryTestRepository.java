package org.coldis.library.test.persistence.settings;

import org.coldis.library.persistence.configuration.DatasourceUnit;
import org.coldis.library.persistence.settings.QuerySettings;
import org.coldis.library.test.persistence.secondary.model.TestSecondaryEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Repository of the secondary unit: its settings target the secondary datasource without naming it.
 */
@org.springframework.stereotype.Repository
@DatasourceUnit(value = "secondary")
public interface QuerySettingsSecondaryTestRepository extends Repository<TestSecondaryEntity, Long> {

	/**
	 * Reads work_mem on the secondary datasource.
	 *
	 * @return work_mem.
	 */
	@QuerySettings(
			workMem = "73MB",
			startTransaction = true
	)
	@Query(
			value = "SELECT current_setting('work_mem')",
			nativeQuery = true
	)
	String findWorkMem();

}

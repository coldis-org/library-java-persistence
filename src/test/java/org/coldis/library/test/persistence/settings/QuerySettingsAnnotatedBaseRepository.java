package org.coldis.library.test.persistence.settings;

import org.coldis.library.persistence.settings.QuerySettings;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Shared parent repository annotated at type level: repositories extending it get the settings.
 *
 * @param <EntityType>     Entity type.
 * @param <IdentifierType> Identifier type.
 */
@NoRepositoryBean
@QuerySettings(
		workMem = "95MB",
		startTransaction = true
)
public interface QuerySettingsAnnotatedBaseRepository<EntityType, IdentifierType> extends Repository<EntityType, IdentifierType> {

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

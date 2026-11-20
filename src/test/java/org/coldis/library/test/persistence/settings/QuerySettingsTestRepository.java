package org.coldis.library.test.persistence.settings;

import org.coldis.library.persistence.settings.PlanCacheMode;
import org.coldis.library.persistence.settings.QuerySettings;
import org.coldis.library.test.persistence.model.TestEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository (a JDK proxy, never woven) with {@link QuerySettings} on its methods.
 */
@org.springframework.stereotype.Repository
public interface QuerySettingsTestRepository extends Repository<TestEntity, Long> {

	/**
	 * Reads work_mem with settings applied.
	 *
	 * @return work_mem.
	 */
	@QuerySettings(
			workMem = "99MB",
			planCacheMode = PlanCacheMode.FORCE_CUSTOM_PLAN
	)
	@Query(
			value = "SELECT current_setting('work_mem') || ',' || current_setting('plan_cache_mode')",
			nativeQuery = true
	)
	String findWorkMemAndPlanCacheMode();

	/**
	 * Reads work_mem with settings applied, inside the method's own transaction.
	 *
	 * @return work_mem.
	 */
	@Transactional(readOnly = true)
	@QuerySettings(workMem = "97MB")
	@Query(
			value = "SELECT current_setting('work_mem')",
			nativeQuery = true
	)
	String findWorkMemInOwnTransaction();

	/**
	 * Reads work_mem without settings.
	 *
	 * @return work_mem.
	 */
	@Query(
			value = "SELECT current_setting('work_mem')",
			nativeQuery = true
	)
	String findWorkMem();

}

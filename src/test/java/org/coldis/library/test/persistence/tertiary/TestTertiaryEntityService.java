package org.coldis.library.test.persistence.tertiary;

import org.coldis.library.test.persistence.tertiary.model.TestTertiaryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits writes to the {@code tertiary} datasource through its transaction manager. Gated on the
 * tertiary datasource being configured.
 */
@Service
@ConditionalOnProperty(name = "org.coldis.configuration.persistence.datasources.tertiary.url")
public class TestTertiaryEntityService {

	/** Tertiary repository. */
	@Autowired
	private TestTertiaryEntityRepository testTertiaryEntityRepository;

	/**
	 * Saves and commits through the tertiary transaction manager.
	 *
	 * @param  entity Entity to save.
	 * @return        The saved entity.
	 */
	@Transactional(transactionManager = "tertiaryTransactionManager")
	public TestTertiaryEntity save(
			final TestTertiaryEntity entity) {
		return this.testTertiaryEntityRepository.save(entity);
	}

}

package org.coldis.library.test.persistence.secondary;

import org.coldis.library.test.persistence.secondary.model.TestSecondaryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits writes to the secondary datasource through the secondary transaction manager (coldis
 * disables Spring Data default transactions, so writes must run inside an explicit transaction).
 * Gated on the same flag as the secondary configuration.
 */
@Service
@ConditionalOnProperty(name = "org.coldis.configuration.persistence.datasources.secondary.url")
public class TestSecondaryEntityService {

	/** Secondary repository. */
	@Autowired
	private TestSecondaryEntityRepository testSecondaryEntityRepository;

	/**
	 * Saves and commits through the secondary transaction manager.
	 *
	 * @param  entity Entity to save.
	 * @return        The saved entity.
	 */
	@Transactional(transactionManager = "secondaryTransactionManager")
	public TestSecondaryEntity save(
			final TestSecondaryEntity entity) {
		return this.testSecondaryEntityRepository.save(entity);
	}

}

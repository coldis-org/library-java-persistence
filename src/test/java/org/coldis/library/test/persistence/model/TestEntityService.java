package org.coldis.library.test.persistence.model;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Test service.
 */
@Service
@Transactional
public class TestEntityService {

	/**
	 * Test entity repository.
	 */
	@Autowired
	private TestEntityRepository testEntityRepository;

	/**
	 * Saves the entity.
	 *
	 * @param  entity Entity to save.
	 * @return        The saved entity.
	 */
	@Transactional
	public TestEntity save(final TestEntity entity) {
		return this.testEntityRepository.save(entity);
	}

}

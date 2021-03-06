package org.coldis.library.test.persistence.history;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test service.
 */
@Controller
@Transactional
public class TestHistoricalEntityService {

	/**
	 * Test entity repository.
	 */
	@Autowired
	private TestHistoricalEntityRepository testHistoricalEntityRepository;

	/**
	 * Saves the entity.
	 *
	 * @param  entity Entity to save.
	 * @return        The saved entity.
	 */
	@Transactional
	public TestHistoricalEntity save(final TestHistoricalEntity entity) {
		return this.testHistoricalEntityRepository.save(entity);
	}

}

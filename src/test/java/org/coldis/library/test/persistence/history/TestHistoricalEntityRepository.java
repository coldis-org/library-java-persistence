package org.coldis.library.test.persistence.history;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Test repository.
 */
@Repository
public interface TestHistoricalEntityRepository extends CrudRepository<TestHistoricalEntity, Long> {

}

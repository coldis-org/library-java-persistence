package org.coldis.library.test.persistence.history.historical.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory;

/**
 * JPA entity history repository for {@link org.coldis.library.test.persistence.history.TestHistoricalEntity}.
 */
@Repository
public interface TestHistoricalEntityHistoryRepository extends CrudRepository<TestHistoricalEntityHistory, Long> {

}

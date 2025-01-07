package org.coldis.library.test.persistence.history.historical.repository;

import org.coldis.library.test.persistence.history.historical.model.TestHistoricalEntityHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA entity history repository for {@link org.coldis.library.test.persistence.history.TestHistoricalEntity}.
 */
@Repository(value = "")
public interface TestHistoricalEntityHistoryRepository extends CrudRepository<TestHistoricalEntityHistory, Long> {

}

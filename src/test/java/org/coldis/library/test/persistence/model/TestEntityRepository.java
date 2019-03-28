package org.coldis.library.test.persistence.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Test repository.
 */
@Repository
public interface TestEntityRepository extends CrudRepository<TestEntity, Long> {

}

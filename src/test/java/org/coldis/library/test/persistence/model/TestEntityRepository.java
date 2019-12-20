package org.coldis.library.test.persistence.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test repository.
 */
@Repository
@Transactional
public interface TestEntityRepository extends CrudRepository<TestEntity, Long> {

}

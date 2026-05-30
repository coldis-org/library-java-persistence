package org.coldis.library.test.persistence.model;

import org.coldis.library.persistence.repository.PostgresJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test repository. Extends {@link PostgresJpaRepository} so the Postgres locking helpers are
 * exercised by {@code PostgresJpaRepositoryTest}.
 */
@Repository
@Transactional
public interface TestEntityRepository extends PostgresJpaRepository<TestEntity, Long> {

}

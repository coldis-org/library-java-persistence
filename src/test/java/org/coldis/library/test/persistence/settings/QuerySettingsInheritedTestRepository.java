package org.coldis.library.test.persistence.settings;

import org.coldis.library.test.persistence.model.TestEntity;

/**
 * Repository inheriting the type-level settings of its parent interface.
 */
@org.springframework.stereotype.Repository
public interface QuerySettingsInheritedTestRepository extends QuerySettingsAnnotatedBaseRepository<TestEntity, Long> {

}

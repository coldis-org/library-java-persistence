package org.coldis.library.test.persistence.tertiary;

import org.coldis.library.persistence.configuration.DatasourceUnit;
import org.coldis.library.test.persistence.tertiary.model.TestTertiaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository bound to the {@code tertiary} datasource by its {@link DatasourceUnit} annotation — a
 * second secondary unit, proving N &gt; 1.
 */
@Repository
@DatasourceUnit(value = "tertiary")
public interface TestTertiaryEntityRepository extends JpaRepository<TestTertiaryEntity, Long> {

}

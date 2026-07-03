package org.coldis.library.test.persistence.tertiary;

import org.coldis.library.test.persistence.tertiary.model.TestTertiaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository bound to the {@code tertiary} datasource (declared by
 * {@code ...datasources.tertiary.repository-packages}) — a second secondary unit, proving N &gt; 1.
 */
@Repository
public interface TestTertiaryEntityRepository extends JpaRepository<TestTertiaryEntity, Long> {

}

package org.coldis.library.test.persistence.secondary;

import org.coldis.library.test.persistence.secondary.model.TestSecondaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository bound to the {@code secondary} datasource. It lives in the package declared by
 * {@code ...datasources.secondary.repository-packages}, so the coldis primary scan excludes it and the
 * secondary unit's registrar binds it — no marker annotation needed.
 */
@Repository
public interface TestSecondaryEntityRepository extends JpaRepository<TestSecondaryEntity, Long> {

}

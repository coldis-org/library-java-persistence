package org.coldis.library.test.persistence.secondary;

import org.coldis.library.persistence.configuration.DatasourceUnit;
import org.coldis.library.test.persistence.secondary.model.TestSecondaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository bound to the {@code secondary} datasource by its {@link DatasourceUnit} annotation: the
 * coldis primary scan excludes it and the secondary unit's registrar binds it — packaging plays no
 * role.
 */
@Repository
@DatasourceUnit(value = "secondary")
public interface TestSecondaryEntityRepository extends JpaRepository<TestSecondaryEntity, Long> {

}

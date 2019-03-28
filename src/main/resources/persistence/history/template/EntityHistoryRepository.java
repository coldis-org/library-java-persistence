package ${historicalEntity.getDaoPackageName()};

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import ${historicalEntity.getEntityQualifiedTypeName()};

/**
 * JPA entity history repository for {@link ${historicalEntity.getOriginalEntityQualifiedTypeName()}}.
 */
@Repository
public interface ${historicalEntity.getDaoTypeName()} extends CrudRepository<${historicalEntity.getEntityTypeName()}, Long> {

}

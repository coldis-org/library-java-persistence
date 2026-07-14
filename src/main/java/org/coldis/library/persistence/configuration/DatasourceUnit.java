package org.coldis.library.persistence.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds an entity class or repository interface to a secondary persistence unit. The value must match
 * a datasource name configured under
 * {@code org.coldis.configuration.persistence.datasources.<name>}: annotated entities are mapped
 * (only) by that unit's {@code EntityManagerFactory} and annotated repositories are bound to that
 * unit's entity manager factory / transaction manager (named {@code <name>EntityManagerFactory} /
 * {@code <name>TransactionManager}).
 *
 * <p>
 * Annotated types never join the primary unit — entities and repositories can live anywhere under the
 * regular base packages ({@code org.coldis} and
 * {@code org.coldis.configuration.persistence.jpa.base-package}), side by side with primary ones; the
 * annotation alone decides the unit. A type annotated with a name that has no configured datasource
 * fails fast at startup. Custom repository implementation fragments ({@code *Impl} classes) of a
 * secondary repository must also carry the annotation, since the unit's repository scan only sees
 * annotated types.
 * </p>
 *
 * <p>
 * Transactions are unaffected: services working against a secondary unit still declare
 * {@code @Transactional(transactionManager = "<name>TransactionManager")}.
 * </p>
 */
@Documented
@Target(value = { ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DatasourceUnit {

	/**
	 * Datasource unit name (the map key under
	 * {@code org.coldis.configuration.persistence.datasources}).
	 */
	String value();

}

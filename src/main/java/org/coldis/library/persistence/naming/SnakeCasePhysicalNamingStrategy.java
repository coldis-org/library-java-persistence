package org.coldis.library.persistence.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Snake case naming strategy.
 */
public class SnakeCasePhysicalNamingStrategy implements PhysicalNamingStrategy {

	/**
	 * Converts to snake case.
	 *
	 * @param  identifier Identifier.
	 * @return            The converted identifier.
	 */
	private Identifier convertToSnakeCase(final Identifier identifier) {
		// Final identifier is the initial one by default.
		Identifier finalIdentifier = identifier;
		// If identifier is given.
		if (identifier != null) {
			// Converts the identifier to snake case.
			final String regex = "([a-z])([A-Z])";
			final String replacement = "$1_$2";
			final String newName = identifier.getText().replaceAll(regex, replacement).toLowerCase();
			finalIdentifier = Identifier.toIdentifier(newName);
		}
		// Returns the final identifier.
		return finalIdentifier;
	}

	/**
	 * @see org.hibernate.boot.model.naming.PhysicalNamingStrategy#toPhysicalCatalogName(org.hibernate.boot.model.naming.Identifier,
	 *      org.hibernate.engine.jdbc.env.spi.JdbcEnvironment)
	 */
	@Override
	public Identifier toPhysicalCatalogName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
		return this.convertToSnakeCase(identifier);
	}

	/**
	 * @see org.hibernate.boot.model.naming.PhysicalNamingStrategy#toPhysicalColumnName(org.hibernate.boot.model.naming.Identifier,
	 *      org.hibernate.engine.jdbc.env.spi.JdbcEnvironment)
	 */
	@Override
	public Identifier toPhysicalColumnName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
		return this.convertToSnakeCase(identifier);
	}

	/**
	 * @see org.hibernate.boot.model.naming.PhysicalNamingStrategy#toPhysicalSchemaName(org.hibernate.boot.model.naming.Identifier,
	 *      org.hibernate.engine.jdbc.env.spi.JdbcEnvironment)
	 */
	@Override
	public Identifier toPhysicalSchemaName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
		return this.convertToSnakeCase(identifier);
	}

	/**
	 * @see org.hibernate.boot.model.naming.PhysicalNamingStrategy#toPhysicalSequenceName(org.hibernate.boot.model.naming.Identifier,
	 *      org.hibernate.engine.jdbc.env.spi.JdbcEnvironment)
	 */
	@Override
	public Identifier toPhysicalSequenceName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
		return this.convertToSnakeCase(identifier);
	}

	/**
	 * @see org.hibernate.boot.model.naming.PhysicalNamingStrategy#toPhysicalTableName(org.hibernate.boot.model.naming.Identifier,
	 *      org.hibernate.engine.jdbc.env.spi.JdbcEnvironment)
	 */
	@Override
	public Identifier toPhysicalTableName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
		return this.convertToSnakeCase(identifier);
	}

}
package org.coldis.library.test.persistence.secondary.model;

import org.coldis.library.persistence.configuration.DatasourceUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity owned by the SECONDARY persistence unit. It lives under the same base packages as the
 * primary entities (like a real consumer's entities) — the {@link DatasourceUnit} annotation alone
 * binds it to the secondary unit, so only the secondary EntityManagerFactory maps it.
 */
@Entity
@DatasourceUnit(value = "secondary")
@Table(name = "test_secondary_entity")
public class TestSecondaryEntity {

	/** Id. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Name. */
	private String name;

	public Long getId() {
		return this.id;
	}

	public void setId(
			final Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(
			final String name) {
		this.name = name;
	}

}

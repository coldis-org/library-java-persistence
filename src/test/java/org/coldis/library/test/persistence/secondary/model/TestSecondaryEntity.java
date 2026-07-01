package org.coldis.library.test.persistence.secondary.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity owned by the SECONDARY persistence unit. It lives under {@code org.coldis} (like a real
 * consumer's entities), so both the primary and the secondary EntityManagerFactory map it — the test
 * proves rows written through the secondary unit land only in the secondary database.
 */
@Entity
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

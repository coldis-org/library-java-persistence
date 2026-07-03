package org.coldis.library.test.persistence.tertiary.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity owned by the {@code tertiary} persistence unit — a second, independent secondary datasource,
 * exercising N &gt; 1.
 */
@Entity
@Table(name = "test_tertiary_entity")
public class TestTertiaryEntity {

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

package org.coldis.library.test.persistence.tertiary.model;

import org.coldis.library.persistence.configuration.DatasourceUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity owned by the {@code tertiary} persistence unit (bound by its {@link DatasourceUnit}
 * annotation) — a second, independent secondary datasource, exercising N &gt; 1.
 */
@Entity
@DatasourceUnit(value = "tertiary")
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

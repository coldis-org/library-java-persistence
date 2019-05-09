package org.coldis.library.test.persistence.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.coldis.library.model.IdentifiedObject;
import org.coldis.library.persistence.model.AbstractTimestampedExpirableEntity;

/**
 * Test entity.
 */
@Entity
public class TestEntity extends AbstractTimestampedExpirableEntity implements IdentifiedObject {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = -1515363774709328294L;

	/**
	 * Identifier.
	 */
	private Long id;

	/**
	 * Gets the id.
	 *
	 * @return The id.
	 */
	@Id
	@Override
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TestEntitySequence")
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id New id.
	 */
	public void setId(final Long id) {
		this.id = id;
	}

}

package org.coldis.library.test.persistence.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.coldis.library.model.IdentifiedObject;
import org.coldis.library.persistence.model.AbstractExpirableEntity;

/**
 * Test entity.
 */
@Entity
public class TestEntity extends AbstractExpirableEntity implements IdentifiedObject {

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
	@SequenceGenerator(name = "test_entity_id")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "test_entity_id")
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

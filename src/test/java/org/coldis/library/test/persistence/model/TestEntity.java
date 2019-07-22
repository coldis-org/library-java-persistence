package org.coldis.library.test.persistence.model;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.coldis.library.model.IdentifiedObject;
import org.coldis.library.model.ModelView;
import org.coldis.library.persistence.converter.TypedObjectJsonConverter;
import org.coldis.library.persistence.model.AbstractTimestampedExpirableEntity;

import com.fasterxml.jackson.annotation.JsonView;

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
	 * Test attribute.
	 */
	private TestObject attribute1;

	/**
	 * Test attribute.
	 */
	private TestObject attribute2;

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

	/**
	 * Gets the attribute1.
	 *
	 * @return The attribute1.
	 */
	@Column(columnDefinition = "JSON")
	@Convert(converter = TypedObjectJsonConverter.class)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public TestObject getAttribute1() {
		return this.attribute1;
	}

	/**
	 * Sets the attribute1.
	 *
	 * @param attribute1 New attribute1.
	 */
	public void setAttribute1(final TestObject attribute1) {
		this.attribute1 = attribute1;
	}

	/**
	 * Gets the attribute2.
	 *
	 * @return The attribute2.
	 */
	@Column(columnDefinition = "JSON")
	@Convert(converter = TypedObjectJsonConverter.class)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public TestObject getAttribute2() {
		return this.attribute2;
	}

	/**
	 * Sets the attribute2.
	 *
	 * @param attribute2 New attribute2.
	 */
	public void setAttribute2(final TestObject attribute2) {
		this.attribute2 = attribute2;
	}

}

package org.coldis.library.test.persistence.model;

import java.util.List;

import org.coldis.library.model.Identifiable;
import org.coldis.library.model.view.ModelView;
import org.coldis.library.persistence.converter.TypableJsonConverter;
import org.coldis.library.persistence.converter.TypableListJsonConverter;
import org.coldis.library.persistence.model.AbstractTimestampableExpirableEntity;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Test entity.
 */
@Entity
public class TestEntity extends AbstractTimestampableExpirableEntity implements Identifiable {

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
	 * Test attribute.
	 */
	private List<TestObject> attribute3;

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
	@Convert(converter = TypableJsonConverter.class)
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
	@Convert(converter = TypableJsonConverter.class)
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

	/**
	 * Gets the attribute3.
	 *
	 * @return The attribute3.
	 */
	@Column(columnDefinition = "JSON")
	@Convert(converter = TypableListJsonConverter.class)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public List<TestObject> getAttribute3() {
		return this.attribute3;
	}

	/**
	 * Sets the attribute3.
	 *
	 * @param attribute3 New attribute3.
	 */
	public void setAttribute3(final List<TestObject> attribute3) {
		this.attribute3 = attribute3;
	}

}

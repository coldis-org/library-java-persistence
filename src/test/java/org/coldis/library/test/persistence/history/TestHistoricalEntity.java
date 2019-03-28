package org.coldis.library.test.persistence.history;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.coldis.library.model.IdentifiedObject;
import org.coldis.library.model.TypedObject;
import org.coldis.library.persistence.history.EntityHistoryListener;
import org.coldis.library.persistence.history.HistoricalEntity;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Test entity.
 */
@Entity
@JsonTypeName(value = TestHistoricalEntity.TYPE_NAME)
@EntityListeners(EntityHistoryListener.class)
@HistoricalEntity(basePackageName = "org.coldis.library.test.persistence.history.history")
public class TestHistoricalEntity implements TypedObject, IdentifiedObject {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = 6666428256737486698L;

	/**
	 * Type name.
	 */
	public static final String TYPE_NAME = "org.coldis.library.test.persistence.history.TestHistoricalEntity";

	/**
	 * Object identifier.
	 */
	private Long id;

	/**
	 * Test attribute.
	 */
	private String test;

	/**
	 * Test constructor.
	 */
	public TestHistoricalEntity() {
	}

	/**
	 * Test constructor.
	 *
	 * @param test Test.
	 */
	public TestHistoricalEntity(final String test) {
		super();
		this.test = test;
	}

	/**
	 * @see org.coldis.library.model.IdentifiedObject#getId()
	 */
	@Id
	@Override
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the identifier.
	 *
	 * @param id New identifier.
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Gets the test.
	 *
	 * @return The test.
	 */
	public String getTest() {
		return this.test;
	}

	/**
	 * Sets the test.
	 *
	 * @param test New test.
	 */
	public void setTest(final String test) {
		this.test = test;
	}

	/**
	 * @see org.coldis.library.model.TypedObject#getTypeName()
	 */
	@Override
	public String getTypeName() {
		return TestHistoricalEntity.TYPE_NAME;
	}

}

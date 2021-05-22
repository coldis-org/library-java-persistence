package org.coldis.library.test.persistence.validation;

import java.util.List;

import org.coldis.library.model.Identifiable;
import org.coldis.library.model.view.ModelView;
import org.coldis.library.persistence.model.AbstractTimestampableExpirableEntity;
import org.coldis.library.persistence.validation.RequiredAttributes;

/**
 * Test entity.
 */
@RequiredAttributes(supplierMethod = "getRequiredAttributes")
@RequiredAttributes(
		supplierMethod = "getRequiredAttributes2",
		groups = ModelView.Sensitive.class
)
public class TestObject extends AbstractTimestampableExpirableEntity implements Identifiable {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = -1515363774709328294L;

	/**
	 * Required attributes.
	 */
	public static List<String> REQUIRED_ATTRIBUTES = List.of();

	/**
	 * Identifier.
	 */
	private Long id;

	/**
	 * Test attribute.
	 */
	private String attribute1;

	/**
	 * Test attribute.
	 */
	private List<TestObject> attribute2;

	/**
	 * Gets the id.
	 *
	 * @return The id.
	 */
	@Override
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id New id.
	 */
	public void setId(
			final Long id) {
		this.id = id;
	}

	/**
	 * Gets the attribute1.
	 *
	 * @return The attribute1.
	 */
	public String getAttribute1() {
		return this.attribute1;
	}

	/**
	 * Sets the attribute1.
	 *
	 * @param attribute1 New attribute1.
	 */
	public void setAttribute1(
			final String attribute1) {
		this.attribute1 = attribute1;
	}

	/**
	 * Gets the attribute2.
	 *
	 * @return The attribute2.
	 */
	public List<TestObject> getAttribute2() {
		return this.attribute2;
	}

	/**
	 * Sets the attribute2.
	 *
	 * @param attribute2 New attribute2.
	 */
	public void setAttribute2(
			final List<TestObject> attribute2) {
		this.attribute2 = attribute2;
	}

	/**
	 * Gets the required attributes.
	 *
	 * @return The required attributes.
	 */
	public static List<String> getRequiredAttributes() {
		return TestObject.REQUIRED_ATTRIBUTES;
	}

	/**
	 * Gets the required attributes.
	 *
	 * @return The required attributes.
	 */
	public static List<String> getRequiredAttributes2() {
		return TestObject.REQUIRED_ATTRIBUTES;
	}

}

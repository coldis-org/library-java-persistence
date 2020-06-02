package org.coldis.library.test.persistence.keyvalue;

import java.util.Objects;

import org.coldis.library.model.Typable;
import org.coldis.library.model.view.ModelView;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Test value.
 */
@JsonTypeName(value = TestValue.TYPE_NAME)
public class TestValue implements Typable {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = -7756044386413062149L;

	/**
	 * Type name.
	 */
	protected static final String TYPE_NAME = "org.coldis.library.test.persistence.keyvalue.TestValue";

	/**
	 * Test attribute.
	 */
	private String attribute1;

	/**
	 * Test attribute.
	 */
	private Long attribute2;

	/**
	 * No arguments constructor.
	 */
	public TestValue() {
		super();
	}

	/**
	 * Default constructor.
	 *
	 * @param attribute1 Test attribute.
	 * @param attribute2 Test attribute.
	 */
	public TestValue(final String attribute1, final Long attribute2) {
		super();
		this.attribute1 = attribute1;
		this.attribute2 = attribute2;
	}

	/**
	 * Gets the attribute1.
	 *
	 * @return The attribute1.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public String getAttribute1() {
		return this.attribute1;
	}

	/**
	 * Sets the attribute1.
	 *
	 * @param attribute1 New attribute1.
	 */
	public void setAttribute1(final String attribute1) {
		this.attribute1 = attribute1;
	}

	/**
	 * Gets the attribute2.
	 *
	 * @return The attribute2.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public Long getAttribute2() {
		return this.attribute2;
	}

	/**
	 * Sets the attribute2.
	 *
	 * @param attribute2 New attribute2.
	 */
	public void setAttribute2(final Long attribute2) {
		this.attribute2 = attribute2;
	}

	/**
	 * @see org.coldis.library.model.Typable#getTypeName()
	 */
	@Override
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public String getTypeName() {
		return TestValue.TYPE_NAME;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.attribute1, this.attribute2);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TestValue)) {
			return false;
		}
		final TestValue other = (TestValue) obj;
		return Objects.equals(this.attribute1, other.attribute1) && Objects.equals(this.attribute2, other.attribute2);
	}

}

package org.coldis.library.test.persistence.model;

import org.coldis.library.model.Typable;
import org.coldis.library.model.view.ModelView;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Test object.
 */
@JsonTypeName(TestObject.TYPE_NAME)
public class TestObject implements Typable {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = -847374221397760551L;

	/**
	 * Type name.
	 */
	protected static final String TYPE_NAME = "TestObject";

	/**
	 * Test attribute.
	 */
	private String attribute1;

	/**
	 * Test attribute.
	 */
	private String attribute2;

	/**
	 * Test attribute.
	 */
	private TestObject attribute3;

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
	@JsonView({ ModelView.Public.class })
	public void setAttribute1(final String attribute1) {
		this.attribute1 = attribute1;
	}

	/**
	 * Gets the attribute2.
	 *
	 * @return The attribute2.
	 */
	public String getAttribute2() {
		return this.attribute2;
	}

	/**
	 * Sets the attribute2.
	 *
	 * @param attribute2 New attribute2.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public void setAttribute2(final String attribute2) {
		this.attribute2 = attribute2;
	}

	/**
	 * Gets the attribute3.
	 *
	 * @return The attribute3.
	 */
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public TestObject getAttribute3() {
		return this.attribute3;
	}

	/**
	 * Sets the attribute3.
	 *
	 * @param attribute3 New attribute3.
	 */
	public void setAttribute3(final TestObject attribute3) {
		this.attribute3 = attribute3;
	}

	/**
	 * @see org.coldis.library.model.Typable#getTypeName()
	 */
	@Override
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public String getTypeName() {
		return TestObject.TYPE_NAME;
	}

}

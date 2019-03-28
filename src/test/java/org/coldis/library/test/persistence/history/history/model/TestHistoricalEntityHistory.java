package org.coldis.library.test.persistence.history.history.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.coldis.library.persistence.history.EntityHistory;
import org.coldis.library.persistence.model.AbstractTimestampedEntity;
import org.coldis.library.test.persistence.history.TestHistoricalEntity;
import org.hibernate.annotations.Type;

/**
 * JPA entity history for
 * {@link org.coldis.library.test.persistence.history.TestHistoricalEntity}.
 */
@Entity
@Table(indexes = { @Index(columnList = "updatedAt") })
public class TestHistoricalEntityHistory extends AbstractTimestampedEntity
implements EntityHistory<TestHistoricalEntity> {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = -1003507974L;

	/**
	 * Object identifier.
	 */
	private Long id;

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
	 * Entity state.
	 */
	private TestHistoricalEntity state;

	/**
	 * @see org.coldis.library.persistence.history.EntityHistory#getState()
	 */
	@Override
	@Type(type = "JSONB")
	@Column(columnDefinition = "JSONB")
	public TestHistoricalEntity getState() {
		return this.state;
	}

	/**
	 * Sets the entity state.
	 *
	 * @param state New entity state.
	 */
	protected void setState(final TestHistoricalEntity state) {
		this.state = state;
	}

	/**
	 * No arguments constructor.
	 */
	public TestHistoricalEntityHistory() {
	}

	/**
	 * Entity state constructor.
	 *
	 * @param state New entity state.
	 */
	public TestHistoricalEntityHistory(final TestHistoricalEntity state) {
		super();
		this.state = state;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.state);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TestHistoricalEntityHistory)) {
			return false;
		}
		final TestHistoricalEntityHistory other = (TestHistoricalEntityHistory) obj;
		return Objects.equals(this.id, other.id) && Objects.equals(this.state, other.state);
	}

}

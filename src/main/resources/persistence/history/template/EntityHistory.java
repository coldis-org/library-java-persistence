package ${historicalEntity.getEntityPackageName()};

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.coldis.library.persistence.model.AbstractTimestampedEntity;
import org.coldis.library.test.persistence.history.history.model.TestHistoricalEntityHistory;
import org.coldis.library.persistence.history.EntityHistory;

import ${historicalEntity.getOriginalEntityQualifiedTypeName()};

/**
 * JPA entity history for {@link ${historicalEntity.getOriginalEntityQualifiedTypeName()}}.
 */
@Entity
@Table(indexes = { @Index(columnList = "updatedAt") })
public class ${historicalEntity.getEntityTypeName()} extends AbstractTimestampedEntity
		implements EntityHistory<${historicalEntity.getOriginalEntityTypeName()}> {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = ${historicalEntity.getEntityQualifiedTypeName().hashCode()}L;
	
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
		return id;
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
	private ${historicalEntity.getOriginalEntityTypeName()} state;

	/**
	 * @see org.coldis.library.persistence.history.EntityHistory#getState()
	 */
	@Column(columnDefinition = "${historicalEntity.getStateColumnDefinition()}")
	@Convert(converter = ${historicalEntity.getStateAttributeConverter()}.class)
	public ${historicalEntity.getOriginalEntityTypeName()} getState() {
		return state;
	}

	/**
	 * Sets the entity state.
	 *
	 * @param state
	 *            New entity state.
	 */
	protected void setState(final ${historicalEntity.getOriginalEntityTypeName()} state) {
		this.state = state;
	}

	/**
	 * No arguments constructor.
	 */
	public ${historicalEntity.getEntityTypeName()}() {
	}

	/**
	 * Entity state constructor.
	 *
	 * @param state New entity state.
	 */
	public ${historicalEntity.getEntityTypeName()}(final ${historicalEntity.getOriginalEntityTypeName()} state) {
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
		if (!(obj instanceof ${historicalEntity.getEntityTypeName()})) {
			return false;
		}
		final ${historicalEntity.getEntityTypeName()} other = (${historicalEntity.getEntityTypeName()}) obj;
		return Objects.equals(this.id, other.id) && Objects.equals(this.state, other.state);
	}

}

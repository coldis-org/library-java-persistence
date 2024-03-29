package ${historicalEntity.getEntityPackageName()};

import java.util.Map;
import java.util.Objects;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.coldis.library.persistence.model.AbstractTimestampableEntity;
import org.coldis.library.helper.DateTimeHelper;
import org.coldis.library.persistence.converter.MapJsonConverter;
import org.coldis.library.persistence.history.EntityHistory;

/**
 * JPA entity history for {@link ${historicalEntity.getOriginalEntityQualifiedTypeName()}}.
 */
@Entity
@Table(indexes = { @Index(columnList = "updatedAt") })
public class ${historicalEntity.getEntityTypeName()} extends AbstractTimestampableEntity
		implements EntityHistory<Map<String, Object>> {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = ${historicalEntity.getEntityQualifiedTypeName().hashCode()}L;
	
	/**
	 * Object identifier.
	 */
	private Long id;
	
	/**
	 * Entity state.
	 */
	private Map<String, Object> state;
	
	/**
	 * User.
	 */
	private String user;
	
	/**
	 * No arguments constructor.
	 */
	public ${historicalEntity.getEntityTypeName()}() {
	}

	/**
	 * Entity state constructor.
	 *
	 * @param state New entity state.
	 * @param createdAt When entity was created.
	 */
	public ${historicalEntity.getEntityTypeName()}(final Map<String, Object> state, final LocalDateTime createdAt) {
		super();
		this.state = state;
		setCreatedAt(createdAt);
	}

	/**
	 * @see org.coldis.library.model.Identifiable${h}getId()
	 */
	@Id
	@Override
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "${historicalEntity.getSequenceName()}")
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
	 * @see org.coldis.library.persistence.history.EntityHistory${h}getState()
	 */
	@Convert(converter = MapJsonConverter.class)
	@Column(columnDefinition = "${historicalEntity.getStateColumnDefinition()}")
	public Map<String, Object> getState() {
		return state;
	}

	/**
	 * Sets the entity state.
	 *
	 * @param state
	 *            New entity state.
	 */
	protected void setState(final Map<String, Object> state) {
		this.state = state;
	}
	
	/**
	 * @see org.coldis.library.persistence.history.EntityHistory${h}getState()
	 */
	@Column(name = "u5er")
	public String getUser() {
		return user;
	}

	/**
	 * Sets the user.
	 *
	 * @param state
	 *            New user.
	 */
	public void setUser(String user) {
		this.user = user;
	}
	
	/**
	 * @see java.lang.Object${h}hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.state, this.user);
	}

	/**
	 * @see java.lang.Object${h}equals(java.lang.Object)
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
		return Objects.equals(this.id, other.id) && Objects.equals(this.state, other.state) && Objects.equals(this.user, other.user);
	}

}

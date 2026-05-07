package org.coldis.library.persistence.model;

import java.time.LocalDateTime;

import org.coldis.library.dto.DtoAttribute;
import org.coldis.library.dto.DtoType;
import org.coldis.library.model.AbstractTimestampable;
import org.coldis.library.model.view.ModelView;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * Abstract time-stamped JPA entity.
 *
 * <p>Declares {@link AbstractTimestampable} as its DTO via {@link DtoType#dtoClass()}: generated
 * DTOs of subclasses {@code extends AbstractTimestampable} so the {@code createdAt} /
 * {@code updatedAt} fields share a declaring class on both peers. Required for Fory cross-class
 * deserialization to populate the timestamp fields (Fory keys field descriptors by the
 * declaring-class FQN, so the DTO and Model sides must agree on which class declares each field).
 */
@MappedSuperclass
@EntityListeners(value = EntityTimestampListener.class)
@DtoType(namespace = "", dtoClass = AbstractTimestampable.class)
public abstract class AbstractTimestampableEntity extends AbstractTimestampable {

	/**
	 * Serial
	 */
	private static final long serialVersionUID = -4206189779959504800L;

	/**
	 * @see org.coldis.library.model.Timestampable#getCreatedAt()
	 */
	@Override
	@DtoAttribute(readOnly = true, usedInComparison = false)
	@Column(columnDefinition = "TIMESTAMPTZ", nullable = false)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getCreatedAt() {
		return super.getCreatedAt();
	}

	/**
	 * @see org.coldis.library.model.Timestampable#getUpdatedAt()
	 */
	@Override
	@DtoAttribute(readOnly = true, usedInComparison = false)
	@Column(columnDefinition = "TIMESTAMPTZ", nullable = false)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public LocalDateTime getUpdatedAt() {
		return super.getUpdatedAt();
	}

}

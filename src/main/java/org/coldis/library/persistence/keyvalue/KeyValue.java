package org.coldis.library.persistence.keyvalue;

import java.io.Serializable;
import java.util.Objects;

import org.coldis.library.dto.DtoAttribute;
import org.coldis.library.model.Typable;
import org.coldis.library.model.view.ModelView;
import org.coldis.library.persistence.converter.TypableJsonConverter;
import org.coldis.library.persistence.model.AbstractTimestampableEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

/**
 * Key value.
 *
 * @param <ValueType> Value type.
 */
@Entity
@ConditionalOnProperty(
		name = "org.coldis.configuration.persistence-keyvalue-enabled",
		havingValue = "true",
		matchIfMissing = true
)
public class KeyValue<ValueType extends Typable> extends AbstractTimestampableEntity {

	/**
	 * Serial.
	 */
	private static final long serialVersionUID = -4687758626282277950L;

	/**
	 * Key.
	 */
	private String key;

	/**
	 * Value.
	 */
	private Serializable internalValue;

	/**
	 * No arguments constructor.
	 */
	public KeyValue() {
	}

	/**
	 * Default constructor.
	 *
	 * @param key   Key.
	 * @param value Value.
	 */
	public KeyValue(final String key, final ValueType value) {
		super();
		this.key = key;
		this.internalValue = value;
	}

	/**
	 * Gets the key.
	 *
	 * @return The key.
	 */
	@Id
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	public String getKey() {
		return this.key;
	}

	/**
	 * Sets the key.
	 *
	 * @param key New key.
	 */
	public void setKey(
			final String key) {
		this.key = key;
	}

	/**
	 * Gets the value.
	 *
	 * @return The value.
	 */
	@JsonIgnore
	@DtoAttribute(ignore = true)
	@Column(columnDefinition = "JSONB")
	@Convert(converter = TypableJsonConverter.class)
	@JsonView({ ModelView.Persistent.class, ModelView.Public.class })
	protected Serializable getInternalValue() {
		return this.internalValue;
	}

	/**
	 * Sets the value.
	 *
	 * @param value New value.
	 */
	protected void setInternalValue(
			final ValueType value) {
		this.internalValue = value;
	}

	/**
	 * Gets the value.
	 *
	 * @return The value.
	 */
	@Transient
	@SuppressWarnings("unchecked")
	public ValueType getValue() {
		return ((ValueType) this.getInternalValue());
	}

	/**
	 * Sets the value.
	 *
	 * @param value New value.
	 */
	public void setValue(
			final ValueType value) {
		this.setInternalValue(value);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.internalValue, this.key);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(
			final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof KeyValue)) {
			return false;
		}
		final KeyValue other = (KeyValue) obj;
		return Objects.equals(this.internalValue, other.internalValue) && Objects.equals(this.key, other.key);
	}

}

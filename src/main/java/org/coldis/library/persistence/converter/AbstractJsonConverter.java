package org.coldis.library.persistence.converter;

import javax.persistence.AttributeConverter;

import org.coldis.library.model.ModelView;
import org.coldis.library.serialization.json.JsonHelper;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract JPA converter to JSON object (String).
 *
 * @param <ObjectType> Any type.
 */
public abstract class AbstractJsonConverter<ObjectType> implements AttributeConverter<ObjectType, String> {

	/**
	 * Serialization view to be used.
	 */
	private static final Class<?> SERIALIZATION_VIEW = ModelView.Persistent.class;

	/**
	 * Object mapper.
	 */
	@Autowired
	protected ObjectMapper objectMapper;

	/**
	 * @see javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.Object)
	 */
	@Override
	public String convertToDatabaseColumn(final ObjectType originalObject) {
		// Returns the JSON object.
		return JsonHelper.serialize(this.objectMapper, originalObject, AbstractJsonConverter.SERIALIZATION_VIEW, false);
	}

	/**
	 * Converts the JSON object to the entity type.
	 *
	 * @param  jsonMapper Object mapper to be used.
	 * @param  jsonObject JSON object.
	 * @return            Converted JSON object.
	 */
	protected abstract ObjectType convertToEntityAttribute(final ObjectMapper jsonMapper, final String jsonObject);

	/**
	 * @see javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang.Object)
	 */
	@Override
	public ObjectType convertToEntityAttribute(final String jsonObject) {
		return jsonObject == null ? null : this.convertToEntityAttribute(this.objectMapper, jsonObject);
	}

}

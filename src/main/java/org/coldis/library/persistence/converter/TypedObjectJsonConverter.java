package org.coldis.library.persistence.converter;

import javax.persistence.Converter;

import org.coldis.library.model.TypedObject;
import org.coldis.library.serialization.ObjectMapperHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Type object from/to JSON converter.
 */
@Converter(autoApply = true)
public class TypedObjectJsonConverter extends AbstractJsonConverter<TypedObject> {

	/**
	 * Converts the JSON object to the entity type.
	 *
	 * @param  jsonMapper             Object mapper to be used.
	 * @param  jsonObject             JSON object.
	 * @param  objectTypeAttribute    TODO
	 * @return                        Converted JSON object.
	 * @throws ClassNotFoundException If the object type cannot be found.
	 */
	protected TypedObject convertToEntityAttribute(final ObjectMapper jsonMapper, final String jsonObject,
			final String objectTypeAttribute) {
		return ObjectMapperHelper.deserialize(jsonMapper, jsonObject, new TypeReference<TypedObject>() {
		}, false);
	}

	/**
	 * @see org.coldis.library.persistence.converter.AbstractJsonConverter#convertToEntityAttribute(com.fasterxml.jackson.databind.ObjectMapper,
	 *      java.lang.String)
	 */
	@Override
	protected TypedObject convertToEntityAttribute(final ObjectMapper jsonMapper, final String jsonObject) {
		return this.convertToEntityAttribute(jsonMapper, jsonObject, "typeName");
	}

}

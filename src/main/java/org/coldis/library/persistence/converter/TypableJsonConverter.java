package org.coldis.library.persistence.converter;

import org.coldis.library.model.Typable;
import org.coldis.library.serialization.ObjectMapperHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Converter;

/**
 * Type object from/to JSON converter.
 */
@Converter(autoApply = true)
public class TypableJsonConverter extends AbstractJsonConverter<Typable> {

	/**
	 * Converts the JSON object to the entity type.
	 *
	 * @param  jsonMapper             Object mapper to be used.
	 * @param  jsonObject             JSON object.
	 * @return                        Converted JSON object.
	 * @throws ClassNotFoundException If the object type cannot be found.
	 */
	@Override
	protected Typable convertToEntityAttribute(final ObjectMapper jsonMapper, final String jsonObject) {
		return ObjectMapperHelper.deserialize(jsonMapper, jsonObject, new TypeReference<Typable>() {
		}, false);
	}

}

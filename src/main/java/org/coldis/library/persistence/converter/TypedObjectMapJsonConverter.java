package org.coldis.library.persistence.converter;

import java.util.Map;

import javax.persistence.Converter;

import org.coldis.library.model.TypedObject;
import org.coldis.library.serialization.ObjectMapperHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Map from/to JSON converter.
 */
@Converter(autoApply = true)
public class TypedObjectMapJsonConverter extends AbstractJsonConverter<Map<String, TypedObject>> {

	/**
	 * @see org.coldis.library.persistence.converter.AbstractJsonConverter#convertToEntityAttribute(com.fasterxml.jackson.databind.ObjectMapper,
	 *      java.lang.String)
	 */
	@Override
	protected Map<String, TypedObject> convertToEntityAttribute(final ObjectMapper jsonMapper,
			final String jsonObject) {
		return ObjectMapperHelper.deserialize(jsonMapper, jsonObject, new TypeReference<Map<String, TypedObject>>() {
		}, false);
	}

}

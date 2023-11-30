package org.coldis.library.persistence.converter;

import java.util.Set;

import org.coldis.library.serialization.ObjectMapperHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Converter;

/**
 * Set from/to JSON converter.
 */
@Converter(autoApply = true)
public class SetJsonConverter extends AbstractJsonConverter<Set<Object>> {

	/**
	 * @see org.coldis.library.persistence.converter.AbstractJsonConverter#convertToEntityAttribute(com.fasterxml.jackson.databind.ObjectMapper,
	 *      java.lang.String)
	 */
	@Override
	protected Set<Object> convertToEntityAttribute(final ObjectMapper jsonMapper, final String jsonObject) {
		return ObjectMapperHelper.deserialize(jsonMapper, jsonObject, new TypeReference<Set<Object>>() {
		}, false);
	}

}

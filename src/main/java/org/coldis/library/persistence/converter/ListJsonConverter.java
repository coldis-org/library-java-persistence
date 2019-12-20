package org.coldis.library.persistence.converter;

import java.util.List;

import javax.persistence.Converter;

import org.coldis.library.serialization.ObjectMapperHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * List from/to JSON converter.
 */
@Converter(autoApply = true)
public class ListJsonConverter extends AbstractJsonConverter<List<Object>> {

	/**
	 * @see org.coldis.library.persistence.converter.AbstractJsonConverter#convertToEntityAttribute(com.fasterxml.jackson.databind.ObjectMapper,
	 *      java.lang.String)
	 */
	@Override
	protected List<Object> convertToEntityAttribute(final ObjectMapper jsonMapper, final String jsonObject) {
		return ObjectMapperHelper.deserialize(jsonMapper, jsonObject, new TypeReference<List<Object>>() {
		}, false);
	}

}

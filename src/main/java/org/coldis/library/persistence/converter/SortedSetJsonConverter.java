package org.coldis.library.persistence.converter;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Converter;

import org.coldis.library.serialization.ObjectMapperHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Sorted set from/to JSON converter.
 */
@Converter(autoApply = true)
public class SortedSetJsonConverter extends AbstractJsonConverter<SortedSet<Object>> {

	/**
	 * @see org.coldis.library.persistence.converter.AbstractJsonConverter#convertToEntityAttribute(com.fasterxml.jackson.databind.ObjectMapper,
	 *      java.lang.String)
	 */
	@Override
	protected SortedSet<Object> convertToEntityAttribute(final ObjectMapper jsonMapper, final String jsonObject) {
		return ObjectMapperHelper.deserialize(jsonMapper, jsonObject, new TypeReference<TreeSet<Object>>() {
		}, false);
	}

}

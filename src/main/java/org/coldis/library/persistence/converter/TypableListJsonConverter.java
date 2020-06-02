package org.coldis.library.persistence.converter;

import java.util.List;

import javax.persistence.Converter;

import org.coldis.library.model.Typable;
import org.coldis.library.serialization.ObjectMapperHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * List from/to JSON converter.
 */
@Converter(autoApply = true)
public class TypableListJsonConverter extends AbstractJsonConverter<List<Typable>> {

	/**
	 * @see org.coldis.library.persistence.converter.AbstractJsonConverter#convertToEntityAttribute(com.fasterxml.jackson.databind.ObjectMapper,
	 *      java.lang.String)
	 */
	@Override
	protected List<Typable> convertToEntityAttribute(final ObjectMapper jsonMapper, final String jsonObject) {
		return ObjectMapperHelper.deserialize(jsonMapper, jsonObject, new TypeReference<List<Typable>>() {
		}, false);
	}

}

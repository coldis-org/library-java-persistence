package org.coldis.library.persistence.converter;

import java.util.Map;

import javax.persistence.Converter;

import org.apache.commons.lang3.ClassUtils;
import org.coldis.library.exception.IntegrationException;
import org.coldis.library.model.SimpleMessage;
import org.coldis.library.model.TypedObject;
import org.coldis.library.serialization.ObjectMapperHelper;

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
		// Converts the JSON into a map.
		final Map<?, ?> mapObject = ObjectMapperHelper.deserialize(jsonMapper, jsonObject, Map.class, false);
		// Returns the object (by converting the map into the object type).
		try {
			return (TypedObject) ObjectMapperHelper.convert(jsonMapper, mapObject,
					ClassUtils.getClass((String) mapObject.get(objectTypeAttribute)), false);
		}
		// // If the object type cannot be found.
		catch (final ClassNotFoundException exception) {
			// Throws a type not found exception.
			throw new IntegrationException(new SimpleMessage("converter.type.notfound"));
		}
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

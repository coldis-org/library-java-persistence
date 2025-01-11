package org.coldis.library.persistence.converter;

import org.coldis.library.model.view.ModelView;
import org.coldis.library.persistence.configuration.JpaAutoConfiguration;
import org.coldis.library.serialization.ObjectMapperHelper;
import org.springframework.core.annotation.AnnotationUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;

/**
 * Abstract JPA converter to JSON object (String).
 *
 * @param <ObjectType> Any type.
 */
public abstract class AbstractJsonConverter<ObjectType> implements AttributeConverter<ObjectType, String> {

	/**
	 * Serialization view to be used.
	 */
	private static final Class<?> DEFAULT_SERIALIZATION_VIEW = ModelView.PersistentAndSensitive.class;

	/**
	 * Returns the object mapper.
	 *
	 * @return The object mapper.
	 */
	protected ObjectMapper getObjectMapper() {
		return (JpaAutoConfiguration.OBJECT_MAPPER == null ? ObjectMapperHelper.createMapper() : JpaAutoConfiguration.OBJECT_MAPPER);
	}

	/**
	 * @see jakarta.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.Object)
	 */
	@Override
	public String convertToDatabaseColumn(
			final ObjectType originalObject) {
		String serializedObject = null;
		if (originalObject != null) {
			// Gets the serialization view.
			final PersistenceView persistenceViewAnnotation = AnnotationUtils.findAnnotation(originalObject.getClass(), PersistenceView.class);
			final Class<?> persistenceView = (persistenceViewAnnotation == null ? AbstractJsonConverter.DEFAULT_SERIALIZATION_VIEW
					: persistenceViewAnnotation.value());
			serializedObject = ObjectMapperHelper.serialize(this.getObjectMapper(), originalObject, persistenceView, false);
		}
		return serializedObject;
	}

	/**
	 * Converts the JSON object to the entity type.
	 *
	 * @param  jsonMapper Object mapper to be used.
	 * @param  jsonObject JSON object.
	 * @return            Converted JSON object.
	 */
	protected abstract ObjectType convertToEntityAttribute(
			final ObjectMapper jsonMapper,
			final String jsonObject);

	/**
	 * @see jakarta.persistence.AttributeConverter#convertToEntityAttribute(java.lang.Object)
	 */
	@Override
	public ObjectType convertToEntityAttribute(
			final String jsonObject) {
		return jsonObject == null ? null : this.convertToEntityAttribute(this.getObjectMapper(), jsonObject);
	}

}

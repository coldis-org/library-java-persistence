package org.coldis.library.persistence.validation;

import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.coldis.library.helper.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Required attributes validator.
 */
public class RequiredAttributesValidator implements ConstraintValidator<RequiredAttributes, Object> {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(RequiredAttributesValidator.class);

	/**
	 * Valid date.
	 */
	private RequiredAttributes requiredAttributes;

	/**
	 * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
	 */
	@Override
	public void initialize(
			final RequiredAttributes requiredAttributes) {
		this.requiredAttributes = requiredAttributes;
	}

	/**
	 * @see javax.validation.ConstraintValidator#isValid(java.lang.Object,
	 *      javax.validation.ConstraintValidatorContext)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean isValid(
			final Object value,
			final ConstraintValidatorContext context) {
		// The value is valid by default.
		Boolean valid = true;
		// Tries to validate the object.
		try {
			// For each required attribute.
			for (final String requiredAttribute : ((Collection<String>) value.getClass().getMethod(this.requiredAttributes.supplierMethod()).invoke(value))) {
				// Gets the attribute value.
				final Object requiredAttributeValue = ReflectionHelper.getAttribute(value, requiredAttribute);
				// If the attribute value is null.
				if (requiredAttributeValue == null) {
					// If the attribute does not pass the null validation.
					if (this.requiredAttributes.notNull()) {
						// The object is not valid.
						valid = false;
						break;
					}
				}
				// If the object is not null.
				else {
					// If the attribute does not pass the not false validation.
					if (this.requiredAttributes.notFalse() && (requiredAttributeValue instanceof Boolean) && !((Boolean) requiredAttributeValue)) {
						// The object is not valid.
						valid = false;
						break;
					}
					// If the attribute does not pass the empty string validation.
					if (this.requiredAttributes.notEmptyString() && (requiredAttributeValue instanceof String)
							&& StringUtils.isEmpty((String) requiredAttributeValue)) {
						// The object is not valid.
						valid = false;
						break;
					}
					// If the attribute does not pass the empty string validation.
					if (this.requiredAttributes.notEmptyCollection() && (requiredAttributeValue instanceof Collection)
							&& CollectionUtils.isEmpty((Collection<?>) requiredAttributeValue)) {
						// The object is not valid.
						valid = false;
						break;
					}
				}
			}
		}
		// If the object cannot be validated.
		catch (final Exception exception) {
			// Logs it.
			RequiredAttributesValidator.LOGGER.error("Object could not be validated: " + exception.getLocalizedMessage());
			RequiredAttributesValidator.LOGGER.debug("Object could not be validated." + exception);
		}
		// Returns if the object is valid.
		return valid;
	}

}

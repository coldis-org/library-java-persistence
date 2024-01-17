package org.coldis.library.persistence.validation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

import org.apache.commons.lang3.StringUtils;
import org.coldis.library.helper.DateTimeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Date validation.
 */
public class DateValidator implements ConstraintValidator<ValidDate, Object> {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(DateValidator.class);

	/**
	 * Valid date.
	 */
	private ValidDate validDate;

	/**
	 * @see jakarta.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
	 */
	@Override
	public void initialize(final ValidDate validDate) {
		this.validDate = validDate;
	}

	/**
	 * Parses a date.
	 *
	 * @param  date The date.
	 * @return      The parsed date.
	 */
	private LocalDateTime parseDate(final String date) {
		// The parsed date.
		LocalDateTime parsedDate = null;
		// If the date is given.
		if (StringUtils.isNotEmpty(date)) {
			// Tries t parse the date.
			try {
				parsedDate = LocalDateTime.parse(date, DateTimeHelper.DATE_TIME_FORMATTER);
			}
			// If the date cannot be parsed.
			catch (final Exception exception) {
				// Ignores it.
			}
		}
		// Returns the parsed date.
		return parsedDate;
	}

	/**
	 * @see jakarta.validation.ConstraintValidator#isValid(java.lang.Object,
	 *      jakarta.validation.ConstraintValidatorContext)
	 */
	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {
		// The value is valid by default.
		Boolean valid = true;
		// Tries checking if the object is valid.
		try {
			// If it is a temporal object
			if (valid && (value instanceof Temporal)) {
				final Temporal temporalValue = (Temporal) value;
				// If the minimum date is defined.
				final Temporal minimumDate = this.parseDate(this.validDate.minimumDate());
				if (valid && (minimumDate != null)) {
					// The object is valid if it is after the minimum date.
					valid = (minimumDate.until(temporalValue, ChronoUnit.SECONDS) >= 0L);
				}
				// If the maximum date is defined.
				final Temporal maximumDate = this.parseDate(this.validDate.maximumDate());
				if (valid && (maximumDate != null)) {
					// The object is valid if it is after the maximum date.
					valid = (maximumDate.until(temporalValue, ChronoUnit.SECONDS) <= 0L);
				}
			}
			// If the object is not temporal.
			else {

			}
		}
		// If there is a problem checking the object validity.
		catch (final Exception exception) {
			// Logs it.
			DateValidator.LOGGER.error("Value '" + value + "' could not be validated: " + exception.getLocalizedMessage());
			DateValidator.LOGGER.debug("Value '" + value + "' could not be validated.", exception);
		}
		// Returns if the object is valid.
		return valid;
	}

}

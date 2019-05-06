package org.coldis.library.persistence.properties;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Default search properties.
 */
public class DefaultSearchProperties {

	/**
	 * Start date.
	 */
	public static final String START_DATE_STR = "2016-01-01";

	/**
	 * Start date.
	 */
	public static final LocalDate START_DATE = LocalDate.parse("2016-01-01");

	/**
	 * End date.
	 */
	public static final String END_DATE_STR = "2100-12-31";

	/**
	 * End date.
	 */
	public static final LocalDate END_DATE = LocalDate.parse("2100-12-31");

	/**
	 * Start date time.
	 */
	public static final String START_DATE_TIME_STR = "2016-01-01T00:00:00.000";

	/**
	 * Start date time.
	 */
	public static final LocalDateTime START_DATE_TIME = LocalDateTime.parse("2016-01-01T00:00:00.000");

	/**
	 * End date time.
	 */
	public static final String END_DATE_TIME_STR = "2100-12-31T00:00:00.000";

	/**
	 * End date time.
	 */
	public static final LocalDateTime END_DATE_TIME = LocalDateTime.parse("2100-12-31T00:00:00.000");

	/**
	 * Page.
	 */
	public static final Integer PAGE = 0;

	/**
	 * Page.
	 */
	public static final String PAGE_STR = "0";

	/**
	 * Page size.
	 */
	public static final Integer PAGE_SIZE = 25;

	/**
	 * Page size.
	 */
	public static final String PAGE_SIZE_STR = "25";

	/**
	 * Sort by id.
	 */
	public static final String SORT_BY_ID = "id";

	/**
	 * Sort by updated at.
	 */
	public static final String SORT_BY_UPDATED_AT = "updatedAt";

	/**
	 * Sort by created at.
	 */
	public static final String SORT_BY_CREATED_AT = "createdAt";

	/**
	 * Ascendent sorting.
	 */
	public static final String ASC_SORT = "ASC";

	/**
	 * Descendant sorting.
	 */
	public static final String DESC_SORT = "DESC";

}

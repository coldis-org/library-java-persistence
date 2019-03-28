package org.coldis.library.persistence.history;

/**
 * JPA historical entity metadata.
 */
public class HistoricalEntityMetadata {

	/**
	 * Entity history package suffix.
	 */
	public static final String ENTITY_PACKAGE_SUFFIX = ".model";

	/**
	 * Entity history DAO package suffix.
	 */
	public static final String DAO_PACKAGE_SUFFIX = ".dao";

	/**
	 * Entity history service package suffix.
	 */
	public static final String SERVICE_PACKAGE_SUFFIX = ".service";

	/**
	 * Entity history name suffix.
	 */
	public static final String ENTITY_TYPE_SUFFIX = "History";

	/**
	 * Entity history DAO name suffix.
	 */
	public static final String DAO_TYPE_SUFFIX = HistoricalEntityMetadata.ENTITY_TYPE_SUFFIX + "Repository";

	/**
	 * Entity history service name suffix.
	 */
	public static final String SERVICE_TYPE_SUFFIX = HistoricalEntityMetadata.ENTITY_TYPE_SUFFIX + "Service";

	/**
	 * Resources path.
	 */
	private String resourcesPath;

	/**
	 * Entity history template relative path (from resources).
	 */
	private String entityTemplatePath;

	/**
	 * Entity history DAO template relative path (from resources).
	 */
	private String daoTemplatePath;

	/**
	 * Entity history service template relative path (from resources).
	 */
	private String serviceTemplatePath;

	/**
	 * Name of the package for entity history classes.
	 */
	private String basePackageName;

	/**
	 * Original entity package name.
	 */
	private String originalEntityPackageName;

	/**
	 * Original entity type name.
	 */
	private String originalEntityTypeName;

	/**
	 * Entity state attribute converter.
	 */
	private String stateAttributeConverter;

	/**
	 * Entity state column definition.
	 */
	private String stateColumnDefinition;

	/**
	 * TODO Javadoc
	 *
	 * @param resourcesPath
	 * @param entityTemplatePath
	 * @param daoTemplatePath
	 * @param serviceTemplatePath
	 * @param basePackageName
	 * @param originalEntityPackageName
	 * @param originalEntityTypeName
	 * @param stateAttributeConverter
	 * @param stateColumnDefinition     Javadoc
	 */
	public HistoricalEntityMetadata(final String resourcesPath, final String entityTemplatePath,
			final String daoTemplatePath, final String serviceTemplatePath, final String basePackageName,
			final String originalEntityPackageName, final String originalEntityTypeName,
			final String stateAttributeConverter, final String stateColumnDefinition) {
		super();
		this.resourcesPath = resourcesPath;
		this.entityTemplatePath = entityTemplatePath;
		this.daoTemplatePath = daoTemplatePath;
		this.serviceTemplatePath = serviceTemplatePath;
		this.basePackageName = basePackageName;
		this.originalEntityPackageName = originalEntityPackageName;
		this.originalEntityTypeName = originalEntityTypeName;
		this.stateAttributeConverter = stateAttributeConverter;
		this.stateColumnDefinition = stateColumnDefinition;
	}

	/**
	 * Gets the resourcesPath.
	 *
	 * @return The resourcesPath.
	 */
	public String getResourcesPath() {
		return this.resourcesPath;
	}

	/**
	 * Sets the resourcesPath.
	 *
	 * @param resourcesPath New resourcesPath.
	 */
	public void setResourcesPath(final String resourcesPath) {
		this.resourcesPath = resourcesPath;
	}

	/**
	 * Gets the entityTemplatePath.
	 *
	 * @return The entityTemplatePath.
	 */
	public String getEntityTemplatePath() {
		return this.entityTemplatePath;
	}

	/**
	 * Sets the entityTemplatePath.
	 *
	 * @param entityTemplatePath New entityTemplatePath.
	 */
	public void setEntityTemplatePath(final String entityTemplatePath) {
		this.entityTemplatePath = entityTemplatePath;
	}

	/**
	 * Gets the daoTemplatePath.
	 *
	 * @return The daoTemplatePath.
	 */
	public String getDaoTemplatePath() {
		return this.daoTemplatePath;
	}

	/**
	 * Sets the daoTemplatePath.
	 *
	 * @param daoTemplatePath New daoTemplatePath.
	 */
	public void setDaoTemplatePath(final String daoTemplatePath) {
		this.daoTemplatePath = daoTemplatePath;
	}

	/**
	 * Gets the serviceTemplatePath.
	 *
	 * @return The serviceTemplatePath.
	 */
	public String getServiceTemplatePath() {
		return this.serviceTemplatePath;
	}

	/**
	 * Sets the serviceTemplatePath.
	 *
	 * @param serviceTemplatePath New serviceTemplatePath.
	 */
	public void setServiceTemplatePath(final String serviceTemplatePath) {
		this.serviceTemplatePath = serviceTemplatePath;
	}

	/**
	 * Gets the basePackageName.
	 *
	 * @return The basePackageName.
	 */
	public String getBasePackageName() {
		return this.basePackageName;
	}

	/**
	 * Sets the basePackageName.
	 *
	 * @param basePackageName New basePackageName.
	 */
	public void setBasePackageName(final String basePackageName) {
		this.basePackageName = basePackageName;
	}

	/**
	 * Gets the originalEntityPackageName.
	 *
	 * @return The originalEntityPackageName.
	 */
	public String getOriginalEntityPackageName() {
		return this.originalEntityPackageName;
	}

	/**
	 * Sets the originalEntityPackageName.
	 *
	 * @param originalEntityPackageName New originalEntityPackageName.
	 */
	public void setOriginalEntityPackageName(final String originalEntityPackageName) {
		this.originalEntityPackageName = originalEntityPackageName;
	}

	/**
	 * Gets the entity package name.
	 *
	 * @return The entity package name.
	 */
	public String getEntityPackageName() {
		return this.getBasePackageName() + HistoricalEntityMetadata.ENTITY_PACKAGE_SUFFIX;
	}

	/**
	 * Gets the DAO package name.
	 *
	 * @return The DAO package name.
	 */
	public String getDaoPackageName() {
		return this.getBasePackageName() + HistoricalEntityMetadata.DAO_PACKAGE_SUFFIX;
	}

	/**
	 * Gets the service package name.
	 *
	 * @return The service package name.
	 */
	public String getServicePackageName() {
		return this.getBasePackageName() + HistoricalEntityMetadata.SERVICE_PACKAGE_SUFFIX;
	}

	/**
	 * Gets the originalEntityTypeName.
	 *
	 * @return The originalEntityTypeName.
	 */
	public String getOriginalEntityTypeName() {
		return this.originalEntityTypeName;
	}

	/**
	 * Sets the originalEntityTypeName.
	 *
	 * @param originalEntityTypeName New originalEntityTypeName.
	 */
	public void setOriginalEntityTypeName(final String originalEntityTypeName) {
		this.originalEntityTypeName = originalEntityTypeName;
	}

	/**
	 * Gets the entity type name.
	 *
	 * @return The entity type name.
	 */
	public String getEntityTypeName() {
		return this.getOriginalEntityTypeName() + HistoricalEntityMetadata.ENTITY_TYPE_SUFFIX;
	}

	/**
	 * Gets the DAO type name.
	 *
	 * @return The DAO type name.
	 */
	public String getDaoTypeName() {
		return this.getOriginalEntityTypeName() + HistoricalEntityMetadata.DAO_TYPE_SUFFIX;
	}

	/**
	 * Gets the service type name.
	 *
	 * @return The service type name.
	 */
	public String getServiceTypeName() {
		return this.getOriginalEntityTypeName() + HistoricalEntityMetadata.SERVICE_TYPE_SUFFIX;
	}

	/**
	 * Gets the original entity qualified type name.
	 *
	 * @return The original entity qualified type name.
	 */
	public String getOriginalEntityQualifiedTypeName() {
		return this.getOriginalEntityPackageName() + "." + this.getOriginalEntityTypeName();
	}

	/**
	 * Gets the entity qualified type name.
	 *
	 * @return The entity qualified type name.
	 */
	public String getEntityQualifiedTypeName() {
		return this.getEntityPackageName() + "." + this.getEntityTypeName();
	}

	/**
	 * Gets the DAO type qualified name.
	 *
	 * @return The DAO type qualified name.
	 */
	public String getDaoQualifiedTypeName() {
		return this.getDaoPackageName() + "." + this.getDaoTypeName();
	}

	/**
	 * Gets the service type qualified name.
	 *
	 * @return The service type qualified name.
	 */
	public String getServiceQualifiedTypeName() {
		return this.getServicePackageName() + "." + this.getServiceTypeName();
	}

	/**
	 * Gets the stateAttributeConverter.
	 *
	 * @return The stateAttributeConverter.
	 */
	public String getStateAttributeConverter() {
		return this.stateAttributeConverter;
	}

	/**
	 * Sets the stateAttributeConverter.
	 *
	 * @param stateAttributeConverter New stateAttributeConverter.
	 */
	public void setStateAttributeConverter(final String stateAttributeConverter) {
		this.stateAttributeConverter = stateAttributeConverter;
	}

	/**
	 * Gets the stateColumnDefinition.
	 *
	 * @return The stateColumnDefinition.
	 */
	public String getStateColumnDefinition() {
		return this.stateColumnDefinition;
	}

	/**
	 * Sets the stateColumnDefinition.
	 *
	 * @param stateColumnDefinition New stateColumnDefinition.
	 */
	public void setStateColumnDefinition(final String stateColumnDefinition) {
		this.stateColumnDefinition = stateColumnDefinition;
	}

}

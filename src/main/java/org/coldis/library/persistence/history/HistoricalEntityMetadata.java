package org.coldis.library.persistence.history;

import java.io.File;

/**
 * JPA historical entity metadata.
 */
public class HistoricalEntityMetadata {

	/**
	 * Entity history package suffix.
	 */
	public static final String ENTITY_PACKAGE_SUFFIX = ".model";

	/**
	 * Entity history repository package suffix.
	 */
	public static final String REPOSITORY_PACKAGE_SUFFIX = ".repository";

	/**
	 * Entity history service package suffix.
	 */
	public static final String SERVICE_PACKAGE_SUFFIX = ".service";

	/**
	 * Entity history name suffix.
	 */
	public static final String ENTITY_TYPE_SUFFIX = "History";

	/**
	 * Entity history repository name suffix.
	 */
	public static final String REPOSITORY_TYPE_SUFFIX = HistoricalEntityMetadata.ENTITY_TYPE_SUFFIX + "Repository";

	/**
	 * Entity history producer service name suffix.
	 */
	public static final String PRODUCER_SERVICE_TYPE_SUFFIX = HistoricalEntityMetadata.ENTITY_TYPE_SUFFIX + "ProducerService";

	/**
	 * Entity history consumer service name suffix.
	 */
	public static final String CONSUMER_SERVICE_TYPE_SUFFIX = HistoricalEntityMetadata.ENTITY_TYPE_SUFFIX + "ConsumerService";

	/**
	 * Producer target path.
	 */
	private String producerTargetPath;

	/**
	 * Consumer target path.
	 */
	private String consumerTargetPath;

	/**
	 * Entity history template path.
	 */
	private String entityTemplatePath;

	/**
	 * Entity history repository template path.
	 */
	private String repositoryTemplatePath;

	/**
	 * Entity history producer service template path.
	 */
	private String producerServiceTemplatePath;

	/**
	 * Entity history consumer service template path.
	 */
	private String consumerServiceTemplatePath;

	/**
	 * Entity history repository bean name.
	 */
	private String repositoryBeanName;

	/**
	 * Entity history producer service bean name.
	 */
	private String producerServiceBeanName;

	/**
	 * Entity history consumer service bean name.
	 */
	private String consumerServiceBeanName;

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
	 * Entity state column definition.
	 */
	private String stateColumnDefinition;

	/**
	 * Default constructor.
	 *
	 * @param producerTargetPath
	 * @param consumerTargetPath
	 * @param entityTemplatePath
	 * @param repositoryTemplatePath
	 * @param producerServiceTemplatePath
	 * @param consumerServiceTemplatePath
	 * @param repositoryBeanName
	 * @param producerServiceBeanName
	 * @param consumerServiceBeanName
	 * @param basePackageName
	 * @param originalEntityPackageName
	 * @param originalEntityTypeName
	 * @param stateColumnDefinition
	 *
	 */
	public HistoricalEntityMetadata(final String producerTargetPath, final String consumerTargetPath, final String entityTemplatePath,
			final String repositoryTemplatePath, final String producerServiceTemplatePath, final String consumerServiceTemplatePath,
			final String repositoryBeanName, final String producerServiceBeanName, final String consumerServiceBeanName, final String basePackageName,
			final String originalEntityPackageName, final String originalEntityTypeName, final String stateColumnDefinition) {
		super();
		this.producerTargetPath = producerTargetPath;
		this.consumerTargetPath = consumerTargetPath;
		this.entityTemplatePath = entityTemplatePath;
		this.repositoryTemplatePath = repositoryTemplatePath;
		this.producerServiceTemplatePath = producerServiceTemplatePath;
		this.consumerServiceTemplatePath = consumerServiceTemplatePath;
		this.repositoryBeanName = repositoryBeanName;
		this.producerServiceBeanName = producerServiceBeanName;
		this.consumerServiceBeanName = consumerServiceBeanName;
		this.basePackageName = basePackageName;
		this.originalEntityPackageName = originalEntityPackageName;
		this.originalEntityTypeName = originalEntityTypeName;
		this.stateColumnDefinition = stateColumnDefinition;
	}

	/**
	 * Gets the producerTargetPath.
	 *
	 * @return The producerTargetPath.
	 */
	public String getProducerTargetPath() {
		return this.producerTargetPath;
	}

	/**
	 * Sets the producerTargetPath.
	 *
	 * @param producerTargetPath New producerTargetPath.
	 */
	public void setProducerTargetPath(final String producerTargetPath) {
		this.producerTargetPath = producerTargetPath;
	}

	/**
	 * Gets the consumerTargetPath.
	 *
	 * @return The consumerTargetPath.
	 */
	public String getConsumerTargetPath() {
		return this.consumerTargetPath;
	}

	/**
	 * Sets the consumerTargetPath.
	 *
	 * @param consumerTargetPath New consumerTargetPath.
	 */
	public void setConsumerTargetPath(final String consumerTargetPath) {
		this.consumerTargetPath = consumerTargetPath;
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
	 * Gets the repositoryTemplatePath.
	 *
	 * @return The repositoryTemplatePath.
	 */
	public String getRepositoryTemplatePath() {
		return this.repositoryTemplatePath;
	}

	/**
	 * Sets the repositoryTemplatePath.
	 *
	 * @param repositoryTemplatePath New repositoryTemplatePath.
	 */
	public void setRepositoryTemplatePath(final String repositoryTemplatePath) {
		this.repositoryTemplatePath = repositoryTemplatePath;
	}

	/**
	 * Gets the producerServiceTemplatePath.
	 *
	 * @return The producerServiceTemplatePath.
	 */
	public String getProducerServiceTemplatePath() {
		return this.producerServiceTemplatePath;
	}

	/**
	 * Sets the producerServiceTemplatePath.
	 *
	 * @param producerServiceTemplatePath New producerServiceTemplatePath.
	 */
	public void setProducerServiceTemplatePath(final String producerServiceTemplatePath) {
		this.producerServiceTemplatePath = producerServiceTemplatePath;
	}

	/**
	 * Gets the consumerServiceTemplatePath.
	 *
	 * @return The consumerServiceTemplatePath.
	 */
	public String getConsumerServiceTemplatePath() {
		return this.consumerServiceTemplatePath;
	}

	/**
	 * Sets the consumerServiceTemplatePath.
	 *
	 * @param consumerServiceTemplatePath New consumerServiceTemplatePath.
	 */
	public void setConsumerServiceTemplatePath(final String consumerServiceTemplatePath) {
		this.consumerServiceTemplatePath = consumerServiceTemplatePath;
	}

	/**
	 * Gets the repositoryBeanName.
	 *
	 * @return The repositoryBeanName.
	 */
	public String getRepositoryBeanName() {
		return this.repositoryBeanName;
	}

	/**
	 * Sets the repositoryBeanName.
	 *
	 * @param repositoryBeanName New repositoryBeanName.
	 */
	public void setRepositoryBeanName(final String repositoryBeanName) {
		this.repositoryBeanName = repositoryBeanName;
	}

	/**
	 * Gets the producerServiceBeanName.
	 *
	 * @return The producerServiceBeanName.
	 */
	public String getProducerServiceBeanName() {
		return this.producerServiceBeanName;
	}

	/**
	 * Sets the producerServiceBeanName.
	 *
	 * @param producerServiceBeanName New producerServiceBeanName.
	 */
	public void setProducerServiceBeanName(final String producerServiceBeanName) {
		this.producerServiceBeanName = producerServiceBeanName;
	}

	/**
	 * Gets the consumerServiceBeanName.
	 *
	 * @return The consumerServiceBeanName.
	 */
	public String getConsumerServiceBeanName() {
		return this.consumerServiceBeanName;
	}

	/**
	 * Sets the consumerServiceBeanName.
	 *
	 * @param consumerServiceBeanName New consumerServiceBeanName.
	 */
	public void setConsumerServiceBeanName(final String consumerServiceBeanName) {
		this.consumerServiceBeanName = consumerServiceBeanName;
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
	 * Gets the entity file package name.
	 *
	 * @return The entity file package name.
	 */
	public String getEntityFilePackageName() {
		return this.getEntityPackageName().replace(".", File.separator);
	}

	/**
	 * Gets the repository package name.
	 *
	 * @return The repository package name.
	 */
	public String getRepositoryPackageName() {
		return this.getBasePackageName() + HistoricalEntityMetadata.REPOSITORY_PACKAGE_SUFFIX;
	}

	/**
	 * Gets the repository file package name.
	 *
	 * @return The repository file package name.
	 */
	public String getRepositoryFilePackageName() {
		return this.getRepositoryPackageName().replace(".", File.separator);
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
	 * Gets the service file package name.
	 *
	 * @return The service file package name.
	 */
	public String getServiceFilePackageName() {
		return this.getServicePackageName().replace(".", File.separator);
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
	 * Gets the repository type name.
	 *
	 * @return The repository type name.
	 */
	public String getRepositoryTypeName() {
		return this.getOriginalEntityTypeName() + HistoricalEntityMetadata.REPOSITORY_TYPE_SUFFIX;
	}

	/**
	 * Gets the producer service type name.
	 *
	 * @return The producer service type name.
	 */
	public String getProducerServiceTypeName() {
		return this.getOriginalEntityTypeName() + HistoricalEntityMetadata.PRODUCER_SERVICE_TYPE_SUFFIX;
	}

	/**
	 * Gets the consumer service type name.
	 *
	 * @return The consumer service type name.
	 */
	public String getConsumerServiceTypeName() {
		return this.getOriginalEntityTypeName() + HistoricalEntityMetadata.CONSUMER_SERVICE_TYPE_SUFFIX;
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
	 * Gets the repository type qualified name.
	 *
	 * @return The repository type qualified name.
	 */
	public String getRepositoryQualifiedTypeName() {
		return this.getRepositoryPackageName() + "." + this.getRepositoryTypeName();
	}

	/**
	 * Gets the producer service type qualified name.
	 *
	 * @return The producer service type qualified name.
	 */
	public String getProducerServiceQualifiedTypeName() {
		return this.getServicePackageName() + "." + this.getProducerServiceTypeName();
	}

	/**
	 * Gets the consumer service type qualified name.
	 *
	 * @return The consumer service type qualified name.
	 */
	public String getConsumerServiceQualifiedTypeName() {
		return this.getServicePackageName() + "." + this.getConsumerServiceTypeName();
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

	/**
	 * Gets the sequence name.
	 *
	 * @return The sequence name.
	 */
	public String getSequenceName() {
		return this.getEntityTypeName() + "Sequence";
	}

	/**
	 * Gets the queue name.
	 *
	 * @return The queue name.
	 */
	public String getQueueName() {
		return this.getEntityTypeName() + ".queue";
	}

}

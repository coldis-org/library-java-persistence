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
	 * Name of the package for entity history classes.
	 */
	private String basePackageName;

	/**
	 * Entity history template path.
	 */
	private String entityTemplatePath;

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
	 * Entity history repository template path.
	 */
	private String repositoryTemplatePath;

	/**
	 * Entity history repository bean name.
	 */
	private String repositoryBeanName;

	/**
	 * Entity history producer service template path.
	 */
	private String producerServiceTemplatePath;

	/**
	 * Entity history consumer service template path.
	 */
	private String consumerServiceTemplatePath;

	/**
	 * Producer target path.
	 */
	private String producerTargetPath;

	/**
	 * Consumer target path.
	 */
	private String consumerTargetPath;

	/**
	 * JMS template qualifier.
	 */
	private String producerJmsTemplateQualifier;

	/**
	 * Consumer container factory.
	 */
	private String consumerJmsContainerFactory;

	/**
	 * Default constructor.
	 * 
	 * @param basePackageName
	 * @param entityTemplatePath
	 * @param originalEntityPackageName
	 * @param originalEntityTypeName
	 * @param stateColumnDefinition
	 * @param repositoryTemplatePath
	 * @param repositoryBeanName
	 * @param producerServiceTemplatePath
	 * @param producerTargetPath
	 * @param producerServiceBeanName
	 * @param consumerServiceTemplatePath
	 * @param consumerTargetPath
	 * @param consumerServiceBeanName
	 * @param consumerConcurrency
	 *
	 */
	public HistoricalEntityMetadata(
			final String basePackageName,
			final String entityTemplatePath,
			final String originalEntityPackageName,
			final String originalEntityTypeName,
			final String stateColumnDefinition,
			final String repositoryTemplatePath,
			final String repositoryBeanName,
			final String producerServiceTemplatePath,
			final String producerTargetPath,
			final String producerJmsTemplateQualifier,
			final String consumerServiceTemplatePath,
			final String consumerTargetPath,
			final String consumerJmsContainerFactory) {
		super();
		this.basePackageName = basePackageName;
		this.entityTemplatePath = entityTemplatePath;
		this.originalEntityPackageName = originalEntityPackageName;
		this.originalEntityTypeName = originalEntityTypeName;
		this.stateColumnDefinition = stateColumnDefinition;
		this.repositoryTemplatePath = repositoryTemplatePath;
		this.repositoryBeanName = repositoryBeanName;
		this.producerServiceTemplatePath = producerServiceTemplatePath;
		this.producerTargetPath = producerTargetPath;
		this.producerJmsTemplateQualifier = producerJmsTemplateQualifier;
		this.consumerServiceTemplatePath = consumerServiceTemplatePath;
		this.consumerTargetPath = consumerTargetPath;
		this.consumerJmsContainerFactory = consumerJmsContainerFactory;
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
	public void setBasePackageName(
			final String basePackageName) {
		this.basePackageName = basePackageName;
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
	public void setEntityTemplatePath(
			final String entityTemplatePath) {
		this.entityTemplatePath = entityTemplatePath;
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
	public void setOriginalEntityPackageName(
			final String originalEntityPackageName) {
		this.originalEntityPackageName = originalEntityPackageName;
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
	public void setOriginalEntityTypeName(
			final String originalEntityTypeName) {
		this.originalEntityTypeName = originalEntityTypeName;
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
	 * Gets the entity type name.
	 *
	 * @return The entity type name.
	 */
	public String getEntityTypeName() {
		return this.getOriginalEntityTypeName() + HistoricalEntityMetadata.ENTITY_TYPE_SUFFIX;
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
	public void setStateColumnDefinition(
			final String stateColumnDefinition) {
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
	public void setRepositoryTemplatePath(
			final String repositoryTemplatePath) {
		this.repositoryTemplatePath = repositoryTemplatePath;
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
	 * Gets the repository type qualified name.
	 *
	 * @return The repository type qualified name.
	 */
	public String getRepositoryQualifiedTypeName() {
		return this.getRepositoryPackageName() + "." + this.getRepositoryTypeName();
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
	 * Gets the repositoryBeanName.
	 *
	 * @return The repositoryBeanName.
	 */
	public String getRepositoryBeanName() {
		return this.repositoryBeanName;
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
	 * Gets the queue name.
	 *
	 * @return The queue name.
	 */
	public String getQueueName() {
		return this.getOriginalEntityTypeName().replaceAll("([A-Z])", "-$1").toLowerCase().substring(1) + "/history";
	}
	
	/**
	 * Sets the repositoryBeanName.
	 *
	 * @param repositoryBeanName New repositoryBeanName.
	 */
	public void setRepositoryBeanName(
			final String repositoryBeanName) {
		this.repositoryBeanName = repositoryBeanName;
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
	public void setProducerServiceTemplatePath(
			final String producerServiceTemplatePath) {
		this.producerServiceTemplatePath = producerServiceTemplatePath;
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
	public void setProducerTargetPath(
			final String producerTargetPath) {
		this.producerTargetPath = producerTargetPath;
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
	 * Gets the producer service type qualified name.
	 *
	 * @return The producer service type qualified name.
	 */
	public String getProducerServiceQualifiedTypeName() {
		return this.getServicePackageName() + "." + this.getProducerServiceTypeName();
	}

	/**
	 * Gets the producerJmsTemplateQualifier.
	 * 
	 * @return The producerJmsTemplateQualifier.
	 */
	public String getProducerJmsTemplateQualifier() {
		return producerJmsTemplateQualifier;
	}

	/**
	 * Sets the producerJmsTemplateQualifier.
	 * 
	 * @param producerJmsTemplateQualifier New producerJmsTemplateQualifier.
	 */
	public void setProducerJmsTemplateQualifier(
			String producerJmsTemplateQualifier) {
		this.producerJmsTemplateQualifier = producerJmsTemplateQualifier;
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
	public void setConsumerServiceTemplatePath(
			final String consumerServiceTemplatePath) {
		this.consumerServiceTemplatePath = consumerServiceTemplatePath;
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
	public void setConsumerTargetPath(
			final String consumerTargetPath) {
		this.consumerTargetPath = consumerTargetPath;
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
	 * Gets the consumer service type qualified name.
	 *
	 * @return The consumer service type qualified name.
	 */
	public String getConsumerServiceQualifiedTypeName() {
		return this.getServicePackageName() + "." + this.getConsumerServiceTypeName();
	}

	/**
	 * Gets the consumerJmsContainerFactory.
	 * 
	 * @return The consumerJmsContainerFactory.
	 */
	public String getConsumerJmsContainerFactory() {
		return consumerJmsContainerFactory;
	}

	/**
	 * Sets the consumerJmsContainerFactory.
	 * 
	 * @param consumerJmsContainerFactory New consumerJmsContainerFactory.
	 */
	public void setConsumerJmsContainerFactory(
			String consumerJmsContainerFactory) {
		this.consumerJmsContainerFactory = consumerJmsContainerFactory;
	}

}

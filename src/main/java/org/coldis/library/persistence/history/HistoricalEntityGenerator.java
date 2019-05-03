package org.coldis.library.persistence.history;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA entity history generator.
 */
@SupportedSourceVersion(value = SourceVersion.RELEASE_11)
@SupportedAnnotationTypes(value = { "org.coldis.library.persistence.history.HistoricalEntity" })
public class HistoricalEntityGenerator extends AbstractProcessor {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoricalEntityGenerator.class);

	/**
	 * Generates the classes from the metadata.
	 *
	 * @param  historicalEntityMetadata Metadata.
	 * @throws IOException              If the classes cannot be generated.
	 */
	private void generateClasses(final HistoricalEntityMetadata historicalEntityMetadata) throws IOException {
		// Gets the velocity engine.
		final VelocityEngine velocityEngine = new VelocityEngine();
		// Configures the resource loader to also look at the classpath.
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		// Initializes the velocity engine.
		velocityEngine.init();
		// Creates a new velocity context and sets its variables.
		final VelocityContext velocityContext = new VelocityContext();
		// Sets the context values.
		velocityContext.put("h", "#");
		velocityContext.put("historicalEntity", historicalEntityMetadata);
		// Gets the templates for the entity history classes.
		final Template entityTemplate = velocityEngine.getTemplate(historicalEntityMetadata.getEntityTemplatePath());
		final Template repositoryTemplate = velocityEngine
				.getTemplate(historicalEntityMetadata.getRepositoryTemplatePath());
		final Template producerServiceTemplate = velocityEngine
				.getTemplate(historicalEntityMetadata.getProducerServiceTemplatePath());
		final Template consumerServiceTemplate = velocityEngine
				.getTemplate(historicalEntityMetadata.getConsumerServiceTemplatePath());
		// Gets the writer for the generated classes.
		final File entityFile = new File(
				historicalEntityMetadata.getConsumerTargetPath() + File.separator
				+ historicalEntityMetadata.getEntityFilePackageName(),
				historicalEntityMetadata.getEntityTypeName() + ".java");
		final File daoFile = new File(
				historicalEntityMetadata.getConsumerTargetPath() + File.separator
				+ historicalEntityMetadata.getRepositoryFilePackageName(),
				historicalEntityMetadata.getRepositoryTypeName() + ".java");
		final File producerServiceFile = new File(
				historicalEntityMetadata.getProducerTargetPath() + File.separator
				+ historicalEntityMetadata.getServiceFilePackageName(),
				historicalEntityMetadata.getProducerServiceTypeName() + ".java");
		final File consumerServiceFile = new File(
				historicalEntityMetadata.getConsumerTargetPath() + File.separator
				+ historicalEntityMetadata.getServiceFilePackageName(),
				historicalEntityMetadata.getConsumerServiceTypeName() + ".java");
		FileUtils.forceMkdir(entityFile.getParentFile());
		FileUtils.forceMkdir(daoFile.getParentFile());
		FileUtils.forceMkdir(producerServiceFile.getParentFile());
		FileUtils.forceMkdir(consumerServiceFile.getParentFile());
		final Writer entityWriter = new FileWriter(entityFile);
		final Writer daoWriter = new FileWriter(daoFile);
		final Writer producerServiceWriter = new FileWriter(producerServiceFile);
		final Writer consumerServiceWriter = new FileWriter(consumerServiceFile);
		// Generates the classes.
		entityTemplate.merge(velocityContext, entityWriter);
		repositoryTemplate.merge(velocityContext, daoWriter);
		producerServiceTemplate.merge(velocityContext, producerServiceWriter);
		consumerServiceTemplate.merge(velocityContext, consumerServiceWriter);
		// Closes the writers.
		entityWriter.close();
		daoWriter.close();
		producerServiceWriter.close();
		consumerServiceWriter.close();
	}

	/**
	 * Gets the historical entity metadata from the entity type.
	 *
	 * @param  entityType Entity type.
	 * @return            Entity type.
	 */
	private HistoricalEntityMetadata getEntityHistoryMetadata(final TypeElement entityType) {
		// Gets the historical entity metadata.
		final HistoricalEntity historicalEntity = entityType.getAnnotation(HistoricalEntity.class); // Tries to get the
		// Creates the default metadata.
		final HistoricalEntityMetadata historicalEntityMetadata = new HistoricalEntityMetadata(
				historicalEntity.producerTargetPath(), historicalEntity.consumerTargetPath(),
				historicalEntity.entityTemplatePath(), historicalEntity.repositoryTemplatePath(),
				historicalEntity.producerServiceTemplatePath(), historicalEntity.consumerServiceTemplatePath(),
				historicalEntity.basePackageName(),
				((PackageElement) entityType.getEnclosingElement()).getQualifiedName().toString(),
				entityType.getSimpleName().toString(), historicalEntity.stateColumnDefinition());
		// Returns the historical entity metadata.
		return historicalEntityMetadata;
	}

	/**
	 * @see javax.annotation.processing.AbstractProcessor#process(java.util.Set,
	 *      javax.annotation.processing.RoundEnvironment)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		HistoricalEntityGenerator.LOGGER.debug("Initializing HistoricalEntityGenerator...");
		// For each historical entity.
		for (final TypeElement entityType : (Set<TypeElement>) roundEnv
				.getElementsAnnotatedWith(HistoricalEntity.class)) {
			HistoricalEntityGenerator.LOGGER
			.debug("Generating entity history classes for '" + entityType.getSimpleName() + "'...");
			// Tries to generate the entity history classes.
			try {
				this.generateClasses(this.getEntityHistoryMetadata(entityType));
				HistoricalEntityGenerator.LOGGER
				.debug("Historical entity '" + entityType.getSimpleName() + "' processed successfully.");
			}
			// If the historical entity could not be processed correctly.
			catch (final IOException exception) {
				// Logs the error.
				HistoricalEntityGenerator.LOGGER.debug(
						"Historical entity '" + entityType.getSimpleName() + "' not processed successfully.",
						exception);
			}
		}
		// Returns that the annotations have been processed.
		HistoricalEntityGenerator.LOGGER.debug("Finishing HistoricalEntityGenerator...");
		return true;
	}

}

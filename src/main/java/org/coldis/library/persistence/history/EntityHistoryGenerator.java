package org.coldis.library.persistence.history;

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
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

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
@SupportedSourceVersion(value = SourceVersion.RELEASE_10)
@SupportedAnnotationTypes(value = { "org.coldis.library.persistence.history.HistoricalEntity" })
public class EntityHistoryGenerator extends AbstractProcessor {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(EntityHistoryGenerator.class);

	/**
	 * Gets a template.
	 *
	 * @param  velocityEngine  Velocity engine.
	 * @param  resourcesFolder The resources folder to be used.
	 * @param  templatePath    The template path.
	 * @return                 The velocity template.
	 */
	private Template getTemplate(final VelocityEngine velocityEngine, final String resourcesFolder,
			final String templatePath) {
		// Configures the classpath resource loader.
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		// Velocity template.
		Template velocityTemplate = null;
		// Tries to get the template for the given path.
		try {
			velocityTemplate = velocityEngine.getTemplate(resourcesFolder + templatePath);
		}
		// If the template cannot be retrieved
		catch (final Exception exception) {
			// Ignores it.
		}
		// If the template has not been found yet.
		if (velocityTemplate == null) {
			// Tries to get the template from the class path.
			velocityTemplate = velocityEngine.getTemplate(templatePath);
		}
		// Returns the found template.
		return velocityTemplate;
	}

	/**
	 * TODO Javadoc
	 *
	 * @param  historicalEntityMetadata
	 * @throws IOException              Javadoc
	 */
	private void generateClasses(final HistoricalEntityMetadata historicalEntityMetadata) throws IOException {
		// Gets the velocity engine and initializes it.
		final VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.init();
		// Creates a new velocity context and sets its variables.
		final VelocityContext velocityContext = new VelocityContext();
		// Sets the context values.
		velocityContext.put("h", "#");
		velocityContext.put("historicalEntity", historicalEntityMetadata);
		// Gets the templates for the entity history classes.
		final Template entityTemplate = this.getTemplate(velocityEngine, historicalEntityMetadata.getResourcesPath(),
				historicalEntityMetadata.getEntityTemplatePath());
		final Template repoTemplate = this.getTemplate(velocityEngine, historicalEntityMetadata.getResourcesPath(),
				historicalEntityMetadata.getDaoTemplatePath());
		final Template srvTemplate = this.getTemplate(velocityEngine, historicalEntityMetadata.getResourcesPath(),
				historicalEntityMetadata.getServiceTemplatePath());
		// Gets the writer for the generated classes.
		final Writer entityWriter = this.processingEnv.getFiler()
				.createSourceFile(historicalEntityMetadata.getEntityQualifiedTypeName()).openWriter();
		final Writer daoWriter = this.processingEnv.getFiler()
				.createSourceFile(historicalEntityMetadata.getDaoQualifiedTypeName()).openWriter();
		final Writer serviceWriter = this.processingEnv.getFiler()
				.createSourceFile(historicalEntityMetadata.getServiceQualifiedTypeName()).openWriter();
		// Generates the classes.
		entityTemplate.merge(velocityContext, entityWriter);
		repoTemplate.merge(velocityContext, daoWriter);
		srvTemplate.merge(velocityContext, serviceWriter);
		// Closes the writers.
		entityWriter.close();
		daoWriter.close();
		serviceWriter.close();
	}

	/**
	 * TODO Javadoc
	 *
	 * @param  historicalEntity
	 * @return                  Javadoc
	 */
	private String getStateAttributeConverter(final HistoricalEntity historicalEntity) {
		// Entity state attribute converter.
		TypeMirror stateAttributeConverter = null;
		// This is a trick to get the class information (catching the
		// MirroredTypeException).
		try {
			historicalEntity.stateAttributeConverter();
		}
		// Catcher mirrored exception.
		catch (final MirroredTypeException exception) {
			// Gets the class information.
			stateAttributeConverter = exception.getTypeMirror();
		}
		// Returns the entity state attribute converter.
		return stateAttributeConverter.toString();
	}

	/**
	 * TODO Javadoc
	 *
	 * @param  entityType
	 * @return            Javadoc
	 */
	private HistoricalEntityMetadata getEntityHistoryMetadata(final TypeElement entityType) {
		// Gets the historical entity metadata.
		final HistoricalEntity historicalEntity = entityType.getAnnotation(HistoricalEntity.class); // Tries to get the
		// Creates the default metadata.
		final HistoricalEntityMetadata historicalEntityMetadata = new HistoricalEntityMetadata(
				historicalEntity.resourcesPath(), historicalEntity.entityTemplatePath(),
				historicalEntity.daoTemplatePath(), historicalEntity.serviceTemplatePath(),
				historicalEntity.basePackageName(),
				((PackageElement) entityType.getEnclosingElement()).getQualifiedName().toString(),
				entityType.getSimpleName().toString(), this.getStateAttributeConverter(historicalEntity),
				historicalEntity.stateColumnDefinition());
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
		EntityHistoryGenerator.LOGGER.debug("Initializing EntityHistoryGenerator...");
		// For each historical entity.
		for (final TypeElement entityType : (Set<TypeElement>) roundEnv
				.getElementsAnnotatedWith(HistoricalEntity.class)) {
			EntityHistoryGenerator.LOGGER
			.debug("Generating entity history classes for '" + entityType.getSimpleName() + "'...");
			// Tries to generate the entity history classes.
			try {
				this.generateClasses(this.getEntityHistoryMetadata(entityType));
				EntityHistoryGenerator.LOGGER
				.debug("Historical entity '" + entityType.getSimpleName() + "' processed successfully.");
			}
			// If the historical entity could not be processed correctly.
			catch (final IOException exception) {
				// Logs the error.
				EntityHistoryGenerator.LOGGER.debug(
						"Historical entity '" + entityType.getSimpleName() + "' not processed successfully.",
						exception);
			}
		}
		// Returns that the annotations have been processed.
		EntityHistoryGenerator.LOGGER.debug("Finishing EntityHistoryGenerator...");
		return true;
	}

}

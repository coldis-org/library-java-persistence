package org.coldis.library.persistence.configuration;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

/**
 * Classpath scan result for {@link DatasourceUnit} annotated types under the given base packages
 * (read from class metadata, without loading classes). {@link SecondaryDatasourcesRegistrar} uses it
 * to assign entities to each secondary unit (and to fail fast on unknown unit names), and
 * {@link PrimaryJpaConfiguration} uses it to keep annotated types out of the primary unit.
 */
final class DatasourceUnitScan {

	/** Every annotated type name (entities and repositories), across all units. */
	private final Set<String> annotatedClassNames;

	/** Annotated type names by unit (entities and repositories). */
	private final Map<String, Set<String>> classNamesByUnit;

	/** Annotated non-interface type names by unit — each unit's managed entity classes. */
	private final Map<String, Set<String>> entityClassNamesByUnit;

	/**
	 * @param annotatedClassNames    Every annotated type name.
	 * @param classNamesByUnit       Annotated type names by unit.
	 * @param entityClassNamesByUnit Annotated non-interface type names by unit.
	 */
	private DatasourceUnitScan(
			final Set<String> annotatedClassNames,
			final Map<String, Set<String>> classNamesByUnit,
			final Map<String, Set<String>> entityClassNamesByUnit) {
		this.annotatedClassNames = annotatedClassNames;
		this.classNamesByUnit = classNamesByUnit;
		this.entityClassNamesByUnit = entityClassNamesByUnit;
	}

	/**
	 * Scans the base packages for {@link DatasourceUnit} annotated types.
	 *
	 * @param  resourceLoader Resource loader.
	 * @param  basePackages   Base packages to scan.
	 * @return                The scan result.
	 */
	public static DatasourceUnitScan of(
			final ResourceLoader resourceLoader,
			final Collection<String> basePackages) {
		final Set<String> annotatedClassNames = new LinkedHashSet<>();
		final Map<String, Set<String>> classNamesByUnit = new LinkedHashMap<>();
		final Map<String, Set<String>> entityClassNamesByUnit = new LinkedHashMap<>();
		final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
		final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		try {
			for (final String basePackage : basePackages) {
				final Resource[] resources = resolver.getResources(
						ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(basePackage) + "/**/*.class");
				for (final Resource resource : resources) {
					if (resource.isReadable()) {
						final AnnotationMetadata metadata = metadataReaderFactory.getMetadataReader(resource).getAnnotationMetadata();
						final Map<String, Object> attributes = metadata.getAnnotationAttributes(DatasourceUnit.class.getName());
						if (attributes != null) {
							final String unit = (String) attributes.get("value");
							annotatedClassNames.add(metadata.getClassName());
							classNamesByUnit.computeIfAbsent(unit, key -> new LinkedHashSet<>()).add(metadata.getClassName());
							if (!metadata.isInterface()) {
								entityClassNamesByUnit.computeIfAbsent(unit, key -> new LinkedHashSet<>()).add(metadata.getClassName());
							}
						}
					}
				}
			}
		}
		catch (final IOException exception) {
			throw new IllegalStateException("Could not scan base packages '" + basePackages + "' for @DatasourceUnit types.", exception);
		}
		return new DatasourceUnitScan(annotatedClassNames, classNamesByUnit, entityClassNamesByUnit);
	}

	/**
	 * @return Every annotated type name (entities and repositories), across all units.
	 */
	public Set<String> getAnnotatedClassNames() {
		return this.annotatedClassNames;
	}

	/**
	 * @return Annotated type names by unit (entities and repositories).
	 */
	public Map<String, Set<String>> getClassNamesByUnit() {
		return this.classNamesByUnit;
	}

	/**
	 * Gets a unit's managed entity class names.
	 *
	 * @param  unit Unit name.
	 * @return      The unit's annotated non-interface type names.
	 */
	public String[] getEntityClassNames(
			final String unit) {
		return this.entityClassNamesByUnit.getOrDefault(unit, Set.of()).toArray(new String[] {});
	}

}

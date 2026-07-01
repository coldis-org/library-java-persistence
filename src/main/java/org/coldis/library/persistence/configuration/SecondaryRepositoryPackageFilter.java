package org.coldis.library.persistence.configuration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Excludes every secondary datasource's repositories — the union of the {@code repository-packages}
 * declared under {@code org.coldis.configuration.persistence.datasources.<name>} — from the coldis
 * primary repository scan. {@link SecondaryDatasourcesRegistrar} reads the same map to include them in
 * their own units. With no datasources configured it matches nothing, so the primary scan is
 * unchanged.
 */
public class SecondaryRepositoryPackageFilter implements TypeFilter, EnvironmentAware {

	/** Resolved secondary repository base packages (union across all datasources). */
	private List<String> secondaryBasePackages = List.of();

	/**
	 * Reads the secondary repository base packages from the environment.
	 *
	 * @param environment Environment.
	 */
	@Override
	public void setEnvironment(
			final Environment environment) {
		final Map<String, SecondaryDatasourceProperties> datasources = Binder.get(environment)
				.bind(SecondaryDatasourcesRegistrar.DATASOURCES_PROPERTY_PREFIX, Bindable.mapOf(String.class, SecondaryDatasourceProperties.class))
				.orElseGet(Map::of);
		this.secondaryBasePackages = datasources.values().stream()
				.map(SecondaryDatasourceProperties::getRepositoryPackages)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.map(StringUtils::trimToNull)
				.filter(StringUtils::isNotBlank)
				.toList();
	}

	/**
	 * Matches (i.e. excludes, since used as an exclude filter) repositories declared under any
	 * configured secondary repository package.
	 *
	 * @param  metadataReader        Candidate metadata.
	 * @param  metadataReaderFactory Metadata reader factory.
	 * @return                       {@code true} when the candidate belongs to a secondary package.
	 */
	@Override
	public boolean match(
			final MetadataReader metadataReader,
			final MetadataReaderFactory metadataReaderFactory) throws IOException {
		final String className = metadataReader.getClassMetadata().getClassName();
		return this.secondaryBasePackages.stream()
				.anyMatch(basePackage -> className.equals(basePackage) || className.startsWith(basePackage + "."));
	}

}

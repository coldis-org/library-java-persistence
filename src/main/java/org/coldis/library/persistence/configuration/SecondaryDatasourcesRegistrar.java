package org.coldis.library.persistence.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.coldis.library.persistence.repository.PostgresJpaRepositoryImpl;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Registers <strong>N</strong> secondary persistence units from properties. For every entry under
 * {@code org.coldis.configuration.persistence.datasources.<name>} it registers a {@code DataSource},
 * an {@code EntityManagerFactory} and a {@code TransactionManager} (named {@code <name>DataSource} /
 * {@code <name>EntityManagerFactory} / {@code <name>TransactionManager}). Unit membership is
 * annotation-driven: entities and repositories annotated with {@link DatasourceUnit @DatasourceUnit}
 * naming the entry are bound to that unit — packaging is free, and the scan covers the same base
 * packages as the primary ({@code org.coldis} plus
 * {@code org.coldis.configuration.persistence.jpa.base-package}). An annotated type naming an
 * unconfigured datasource fails fast at startup.
 *
 * <p>
 * The primary unit is unaffected: it stays on {@code spring.datasource} (see
 * {@link PrimaryJpaConfiguration}, which a multi-datasource service enables with
 * {@code org.coldis.configuration.persistence.explicit-primary=true} so Boot's primary auto-config
 * does not back off), and annotated types are kept out of the primary unit — repositories through the
 * {@link JpaAutoConfiguration} scan's exclude filter, entities through the primary unit's managed
 * types. With no datasources configured this registers nothing, so single-datasource consumers are
 * unchanged.
 * </p>
 *
 * <p>
 * Repositories are registered through Spring Data's {@link RepositoryConfigurationDelegate} with the
 * coldis defaults baked in ({@link PostgresJpaRepositoryImpl},
 * {@code enableDefaultTransactions = false}), so a secondary unit can never silently diverge from the
 * primary.
 * </p>
 */
public class SecondaryDatasourcesRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

	/** Prefix of the secondary datasources map. */
	public static final String DATASOURCES_PROPERTY_PREFIX = "org.coldis.configuration.persistence.datasources";

	/** Base package property (the same one driving the primary entity/repository scan). */
	public static final String BASE_PACKAGE_PROPERTY = "org.coldis.configuration.persistence.jpa.base-package";

	/** Environment. */
	private Environment environment;

	/** Resource loader. */
	private ResourceLoader resourceLoader;

	/**
	 * @see org.springframework.context.EnvironmentAware#setEnvironment(org.springframework.core.env.Environment)
	 */
	@Override
	public void setEnvironment(
			final Environment environment) {
		this.environment = environment;
	}

	/**
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	@Override
	public void setResourceLoader(
			final ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Binds the {@code datasources.<name>} map from the environment.
	 *
	 * @return The configured secondary datasources (empty when none configured).
	 */
	private Map<String, SecondaryDatasourceProperties> bindDatasources() {
		return Binder.get(this.environment)
				.bind(SecondaryDatasourcesRegistrar.DATASOURCES_PROPERTY_PREFIX, Bindable.mapOf(String.class, SecondaryDatasourceProperties.class))
				.orElseGet(Map::of);
	}

	/**
	 * Resolves the base packages scanned for {@link DatasourceUnit} types — the same packages the
	 * primary entity/repository scan uses.
	 *
	 * @return The base packages.
	 */
	private List<String> resolveBasePackages() {
		final List<String> basePackages = new ArrayList<>();
		basePackages.add(PersistenceAutoConfiguration.PERSISTENCE_PACKAGE);
		final String configuredPackages = this.environment.getProperty(SecondaryDatasourcesRegistrar.BASE_PACKAGE_PROPERTY, "");
		for (final String configuredPackage : configuredPackages.split(",")) {
			if (StringUtils.isNotBlank(configuredPackage)) {
				basePackages.add(configuredPackage.trim());
			}
		}
		return basePackages;
	}

	/**
	 * Fails fast when an annotated type names a unit with no configured datasource.
	 *
	 * @param datasources Configured datasources.
	 * @param scan        Annotated types scan.
	 */
	private void validateUnits(
			final Map<String, SecondaryDatasourceProperties> datasources,
			final DatasourceUnitScan scan) {
		for (final Map.Entry<String, Set<String>> unit : scan.getClassNamesByUnit().entrySet()) {
			if (!datasources.containsKey(unit.getKey())) {
				throw new IllegalStateException("Types " + unit.getValue() + " are annotated with @DatasourceUnit(\"" + unit.getKey()
						+ "\"), but no datasource is configured under '" + SecondaryDatasourcesRegistrar.DATASOURCES_PROPERTY_PREFIX + "." + unit.getKey()
						+ "'.");
			}
		}
	}

	/**
	 * Registers, per configured datasource, its datasource, entity manager factory, transaction
	 * manager and repositories.
	 *
	 * @param importingClassMetadata Importing class metadata (unused).
	 * @param registry               Bean definition registry.
	 */
	@Override
	public void registerBeanDefinitions(
			final AnnotationMetadata importingClassMetadata,
			final BeanDefinitionRegistry registry) {
		final Map<String, SecondaryDatasourceProperties> datasources = this.bindDatasources();
		if (!datasources.isEmpty()) {
			final List<String> basePackages = this.resolveBasePackages();
			final DatasourceUnitScan scan = DatasourceUnitScan.of(this.resourceLoader, basePackages);
			this.validateUnits(datasources, scan);
			for (final Map.Entry<String, SecondaryDatasourceProperties> entry : datasources.entrySet()) {
				final String name = entry.getKey();
				final SecondaryDatasourceProperties properties = entry.getValue();
				this.registerDataSource(registry, name, properties);
				this.registerEntityManagerFactory(registry, name, scan.getEntityClassNames(name));
				this.registerTransactionManager(registry, name);
				this.registerRepositories(registry, name, basePackages);
			}
		}
	}

	/**
	 * Registers the {@code <name>DataSource} bean (a Hikari datasource with its {@code <name>.hikari.*}
	 * pool tuning bound in).
	 */
	private void registerDataSource(
			final BeanDefinitionRegistry registry,
			final String name,
			final SecondaryDatasourceProperties properties) {
		final String prefix = SecondaryDatasourcesRegistrar.DATASOURCES_PROPERTY_PREFIX + "." + name;
		final RootBeanDefinition definition = new RootBeanDefinition(HikariDataSource.class);
		definition.setInstanceSupplier(() -> this.createDataSource(prefix, properties));
		definition.setDestroyMethodName("close");
		registry.registerBeanDefinition(name + "DataSource", definition);
	}

	/**
	 * Builds a Hikari datasource from an entry's properties.
	 */
	private HikariDataSource createDataSource(
			final String prefix,
			final SecondaryDatasourceProperties properties) {
		final DataSourceBuilder<HikariDataSource> builder = DataSourceBuilder.create().type(HikariDataSource.class)
				.url(properties.getUrl()).username(properties.getUsername()).password(properties.getPassword());
		if (StringUtils.isNotBlank(properties.getDriverClassName())) {
			builder.driverClassName(properties.getDriverClassName());
		}
		final HikariDataSource dataSource = builder.build();
		Binder.get(this.environment).bind(prefix + ".hikari", Bindable.ofInstance(dataSource));
		return dataSource;
	}

	/**
	 * Registers the {@code <name>EntityManagerFactory} bean, built from the shared
	 * {@link EntityManagerFactoryBuilder} over the entry's datasource and the entities annotated with
	 * the entry's {@link DatasourceUnit} name.
	 */
	private void registerEntityManagerFactory(
			final BeanDefinitionRegistry registry,
			final String name,
			final String[] entityClassNames) {
		final RootBeanDefinition definition = new RootBeanDefinition();
		definition.setBeanClass(SecondaryDatasourcesRegistrar.class);
		definition.setFactoryMethodName("createEntityManagerFactory");
		final ConstructorArgumentValues arguments = new ConstructorArgumentValues();
		arguments.addIndexedArgumentValue(0, new RuntimeBeanReference(EntityManagerFactoryBuilder.class));
		arguments.addIndexedArgumentValue(1, new RuntimeBeanReference(name + "DataSource"));
		arguments.addIndexedArgumentValue(2, name);
		arguments.addIndexedArgumentValue(3, entityClassNames);
		definition.setConstructorArgumentValues(arguments);
		registry.registerBeanDefinition(name + "EntityManagerFactory", definition);
	}

	/**
	 * Static factory building a secondary entity manager factory (referenced by
	 * {@link #registerEntityManagerFactory}).
	 *
	 * @param  builder           Shared entity manager factory builder.
	 * @param  dataSource        Entry datasource.
	 * @param  unit              Persistence unit name.
	 * @param  entityClassNames  Entity classes managed by the unit.
	 * @return                   The secondary entity manager factory.
	 */
	public static LocalContainerEntityManagerFactoryBean createEntityManagerFactory(
			final EntityManagerFactoryBuilder builder,
			final DataSource dataSource,
			final String unit,
			final String[] entityClassNames) {
		return builder.dataSource(dataSource).persistenceUnit(unit).managedTypes(PersistenceManagedTypes.of(entityClassNames)).build();
	}

	/**
	 * Registers the {@code <name>TransactionManager} bean over the entry's entity manager factory.
	 */
	private void registerTransactionManager(
			final BeanDefinitionRegistry registry,
			final String name) {
		final RootBeanDefinition definition = new RootBeanDefinition(JpaTransactionManager.class);
		final ConstructorArgumentValues arguments = new ConstructorArgumentValues();
		arguments.addIndexedArgumentValue(0, new RuntimeBeanReference(name + "EntityManagerFactory"));
		definition.setConstructorArgumentValues(arguments);
		registry.registerBeanDefinition(name + "TransactionManager", definition);
	}

	/**
	 * Registers the entry's repositories — those annotated with the entry's {@link DatasourceUnit}
	 * name, anywhere under the base packages — bound to its entity manager factory / transaction
	 * manager, via Spring Data's programmatic configuration.
	 */
	private void registerRepositories(
			final BeanDefinitionRegistry registry,
			final String name,
			final List<String> basePackages) {
		final AnnotationMetadata metadata = AnnotationMetadata.introspect(RepositoryConfigurationTemplate.class);
		final AnnotationRepositoryConfigurationSource configurationSource = new NamedRepositoryConfigurationSource(metadata, this.resourceLoader,
				this.environment, registry, basePackages, name, name + "EntityManagerFactory", name + "TransactionManager");
		final RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(configurationSource, this.resourceLoader, this.environment);
		delegate.registerRepositoriesIn(registry, new JpaRepositoryConfigExtension());
	}

	/**
	 * Template carrying the coldis {@code @EnableJpaRepositories} defaults (base class, no default
	 * transactions). Its metadata seeds {@link NamedRepositoryConfigurationSource}; the base packages,
	 * unit filter and unit refs are overridden there.
	 */
	@EnableJpaRepositories(
			repositoryBaseClass = PostgresJpaRepositoryImpl.class,
			enableDefaultTransactions = false)
	private static final class RepositoryConfigurationTemplate {
	}

	/**
	 * Annotation-driven configuration source that keeps the template's defaults but scans the shared
	 * base packages for repositories annotated with the unit's {@link DatasourceUnit} name and binds
	 * them to that unit's refs. Spring Data's default repository detection stays intact; an extra
	 * exclude filter prunes every candidate not annotated with this unit.
	 */
	private static final class NamedRepositoryConfigurationSource extends AnnotationRepositoryConfigurationSource {

		/** Repository base packages (shared with the primary scan). */
		private final org.springframework.data.util.Streamable<String> basePackages;

		/** Datasource unit name. */
		private final String unit;

		/** Entity manager factory bean name for this entry. */
		private final String entityManagerFactoryRef;

		/** Transaction manager bean name for this entry. */
		private final String transactionManagerRef;

		private NamedRepositoryConfigurationSource(
				final AnnotationMetadata metadata,
				final ResourceLoader resourceLoader,
				final Environment environment,
				final BeanDefinitionRegistry registry,
				final List<String> basePackages,
				final String unit,
				final String entityManagerFactoryRef,
				final String transactionManagerRef) {
			super(metadata, EnableJpaRepositories.class, resourceLoader, environment, registry);
			this.basePackages = org.springframework.data.util.Streamable.of(basePackages);
			this.unit = unit;
			this.entityManagerFactoryRef = entityManagerFactoryRef;
			this.transactionManagerRef = transactionManagerRef;
		}

		@Override
		public org.springframework.data.util.Streamable<String> getBasePackages() {
			return this.basePackages;
		}

		@Override
		public org.springframework.data.util.Streamable<TypeFilter> getExcludeFilters() {
			final TypeFilter otherUnitFilter = (metadataReader, metadataReaderFactory) -> {
				final Map<String, Object> attributes = metadataReader.getAnnotationMetadata().getAnnotationAttributes(DatasourceUnit.class.getName());
				return ((attributes == null) || !this.unit.equals(attributes.get("value")));
			};
			return super.getExcludeFilters().and(otherUnitFilter);
		}

		@Override
		public AnnotationAttributes getAttributes() {
			final AnnotationAttributes attributes = new AnnotationAttributes(super.getAttributes());
			attributes.put("entityManagerFactoryRef", this.entityManagerFactoryRef);
			attributes.put("transactionManagerRef", this.transactionManagerRef);
			return attributes;
		}

		@Override
		public Optional<String> getAttribute(
				final String name) {
			if ("entityManagerFactoryRef".equals(name)) {
				return Optional.of(this.entityManagerFactoryRef);
			}
			if ("transactionManagerRef".equals(name)) {
				return Optional.of(this.transactionManagerRef);
			}
			return super.getAttribute(name);
		}

		@Override
		public <T> Optional<T> getAttribute(
				final String name,
				final Class<T> type) {
			if (String.class.equals(type) && ("entityManagerFactoryRef".equals(name))) {
				return Optional.of(type.cast(this.entityManagerFactoryRef));
			}
			if (String.class.equals(type) && ("transactionManagerRef".equals(name))) {
				return Optional.of(type.cast(this.transactionManagerRef));
			}
			return super.getAttribute(name, type);
		}

	}

}

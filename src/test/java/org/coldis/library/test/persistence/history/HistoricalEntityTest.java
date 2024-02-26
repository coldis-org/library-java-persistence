package org.coldis.library.test.persistence.history;

import org.apache.commons.collections4.IterableUtils;
import org.coldis.library.serialization.ObjectMapperHelper;
import org.coldis.library.test.ContainerExtension;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.persistence.TestApplication;
import org.coldis.library.test.persistence.history.historical.repository.TestHistoricalEntityHistoryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.testcontainers.containers.GenericContainer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/*** Entity history test. */
@ExtendWith(ContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = TestApplication.class
)
public class HistoricalEntityTest {

	/**
	 * Postgres container.
	 */
	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	/**
	 * Artemis container.
	 */
	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	/**
	 * Object mapper.
	 */
	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Test entity service.
	 */
	@Autowired
	private TestHistoricalEntityService testHistoricalEntityService;

	/**
	 * Test entity history repository.
	 */
	@Autowired
	private TestHistoricalEntityHistoryRepository testHistoricalEntityHistoryRepository;

	/**
	 * Tests the history change tracking.
	 *
	 * @throws Exception If the test did not pass.
	 */
	@Test
	public void testEntityHistoryChange() throws Exception {
		// Creates a new test entity.
		final TestHistoricalEntity testHistoricalEntity1 = this.testHistoricalEntityService.save(new TestHistoricalEntity("1"));
		// Makes sure the new entity state is also replicated as historical data.
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> this.testHistoricalEntityHistoryRepository.findAll(), (
				entityHistoryList) -> IterableUtils.toList(entityHistoryList).stream().anyMatch((
						entity) -> {
					// Converts the state into an entity.
					final TestHistoricalEntity testHistoricalEntity = ObjectMapperHelper.convert(this.objectMapper, entity.getState(),
							new TypeReference<TestHistoricalEntity>() {}, false);
					// Compares the two entities.
					return testHistoricalEntity.equals(testHistoricalEntity1);
				}), TestHelper.LONG_WAIT, TestHelper.SHORT_WAIT));
		// Updates the test entity.
		testHistoricalEntity1.setTest("2");
		final TestHistoricalEntity testHistoricalEntity2 = this.testHistoricalEntityService.save(testHistoricalEntity1);
		// Makes sure the new entity state (and the old one) is also replicated as
		// historical data.
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> this.testHistoricalEntityHistoryRepository.findAll(), (
				entityHistoryList) -> IterableUtils.toList(entityHistoryList).stream().anyMatch((
						entity) -> {
					// Converts the state into an entity.
					final TestHistoricalEntity testHistoricalEntity = ObjectMapperHelper.convert(this.objectMapper, entity.getState(),
							new TypeReference<TestHistoricalEntity>() {}, false);
					// Compares the two entities.
					return testHistoricalEntity.equals(testHistoricalEntity1);
				}), TestHelper.LONG_WAIT, TestHelper.SHORT_WAIT));
		Assertions.assertTrue(TestHelper.waitUntilValid(() -> this.testHistoricalEntityHistoryRepository.findAll(), (
				entityHistoryList) -> IterableUtils.toList(entityHistoryList).stream().anyMatch((
						entity) ->

		{
					// Converts the state into an entity.
					final TestHistoricalEntity testHistoricalEntity = ObjectMapperHelper.convert(this.objectMapper, entity.getState(),
							new TypeReference<TestHistoricalEntity>() {}, false);
					// Compares the two entities.
					return testHistoricalEntity.equals(testHistoricalEntity2);
				}), TestHelper.LONG_WAIT, TestHelper.SHORT_WAIT));
	}

}

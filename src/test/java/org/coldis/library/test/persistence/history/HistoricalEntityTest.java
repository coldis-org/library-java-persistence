package org.coldis.library.test.persistence.history;

import org.apache.commons.collections4.IterableUtils;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.persistence.history.historical.repository.TestHistoricalEntityHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Entity history test.
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class HistoricalEntityTest {

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
	public void test00EntityHistory() throws Exception {
		// Creates a new test entity.
		final TestHistoricalEntity testHistoricalEntity1 = this.testHistoricalEntityService
				.save(new TestHistoricalEntity("1"));
		// Makes sure the new entity state is also replicated as historical data.
		TestHelper.waitUntilValid(() -> this.testHistoricalEntityHistoryRepository.findAll(), entityHistoryList -> {
			return IterableUtils.toList(entityHistoryList).stream()
					.anyMatch(entity -> entity.getState().equals(testHistoricalEntity1));
		}, TestHelper.REGULAR_WAIT, TestHelper.SHORT_WAIT);
		// Updates the test entity.
		testHistoricalEntity1.setTest("2");
		final TestHistoricalEntity testHistoricalEntity2 = this.testHistoricalEntityService.save(testHistoricalEntity1);
		// Makes sure the new entity state (and the old one) is also replicated as
		// historical data.
		TestHelper.waitUntilValid(() -> this.testHistoricalEntityHistoryRepository.findAll(), entityHistoryList -> {
			return IterableUtils.toList(entityHistoryList).stream()
					.anyMatch(entity -> entity.getState().equals(testHistoricalEntity1));
		}, TestHelper.REGULAR_WAIT, TestHelper.SHORT_WAIT);
		TestHelper.waitUntilValid(() -> this.testHistoricalEntityHistoryRepository.findAll(), entityHistoryList -> {
			return IterableUtils.toList(entityHistoryList).stream()
					.anyMatch(entity -> entity.getState().equals(testHistoricalEntity2));
		}, TestHelper.REGULAR_WAIT, TestHelper.SHORT_WAIT);
	}

}

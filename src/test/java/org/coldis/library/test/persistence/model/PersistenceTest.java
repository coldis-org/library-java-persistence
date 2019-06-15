package org.coldis.library.test.persistence.model;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Persistence model test.
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, properties = "test.properties")
public class PersistenceTest {

	/**
	 * Test entity service.
	 */
	@Autowired
	private TestEntityService testEntityService;

	/**
	 * Tests timestamp and expiration.
	 */
	@Test
	public void test00TimestampAndExpiration() {
		// Creates a new test entity.
		TestEntity testEntity = new TestEntity();
		// Asserts that there is no information on id, creation, update or expiration.
		Assertions.assertNull(testEntity.getId());
		Assertions.assertNull(testEntity.getCreatedAt());
		Assertions.assertNull(testEntity.getUpdatedAt());
		Assertions.assertNull(testEntity.getExpiredAt());
		// Saves the entity.
		testEntity = this.testEntityService.save(testEntity);
		// Gets the last update date/time.
		final LocalDateTime updatedAt1 = testEntity.getUpdatedAt();
		// Makes sure the id, creation and last update have been set.
		Assertions.assertNotNull(testEntity.getId());
		Assertions.assertNotNull(testEntity.getCreatedAt());
		Assertions.assertNotNull(testEntity.getUpdatedAt());
		// Sets an expired expiration date.
		testEntity.setExpiredAt(testEntity.getCreatedAt());
		// Saves the entity.
		testEntity = this.testEntityService.save(testEntity);
		// Gets the last update date/time.
		final LocalDateTime updatedAt2 = testEntity.getUpdatedAt();
		// Makes sure the expiration has been set and entity is expired.
		Assertions.assertNotNull(testEntity.getExpiredAt());
		Assertions.assertTrue(testEntity.getExpired());
		// Sets a non-expired expiration date.
		testEntity.setExpiredAt(testEntity.getCreatedAt().plusDays(2));
		// Saves the entity.
		testEntity = this.testEntityService.save(testEntity);
		// Gets the last update date/time.
		final LocalDateTime updatedAt3 = testEntity.getUpdatedAt();
		// Makes sure the expiration has been set and entity is not expired.
		Assertions.assertNotNull(testEntity.getExpiredAt());
		Assertions.assertFalse(testEntity.getExpired());
		// Asserts that the last update was updated correctly.
		Assertions.assertTrue(updatedAt1.isBefore(updatedAt2));
		Assertions.assertTrue(updatedAt2.isBefore(updatedAt3));
	}

}

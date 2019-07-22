package org.coldis.library.test.persistence.model;

import java.time.LocalDateTime;

import org.coldis.library.test.persistence.TestApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Persistence model test.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "test.properties",
classes = TestApplication.class)
public class PersistenceTest {

	/**
	 * Test entity repository.
	 */
	@Autowired
	private TestEntityRepository testEntityRepository;

	/**
	 * Test entity service.
	 */
	@Autowired
	private TestEntityService testEntityService;

	/**
	 * Tests timestamp and expiration.
	 */
	@Test
	public void testTimestampAndExpiration() {
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

	/**
	 * Tests typed object converter.
	 */
	@Test
	public void testTypedObjectConverter() {
		// Creates a new test entity.
		TestEntity testEntity = new TestEntity();
		testEntity.setAttribute1(new TestObject());
		testEntity.getAttribute1().setAttribute1("1");
		testEntity.getAttribute1().setAttribute2("2");
		testEntity.getAttribute1().setAttribute3(new TestObject());
		testEntity.getAttribute1().getAttribute3().setAttribute1("1");
		testEntity.getAttribute1().getAttribute3().setAttribute2("2");
		// Saves the entity.
		testEntity = this.testEntityService.save(testEntity);
		testEntity = this.testEntityRepository.findById(testEntity.getId()).orElse(null);
		// Makes sure the id, creation and last update have been set.
		Assertions.assertNotNull(testEntity.getId());
		Assertions.assertNotNull(testEntity.getCreatedAt());
		Assertions.assertNotNull(testEntity.getUpdatedAt());
		// Asserts that the data has been persisted.
		Assertions.assertNull(testEntity.getAttribute1().getAttribute1());
		Assertions.assertEquals("2", testEntity.getAttribute1().getAttribute2());
		Assertions.assertNull(testEntity.getAttribute1().getAttribute3().getAttribute1());
		Assertions.assertEquals("2", testEntity.getAttribute1().getAttribute3().getAttribute2());
		Assertions.assertNull(testEntity.getAttribute2());
	}

}

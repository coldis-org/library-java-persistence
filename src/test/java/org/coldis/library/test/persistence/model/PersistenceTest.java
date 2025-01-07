package org.coldis.library.test.persistence.model;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.coldis.library.test.StartTestWithContainerExtension;
import org.coldis.library.test.StopTestWithContainerExtension;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.persistence.TestApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.testcontainers.containers.GenericContainer;

/**
 * Persistence model test.
 */
@ExtendWith(StartTestWithContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "test.properties",
		classes = TestApplication.class
)
@ExtendWith(StopTestWithContainerExtension.class)
public class PersistenceTest {

	/**
	 * Postgres container.
	 */
	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	/**
	 * Artemis container.
	 */
	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

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
	public void testTypableConverter() {
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
		// Asserts that the data has been persisted.
		Assertions.assertNull(testEntity.getAttribute1().getAttribute1());
		Assertions.assertEquals("2", testEntity.getAttribute1().getAttribute2());
		Assertions.assertNull(testEntity.getAttribute1().getAttribute3().getAttribute1());
		Assertions.assertEquals("2", testEntity.getAttribute1().getAttribute3().getAttribute2());
		Assertions.assertNull(testEntity.getAttribute2());
	}

	/**
	 * Tests typed object list converter.
	 */
	@Test
	public void testTypableListConverter() {
		// Creates a new test entity.
		TestEntity testEntity = new TestEntity();
		testEntity.setAttribute3(new ArrayList<>());
		TestObject testObject = new TestObject();
		testObject.setAttribute1("11");
		testObject.setAttribute2("12");
		testEntity.getAttribute3().add(testObject);
		testObject = new TestObject();
		testObject.setAttribute1("21");
		testObject.setAttribute2("22");
		testEntity.getAttribute3().add(testObject);
		// Saves the entity.
		testEntity = this.testEntityService.save(testEntity);
		testEntity = this.testEntityRepository.findById(testEntity.getId()).orElse(null);
		// Asserts that the data has been persisted.
		Assertions.assertNull(testEntity.getAttribute3().get(0).getAttribute1());
		Assertions.assertEquals("12", testEntity.getAttribute3().get(0).getAttribute2());
		Assertions.assertNull(testEntity.getAttribute3().get(1).getAttribute1());
		Assertions.assertEquals("22", testEntity.getAttribute3().get(1).getAttribute2());
	}

}

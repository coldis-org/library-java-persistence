package org.coldis.library.test.persistence.keyvalue;

import java.util.List;

import org.coldis.library.persistence.keyvalue.KeyValue;
import org.coldis.library.persistence.keyvalue.KeyValueRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

/**
 * Key/value test.
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class KeyValueTest {

	/**
	 * Test data.
	 */
	private static final List<KeyValue<TestValue>> TEST_DATA = List.of(new KeyValue<>("1", new TestValue("1", 1L)),
			new KeyValue<>("2", new TestValue("2", 2L)), new KeyValue<>("3", new TestValue("3", 3L)));

	/**
	 * Key/value repository.
	 */
	@Autowired
	private KeyValueRepository<TestValue> keyValueRepository;

	/**
	 * Saves the key/value.
	 *
	 * @param  keyValue Key/value.
	 * @return          The saved key/value.
	 */
	@Transactional
	public KeyValue<TestValue> save(final KeyValue<TestValue> keyValue) {
		return this.keyValueRepository.save(keyValue);
	}

	/**
	 * Tests the key/value persistence
	 */
	@Test
	public void testKeyValuePersistence() {
		// For each test object.
		for (final KeyValue<TestValue> keyValue : KeyValueTest.TEST_DATA) {
			// Saves the value.
			this.save(keyValue);
			// Makes sure the value is persisted correctly.
			Assertions.assertEquals(keyValue, this.keyValueRepository.findById(keyValue.getKey()).orElse(null));
		}
	}
}

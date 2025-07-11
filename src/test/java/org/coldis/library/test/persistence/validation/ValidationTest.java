package org.coldis.library.test.persistence.validation;

import java.util.ArrayList;
import java.util.List;

import org.coldis.library.model.view.ModelView;
import org.coldis.library.test.SpringTestHelper;
import org.coldis.library.test.StartTestWithContainerExtension;
import org.coldis.library.test.StopTestWithContainerExtension;
import org.coldis.library.test.TestHelper;
import org.coldis.library.test.TestWithContainer;
import org.coldis.library.test.persistence.TestApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;

import jakarta.validation.Validator;

/**
 * Persistence model test.
 */
@TestWithContainer
@ExtendWith(StartTestWithContainerExtension.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "test.properties",
		classes = TestApplication.class
)
@ExtendWith(StopTestWithContainerExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ValidationTest extends SpringTestHelper {

	/**
	 * Postgres container.
	 */
	public static GenericContainer<?> POSTGRES_CONTAINER = TestHelper.createPostgresContainer();

	/**
	 * Artemis container.
	 */
	public static GenericContainer<?> ARTEMIS_CONTAINER = TestHelper.createArtemisContainer();

	/**
	 * Validator.
	 */
	@Autowired
	private Validator validator;

	/**
	 * Tests the required attributes annotation.
	 */
	@Test
	public void testRequiredAttributes() {
		// Creates an empty object.
		final TestObject testObject = new TestObject();
		// It should be valid since no attribute defined as required.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 0);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 0);
		// Sets the first attribute as mandatory.
		TestObject.REQUIRED_ATTRIBUTES = List.of("id");
		// Makes sure the validation does not pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 1);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 1);
		// Fills the attribute.
		testObject.setId(1L);
		// Makes sure the validation does pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 0);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 0);
		// Sets the second attribute as mandatory.
		TestObject.REQUIRED_ATTRIBUTES = List.of("id", "attribute1");
		// Makes sure the validation does not pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 1);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 1);
		// Fills the attribute with an empty string.
		testObject.setAttribute1("");
		// Makes sure the validation does not pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 1);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 1);
		// Fills the attribute.
		testObject.setAttribute1("abc");
		// Makes sure the validation does pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 0);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 0);
		// Sets the third attribute as mandatory.
		TestObject.REQUIRED_ATTRIBUTES = List.of("id", "attribute1", "attribute2");
		// Makes sure the validation does not pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 1);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 1);
		// Fills the attribute with an empty list.
		testObject.setAttribute2(new ArrayList<>());
		// Makes sure the validation does not pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 1);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 1);
		// Adds one item to the attribute list.
		testObject.getAttribute2().add(new TestObject());
		// Makes sure the validation does pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 0);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 0);
		// Un-fills the attribute.
		testObject.setId(null);
		// Makes sure the validation does not pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 1);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 1);
		// Sets the fourth attribute as mandatory.
		TestObject.REQUIRED_ATTRIBUTES = List.of("attribute3");
		// Sets the object as false.
		testObject.setAttribute3(false);
		// Makes sure the validation does not pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 1);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 1);
		// Sets the object as false.
		testObject.setAttribute3(true);
		// Makes sure the validation does pass.
		Assertions.assertTrue(this.validator.validate(testObject).size() == 0);
		Assertions.assertTrue(this.validator.validate(testObject, ModelView.Sensitive.class).size() == 0);

	}

}

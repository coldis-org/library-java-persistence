package org.coldis.library.test.persistence.settings;

import org.coldis.library.persistence.settings.QuerySetting;
import org.coldis.library.persistence.settings.QuerySettings;

/**
 * Invalid {@link QuerySettings} usages for the start-up check (not beans of the test context).
 */
public final class QuerySettingsFixtures {

	/**
	 * No instances.
	 */
	private QuerySettingsFixtures() {
	}

	/** Sets a parameter outside the allow-list. */
	public static class DisallowedSetting {

		/** Annotated method. */
		@QuerySettings(settings = { @QuerySetting(name = "search_path", value = "public") })
		public void run() {
		}

	}

	/** Sets the same parameter twice. */
	public static class DuplicateSetting {

		/** Annotated method. */
		@QuerySettings(
				workMem = "1MB",
				settings = { @QuerySetting(name = "WORK_MEM", value = "2MB") }
		)
		public void run() {
		}

	}

	/** Sets nothing. */
	public static class NoSetting {

		/** Annotated method. */
		@QuerySettings
		public void run() {
		}

	}

	/** Targets a datasource that does not exist. */
	public static class UnknownDatasource {

		/** Annotated method. */
		@QuerySettings(
				workMem = "1MB",
				datasource = "missing"
		)
		public void run() {
		}

	}

	/** Interface (not a repository) with an annotated method: nothing would apply it. */
	public interface AnnotatedInterface {

		/** Annotated method. */
		@QuerySettings(workMem = "1MB")
		void run();

	}

	/** Implementation of the annotated interface. */
	public static class AnnotatedInterfaceImplementation implements AnnotatedInterface {

		/** Not annotated itself. */
		@Override
		public void run() {
		}

	}

}

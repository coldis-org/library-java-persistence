package org.coldis.library.persistence;

/**
 * Lock behavior.
 */
public enum LockBehavior {

	NO_LOCK,

	WAIT_AND_LOCK,

	LOCK_FAIL_FAST,

	LOCK_SKIP,

	;

}

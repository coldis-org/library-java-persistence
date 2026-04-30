package org.coldis.library.persistence.lock;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Internal lock-table marker row used by {@link LockServiceComponent} when {@link LockType#TABLE}
 * is requested. Each acquired lock corresponds to a row inserted at acquire time and deleted
 * before the surrounding transaction commits, so the table stays empty in steady state.
 *
 * <p>The id is a single string formed by the caller's namespace and key concatenated with a
 * delimiter; real string equality avoids the hash-collision risk of advisory locks.
 *
 * <p><b>This is intentionally a marker entity, not a domain model.</b> It carries no behavior and
 * exists only so {@code lock_key} appears in JPA's schema generation; the row's mere presence /
 * absence under a transaction's xmin is what serializes lock acquisition. Adding business logic
 * here would be inappropriate.
 */
@Entity
@Table(name = "lock_key")
public class LockKey implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;

	public LockKey() {
	}

	public LockKey(final String id) {
		this.id = id;
	}

	@Id
	@Column(name = "id")
	public String getId() {
		return this.id;
	}

	public void setId(
			final String id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}

	@Override
	public boolean equals(
			final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LockKey)) {
			return false;
		}
		return Objects.equals(this.id, ((LockKey) obj).id);
	}

}

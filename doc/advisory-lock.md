# Advisory Lock

PostgreSQL transaction-scoped advisory locks for arbitrary string keys. Useful when you need to serialize concurrent writers on application-defined keys without locking actual table rows — for example, narrowing the race window on cross-instance batched inserts.

## Class

`org.coldis.library.persistence.lock.AdvisoryLockServiceComponent`

```java
@Service
public class AdvisoryLockServiceComponent {
    public static final int DEFAULT_NAMESPACE = 0;

    public void lockKeys(Collection<String> keys);
    public void lockKeys(int namespace, Collection<String> keys);
}
```

## Behavior

- **Transaction-scoped.** Calls `pg_advisory_xact_lock(namespace, hashtext(key))`. Locks are released automatically on `COMMIT` or `ROLLBACK` of the surrounding transaction.
- **Caller is responsible for the transaction.** Must be called from inside an active transaction (typically a `@Transactional` method on the caller). When called outside a transaction, the JDBC connection runs in autocommit and the lock is released as soon as the statement completes — defeating the purpose.
- **Single round-trip.** Issues one SQL statement that acquires every lock in the batch.
- **Deadlock-free across overlapping batches.** Keys are sorted by hash before acquisition, so two concurrent batches with overlapping keys always lock them in the same order.
- **Locks nothing in the database.** Advisory locks are application-defined mutexes tracked by the Postgres lock manager — they do not lock rows, tables, indexes, or anything else readable.

## Namespaces

The two-argument form `lockKeys(int namespace, Collection<String> keys)` lets callers reserve a numeric namespace per subsystem. Different namespaces never collide, even on identical key strings.

```java
public final class StatisticsLockNamespace {
    public static final int VALUE = 0x53544154; // "STAT"
    private StatisticsLockNamespace() {}
}

advisoryLockService.lockKeys(StatisticsLockNamespace.VALUE, eventKeys);
```

The default namespace `0` is fine when there is no risk of cross-subsystem hash collisions on the key string.

## Usage

```java
@Service
public class MyServiceComponent {

    @Autowired
    private AdvisoryLockServiceComponent advisoryLockService;

    @Transactional
    public void processBatch(List<MyKey> keys) {
        final List<String> lockKeys = keys.stream()
                .map(k -> "my-subsystem:" + k.toLockString())
                .toList();
        this.advisoryLockService.lockKeys(MY_NAMESPACE, lockKeys);
        // ... reads, writes, computations under the lock ...
        // Locks released automatically when this @Transactional method commits.
    }
}
```

## When to use

- **Cross-instance race on missing rows.** Two service instances both inserting the same brand-new key would otherwise collide on a unique constraint. Acquiring an advisory lock on the key serializes them per-key without serializing on a shared row or table.
- **Coordinating work that does not have a natural row to `SELECT FOR UPDATE`.** For example, an insert that has not happened yet, or a key that spans multiple tables.
- **Avoiding row-lock escalation.** Advisory locks are O(1) per lock and require no I/O, table scan, or index lookup.

## When not to use

- The work already operates on an existing row → use `SELECT ... FOR UPDATE` instead. Row locks are visible in the standard locking views and integrate with cascades.
- The lock must outlive the transaction → use `pg_advisory_lock` (session-scoped) directly. This component intentionally only exposes the transaction-scoped variant.
- A non-PostgreSQL database is in play → advisory locks are PostgreSQL-specific.

## Caveats

- **Hash collisions are theoretically possible.** `hashtext(key)` is a 32-bit hash. With the two-argument form, the lock id is `(namespace, 32-bit hash)` so collisions are rare but possible across different keys within the same namespace. If unrelated keys collide they serialize unnecessarily, which is correct but suboptimal.
- **All writers on a key must take the lock.** If subsystem A acquires the lock for key K but subsystem B writes to the underlying row without taking the lock, serialization is broken. The lock is purely cooperative.
- **Cross-transaction propagation must be handled explicitly.** A nested `@Transactional(REQUIRES_NEW)` opens a separate transaction with a separate connection — advisory locks acquired there are released when that inner transaction commits, not when the outer one does.

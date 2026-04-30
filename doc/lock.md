# Lock Service

Transaction-scoped locks on arbitrary string keys via either Postgres advisory locks or row locks on a dedicated `lock_key` table. Used to serialize concurrent writers on application-defined keys without locking actual domain rows — e.g., narrowing the race window on cross-instance batched inserts.

## Class

`org.coldis.library.persistence.lock.LockServiceComponent`

```java
@Service
public class LockServiceComponent {
    public static final int DEFAULT_NAMESPACE = 0;
    public static final String LOCK_NOT_ACQUIRED_CODE = "lock.notacquired";

    // Convenience overloads — default to ADVISORY + WAIT_AND_LOCK.
    public void lockKeys(Collection<String> keys) throws BusinessException;
    public void lockKeys(int namespace, Collection<String> keys) throws BusinessException;
    public void lockKeys(String namespace, Collection<String> keys) throws BusinessException;

    // LockBehavior-aware overloads — default to ADVISORY.
    public boolean lockKeys(LockBehavior, int namespace, Collection<String> keys) throws BusinessException;
    public boolean lockKeys(LockBehavior, String namespace, Collection<String> keys) throws BusinessException;

    // Full API: explicit LockType.
    public boolean lockKeys(LockBehavior, LockType, int namespace, Collection<String> keys) throws BusinessException;
    public boolean lockKeys(LockBehavior, LockType, String namespace, Collection<String> keys) throws BusinessException;
}
```

## LockType — advisory vs table

The lock primitive is selectable per-call via `LockType`:

### `LockType.ADVISORY`

Postgres advisory locks via `pg_advisory_xact_lock` / `pg_try_advisory_xact_lock`.

- **Pros:** pure in-memory in Postgres' lock manager, O(1) acquisition, no heap or WAL I/O, single-round-trip batches. Best choice for high-frequency locking on a bounded set of subsystem labels.
- **Cons:** the `(namespace, key)` pair is hashed to a 64-bit id, so unrelated keys can theoretically collide. Negligible in practice for typical key cardinalities, but the consequences differ by `LockBehavior`:
  - `WAIT_AND_LOCK` — extra wait on an unrelated holder; self-corrects.
  - `LOCK_FAIL_FAST` — false failure surfaced as a `BusinessException`; loud and recoverable.
  - **`LOCK_SKIP` — silent skip of work that should have run; no signal, no retry.** Prefer `TABLE` when correctness under `LOCK_SKIP` matters.
- **Observability:** shows up in `pg_locks` with `locktype='advisory'` and the numeric id only — you cannot recover the original string key from the lock view.

### `LockType.TABLE`

Row-level locks on a dedicated `lock_key` table. Each acquired lock is an `INSERT INTO lock_key (id) VALUES (?) ON CONFLICT DO NOTHING` followed by a DELETE registered via Spring's `TransactionSynchronization.beforeCommit` hook — so the table stays empty in steady state.

- **Pros:** compares strings literally — **no hash collisions**. Lock state is visible in normal SQL views (a row exists in `lock_key` for every currently-held lock) and in `pg_locks` as standard row-level locks. Use when `LOCK_SKIP` correctness matters or when you need to audit lock activity.
- **Cons:** every acquisition costs a real INSERT (heap + index + WAL) and a DELETE, versus advisory's pure-memory operation. Non-blocking modes (`LOCK_SKIP`, `LOCK_FAIL_FAST`) are implemented via `SET LOCAL lock_timeout = '1ms'` around the INSERT and translate the resulting `55P03` (lock_not_available) error into the matching response — practically equivalent to `NOWAIT`, but not literally instantaneous.

## LockBehavior modes

Both `LockType` variants honor the same `LockBehavior` semantics:

| `LockBehavior`               | ADVISORY primitive             | TABLE primitive                                          | On contention                                             |
|------------------------------|--------------------------------|----------------------------------------------------------|-----------------------------------------------------------|
| `WAIT_AND_LOCK` / `NO_LOCK` / `null` | `pg_advisory_xact_lock`        | `INSERT … ON CONFLICT DO NOTHING` (blocks naturally)     | Blocks until acquired. Returns `true`.                    |
| `LOCK_SKIP`                  | `pg_try_advisory_xact_lock`    | `SET LOCAL lock_timeout = '1ms'` + INSERT, catch `55P03` | Returns `false` immediately if any key is held elsewhere. |
| `LOCK_FAIL_FAST`             | `pg_try_advisory_xact_lock`    | same as SKIP                                             | Throws `BusinessException` (code `lock.notacquired`).     |

For `LOCK_SKIP` and `LOCK_FAIL_FAST` on `ADVISORY`, partial acquisitions in a multi-key batch (some keys acquired, others not) remain held until tx end — harmless because advisory locks are cooperative and consume no row state.

## Behavior

- **Transaction-scoped.** Locks are released automatically on `COMMIT` or `ROLLBACK` of the surrounding transaction.
- **Caller is responsible for the transaction.** Must be invoked from inside an active transaction (typically a `@Transactional` method on the caller). When called outside a transaction, advisory locks autorelease at statement end (defeating the purpose), and `TABLE` mode's `beforeCommit` synchronization has nothing to register against.
- **Single round-trip per acquire.** ADVISORY issues one SQL statement that acquires every lock in the batch. TABLE issues one INSERT statement (and one DELETE before commit).
- **Deadlock-free across overlapping batches.** Keys within a single call are sorted (by hash for ADVISORY, alphabetically for TABLE) before locking, so two concurrent batches with overlapping keys always acquire them in the same order.
- **Locks nothing else in the database.** ADVISORY locks are application-defined mutexes tracked by the Postgres lock manager. TABLE locks only touch the dedicated `lock_key` table.

## Namespaces

Both `lockKeys(int namespace, …)` and `lockKeys(String namespace, …)` partition the lock keyspace per subsystem. For ADVISORY the string namespace is hashed to a 32-bit int via `String.hashCode()` (spec-stable across JVMs). For TABLE the namespace is concatenated with the key as `namespace:key` so different subsystems' keys never collide on the lock table.

```java
private static final String NAMESPACE = "statistics";

lockService.lockKeys(NAMESPACE, eventKeys);
```

The default namespace `0` is fine when there is no risk of cross-subsystem hash collisions on the key string.

For ADVISORY, PostgreSQL advisory locks always identify the lock by 64 bits total (two `int4`s, or one `bigint`); the two-argument form used here splits that as `(namespace_int4, hashtext(key)_int4)`. Birthday-paradox collision probability is in the lottery-ticket range for typical subsystem-label counts (~1 in 86M for 10 labels, ~1 in 860k for 100). The key-side collision risk per namespace has the same order of magnitude — and is the reason the SKIP-on-collision case can silently drop work.

## Usage

Blocking acquisition (most common case):

```java
@Service
public class MyServiceComponent {

    private static final String NAMESPACE = "my-subsystem";

    @Autowired
    private LockServiceComponent lockService;

    @Transactional
    public void processBatch(List<MyKey> keys) throws BusinessException {
        final List<String> lockKeys = keys.stream().map(MyKey::toLockString).toList();
        this.lockService.lockKeys(NAMESPACE, lockKeys);
        // ... reads, writes, computations under the lock ...
        // Locks released automatically when this @Transactional method commits.
    }
}
```

Pass-through `LockBehavior` (mirrors row-level lock semantics):

```java
@Transactional
public Result process(String key, LockBehavior behavior) throws BusinessException {
    final boolean acquired = lockService.lockKeys(behavior, NAMESPACE, List.of(key));
    Result result = null;
    if (acquired) {
        result = doWork(key);
    }
    return result;
}
```

Collision-free acquisition for SKIP-critical work:

```java
@Transactional
public void processIfFree(String key) throws BusinessException {
    final boolean acquired = lockService.lockKeys(LockBehavior.LOCK_SKIP, LockType.TABLE, NAMESPACE, List.of(key));
    if (acquired) {
        doWork(key);
    }
    // No silent-skip risk from hash collision — string equality on the lock_key table.
}
```

## When to use

- **Cross-instance race on missing rows.** Two service instances both inserting the same brand-new key would otherwise collide on a unique constraint. Acquiring a lock on the key serializes them per-key without serializing on a shared row or table.
- **Coordinating work that does not have a natural row to `SELECT FOR UPDATE`.** For example, an insert that has not happened yet, or a key that spans multiple tables.
- **Avoiding row-lock escalation.** ADVISORY locks are O(1) per lock and require no I/O, table scan, or index lookup.

## When not to use

- **The work already operates on an existing row** → use `SELECT ... FOR UPDATE` directly. Row locks are visible in the standard locking views and integrate with cascades.
- **The race is a missing-row insert and you own the target table** → use `INSERT ... ON CONFLICT DO NOTHING` directly on that table. Eliminates the race architecturally without any separate lock primitive. (See `KeyValueServiceComponent.lock` for an example — it uses `KeyValueRepository.insertIfAbsent` instead of acquiring a lock.)
- **The lock must outlive the transaction** → use session-scoped advisory variants directly. This component intentionally only exposes the transaction-scoped variants.
- **A non-PostgreSQL database is in play** → advisory locks are PostgreSQL-specific; the `ON CONFLICT` syntax used in TABLE mode is too.

## Caveats

- **All writers on a key must take the lock.** If subsystem A acquires the lock for key K but subsystem B writes to the underlying row without taking the lock, serialization is broken. The lock is purely cooperative.
- **Cross-transaction propagation must be handled explicitly.** A nested `@Transactional(REQUIRES_NEW)` opens a separate transaction with a separate connection — locks acquired there are released when that inner transaction commits, not when the outer one does.
- **TABLE mode requires a writable transaction.** The `beforeCommit` DELETE hook can't run in a read-only transaction, so TABLE locks must be acquired from a regular `@Transactional` (not `readOnly = true`).

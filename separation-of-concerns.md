# Separation of Concerns: Service vs ServiceComponent

Refactoring to align with the architecture rule: **Service** = interface/entry point (HTTP/JMS), **ServiceComponent** = implementation (business logic).

## Changes

### 1. KeyValueService (done)
- Renamed `KeyValueService` → `KeyValueServiceComponent`
- No API exposed, purely business logic with `@JmsListener`

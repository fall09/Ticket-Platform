# Architecture

> Status: DRAFT (Architecture session 1). Items marked **[PROPOSED]** are suggestions waiting for a decision. Items marked **[OPEN]** are not decided.

## 1. Style

A **modular monolith**: one application, one database, split into modules with strict boundaries. Microservices were considered and postponed. The first modules to move out into separate services are `notification` and `reservation-expiration` (Phase 4), documented with an ADR that records why and what it cost. The boundaries below are drawn so that this move is possible without rewriting the business logic.

## 2. Technology (decided so far)

| Area | Choice |
|---|---|
| Language and build | Java 25 (LTS), Maven |
| Framework | Spring Boot 4.x (supports Java 17 to 26, checked on docs.spring.io) |
| Database | PostgreSQL |
| Migrations | Flyway (plain SQL files, one schema per module) |
| Security | Spring Security with JWT |
| API contract | OpenAPI (`docs/api/openapi.yaml`), contract first |
| Messaging | RabbitMQ with the transactional outbox, from Release 3 |
| Tests | JUnit 5, Mockito, Testcontainers (see `test-plan.md`) |
| Local environment | Docker Compose |

## 3. Modules and data ownership

| Module | Responsibility | Owns (tables) |
|---|---|---|
| `identity` | Registration, login, roles, organizer applications, account lockout | user, organizer_application |
| `catalog` | Venues, events, ticket categories (definition, price, capacity), browsing | venue, event, ticket_category |
| `booking` | Reservations, per-category stock counters, per-user limit, overselling protection | category_stock, reservation, reservation_line |
| `payment` | Simulated payments, attempts, amount components, refunds | payment, payment_component, refund |
| `ticketing` | Purchases (booking code), issued tickets, QR code, ticket verification | purchase, ticket |
| `notification` | E-mail templates, sending, retries, failed messages; channel-agnostic | notification_message |
| `reservation-expiration` | Periodic job that releases expired reservations (no tables of its own) | none |
| shared kernel | Audit log, configuration, outbox, error format, clock | audit_log, outbox_event |

Decisions behind this table:
- `payment` and `ticketing` are separate modules: one handles money, the other tickets, and they fail in different ways (a payment can succeed while ticket issuing fails, which needs a refund).
- Venues are their own shared record (FR-VEN-1).
- Stock counters (reserved, sold) belong to `booking`, not to `catalog`. `catalog` owns the capacity and gets the remaining tickets through an interface that `booking` implements (see section 4). This keeps the overselling rule in one module.

## 4. Rules between modules

1. A module reads and writes only its own tables. No SQL or repository access across modules.
2. A module talks to another one only through that module's public interface (a small set of service interfaces and data transfer objects). Everything else in a module is package-private.
3. Releases 1 and 2: calls between modules are plain method calls through these interfaces, inside one transaction where needed.
4. From Release 3: the flows that must not depend on each other (notifications, refunds after cancellation) use events through the outbox and RabbitMQ.
5. No circular dependencies between modules.
6. An architecture test fails the build when a rule is broken.

Allowed dependencies **[PROPOSED]** (an arrow means "uses the public interface of"):

```
identity     <- everything may read the current user
booking      -> catalog (price, capacity, event rules)
payment      -> booking (reservation), ticketing (create purchase and tickets)
ticketing    -> booking, catalog
notification -> receives events only
reservation-expiration -> booking
```

`catalog` shows "remaining tickets" but must not depend on `booking`, otherwise the two modules would depend on each other in a circle. The fix: `catalog` defines a tiny interface of its own ("give me the remaining tickets for these categories") and `booking` implements it. In the code, `catalog` knows only its own interface; `booking` plugs in. This is the dependency inversion principle, and the architecture test checks that no circle exists.

Capacity changes go the other way: when a category is created or its capacity changes, `catalog` publishes an in-process event (for example `TicketCategoryCreated`, `CapacityChanged`) and `booking` listens and updates its own copy of the capacity in `category_stock`. The stock check (reserved + sold + requested must not exceed capacity) is then one atomic update inside `booking`. Decided by Eylül (Oct 19).

## 5. Main flow: reserve and pay (Release 1, synchronous)

1. The user sends a reservation request to `booking`.
2. `booking` asks `catalog` for the event rules and the current category prices.
3. `booking`, in one transaction, checks the per-user limit and updates the stock counters so that reserved plus sold never exceeds the capacity, then stores the reservation with frozen prices.
4. The user pays: `payment` checks the reservation through `booking`, runs the simulated payment, stores the amount components.
5. On success, `ticketing` creates the purchase with its booking code and one ticket per unit, and `booking` turns reserved into sold.
6. The user reads their tickets from `ticketing`.

Release 2 hardens this flow (idempotency, retries, late payment, compensation). Release 3 turns steps after payment success into events.

## 6. Open points

- **[OPEN]** How the concurrency control is done in `booking` (atomic update, optimistic or pessimistic locking). This is decided in an ADR when the story is built, after measuring.
- **[OPEN]** Package layout inside a module.
- **[OPEN]** Error format and pagination format (decided with the OpenAPI contract).

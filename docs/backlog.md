# Backlog

> Status: DRAFT (PM session 1). Order, priorities and steps are a plan, not a promise. They are adjusted after each step based on what we learn.

## 1. How this backlog is organized

- Work is ordered in **vertical slices**: the thinnest end-to-end flow first, then each slice adds depth. Every release ends with a system that runs and can be tested.
- Each item is a user story from `user-stories.md` (ID and title) or a technical enabler (TE-), which is needed work that is not a user story.
- Priority uses MoSCoW: **Must** (the release is useless without it), **Should** (important, but the release still works), **Could** (nice to have).
- There are no time estimates. Eylül decides the pace; the order and the scope of each step matter, not the dates.

## 2. Release 1: "I can buy a ticket" (MVP core)

Goal: a registered user reserves tickets for a published event, pays (simulated) and sees their tickets. Everything runs synchronously, there is no message broker yet.

| ID | Item | Priority |
|---|---|---|
| TE-1 | Project skeleton (Spring Boot, Maven, Java 21), package-by-module structure, PostgreSQL via Docker Compose, Flyway migrations | Must |
| TE-2 | OpenAPI contract for Release 1 endpoints (`docs/api/openapi.yaml`), consistent error format | Must |
| TE-3 | CI pipeline (build and tests on every push) | Should |
| US-ACC-1 | Register | Must |
| US-ACC-2 | Log in (JWT) | Must |
| US-ACC-7 | First admin account | Must |
| US-ACC-5 | Apply to become an organizer | Must |
| US-ACC-6 | Review organizer applications | Must |
| US-EVT-1 | Create an event | Must |
| US-EVT-2 | Add ticket categories | Must |
| US-EVT-3 | Publish an event | Must |
| US-EVT-5 | Manage only my own events (ownership checks; admin audit log comes in Release 3) | Must |
| US-BRW-1 | See upcoming events (paging, no filters yet) | Must |
| US-BRW-3 | View event details | Must |
| US-BRW-4 | Browse without logging in (including the config switch) | Must |
| US-RES-1 | Reserve tickets (several categories, frozen price) | Must |
| US-RES-2 | Respect the per-user ticket limit | Must |
| US-RES-3 | Cancel my own reservation | Should |
| US-RES-4 | Expire reservations automatically (`@Scheduled`) | Must |
| US-RES-5 | Never oversell (concurrency tests included) | Must |
| US-PAY-1 | Pay for a reservation (simulated; booking code, ticket codes, price components) | Must |
| US-PAY-6 | See my purchases and tickets | Must |

Done when: the whole flow works through the API, a concurrency test proves no overselling, and Swagger UI shows the contract.

## 3. Release 2: "Money is handled reliably"

Goal: payments survive retries, duplicates and timing problems.

| ID | Item | Priority |
|---|---|---|
| US-PAY-2 | Retry a failed payment (max 3) | Must |
| US-PAY-3 | No duplicate payments or tickets (idempotency) | Must |
| US-PAY-4 | Late payment after expiry | Must |
| US-PAY-5 | Undo the payment if ticket issuing fails (compensation) | Must |
| US-RES-6 | See my reservations | Could |
| US-EVT-4 | Edit a published event | Should |
| US-EVT-7 | Event completion (scheduled job) | Should |
| US-BRW-2 | Search and filter events (Specification, combined filters) | Should |
| TE-4 | Integration tests with Testcontainers for the money flows | Must |

## 4. Release 3: "Asynchronous and event-driven"

Goal: notifications and refunds run through RabbitMQ with the transactional outbox, retries and a dead-letter queue. This is where the notification module becomes channel-agnostic (and the first candidate for extraction in Phase 4).

| ID | Item | Priority |
|---|---|---|
| TE-5 | RabbitMQ in Docker Compose, outbox table and publisher, idempotent consumers, event catalog (`docs/events.md`) | Must |
| US-NTF-1 | Get e-mails for important events (purchase e-mail first) | Must |
| US-NTF-2 | A failed e-mail never breaks my purchase (retry, DLQ) | Must |
| US-NTF-6 | Prepare for more channels and languages (templates, strategy) | Should |
| US-NTF-3 | Inspect failed notifications (admin) | Could |
| US-CXL-1 | Cancel an event (also covers US-EVT-6) | Must |
| US-CXL-2 | Refund everything on cancellation | Must |
| US-CXL-3 | Retry failed refunds | Must |
| US-CXL-4 | Handle pending refunds (admin) | Should |
| TE-6 | Audit log for admin actions on other people's data (AOP) | Should |

## 5. Release 4: "Entrance, protection and polish"

| ID | Item | Priority |
|---|---|---|
| US-PAY-7 | Get a QR code for a ticket | Should |
| US-PAY-8 | Verify a ticket at the entrance (parallel scans: exactly one success) | Should |
| US-NTF-4 | Get a reminder before the event (scheduled job, no duplicates) | Should |
| US-NTF-5 | Add the event to my calendar (`.ics`) | Could |
| US-ACC-3 | Progressive account lockout | Should |
| US-ACC-4 | Lockout notification and audit log | Should |
| TE-7 | Observability: correlation ID, metrics, Prometheus and Grafana | Should |
| TE-8 | Load test (1,000 concurrent users), baseline first, then a p95 target | Should |
| TE-9 | README, architecture diagram, ERD, Postman collection, ADRs finished | Must |

## 6. Later (not planned yet)

- Extract notification and reservation-expiration into separate services (Phase 4, with an ADR on why and what it cost).
- Price tiers (early bird by date or sold percentage).
- Refund policies defined by the organizer (paid refund right, time-based percentages, non-refundable extras). The purchase already stores amount components for this.
- Turkish notification templates and a user language setting.
- Favorites and reminders for logged-in users.
- Coupons, student discount, membership and loyalty program.
- Seating plan and seat selection.
- Advanced event search.
- SMS as a second channel.
- IP-based rate limiting for login.
- Frontend (user web app and admin panel) on top of the OpenAPI contract.

## 7. Step plan (no dates)

The work is split into steps in order. A step is finished when its items are done, not when a date arrives. Eylül moves to the next step at her own pace.

| Step | Candidate content |
|---|---|
| 1 (now) | Analysis and planning (this documentation). Still to do: architecture notes, ERD draft, doc stubs in the repo |
| 2 | TE-1, TE-2 (accounts part), US-ACC-1, US-ACC-2, US-ACC-7 |
| 3 | US-ACC-5, US-ACC-6, US-EVT-1, US-EVT-2, US-EVT-3, US-EVT-5 |
| 4 | US-BRW-1, US-BRW-3, US-BRW-4, start of US-RES-1 |
| 5 | US-RES-1 to US-RES-5, concurrency tests |
| 6 | US-PAY-1, US-PAY-6, Release 1 review |

Release 2 onward is planned after Release 1 is done.

## 8. Open points for planning

- Whether the architecture notes and the ERD are written in step 1 or become the first part of step 2.

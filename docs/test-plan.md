# Test Plan

> Status: DRAFT (Tester session 1). Strategy is complete. Test cases exist for Release 1 only; cases for Release 2 to 4 are added when those releases are planned (see `backlog.md`).

## 1. Purpose and principles

- Every acceptance criterion in `user-stories.md` has at least one test case. A test case name starts with the story ID, so a missing test is easy to find.
- The tester role works from `requirements.md`, `user-stories.md` and the OpenAPI contract (`docs/api/openapi.yaml`). Test cases are written **before** the code of the story, and are not adapted to the code afterwards. If the code and a test disagree, we first ask which of the two follows the requirement.
- Correctness rules are never negotiable: no overselling, no duplicate payments, no duplicate tickets, no lost refunds. They get the most tests, including parallel ones.
- Tests must be repeatable: every test starts from a known database state and does not depend on another test.

## 2. Test levels

| Level | What it checks | Tools | When |
|---|---|---|---|
| Unit | One class or method without database or network (validation rules, price calculation, limit rules, status changes) | JUnit 5, Mockito | With every story |
| Integration (API) | A real HTTP request through the whole application with a real PostgreSQL: status codes, JSON, security, database state | Spring Boot Test, MockMvc or WebTestClient, Testcontainers (PostgreSQL) | With every story |
| Concurrency | Many requests at the same moment against the same data (overselling, limits, double scans) | Java `ExecutorService` / CountDownLatch inside integration tests, Testcontainers | Stories that touch stock, limits, payments |
| Messaging | Messages are delivered, retried, moved to the DLQ, and processed once | Testcontainers (RabbitMQ) | Release 3 |
| Contract | The running API matches `openapi.yaml` | Schema validation in tests, Postman collection run | Every release |
| Load | 1,000 concurrent users at sale opening (NFR-LOAD-1), baseline first (NFR-PERF-1) | k6 or Gatling | Release 4 |
| Exploratory / manual | Trying the API like a user, looking for surprises | Swagger UI, Postman | End of every release |

## 3. Test environment and data

- Tests run against a throwaway PostgreSQL started by Testcontainers; no shared database, no real e-mail or payment service (both are simulated).
- Time is controlled: the application reads time from a replaceable clock, so tests about expiry, sales start and event end do not wait in real time.
- Standard test data (built by helper code, never by hand): one admin, two organizers (A and B), three users (U1, U2, U3), one published event with two categories (Standard: 100 tickets, VIP: 10 tickets) and limit N = 4.
- Secrets in tests are fake values; no real credentials anywhere in the repository.

## 4. Test case format and naming

- ID: `TC-<story id>-<number>`, for example `TC-RES-1-03`.
- Each case lists: the criterion it covers, the scenario, the expected result and the level.
- Priority follows the backlog: cases of Must stories are run on every build.

## 5. Entry and exit criteria

- **Entry (a story may be tested):** acceptance criteria are written, the endpoint is described in the OpenAPI contract, test cases for the story exist.
- **Exit (a story is done):** all its test cases pass, no open defect of severity High or Critical, the OpenAPI contract matches the behavior.
- **Exit (a release is done):** all stories of the release are done, the full regression suite is green, the exploratory session has no open High defect.

## 6. Defects

- A defect is written as: ID, title, steps to reproduce, expected, actual, severity (Critical: money or data is wrong; High: a main flow is broken; Medium: wrong behavior with a workaround; Low: cosmetic), story ID.
- Any defect found in production-like testing gets a regression test before it is closed.

## 7. Release 1 test cases

Notation: **U** unit, **I** integration (API), **C** concurrency.

### 7.1 Accounts

| ID | Criterion | Scenario | Expected | Level |
|---|---|---|---|---|
| TC-ACC-1-01 | 1 | Register with a new e-mail and a valid password | 201, account with role USER | I |
| TC-ACC-1-02 | 2 | Register again with the same e-mail | 409 conflict, still one account | I |
| TC-ACC-1-03 | 2 | Register with the same e-mail in different letter case | 409 conflict (e-mail compared without case) | I |
| TC-ACC-1-04 | 3 | Invalid e-mail format | 400, error names the e-mail field | I |
| TC-ACC-1-05 | 3 | Password with 7 characters | 400, error names the length rule | U, I |
| TC-ACC-1-06 | 3 | Password with 73 characters | 400, error names the length rule | U |
| TC-ACC-1-07 | 3 | Password without uppercase, without digit, without special character (one case each) | 400, each error names the broken rule | U |
| TC-ACC-1-08 | 3 | Password with exactly 8 and exactly 72 valid characters | Accepted | U |
| TC-ACC-1-09 | 4 | Read the password column and the registration response | Stored value is a hash, response has no password | I |
| TC-ACC-1-10 | FR-ROLE-4 | Register with a `role` field set to ADMIN | Field ignored or rejected, account is USER | I |
| TC-ACC-2-01 | 1 | Login with correct credentials | 200, token with user id and role | I |
| TC-ACC-2-02 | 2 | Wrong password | 401 generic error | I |
| TC-ACC-2-03 | 2 | Unknown e-mail | 401, the same message as the wrong password | I |
| TC-ACC-2-04 | NFR-SEC-1 | Call a protected endpoint without a token, with a broken token, with an expired token | 401 | I |
| TC-ACC-5-01 | 1 | USER applies | Application PENDING | I |
| TC-ACC-5-02 | 2 | USER applies twice | Second request rejected | I |
| TC-ACC-5-03 | 3 | ORGANIZER applies | Rejected | I |
| TC-ACC-5-04 | 4 | Applies after a rejection with "may not apply again" | Rejected | I |
| TC-ACC-5-05 | 5 | Applies after a rejection with "may apply again" | New PENDING application | I |
| TC-ACC-5-06 | 1 | Two parallel applications from the same user | Exactly one PENDING application exists | C |
| TC-ACC-6-01 | 1 | Admin approves | User gets ORGANIZER, status APPROVED | I |
| TC-ACC-6-02 | 2 | Admin rejects with reason and flag | Status REJECTED, user stays USER, user can read the reason | I |
| TC-ACC-6-03 | 2 | Admin rejects without a reason | 400 | I |
| TC-ACC-6-04 | 3 | USER or ORGANIZER tries to review | 403 | I |
| TC-ACC-6-05 | 4 | Review an application that is already APPROVED or REJECTED | Rejected | I |
| TC-ACC-6-06 | 1 | Two admins review the same application at the same time | Exactly one decision is saved | C |
| TC-ACC-7-01 | 1 | Start with an empty database and admin settings | Admin exists with role ADMIN | I |
| TC-ACC-7-02 | 2 | Start twice | Still one admin, nothing changed | I |
| TC-ACC-7-03 | 3 | Start without admin settings | No admin, warning in the log, application starts | I |
| TC-ACC-7-04 | 4 | Search logs and repository for the admin password | Not found | I |
| TC-ACC-7-05 | 5 | Try every public endpoint to create an admin or choose a role | None possible | I |

### 7.2 Events and ticket categories

| ID | Criterion | Scenario | Expected | Level |
|---|---|---|---|---|
| TC-EVT-1-01 | 1 | Organizer creates an event with all required fields | 201, status DRAFT, not in the public list | I |
| TC-EVT-1-02 | 2 | Event date in the past or now | 400 | U, I |
| TC-EVT-1-03 | 3 | Limit 0, 11, -1 | 400 each | U |
| TC-EVT-1-04 | 3 | Limit 1, 10 and no limit | Accepted; no limit gives 4 | U, I |
| TC-EVT-1-05 | 4 | Sales start equal to or after the event start | 400 | U |
| TC-EVT-1-06 | 5 | No sales end time | Sales end equals event start | U |
| TC-EVT-1-07 | 6 | Sales end after event start, no event end time | 400 | U |
| TC-EVT-1-08 | 6 | Sales end after event start, with an event end time; and sales end after the event end time | First accepted, second 400 | U |
| TC-EVT-1-09 | 7 | USER and anonymous try to create an event | 403 and 401 | I |
| TC-EVT-1-10 | FR-EVT-1 | Missing title or venue | 400 names the field | I |
| TC-EVT-2-01 | 1 | Add Standard and VIP to my event | Saved with price and quantity | I |
| TC-EVT-2-02 | 2 | Quantity 0, quantity -5, price -1 | 400 each | U |
| TC-EVT-2-03 | 4 | Price 0 | Accepted as a free category | U |
| TC-PAY-1-07 | 4 | Reserve and pay for a free category (amount 0) | Normal purchase: booking code, tickets, limit counted, stored amount 0 | I |
| TC-EVT-2-04 | 3 | Duplicate category name in the same event; same name in another event | First rejected, second accepted | I |
| TC-EVT-8-01 | 1 | Search venues by name and by city | Matching venues returned | I |
| TC-EVT-8-02 | 2 | Organizer creates a new venue | Saved, visible to another organizer | I |
| TC-EVT-8-03 | 3 | Create "Harbiye  Açıkhava!" when "harbiye açıkhava" exists in the same city | Nothing created, existing venue returned | U, I |
| TC-EVT-8-04 | 3 | Same name in a different city | New venue created | I |
| TC-EVT-8-05 | 4 | Create a similar name in the same city | Similar venues listed, nothing created until confirmed; with confirmation it is created | I |
| TC-EVT-8-06 | 5 | Create an event with a venue | Event city equals venue city | I |
| TC-EVT-8-07 | 6 | Missing name, address or city | 400 | I |
| TC-EVT-8-08 | 7 | USER creates a venue | 403 | I |
| TC-EVT-8-09 | 3 | Two parallel creations of the same venue | Exactly one venue exists | C |
| TC-EVT-3-01 | 1 | Publish a DRAFT event with one category | Status PUBLISHED, visible in the list | I |
| TC-EVT-3-02 | 2 | Publish without categories | Rejected | I |
| TC-EVT-3-03 | 3 | Publish with a future sales start | Visible, reserving rejected until the time (clock moved) | I |
| TC-EVT-3-04 | 4 | Publish without sales start | Reserving works at once | I |
| TC-EVT-3-05 | FR-EVT-2 | Publish an event that is already PUBLISHED or CANCELLED | Rejected | I |
| TC-EVT-5-01 | 1 | Organizer B changes, publishes or reads management data of A's event | 403 each | I |
| TC-EVT-5-02 | 2 | Admin does the same | Allowed | I |
| TC-EVT-5-03 | 3 | Admin action on A's event (audit log is Release 3) | Planned for Release 3 | I |

### 7.3 Browsing

| ID | Criterion | Scenario | Expected | Level |
|---|---|---|---|---|
| TC-BRW-1-01 | 1 | List with 3 published events | Earliest start first | I |
| TC-BRW-1-02 | 1 | 45 events, default page and page size 100, and page size 101 | Page of 20; 100 accepted; 101 capped or rejected as defined in OpenAPI | I |
| TC-BRW-1-03 | 2 | DRAFT, CANCELLED, COMPLETED and ended events exist | None appear | I |
| TC-BRW-1-04 | 3 | An entry in the list | Title, start, city, venue, lowest current price, sales status | I |
| TC-BRW-1-05 | 4 | Events before sales start, on sale, sold out, after sales end | Statuses NOT_YET_ON_SALE, ON_SALE, SOLD_OUT, SALES_ENDED | I |
| TC-BRW-1-06 | 5 | No events | 200 with an empty list | I |
| TC-BRW-3-01 | 1 | Open a published event | Description, venue, city, times, every category with price and remaining | I |
| TC-BRW-3-02 | 2 | Open a DRAFT event | 404 | I |
| TC-BRW-3-03 | 3 | Open a CANCELLED event | Visible with its status, reserving rejected | I |
| TC-BRW-3-04 | 4 | Remaining count after a reservation | Reflects sold plus reserved | I |
| TC-BRW-4-01 | 1 | Anonymous list and details | 200 | I |
| TC-BRW-4-02 | 2 | Anonymous reserve | 401 | I |
| TC-BRW-4-03 | 3 | Switch "login required for details" on; anonymous details and list | Details 401, list 200 | I |
| TC-BRW-4-04 | 3 | Same switch on, logged-in user | Details 200 | I |

### 7.4 Reservation

| ID | Criterion | Scenario | Expected | Level |
|---|---|---|---|---|
| TC-RES-1-01 | 1 | Reserve 2 Standard and 1 VIP | One reservation, two lines, expiry in 10 minutes | I |
| TC-RES-1-02 | 2 | Reserve, then organizer changes the price | Reservation keeps the old price, a new reservation gets the new one | I |
| TC-RES-1-03 | 3 | VIP has 1 left, request 2 Standard and 2 VIP | Nothing reserved, Standard stock unchanged, error names VIP | I |
| TC-RES-1-04 | 4 | N = 4, request 5 tickets in total, and 3 + 2 across two categories | Both rejected | I |
| TC-RES-1-05 | 5 | Event DRAFT, CANCELLED, COMPLETED, not yet on sale, sales ended | Each rejected | I |
| TC-RES-1-06 | 6 | No token | 401 | I |
| TC-RES-1-07 | 7 | Quantity 0, negative quantity, category of another event, unknown category | 400 or 404 each | I |
| TC-RES-1-08 | FR-RES-2 | Hold time changed in configuration | New reservations use the new value | I |
| TC-RES-2-01 | 1 | N = 4, hold 3 tickets, reserve 2; then reserve 1 | 2 rejected, 1 accepted | I |
| TC-RES-2-02 | 2 | Four reservations of 1 ticket, then a fifth | Four accepted, fifth rejected | I |
| TC-RES-2-03 | 3 | Expired or cancelled reservation, then reserve again | Quota is free again | I |
| TC-RES-2-04 | 4 | Two parallel requests of 3 tickets each with N = 4 | At most one succeeds | C |
| TC-RES-2-05 | 1 | Bought tickets plus a new reservation | Purchases count toward N | I |
| TC-RES-3-01 | 1 | Cancel my unpaid reservation | Stock and limit restored at once | I |
| TC-RES-3-02 | 2 | Cancel a paid, expired or already cancelled reservation | Rejected | I |
| TC-RES-3-03 | 3 | Cancel another user's reservation | 403 or 404 | I |
| TC-RES-3-04 | 1 | Cancel the same reservation twice in parallel | Stock restored exactly once | C |
| TC-RES-4-01 | 1 | Move the clock past the hold time, run the job | EXPIRED, stock restored | I |
| TC-RES-4-02 | 2 | Run the job twice, and two instances at once | Stock restored once | C |
| TC-RES-4-03 | 1 | Reservation not yet expired | Untouched by the job | I |
| TC-RES-4-04 | FR-RES-3 | Pay on an expired reservation | Rejected in Release 1; late payment handling comes in Release 2 | I |
| TC-RES-5-01 | 1 | 10 tickets left, 100 users reserve 1 each at once | Exactly 10 succeed, 90 are rejected, remaining is 0 | C |
| TC-RES-5-02 | 1 | 10 tickets left, 20 users reserve 2 each at once | Exactly 5 succeed, stock never negative | C |
| TC-RES-5-03 | 2 | Two users request {Standard, VIP} in opposite orders, repeated many times | No request hangs, no deadlock error remains, stock correct | C |
| TC-RES-5-04 | 3 | After each concurrency test | Sold plus reserved never exceeds quantity | C |
| TC-RES-5-05 | 1 | Same concurrency tests with reserve, cancel and expire mixed | Totals stay consistent | C |

### 7.5 Payment and tickets (Release 1 part)

| ID | Criterion | Scenario | Expected | Level |
|---|---|---|---|---|
| TC-PAY-1-01 | 1 | Pay for a reservation with 3 units, simulated success | 3 tickets with unique codes, one booking code | I |
| TC-PAY-1-02 | 2 | Look at the stored purchase | Amounts stored as components (ticket price, extras, fees), each marked refundable or not, at the frozen price | I |
| TC-PAY-1-03 | 4 | Pay on expired or cancelled reservation | Rejected | I |
| TC-PAY-1-04 | 5 | Pay for another user's reservation | 403 or 404 | I |
| TC-PAY-1-05 | 1 | Ticket codes of many purchases | No duplicates | U |
| TC-PAY-1-06 | 1 | After payment | Reserved turns into sold, remaining unchanged | I |
| TC-PAY-6-01 | 1 | Open my tickets | Each purchase with booking code, event, date; tickets with code, category, paid price, status VALID | I |
| TC-PAY-6-02 | 3 | Open another user's purchase or ticket | 404 | I |
| TC-PAY-6-03 | 4 | Look up own booking code, someone else's code, unknown code | Own found; others not found | I |
| TC-PAY-6-04 | 2 | Purchase exists, no e-mail was sent (no notifications in Release 1) | Everything still visible | I |

## 8. Traceability

Each test case lists the criterion number it covers. At the end of every step a short check is done: for each story in the step, every criterion has at least one passing test case. Gaps are written down in `progress.md`.

## 9. Open points

- Page size above 100: capped or rejected (decide when writing the OpenAPI contract).
- Test cases for Release 2 to 4 are written when each release starts.

## 10. Test status (updated as tests are written)

The case tables above are not changed when code is written; progress is tracked here. Status values: **Automated** (test class and method), **Manual** (tried by hand in Postman or curl, date), **Open** (not tried yet). Rows are added when work on a story starts.

### 10.1 Accounts

| ID | Status | Where / how | Date |
|---|---|---|---|
| TC-ACC-1-01 | Automated | `RegisterTests.registersNewUserWithRoleUser` | 2026-10-22 |
| TC-ACC-1-02 | Automated | `RegisterTests.givenExistingEmail_whenRegister_thenReturn409` | 2026-10-23 |
| TC-ACC-1-03 | Automated | `RegisterTests` (same e-mail in different letter case gives 409) | 2026-10-23 |
| TC-ACC-1-04 | Automated | `RegisterTests.givenInvalidEmail_whenRegister_thenReturn400` (400, title, first error names `email`) | 2026-10-23 |
| TC-ACC-1-05 to 1-08 | Manual (partly) | 400 with an `errors` list tried in Postman; each password rule and the 8/72 limits still to automate | 2026-10-20 |
| TC-ACC-1-09 | Automated | `RegisterTests.givenValidRequest_whenRegister_thenUserIsSavedWithHashedPassword` (no password in the response; stored value is not the plain password and starts with `$2`; OWASP A02). To strengthen: check the hash with `passwordEncoder.matches` | 2026-10-23 |
| TC-ACC-1-10 | Automated | `RegisterTests.givenRoleFieldInBody_whenRegister_thenRoleIsStillUser` (201, role is USER; OWASP A01 mass assignment) | 2026-10-23 |
| TC-ACC-2-01 | Manual | login returns a token | 2026-10-22 |
| TC-ACC-2-02 | Manual | wrong password gives 401 | 2026-10-22 |
| TC-ACC-2-03 | Manual | unknown e-mail gives 401 with the same message | 2026-10-22 |
| TC-ACC-2-04 | Manual (partly) | no token and broken token give 401; expired token not tried | 2026-10-22 |
| TC-ACC-7-01 | Manual | admin created from `ADMIN_EMAIL` / `ADMIN_PASSWORD`; login as admin shows role ADMIN | 2026-10-22 |
| TC-ACC-7-02 | Manual | second start creates nothing, one admin | 2026-10-22 |
| TC-ACC-7-03 | Manual | no settings: warning in the log, application starts | 2026-10-22 |
| TC-ACC-7-04 | Manual (partly) | password only goes into `encode`, not into log messages; `git grep` finds no password in the repository | 2026-10-22 |
| TC-ACC-7-05 | Manual (partly) | register ignores a `role` field (automated in TC-ACC-1-10); no other public endpoint creates users | 2026-10-23 |

# User Stories

> Status: DRAFT, work in progress (BA session 3). All seven epics are written; waiting for Eylül's review.

Format: **As a** `<role>`, **I want** `<goal>`, **so that** `<benefit>`.
Each story lists acceptance criteria in Given / When / Then form and links to the requirement IDs in `requirements.md`.
Exact endpoints, status codes and payloads are defined later in the OpenAPI contract (`docs/api/openapi.yaml`).

---

## Epic 1: Accounts and access

### US-ACC-1: Register

**As a** visitor, **I want** to create an account with my e-mail and a password, **so that** I can buy tickets.

Acceptance criteria:
1. **Given** an e-mail that is not registered and a valid password, **when** I register, **then** an account is created with the role USER.
2. **Given** an e-mail that is already registered, **when** I register, **then** the request is rejected with a conflict error and no second account is created.
3. **Given** an invalid e-mail or a password that breaks the password rules, **when** I register, **then** the request is rejected and the error tells me which field is wrong and, for the password, which rule was broken.
4. The password is never stored or returned in plain text.

Links: NFR-SEC-1, FR-AUTH-4.

### US-ACC-2: Log in

**As a** registered user, **I want** to log in with my e-mail and password, **so that** I can use the platform as myself.

Acceptance criteria:
1. **Given** correct credentials, **when** I log in, **then** I receive an access token that identifies me and my role.
2. **Given** wrong credentials, **when** I log in, **then** the request is rejected with a generic error that does not reveal whether the e-mail exists.
3. **Given** my account is temporarily locked, **when** I log in (even with the correct password), **then** the request is rejected with an error saying the account is temporarily locked.

Links: FR-AUTH-1.

### US-ACC-3: Progressive account lockout

**As a** user, **I want** my account to be locked for a short time after repeated wrong passwords, **so that** nobody can guess my password by trying again and again.

Acceptance criteria:
1. **Given** 3 failed logins in a row, **when** the 3rd one fails, **then** the account is locked for 1 minute.
2. **Given** the 1-minute lock has ended and 2 more logins fail, **when** the 2nd one fails, **then** the account is locked for 3 minutes.
3. **Given** further failures after that, **when** they happen, **then** the account is locked for 15 minutes.
4. **Given** a successful login, **when** it happens, **then** the failure counter is reset.
5. The numbers of attempts and the lock durations come from configuration, not from the code.

Links: FR-AUTH-1.

### US-ACC-4: Lockout notification and audit log

**As a** user, **I want** to be told when my account gets locked, **so that** I notice if someone is trying to break in.

Acceptance criteria:
1. **Given** my account gets locked, **when** the lock starts, **then** I receive one notification (simulated e-mail); not one per failed attempt.
2. **Given** any failed login, **when** it happens, **then** a log entry with time, username and IP address is written.
3. No log entry ever contains a password, not even a wrong one.

Links: FR-AUTH-2, FR-AUTH-3, FR-NTF-2.

### US-ACC-5: Apply to become an organizer

**As a** user, **I want** to apply to become an organizer, **so that** I can create and sell my own events.

Acceptance criteria:
1. **Given** I am a USER with no open application, **when** I apply, **then** an application with the status PENDING is created.
2. **Given** I already have a PENDING application, **when** I apply again, **then** the request is rejected.
3. **Given** I am already an ORGANIZER, **when** I apply, **then** the request is rejected.
4. **Given** my previous application was rejected and re-application was not allowed, **when** I apply, **then** the request is rejected.
5. **Given** my previous application was rejected and re-application was allowed, **when** I apply, **then** a new PENDING application is created.

Links: FR-ROLE-1.

### US-ACC-6: Review organizer applications

**As an** admin, **I want** to approve or reject organizer applications, **so that** only trusted people can publish events.

Acceptance criteria:
1. **Given** a PENDING application, **when** I approve it, **then** the applicant gets the ORGANIZER role and the application status becomes APPROVED.
2. **Given** a PENDING application, **when** I reject it, **then** I must give a reason and say whether the applicant may apply again; the applicant stays a USER, the status becomes REJECTED and the applicant can see the reason.
3. **Given** I am not an admin, **when** I try to review an application, **then** the request is refused.
4. **Given** an application that is not PENDING, **when** I try to review it again, **then** the request is rejected.

Links: FR-ROLE-2, FR-ROLE-3.

Decision: re-application after a rejection depends on the rejection reason; the admin sets the "may apply again" flag when rejecting (see US-ACC-5).

### US-ACC-7: First admin account

**As the** platform operator, **I want** the first admin account to be created automatically from configuration when the application starts, **so that** there is an admin without any public way to become one.

Acceptance criteria:
1. **Given** no admin exists and the admin e-mail and password are set in the environment, **when** the application starts, **then** an admin account is created.
2. **Given** an admin already exists, **when** the application starts, **then** nothing is created or changed.
3. **Given** the admin settings are missing, **when** the application starts, **then** no admin is created and a clear warning is logged.
4. The admin password is never written in the repository or in the logs.
5. No public endpoint can create an admin or let a user choose a role.

Links: FR-ROLE-4, FR-ROLE-5.

---

## Epic 2: Events and ticket categories (organizer)

### US-EVT-1: Create an event

**As an** organizer, **I want** to create an event, **so that** I can prepare it before selling tickets.

Acceptance criteria:
1. **Given** I am an ORGANIZER, **when** I create an event with a title, description, a venue from the venue list (US-EVT-8), date and time, **then** the event is saved with the status DRAFT and is not visible to users.
2. **Given** an event date that is not in the future, **when** I create the event, **then** the request is rejected.
3. **Given** a per-reservation ticket limit outside 1 to 10, **when** I create the event, **then** the request is rejected; if I give no limit, the default is 4.
4. **Given** an optional sales start time that is not before the event date, **when** I create the event, **then** the request is rejected.
5. **Given** I give no sales end time, **when** I create the event, **then** sales will end when the event starts.
6. **Given** a sales end time later than the event start but no event end time, **when** I create the event, **then** the request is rejected; with an event end time it is accepted as long as the sales end time is not after the event end time.
7. **Given** I am not an ORGANIZER, **when** I try to create an event, **then** the request is refused.

Links: FR-EVT-1, FR-EVT-2, FR-EVT-6, FR-EVT-10, FR-EVT-11, FR-EVT-12.

### US-EVT-2: Add ticket categories

**As an** organizer, **I want** to add ticket categories (for example Standard, VIP) with a price and a quantity, **so that** users can choose what to buy.

Acceptance criteria:
1. **Given** my event, **when** I add a category with a name, a price and a quantity, **then** the category is saved for that event.
2. **Given** a quantity below 1 or a negative price, **when** I add a category, **then** the request is rejected.
3. **Given** a category name that already exists in the same event, **when** I add it, **then** the request is rejected.
4. **Given** a price of 0, **when** I add a category, **then** it is accepted as a free category; tickets of it are reserved and "paid" through the normal flow with an amount of 0.

Links: FR-EVT-1, FR-PRC-1.

### US-EVT-3: Publish an event

**As an** organizer, **I want** to publish my event, **so that** users can see it and buy tickets.

Acceptance criteria:
1. **Given** a DRAFT event with at least one ticket category, **when** I publish it, **then** the status becomes PUBLISHED and users can see it.
2. **Given** a DRAFT event without any ticket category, **when** I publish it, **then** the request is rejected.
3. **Given** a sales start time in the future, **when** the event is published, **then** it is visible but tickets cannot be reserved until that time.
4. **Given** no sales start time, **when** the event is published, **then** sales begin immediately.

Links: FR-EVT-2, FR-EVT-3, FR-EVT-10.

### US-EVT-4: Edit a published event

**As an** organizer, **I want** to correct my event after publishing, **so that** I do not have to cancel it for small mistakes.

Acceptance criteria:
1. **Given** a published event, **when** I change the title or description, **then** the change is saved.
2. **Given** a published event, **when** I try to change the date or venue, **then** the request is rejected and the message tells me to cancel and create a new event.
3. **Given** a category, **when** I change its price, **then** the new price applies only to future reservations; existing reservations and purchases keep their price.
4. **Given** a category with 100 sold or reserved tickets, **when** I set the quantity to 80, **then** the request is rejected.
5. **Given** the same category, **when** I set the quantity to 150, **then** the change is saved.

Links: FR-EVT-7, FR-EVT-8, FR-EVT-9, FR-PRC-2.

### US-EVT-5: Manage only my own events

**As an** organizer, **I want** only me to be able to change my events, **so that** other organizers cannot interfere.

Acceptance criteria:
1. **Given** an event created by another organizer, **when** I try to view the management data, change, publish or cancel it, **then** the request is refused.
2. **Given** I am an ADMIN, **when** I try the same, **then** it is allowed (admins can step in to protect the system).
3. **Given** an admin changes or cancels another organizer's event, **when** the action completes, **then** an audit log entry records who did what, when and on which event.

Links: FR-EVT-4, FR-ROLE-6.

### US-EVT-6: Cancel an event

**As an** organizer, **I want** to cancel an event, **so that** buyers are protected when the event cannot take place.

Acceptance criteria:
1. **Given** a PUBLISHED event, **when** I cancel it, **then** the status becomes CANCELLED and no new reservations are possible.
2. **Given** reservations that are still waiting for payment, **when** the event is cancelled, **then** they are cancelled and their tickets return to stock.
3. **Given** tickets that were already sold, **when** the event is cancelled, **then** they are cancelled and the payments are refunded automatically at the paid price (see Epic 6).
4. **Given** an event that is COMPLETED or already CANCELLED, **when** I try to cancel it, **then** the request is rejected.

Links: FR-EVT-5, FR-PRC-2.

### US-EVT-7: Event completion

**As the** system, **I want** to mark events as COMPLETED after their time has passed, **so that** old events no longer accept sales or appear as upcoming.

Acceptance criteria:
1. **Given** a PUBLISHED event whose end time (or start time, if there is no end time) has passed, **when** the periodic check runs, **then** the status becomes COMPLETED.
2. **Given** the sales end time has passed, **when** someone tries to reserve tickets, **then** the request is rejected.

Links: FR-EVT-11, FR-EVT-13.

### US-EVT-8: Find, add and choose venues

**As an** organizer, **I want** to pick my event's venue from a shared list or add it when it is missing, **so that** I do not retype addresses and the same venue is never stored twice.

Acceptance criteria:
1. **Given** I am an ORGANIZER or ADMIN, **when** I search venues by name and city, **then** I get the matching venues (name, address, city).
2. **Given** a venue that does not exist, **when** I create it with a name, address and city, **then** it is saved in the shared list and every organizer can use it.
3. **Given** a venue whose normalized name and city equal an existing venue, **when** I create it, **then** no new venue is created and the existing one is returned.
4. **Given** a name that is only similar to existing venues in the same city, **when** I create it, **then** the response lists the similar venues and nothing is created until I confirm explicitly.
5. **Given** a venue, **when** I create an event, **then** the event is linked to it and its city is the venue's city.
6. **Given** a name, address or city that is missing, **when** I create a venue, **then** the request is rejected.
7. **Given** I am a USER, **when** I try to create a venue, **then** the request is refused.

Links: FR-VEN-1, FR-VEN-2, FR-VEN-3.

### US-EVT-9: Manage venues (admin)

**As an** admin, **I want** to correct, delete and merge venues, **so that** the shared list stays clean.

Acceptance criteria:
1. **Given** a venue, **when** I edit its name, address or city, **then** the change is saved and all its events show the new data.
2. **Given** a venue that has no events, **when** I delete it, **then** it is removed; a venue with events cannot be deleted.
3. **Given** two duplicate venues, **when** I merge them, **then** all events of the duplicate move to the venue I keep and the duplicate is removed.
4. **Given** I am an ORGANIZER or USER, **when** I try to edit, delete or merge a venue, **then** the request is refused.
5. **Given** any of these actions, **then** an audit log entry records who did what and when.

Links: FR-VEN-4, FR-VEN-5, FR-ROLE-6.

---

## Epic 3: Browsing events (user)

### US-BRW-1: See upcoming events

**As a** visitor, **I want** to see a list of upcoming events, **so that** I can find something to attend.

Acceptance criteria:
1. **Given** published events, **when** I open the list, **then** I see only PUBLISHED events that have not ended, earliest start first, in pages (default page size 20, maximum 100).
2. **Given** DRAFT, CANCELLED or COMPLETED events, **when** I open the list, **then** they do not appear.
3. Each entry shows the title, start time, city, venue, the lowest current category price and a sales status.
4. The sales status is NOT_YET_ON_SALE before the sales start, ON_SALE during sales, SOLD_OUT when no category has tickets left, and SALES_ENDED after the sales end time.
5. **Given** there are no matching events, **when** I open the list, **then** I get an empty list, not an error.

Links: FR-BRW-2.

### US-BRW-2: Search and filter events

**As a** visitor, **I want** to filter events, **so that** I find the right one quickly.

Acceptance criteria:
1. **Given** a keyword, **when** I search, **then** events whose title contains it (ignoring upper and lower case) are returned.
2. **Given** a date range, **when** I filter, **then** only events starting inside it are returned.
3. **Given** a city, **when** I filter, **then** only events in that city are returned.
4. **Given** a price range, **when** I filter, **then** events with at least one category whose current price is inside the range are returned.
5. **Given** several filters at once, **when** I search, **then** an event must match all of them.
6. **Given** a range whose start is after its end, **when** I filter, **then** the request is rejected with an error.
7. **Given** nothing matches, **when** I search, **then** I get an empty list.

Links: FR-BRW-3. Advanced search (tags, popularity, recommendations) is a later idea.

### US-BRW-3: View event details

**As a** visitor, **I want** to see the details of an event, **so that** I can decide what to buy. (By default no login is needed, see US-BRW-4.)

Acceptance criteria:
1. **Given** a PUBLISHED event, **when** I open it, **then** I see the description, venue, city, times and every ticket category with its current price and remaining tickets.
2. **Given** a DRAFT event, **when** I open it, **then** it is treated as not found.
3. **Given** a CANCELLED event, **when** I open it, **then** I can see that it is cancelled and no tickets can be reserved.
4. Remaining ticket numbers are informational; the final check happens when reserving.

Links: FR-BRW-4, FR-BRW-5, FR-RES-4.

### US-BRW-4: Browse without logging in

**As a** visitor, **I want** to browse events without an account, **so that** I can look around before registering.

Acceptance criteria:
1. **Given** I am not logged in, **when** I list, search or view events, **then** it works.
2. **Given** I am not logged in, **when** I try to reserve tickets, **then** the request is refused and I am told to log in.
3. **Given** the configuration setting "require login for event details" is on and I am not logged in, **when** I open the details of an event, **then** the request is refused and I am told to log in, while the list and the search still work.

Links: FR-BRW-1.

---

## Epic 4: Reservation

### US-RES-1: Reserve tickets

**As a** logged-in user, **I want** to reserve tickets for an event, **so that** nobody else can take them while I pay.

Acceptance criteria:
1. **Given** a PUBLISHED event that is on sale, **when** I reserve tickets from one or more categories (for example 2 Standard and 1 VIP), **then** one reservation is created with one line per category, and the tickets are held for 10 minutes (configurable).
2. **Given** the current prices, **when** the reservation is created, **then** the price of each line is frozen; later price changes do not affect it.
3. **Given** one of the lines cannot be satisfied (not enough tickets), **when** I reserve, **then** nothing is reserved and the error says which category is short.
4. **Given** the total number of tickets in my request is above the event's limit N, **when** I reserve, **then** the request is rejected.
5. **Given** an event that is DRAFT, CANCELLED, COMPLETED, not yet on sale, or past its sales end time, **when** I reserve, **then** the request is rejected.
6. **Given** I am not logged in, **when** I reserve, **then** the request is refused (US-BRW-4).
7. **Given** a request with quantity 0 or a negative quantity, or a category of another event, **when** I reserve, **then** the request is rejected with a validation error.

Links: FR-RES-1, FR-RES-2, FR-RES-7, FR-PRC-2, FR-EVT-10, FR-EVT-11.

### US-RES-2: Respect the per-user ticket limit

**As an** organizer, **I want** every buyer to be limited to N tickets for my event in total, **so that** nobody can hoard tickets by making many reservations.

Acceptance criteria:
1. **Given** N = 4 and I already hold or have bought 3 tickets for this event, **when** I reserve 2 more, **then** the request is rejected; reserving 1 more works.
2. **Given** N = 4, **when** I make four reservations of 1 ticket each, **then** all four work and a fifth is rejected.
3. **Given** a reservation of mine expired or was cancelled, **when** I reserve again, **then** its tickets do not count against my limit.
4. **Given** I send two reservation requests at the same moment that together would exceed N, **when** both are processed, **then** at most the allowed amount succeeds.

Links: FR-RES-5, FR-EVT-6, FR-RES-4.

### US-RES-3: Cancel my own reservation

**As a** user, **I want** to cancel a reservation I have not paid for, **so that** the tickets go back to other buyers.

Acceptance criteria:
1. **Given** my unpaid, active reservation, **when** I cancel it, **then** its tickets return to stock immediately and my limit is freed.
2. **Given** a reservation that is already paid, expired or cancelled, **when** I cancel it, **then** the request is rejected.
3. **Given** someone else's reservation, **when** I try to cancel it, **then** the request is refused.

Links: FR-RES-6, FR-RES-5. Cancelling a paid purchase is out of the MVP.

### US-RES-4: Expire reservations automatically

**As the** system, **I want** to release reservations that were not paid in time, **so that** held tickets do not stay blocked.

Acceptance criteria:
1. **Given** an active reservation older than the hold time, **when** the periodic check runs, **then** it becomes EXPIRED and its tickets return to stock.
2. **Given** the check runs twice or runs in two places at once, **when** it handles the same reservation, **then** the tickets are returned only once.
3. **Given** a reservation with a payment still in progress, **when** it expires, **then** a payment result that arrives later is handled by US-PAY-4.

Links: FR-RES-3, FR-RES-4.

### US-RES-5: Never oversell

**As an** organizer, **I want** the system to never sell more tickets than I created, **so that** nobody arrives with a ticket for a full venue.

Acceptance criteria:
1. **Given** 10 tickets left and 100 users reserving 1 ticket at the same moment, **when** all requests finish, **then** exactly 10 succeed and 90 are rejected.
2. **Given** two users each want 2 categories in opposite order at the same moment, **when** both are processed, **then** neither blocks the other forever (no deadlock) and stock stays correct.
3. At any time, sold plus reserved quantity of a category never exceeds its quantity.

Links: FR-RES-4, FR-RES-7, NFR-LOAD-1.

### US-RES-6: See my reservations

**As a** user, **I want** to see my active reservations and the time left, **so that** I know when I must pay.

Acceptance criteria:
1. **Given** I have an active reservation, **when** I open my reservations, **then** I see its lines, frozen prices, total and expiry time.
2. **Given** I open someone else's reservation, **then** it is treated as not found.

Links: FR-RES-2, FR-RES-7.

---

## Epic 5: Payment and tickets

### US-PAY-1: Pay for a reservation

**As a** user, **I want** to pay for my reservation, **so that** I receive my tickets.

Acceptance criteria:
1. **Given** an active reservation of mine, **when** the (simulated) payment succeeds, **then** the purchase is completed: one ticket per reserved unit, each with its own unique code, plus one booking code for the whole purchase.
2. **Given** the purchase is completed, **then** the paid amount is stored in components (ticket price, extras, fees), each marked refundable or not, at the frozen price.
3. **Given** the payment succeeded, **then** a purchase e-mail is triggered (US-NTF-1) but the purchase does not depend on it.
4. **Given** an expired or cancelled reservation, **when** I try to pay, **then** the request is rejected (a late payment result is handled by US-PAY-4).
5. **Given** someone else's reservation, **when** I try to pay, **then** the request is refused.

Links: FR-PAY-1, FR-PAY-2, FR-PAY-5, FR-PRC-2.

### US-PAY-2: Retry a failed payment

**As a** user, **I want** to try again when my payment fails, **so that** I do not lose my tickets because of one declined card.

Acceptance criteria:
1. **Given** a failed payment and time left on the reservation, **when** I pay again, **then** a new attempt starts for the same reservation.
2. **Given** three failed attempts (configurable), **then** the reservation is released and its tickets return to stock.
3. **Given** the reservation expires before a new attempt, **then** no further attempt is allowed.

Links: FR-PAY-8.

### US-PAY-3: No duplicate payments or tickets

**As a** user, **I want** a repeated request or callback not to charge me twice, **so that** a network problem does not cost me money.

Acceptance criteria:
1. **Given** the same payment request is sent twice (same idempotency key), **when** both are processed, **then** one payment and one set of tickets exist and both calls get the same result.
2. **Given** the payment callback arrives twice, **when** both are processed, **then** tickets are issued only once.
3. **Given** two parallel payment requests for one reservation, **then** only one payment is accepted.

Links: FR-PAY-4.

### US-PAY-4: Late payment after expiry

**As a** user, **I want** a payment that finished after my reservation expired to be handled fairly, **so that** I either get my tickets or my money back.

Acceptance criteria:
1. **Given** a successful payment arrives after the reservation expired and the tickets are still free, **when** it is processed, **then** the purchase is completed at the frozen price.
2. **Given** the tickets are no longer free (or my limit would be exceeded), **when** it is processed, **then** the payment is refunded automatically and I am notified.
3. The outcome is always one of the two: never a payment without tickets, never tickets without a payment.

Links: FR-PAY-7, FR-PAY-1.

### US-PAY-5: Undo the payment if ticket issuing fails

**As a** user, **I want** my money back if the system fails to issue my tickets, **so that** I never pay for nothing.

Acceptance criteria:
1. **Given** a successful payment and a failure while issuing tickets, **when** the failure is detected, **then** the payment is refunded (compensation) and no half-issued tickets remain.
2. **Given** the refund itself fails, **then** it is retried and, if it keeps failing, moves to the "refund pending" list (US-CXL-3).

Links: FR-PAY-3.

### US-PAY-6: See my purchases and tickets

**As a** user, **I want** to see my purchases and tickets at any time, **so that** I do not depend on an e-mail.

Acceptance criteria:
1. **Given** I have purchases, **when** I open my tickets, **then** I see each purchase (booking code, event, date) with its tickets (ticket code, category, paid price, status VALID, USED or CANCELLED).
2. **Given** the notification e-mail failed, **then** my purchase and tickets are still fully visible.
3. **Given** I open someone else's purchase or ticket, **then** it is treated as not found.
4. **Given** a booking code, **when** I look it up, **then** I see my own purchase; a code that belongs to someone else or does not exist is not found.

Links: FR-PAY-9, FR-PAY-5, FR-PAY-6.

### US-PAY-7: Get a QR code for a ticket

**As a** ticket holder, **I want** a QR code for each ticket, **so that** it can be scanned at the entrance.

Acceptance criteria:
1. **Given** a ticket of mine, **when** I request its QR code, **then** I get an image that carries the ticket code.
2. **Given** a ticket that belongs to someone else, **then** the request is treated as not found.
3. The QR code does not prove validity; validity is always checked on the server (US-PAY-8).

Links: FR-PAY-10.

### US-PAY-8: Verify a ticket at the entrance

**As an** organizer, **I want** to verify a ticket code of my event, **so that** only valid tickets get in and nobody enters twice.

Acceptance criteria:
1. **Given** a VALID ticket of my event, **when** I verify it, **then** the answer is "valid" and the ticket becomes USED.
2. **Given** a USED ticket, **when** I verify it again, **then** the answer is "already used" and nothing changes.
3. **Given** a CANCELLED ticket or an unknown code, **then** the answer is "cancelled" or "not found".
4. **Given** a ticket of another organizer's event, **when** I verify it, **then** the request is refused; an admin may verify any ticket.
5. **Given** the same ticket is verified twice at the same moment, **then** exactly one request succeeds.

Links: FR-PAY-11, FR-EVT-4.

---

## Epic 6: Event cancellation and refunds

### US-CXL-1: Cancel an event

**As an** organizer, **I want** to cancel my event, **so that** buyers are told and refunded when it cannot take place.

Acceptance criteria:
1. **Given** my PUBLISHED event, **when** I cancel it, **then** the cancellation completes immediately: status CANCELLED, sales stop, open reservations are released and sold tickets become CANCELLED.
2. **Given** an event that is DRAFT, already CANCELLED or COMPLETED, **when** I cancel it, **then** the request is rejected (a DRAFT can simply be deleted or left).
3. **Given** another organizer's event, **when** I cancel it, **then** the request is refused; an admin may cancel any event and the action is audit-logged (US-EVT-5).
4. **Given** the cancellation, **then** a refund is created for every payment of the event and each buyer gets an event-cancelled e-mail.

Links: FR-EVT-5, FR-EVT-14, FR-EVT-4, FR-ROLE-6.

### US-CXL-2: Refund everything on cancellation

**As a** buyer, **I want** to get back everything I paid when the organizer cancels, **so that** I lose nothing because of a decision I did not make.

Acceptance criteria:
1. **Given** a cancelled event, **when** refunds are processed in the background, **then** each payment is refunded in full, including components marked non-refundable for buyer-requested refunds.
2. **Given** the amount, **then** the refund equals the price actually paid (frozen price), regardless of later price changes.
3. **Given** a refund is processed twice (retry or duplicate message), **then** the payment is refunded only once.
4. **Given** a refund is completed, **then** I get a refund-completed e-mail.

Links: FR-EVT-14, FR-EVT-15, FR-EVT-16, FR-PRC-2, FR-PAY-4.

### US-CXL-3: Retry failed refunds

**As the** system, **I want** failed refunds to be retried and never lost, **so that** every buyer is eventually refunded.

Acceptance criteria:
1. **Given** a refund fails, **when** the failure is detected, **then** it is retried automatically (number and delays configurable).
2. **Given** the retries are used up, **then** the refund moves to the "refund pending" list and is not deleted.
3. **Given** the cancellation of an event is in progress, **then** one failing refund does not stop the other refunds.

Links: FR-EVT-15.

### US-CXL-4: Handle pending refunds (admin)

**As an** admin, **I want** to see and retry refunds that could not be completed, **so that** nobody stays without their money.

Acceptance criteria:
1. **Given** pending refunds, **when** I open the list, **then** I see event, buyer, amount, failure reason and number of attempts.
2. **Given** a pending refund, **when** I retry it and it succeeds, **then** it is completed, removed from the list and the buyer is notified.
3. **Given** I am not an admin, **then** the request is refused.
4. **Given** I retry a refund, **then** the action is audit-logged.

Links: FR-EVT-15, FR-ROLE-6.

---

## Epic 7: Notifications

### US-NTF-1: Get e-mails for important events

**As a** user, **I want** an e-mail when something important happens to me, **so that** I stay informed.

Acceptance criteria:
1. **Given** a successful purchase, **then** I get an e-mail with the booking code and the event details.
2. **Given** my account gets locked, **then** I get one e-mail about the lock (not one per failed attempt).
3. **Given** a refund is completed, **then** I get an e-mail with the refunded amount.
4. **Given** an event I hold tickets for is cancelled, **then** I get an e-mail.
5. **Given** my organizer application is decided, **then** I get an e-mail: approval, or rejection with the reason.
6. **Given** a reservation is about to expire or has expired, **then** no e-mail is sent (not in the MVP).

Links: FR-NTF-1, FR-NTF-4, FR-AUTH-2, FR-ROLE-3.

### US-NTF-2: A failed e-mail never breaks my purchase

**As a** buyer, **I want** my purchase to stay valid even when an e-mail cannot be sent, **so that** a mail problem never costs me my tickets.

Acceptance criteria:
1. **Given** the mail service is down, **when** I complete a purchase, **then** the purchase succeeds and I can see it through the API (US-PAY-6).
2. **Given** an e-mail fails, **then** it is retried automatically (number and delays configurable).
3. **Given** the retries are used up, **then** the message moves to a failed-messages queue and is not lost.
4. **Given** the same notification is processed twice, **then** the user gets one e-mail.

Links: FR-NTF-2, NFR-REL-1.

### US-NTF-3: Inspect failed notifications (admin)

**As an** admin, **I want** to see notifications that could not be delivered, **so that** I can find out why and resend them.

Acceptance criteria:
1. **Given** failed notifications, **when** I open the list, **then** I see recipient, type, failure reason and attempts.
2. **Given** a failed notification, **when** I resend it and it succeeds, **then** it leaves the list.
3. **Given** I am not an admin, **then** the request is refused.

Links: FR-NTF-2.

### US-NTF-4: Get a reminder before the event

**As a** ticket holder, **I want** a reminder one day before the event, **so that** I do not forget it.

Acceptance criteria:
1. **Given** a PUBLISHED event starting in about one day, **when** the periodic check runs, **then** every holder of a VALID ticket gets one reminder e-mail.
2. **Given** a cancelled event or a CANCELLED ticket, **then** no reminder is sent.
3. **Given** the check runs twice or in two places, **then** nobody gets the reminder twice.
4. **Given** an event published or bought less than a day before the start, **then** no reminder is sent for it (or the one purchase e-mail is enough).

Links: FR-NTF-5.

### US-NTF-5: Add the event to my calendar

**As a** buyer, **I want** a calendar file in my e-mail, **so that** I can add the event to my calendar with one click.

Acceptance criteria:
1. **Given** the purchase e-mail or the reminder e-mail, **then** it carries an `.ics` file with title, venue, city and start time.
2. **Given** the file is opened in a calendar app, **then** the event is created at the right local time.

Links: FR-NTF-6.

### US-NTF-6: Prepare for more channels and languages

**As a** developer, **I want** channels and languages to be added without changing the purchase flow, **so that** SMS and Turkish can come later.

Acceptance criteria:
1. **Given** the notification module, **then** the business flows only say what happened; how and in which language it is sent is decided inside the module.
2. **Given** a message type, **then** its text comes from a template chosen by language; the MVP ships English templates only.
3. **Given** a new channel is added, **then** no code of the purchase, refund or account flows changes.

Links: FR-NTF-3, FR-NTF-7.

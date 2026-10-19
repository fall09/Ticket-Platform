# Requirements

> Status: DRAFT, work in progress (BA sessions 1 and 2). Items marked **[OPEN]** are not decided yet.

## 1. Purpose and scope

Ticket Platform is a backend system for event ticketing and reservations. Organizers publish events and ticket categories, users reserve and buy tickets, payment is simulated, and tickets are issued after a successful payment. There is no frontend. The API is defined by an OpenAPI contract (`docs/api/openapi.yaml`) and explored through Swagger UI (which renders the OpenAPI file) and a Postman collection.

## 2. User roles

| Role | Description |
|---|---|
| USER | Registers, browses published events, reserves and buys tickets. |
| ORGANIZER | Creates and manages their own events and ticket categories. |
| ADMIN | Manages the platform, approves organizer applications. |

- **FR-ROLE-1:** A user can apply to become an organizer.
- **FR-ROLE-2:** An admin approves or rejects organizer applications. Only approved users get the ORGANIZER role.
- **FR-ROLE-3:** A rejection carries a reason and a flag saying whether the applicant may apply again. The applicant can see the reason.
- **FR-ROLE-4:** A user cannot choose a role at registration. Everyone starts as USER; ORGANIZER comes only through an approved application.
- **FR-ROLE-5:** There is no admin panel (no frontend). Admin functions are protected API endpoints that require the ADMIN role. The first admin account is created at application startup from configuration (environment variables), never through the public API and never with a password stored in the repository.
- **FR-ROLE-6:** Every action an admin performs on data that belongs to someone else is written to an audit log (who, what, when, on which object).

## 3. Functional requirements

### 3.1 Events

- **FR-EVT-1:** An organizer can create an event (title, description, venue, city, start time) with one or more ticket categories (for example Standard, VIP). Each category has a price and a quantity.
- **FR-EVT-2:** An event has a status: `DRAFT` -> `PUBLISHED` -> `CANCELLED` or `COMPLETED`.
- **FR-EVT-3:** Only `PUBLISHED` events are visible to users and available for reservation.
- **FR-EVT-4:** An organizer can manage only their own events. An admin can view, edit and cancel any event.
- **FR-EVT-5:** When an event is cancelled, all sold tickets are cancelled and the payments are refunded automatically.
- **FR-EVT-6:** The organizer sets the maximum number of tickets per reservation for each event. Default is 4, allowed range is 1 to 10.
- **FR-EVT-7:** After publishing, the organizer can change the title and description freely. Date and venue cannot be changed in the MVP; to change them the organizer cancels the event and creates a new one.
- **FR-EVT-8:** The organizer can change the price of a category at any time (for example early-bird phases). A price change affects only future reservations.
- **FR-EVT-9:** The quantity of a category can be increased, but never below the number of sold plus reserved tickets.
- **FR-EVT-10:** The organizer can optionally set a sales start time for an event. Before that time the event is visible but tickets cannot be reserved. Without a sales start time, sales begin when the event is published.
- **FR-EVT-11:** Ticket sales end at the sales end time. By default the sales end time is the event start time; the organizer can set a later one (see FR-EVT-12).
- **FR-EVT-12:** The organizer can set an optional event end time (for longer or multi-day events). A sales end time later than the event start is allowed only if an event end time is set, and it can never be later than the event end time.
- **FR-EVT-13:** After the event end time (or the start time, if no end time is set) has passed, the system marks the event as COMPLETED automatically.
- **FR-EVT-14:** Cancelling an event completes immediately: the event becomes `CANCELLED`, sales stop, open reservations are released and sold tickets are marked `CANCELLED`. The refunds are processed in the background, one per payment, at the price that was actually paid (FR-PRC-2).
- **FR-EVT-15:** A failed refund is retried automatically. If it keeps failing, it moves to a "refund pending" list that an admin can inspect and retry. No refund is ever lost, and no payment is refunded twice (idempotency, FR-PAY-4). Each user is notified when their refund is completed.
- **FR-EVT-16:** When the organizer (or an admin) cancels an event, buyers get back everything they paid for that event, because the cancellation is not their choice. Refund policies of the organizer (future idea) apply only when the buyer asks for the refund.

### 3.2 Reservation

- **FR-RES-1:** A user can reserve tickets of a published event, up to the event's per-reservation limit.
- **FR-RES-2:** A reservation holds the tickets for 10 minutes (configurable).
- **FR-RES-3:** If the payment is not completed in time, the reservation expires and the tickets return to stock.
- **FR-RES-4:** Tickets must never be oversold: sold plus reserved quantity never exceeds the category quantity, even under concurrent requests.
- **FR-RES-5:** The per-reservation limit of an event (FR-EVT-6) is also the total limit per user for that event. The tickets a user has bought plus the tickets in their active reservations can never exceed N, however many reservations they make (for example N = 4: four reservations of 1 ticket fill the limit). Expired or cancelled reservations do not count.
- **FR-RES-6:** A user can cancel their own unpaid reservation. The tickets return to stock immediately and the user's limit (FR-RES-5) is freed. Cancelling a paid purchase is a different case and is out of the MVP.
- **FR-RES-7:** One reservation can contain tickets from several categories of the same event (for example 2 Standard and 1 VIP), as separate lines with a quantity and a frozen price each. The reservation is all or nothing: if any line cannot be satisfied, nothing is reserved. The total number of tickets in the reservation counts against the limit in FR-RES-1 and FR-RES-5. Concurrent reservations must never block each other forever (no deadlocks) and must never oversell (FR-RES-4).

### 3.3 Payment and tickets

- **FR-PAY-1:** Payment is simulated (success, failure, late callback).
- **FR-PAY-2:** A successful payment issues exactly one ticket for each reserved unit, each with a unique code.
- **FR-PAY-3:** If ticket issuing fails after a successful payment, the payment is refunded (compensation).
- **FR-PAY-4:** Repeating the same payment request or callback must not create duplicate payments or tickets (idempotency).
- **FR-PAY-5:** Every successful purchase gets a unique booking code that is stored in the system and can be verified. Each ticket also keeps its own unique code (FR-PAY-2).
- **FR-PAY-6:** The user can always retrieve their bookings and tickets through the API, independent of notifications.
- **FR-PAY-7:** When a payment succeeds after the reservation has expired (late callback), the system checks the stock again. If the tickets are still available, the purchase is completed at the frozen price (FR-PRC-2); if not, the payment is refunded automatically and the user is notified. The result is always one of the two, never a payment without tickets or tickets without a payment.
- **FR-PAY-8:** If a payment fails, the user can try again for the same reservation until the reservation expires, up to 3 attempts (configurable). After the third failed attempt the reservation is released and the tickets return to stock. Repeating the same attempt must not create a second payment (FR-PAY-4).
- **FR-PAY-9:** The user has a "my tickets" view: every purchase (booking code, event, date) with its tickets (ticket code, category, paid price, status). A ticket status is `VALID`, `USED` or `CANCELLED`.
- **FR-PAY-10:** Each ticket has a QR code generated from its ticket code, so it can be scanned at the entrance. The QR code only carries the code; the validity check always happens on the server (FR-PAY-11).
- **FR-PAY-11:** An organizer can verify a ticket code of their own event (an admin of any event). The system answers whether the code is valid, already used or cancelled. Verifying a valid ticket marks it `USED`; scanning it a second time is rejected. Two parallel scans of the same ticket must result in exactly one success.
- **FR-NTF-1:** The user is notified by e-mail after a successful purchase, including the booking code.
- **FR-NTF-2:** If sending a notification fails, the purchase stays valid and the notification is retried automatically. After the maximum number of retries it is moved to a failed-messages queue for inspection.
- **FR-NTF-3:** The notification module must allow adding further channels (for example SMS) without changing the purchase flow.
- **FR-NTF-4:** The MVP sends e-mail notifications for these events: successful purchase (with booking code), account lock (FR-AUTH-2), refund completed (FR-EVT-15), event cancelled, and the result of an organizer application (approval, or rejection with the reason, FR-ROLE-3). Reservation expiry warnings and expiry notices are not sent in the MVP.
- **FR-NTF-5:** Every ticket holder gets a reminder e-mail one day before the event starts. Cancelled events and cancelled tickets get no reminder. The same reminder is never sent twice.
- **FR-NTF-6:** The purchase e-mail and the reminder e-mail contain an add-to-calendar file (iCalendar `.ics`) with the event title, venue, city and start time, so the buyer can put the event in any calendar app.
- **FR-NTF-7:** Notification texts are English in the MVP, but they are built from message templates that are separate from the code and chosen by language, so Turkish can be added later without changing the flow. A user language setting is added together with the Turkish templates (see future ideas).

### 3.4 Pricing

- **FR-PRC-1:** Each ticket category has a single current price. The price may be 0 (free tickets); it can never be negative. A free reservation still goes through the normal purchase flow (booking code, tickets, limits) with a payment amount of 0.
- **FR-PRC-2:** The price is frozen when a reservation is created, and the paid price is stored with the purchase. Later price changes never change what a user pays or what is refunded.

### 3.5 Authentication and account protection

- **FR-AUTH-1:** After repeated failed logins the account is locked progressively: 3 failed attempts lock it for 1 minute, 2 more failed attempts after unlock lock it for 3 minutes, further failures lock it for 15 minutes. A successful login resets the counter. Thresholds and durations are configurable.
- **FR-AUTH-2:** When an account gets locked, the user is notified by e-mail (simulated by the notification module). No e-mail is sent for each single failed attempt.
- **FR-AUTH-3:** Every failed login is logged with time, username and IP address. Passwords (including wrong ones) are never logged.
- **FR-AUTH-4:** A password must have 8 to 72 characters and contain at least one uppercase letter, one digit and one special character. When a password is rejected, the error says which rule was broken.

### 3.6 Browsing events

- **FR-BRW-1:** Listing and searching published events is public. Viewing event details is public by default; a configuration setting can require a login for the details (the list stays public). Reserving and buying always require a login.
- **FR-BRW-2:** The event list shows only PUBLISHED events that have not ended, earliest start first, in pages. Each entry shows title, start time, city, venue, the lowest current category price and a sales status (NOT_YET_ON_SALE, ON_SALE, SOLD_OUT, SALES_ENDED).
- **FR-BRW-3:** The list can be filtered by title keyword, date range, city and price range, and the filters can be combined.
- **FR-BRW-4:** The event detail shows the description, venue, city, times and every ticket category with its current price and remaining tickets. DRAFT events are not visible to users. A CANCELLED event stays viewable with its status, so buyers can see what happened.
- **FR-BRW-5:** Remaining ticket numbers shown to users are informational; the final check happens when reserving (FR-RES-4).

## 4. Non-functional requirements

| ID | Requirement | Status |
|---|---|---|
| NFR-LOAD-1 | The system is load-tested with 1,000 concurrent users at ticket-sale opening (test target, may be adjusted after measurements). | Decided |
| NFR-PERF-1 | Reservation request response time at p95: no fixed target yet. First measure a baseline with the load test, then set the target from real numbers (starting hypothesis: under 500 ms). | Decided: baseline first |
| NFR-SEC-1 | Authentication, role-based and ownership-based authorization. Brute-force protection through progressive account lockout (see FR-AUTH-1 to FR-AUTH-3). | Partly decided, other details **[OPEN]** |
| NFR-REL-1 | A failure in a secondary function (such as sending a notification) must never break or undo a completed purchase (see FR-PAY-5, FR-PAY-6, FR-NTF-2). Messages must not be lost or processed twice with visible effects. | Partly decided, other details **[OPEN]** |
| NFR-OBS-1 | Requests are traceable end to end (correlation ID, metrics). | **[OPEN]** details |

## 5. Out of scope (MVP)

- Real payment provider integration.
- Seat map and seat selection.
- User-initiated cancellation and refund after payment.
- Discounts, coupons, student discount, membership and loyalty programs, special roles.
- Frontend.

## 6. Future ideas (not in MVP)

- Refund policy per event, defined by the organizer, for refunds that the buyer asks for. Examples from Eylül: (1) an optional paid "refund right" add-on: pay 550 (500 ticket + 50 add-on); when the buyer refunds, 500 comes back and the 50 add-on fee stays; (2) time-based rules: no refund within 24 hours of the event, 50% refund when cancelled 48 hours before, and so on; (3) paid extras such as a chosen seat (100 extra) can be marked non-refundable. Design consequence to keep from the start: a purchase stores its amounts as separate components (ticket price, extras, fees), each marked refundable or not, never as one total only. This is what makes these policies possible later without changing the data model.
- Price tiers (early-bird pricing): the organizer defines ordered tiers per category, each with its own price. A tier ends automatically when its end date passes or when a given percentage of the category is sold, whichever comes first, and the next tier's price applies to new reservations. FR-PRC-2 already protects existing reservations and purchases. Open: whether the sold percentage also counts reserved tickets. Decided: this comes after the core flow (reservation, payment, tickets) is finished; until then the organizer changes prices manually (FR-EVT-8).
- Other pricing rules: coupons, student discount.
- Membership and loyalty program.
- Seating plan and seat selection, with the plan provided by the organizer. The same protection rules would apply (sold or reserved seats cannot be removed or re-priced), and the model should leave room for seat-level tickets.
- A frontend (user web app and admin panel) built on top of the same OpenAPI contract, only after the backend is finished. CORS settings are planned for that moment.
- Turkish notification templates (decided: this will come; the template structure is prepared in the MVP, FR-NTF-7), with a language setting per user.
- Member-only convenience features: favoriting events and reminders (for example "remind me when sales start"). Browsing stays free; these features need a login because they are tied to a user account (this is the natural place where an account is asked for besides reserving and buying).
- Advanced event search: category tags, sorting by popularity, recommendations.
- SMS as a second notification channel (the notification module is designed for extra channels, FR-NTF-3).
- IP-based rate limiting for login, to stop attackers from locking other users' accounts on purpose (account-only lockout can be abused).

## 7. Open questions

- Response time target (NFR-PERF-1): to be set after the first baseline measurement.
- Remaining security expectations (token lifetime and refresh).
- Reliability expectations (what happens when the message broker is down).
- Observability expectations.

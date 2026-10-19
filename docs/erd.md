# Data Model (ERD)

> Status: DRAFT (Architecture session 1). Column lists are a first version; types and exact names are finalized when the Flyway migrations are written. Items marked **[PROPOSED]** or **[OPEN]** are not final.

## 1. Rules

- One database schema per module (see `architecture.md`). A module reads and writes only its own tables.
- **No database foreign keys between modules.** A table that points to another module's data stores the plain id (for example `organizer_id`), and the module checks it through the other module's public interface. This keeps modules separable. Foreign keys exist only inside a module.
- Money is stored as `numeric(12,2)` in one currency (the currency is a project setting, not a column, in the MVP).
- All times are stored in UTC (`timestamptz`).
- Ids are UUIDs (decided by Eylül). Prefer time-ordered UUIDs (UUIDv7) so that indexes stay compact; the exact generator is chosen when the first entity is built. Public codes (booking code, ticket code) are separate random values.

## 2. identity

```mermaid
erDiagram
    APP_USER ||--o{ ORGANIZER_APPLICATION : submits
    APP_USER {
        uuid id PK
        string email UK "normalized, lower case"
        string password_hash
        string role "USER, ORGANIZER, ADMIN"
        int failed_login_count
        timestamptz locked_until
        timestamptz created_at
    }
    ORGANIZER_APPLICATION {
        uuid id PK
        uuid user_id FK
        string status "PENDING, APPROVED, REJECTED"
        string rejection_reason
        boolean may_reapply
        uuid reviewed_by "admin user id"
        timestamptz created_at
        timestamptz reviewed_at
    }
```

Notes: at most one PENDING application per user (partial unique index). Lockout counters live on the user row (FR-AUTH-1).

## 3. catalog

```mermaid
erDiagram
    VENUE ||--o{ EVENT : hosts
    EVENT ||--|{ TICKET_CATEGORY : has
    VENUE {
        uuid id PK
        string name
        string normalized_name
        string address
        string city
        uuid created_by "user id"
        timestamptz created_at
    }
    EVENT {
        uuid id PK
        uuid organizer_id "user id"
        uuid venue_id FK
        string title
        string description
        string status "DRAFT, PUBLISHED, CANCELLED, COMPLETED"
        timestamptz starts_at
        timestamptz ends_at "optional"
        timestamptz sales_start_at "optional"
        timestamptz sales_end_at
        int max_tickets_per_user "N, 1 to 10, default 4"
        timestamptz created_at
        timestamptz updated_at
    }
    TICKET_CATEGORY {
        uuid id PK
        uuid event_id FK
        string name
        numeric price "0 allowed"
        int capacity
    }
```

Notes: unique (`normalized_name`, `city`) on venue (FR-VEN-3). Unique (`event_id`, `name`) on ticket category. The city of an event is its venue's city.

## 4. booking

```mermaid
erDiagram
    RESERVATION ||--|{ RESERVATION_LINE : contains
    CATEGORY_STOCK {
        uuid category_id PK "ticket category id"
        uuid event_id
        int capacity "copy, kept in sync"
        int reserved
        int sold
    }
    USER_EVENT_QUOTA {
        uuid user_id PK
        uuid event_id PK
        int tickets_held "active reserved + sold"
    }
    RESERVATION {
        uuid id PK
        uuid user_id
        uuid event_id
        string status "ACTIVE, PAID, EXPIRED, CANCELLED"
        numeric total_amount
        timestamptz expires_at
        timestamptz created_at
    }
    RESERVATION_LINE {
        uuid id PK
        uuid reservation_id FK
        uuid category_id
        int quantity
        numeric unit_price "frozen at reservation time"
    }
```

Notes: `category_stock` is what makes overselling impossible: reserved + sold may never exceed capacity (checked in one atomic update). `user_event_quota` makes the per-user limit (FR-RES-5) enforceable under parallel requests. Decided: `capacity` in `category_stock` is a copy of the catalog capacity, kept in sync by in-process events (category created, capacity changed). The stock update can then check the limit in a single statement, and `catalog` does not depend on `booking`.

## 5. payment

```mermaid
erDiagram
    PAYMENT ||--|{ PAYMENT_COMPONENT : splits_into
    PAYMENT ||--o{ REFUND : may_have
    PAYMENT {
        uuid id PK
        uuid reservation_id
        uuid user_id
        string status "PENDING, SUCCEEDED, FAILED, REFUNDED"
        numeric amount
        int attempt_no "1 to 3"
        string idempotency_key UK
        timestamptz created_at
    }
    PAYMENT_COMPONENT {
        uuid id PK
        uuid payment_id FK
        string type "TICKET, EXTRA, FEE"
        numeric amount
        boolean refundable
    }
    REFUND {
        uuid id PK
        uuid payment_id FK
        numeric amount
        string reason "EVENT_CANCELLED, ISSUING_FAILED, LATE_PAYMENT"
        string status "PENDING, COMPLETED, PENDING_MANUAL"
        int attempts
        string last_error
        timestamptz created_at
        timestamptz completed_at
    }
```

Notes: unique (`payment_id`, `reason`) on refund, so a payment is never refunded twice for the same reason (FR-EVT-15). Amount components are what later refund policies will use (future idea in `requirements.md`).

## 6. ticketing

```mermaid
erDiagram
    PURCHASE ||--|{ TICKET : contains
    PURCHASE {
        uuid id PK
        string booking_code UK
        uuid user_id
        uuid event_id
        uuid reservation_id UK
        uuid payment_id
        numeric total_paid
        timestamptz created_at
    }
    TICKET {
        uuid id PK
        uuid purchase_id FK
        uuid category_id
        string ticket_code UK
        numeric price_paid
        string status "VALID, USED, CANCELLED"
        timestamptz used_at
    }
```

Notes: unique `reservation_id` on purchase prevents a second purchase for the same reservation (FR-PAY-4). Marking a ticket USED is a single conditional update (`status = 'VALID'`), so two parallel scans give exactly one success (FR-PAY-11).

## 7. notification

```mermaid
erDiagram
    NOTIFICATION_MESSAGE {
        uuid id PK
        uuid user_id
        string type "PURCHASE, ACCOUNT_LOCKED, REFUND_COMPLETED, EVENT_CANCELLED, APPLICATION_RESULT, EVENT_REMINDER"
        string channel "EMAIL"
        string language "EN"
        string payload "json"
        string dedupe_key UK "e.g. reminder:user:event"
        string status "PENDING, SENT, FAILED"
        int attempts
        string last_error
        timestamptz created_at
        timestamptz sent_at
    }
```

Notes: `dedupe_key` stops duplicate messages and duplicate reminders (FR-NTF-5). Messages that keep failing stay with status FAILED and are shown to the admin (US-NTF-3).

## 8. shared

```mermaid
erDiagram
    AUDIT_LOG {
        uuid id PK
        uuid actor_id "user id"
        string action
        string object_type
        uuid object_id
        string details "json"
        timestamptz created_at
    }
    OUTBOX_EVENT {
        uuid id PK
        string aggregate_type
        uuid aggregate_id
        string event_type
        string payload "json"
        timestamptz created_at
        timestamptz published_at "null until sent"
    }
```

Notes: `outbox_event` is added in Release 3 (see `backlog.md`).

## 9. References between modules (ids only, no foreign keys)

| From | Column | Points to |
|---|---|---|
| catalog.event | organizer_id | identity.app_user |
| catalog.venue | created_by | identity.app_user |
| booking.category_stock | category_id, event_id | catalog.ticket_category, catalog.event |
| booking.reservation | user_id, event_id | identity.app_user, catalog.event |
| booking.reservation_line | category_id | catalog.ticket_category |
| payment.payment | reservation_id, user_id | booking.reservation, identity.app_user |
| ticketing.purchase | reservation_id, payment_id, user_id, event_id | booking.reservation, payment.payment, identity.app_user, catalog.event |
| ticketing.ticket | category_id | catalog.ticket_category |
| notification.notification_message | user_id | identity.app_user |

## 10. Open points

- **[OPEN]** Index design (decided with the first query measurements).

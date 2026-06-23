# MemoAlgo Backend

REST API powering [MemoAlgo](https://memoalgo.dev) — a spaced-repetition revision tracker for DSA. Built with Spring Boot, with the SM-2 algorithm (the same spaced-repetition math behind Anki) as the scheduling engine.

**Live:** [MemoAlgo](https://memoalgo.dev)
**Frontend repo:** [memoalgo-frontend](https://github.com/HetMamtora/memoalgo-frontend)

---

## Architecture

Standard layered architecture: `Controller → Service → Repository`, with ownership enforcement (every query scoped to the authenticated user) handled at the service layer rather than left to the database alone.

**Design decisions:**

- **Strategy pattern for spaced repetition.** `ReviewService` depends on the `SpacedRepetitionAlgorithm` interface, not `SM2Algorithm` directly — swapping in a different scheduling algorithm later means implementing one new class, not touching `ReviewService`.
- **Two-table OTP staging, not a verified-flag on `users`.** Registration and password-reset attempts are staged in `pending_registrations` / `password_reset_otps` — no `User` row is created, and no real password is overwritten, until the emailed code is verified. Both share one `OtpService` for code generation/hashing/verification, even though the two tables hold different payloads.
- **Append-only review history.** `reviews` holds the *live* SM-2 state (one row per problem); `review_history` is a permanent, append-only log of every rating ever submitted. Streaks, retention rate, and the activity heatmap are all derived from `review_history`, not from the mutable `reviews` table — so changing the live schedule never rewrites history.
- **Soft deletes.** Deleting a problem sets `is_active = false` rather than issuing a SQL `DELETE`, preserving historical review data for stats even after a problem is "removed" from the library.

## Tech stack

Java 21 · Spring Boot 3.3 · Spring Security · JWT · PostgreSQL · Flyway · JUnit · Maven · Railway

**Infrastructure:** Railway (app + managed PostgreSQL), Resend (transactional email)

## API overview

Full interactive docs (request/response shapes, try-it-out) are in Swagger at `/swagger-ui.html`.

| Resource | Endpoints |
|---|---|
| Auth | OTP-gated register (`/initiate` → `/verify`), login, OTP-gated password reset (`/initiate` → `/verify`) |
| Problems | Full CRUD, filterable by difficulty/topic |
| Topics | Read-only list (seeded) |
| Reviews | Get due queue, submit a rating |
| Stats | Aggregated retention/streak/topic/difficulty/activity data |

All endpoints except `/auth/**` and the Swagger/health routes require a `Bearer` JWT.

## Database schema

```
users ──┬── problems ──┬── reviews ── review_history
        │              └── problem_tags ── tags
        └── (pending_registrations, password_reset_otps:
             independent staging tables, not FK'd to users
             until verification succeeds)
topics ── problems (optional FK; a problem may have no topic)
```

Migrations are managed by Flyway — Hibernate's `ddl-auto` is set to `validate`, never `update`. Schema changes always go through a new versioned migration file.

## Testing


49 tests covering authentication, CRUD operations, and the SM-2 scheduling logic (ease factor adjustment, interval growth, repetition reset on a failed review).
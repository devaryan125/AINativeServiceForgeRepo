# Feature 1 — Technician Availability Calendar

| Field | Value |
|---|---|
| **Status** | Pre-baked — already built through the pipeline described in `pipeline/orchestration.md`; this is the worked reference, not something you build |
| **One-line intent** | Let a technician's job commitments be scheduled and checked for conflicts |
| **Depends on** | None — this is the root of the feature chain |
| **In scope** | Booking a technician to a job slot; listing a technician's jobs; rejecting bookings that overlap an existing one for the same technician |
| **Out of scope** | Parts, inventory, or stock of any kind — that starts with Feature 2 |
| **Definition of Done** | A technician can be booked for a job; a technician's jobs can be listed; overlapping bookings for the same technician are rejected with a clear conflict error |

## What's implemented

- `GET /api/technicians` — list all technicians (mock, seeded at startup)
- `GET /api/technicians/{id}/jobs` — a technician's booked jobs
- `POST /api/jobs` — book a new job for a technician

## Fixed overlap behavior

`TechnicianAvailabilityService.bookJob(...)` rejects overlapping job intervals for the same technician and requires the end time to be after the start time. Intervals use half-open boundaries (`[start, end)`): bookings touching at either endpoint do not overlap. The check-and-save operation is serialized to prevent concurrent requests from both booking the same time. The overlap check compares job times only; this fix does not add travel-buffer enforcement or change the 45-minute travel-buffer decision recorded in `pipeline/decisions/feature-1-decisions.md`.
Future changes must preserve this behavior as specified in `pipeline/rules/no-overlap-booking.md`.

## Artifacts this feature touches

- `backend/.../service/TechnicianAvailabilityService.java`
- `backend/.../service/TechnicianAvailabilityServiceTest.java` — covers interval overlaps, boundaries, invalid ranges, technician scope, and concurrent requests
- `backend/.../controller/JobControllerTest.java` — covers conflict and invalid-range responses
- `backend/.../controller/TechnicianController.java`, `JobController.java`
- `backend/.../model/Technician.java`, `Job.java`, `JobStatus.java`
- `backend/.../data/MockDataStore.java`
- `pipeline/decisions/feature-1-decisions.md`
- `pipeline/rules/no-overlap-booking.md`
- `.claude/agents/bugfix.md`

# Feature 1 — Decision Log

Decisions made while building Feature 1 that later features may need to retrieve. This file exists so a later feature's memory-retrieval step has something concrete to pull a single fact from, instead of needing the entire build history in context.

## Decision: standard travel buffer per job is 45 minutes

**What was decided:** every booked job reserves not just its own start/end time on a technician's calendar, but an additional 45-minute travel buffer, to account for the technician getting to the next job. This is encoded as `TRAVEL_BUFFER_MINUTES = 45` in `TechnicianAvailabilityService`.

**Why it matters beyond Feature 1:** this buffer is what actually limits how many jobs — and, later, how many parts-reservations tied to those jobs — can realistically be scheduled for one technician in a day. Anything built on top of the calendar that reasons about "how many jobs can fit today" needs this number, not a guess.

**Where it lives in code:** `backend/src/main/java/com/serviceforge/service/TechnicianAvailabilityService.java`, `TRAVEL_BUFFER_MINUTES` constant.

## Decision: booking conflicts use half-open interval overlap

**What was decided:** `bookJob(...)` rejects a new booking when its job interval overlaps an existing job for the same technician. Intervals are treated as half-open (`[start, end)`), so adjacent bookings whose end and start times are equal do not overlap. The check is limited to job intervals; it does not add travel-buffer enforcement.

**Why it matters:** callers can rely on a technician not receiving bookings with overlapping job intervals, while the separate 45-minute travel-buffer policy remains unchanged.

**Standing rule:** `pipeline/rules/no-overlap-booking.md` makes this behavior mandatory for future changes.

**When doing became deciding:** the exact step was after the new 10:00–12:00 regression booking against the seeded 09:00–11:00 job failed because no exception was thrown, and before changing production code. At that point I stopped reproducing the defect and chose the interval-overlap predicate and half-open boundary semantics.

**What did not change:** the existing 45-minute travel-buffer policy (`TRAVEL_BUFFER_MINUTES = 45`) was left untouched; the fix checks job intervals only.

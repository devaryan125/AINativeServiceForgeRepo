# No overlapping technician bookings

When booking a job for a technician, reject it if its time interval overlaps any existing job
for that same technician. Treat job intervals as half-open (`[start, end)`): they overlap exactly
when `existing.start < requested.end` and `requested.start < existing.end`. A booking that starts
when another ends is allowed.

Reject missing or non-positive time ranges as bad input. Ensure the overlap check and save are
atomic so simultaneous requests cannot both book overlapping time for one technician.

Return the repository's standard conflict response for rejected bookings. Cover nested and partial
overlaps, both touching endpoints, invalid time ranges, concurrent overlapping requests, and the
HTTP conflict/bad-request responses in tests.

This rule concerns job intervals only. Do not apply or change the 45-minute travel-buffer policy
as part of overlap detection; that decision remains separately documented in
`pipeline/decisions/feature-1-decisions.md`.

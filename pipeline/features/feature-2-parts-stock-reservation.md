# Feature 2 — Parts Stock and Reservation

| Field | Value |
|---|---|
| **Status** | Candidate-built / in progress |
| **One-line intent** | Check part stock availability and reserve requested quantities for a scheduled job. |
| **Depends on** | None — no named Feature 1 decision governs inventory behavior. Reservation requests reference jobs and statuses provided by Feature 1. |
| **In scope** | Check each known part's on-hand, reserved, and available quantities, where available equals on-hand minus active reservations. For a positive whole-unit request against an existing `SCHEDULED` job, reserve available units up to the requested amount. For partial stock, report the unfilled amount without storing it as an order or backorder. Reject unknown parts/jobs, ineligible jobs, invalid quantities, and requests when no stock is available. Apply reservation updates atomically and prevent concurrent over-reservation. Keep inventory and reservation data in the in-memory mock store. Assumption: accepted reservations remain active for the lifetime of the mock data; they are not automatically released or consumed. |
| **Out of scope** | Backorder tracking or fulfillment, purchasing, replenishment, receiving, stock adjustments, low-stock alerts, external inventory integrations, and reservation release, consumption, transfer, or automatic cancellation when a job changes or is removed. |
| **Definition of Done** | - A stock check reports correct on-hand, reserved, and available quantities.<br>- A fully stocked request reserves the full quantity; a partially stocked request reserves only available units and reports the unfilled amount without persisting it.<br>- A zero-stock request is rejected without creating an order or reservation or changing state.<br>- Reservations for different jobs aggregate for the same part.<br>- Invalid quantities and unknown part/job references fail without state changes.<br>- Completed or cancelled jobs cannot receive new reservations.<br>- Concurrent requests cannot cause reserved stock to exceed on-hand stock.<br>- Accepted partial requests account for the requested quantity as reserved plus unfilled, with no unfilled quantity stored.<br>- All rejected requests return the specified `ApiError` response. |

## Artifacts this feature touches
<filled in as the developer and tester agents do their work>

## Requirements detail

### Dependencies

- **Feature 1 job model:** reservations reference jobs created by the scheduling feature. A job must exist and have status `SCHEDULED` when the reservation is made.
- **Feature 1 decisions:** no named calendar decision is required. The 45-minute travel buffer and half-open technician-booking interval rules do not affect stock calculations or reservation eligibility.
- **Project constraints:** use the in-memory mock store; do not introduce a database or external service.

### Definitions and assumptions

- **On-hand:** the seeded quantity of a part in the mock inventory.
- **Reserved:** the sum of active reservations for that part.
- **Available:** `onHand - reserved`.
- **Unfilled quantity:** requested quantity minus quantity reserved by the current request. It is returned only for a partially fulfilled request and is not stored as a backorder or future order.
- Quantities are whole, non-negative units; a reservation quantity must be a positive whole number.
- A request is made for exactly one part and one job.
- Every accepted reservation stays active for the lifetime of the mock data. Changes to a job's status do not release an existing reservation.
- Repeating a request is a new request, not an idempotent retry; it is evaluated against current availability. No unfilled quantity from an earlier request is retained.

### Functional requirements

1. The system shall expose each known part's on-hand, reserved, and available quantities.
2. The system shall calculate reserved stock from active reservations and available stock as `onHand - reserved`.
3. The system shall allow a request for exactly one known part and one existing `SCHEDULED` job.
4. The system shall reject a request if its quantity is zero, negative, or fractional.
5. The system shall reject requests referencing an unknown part, unknown job, or job that is not `SCHEDULED`.
6. If available stock is positive, the system shall reserve `min(requested quantity, available quantity)`. If the request is only partially fulfilled, the system shall report the unfilled quantity without storing it as an order or backorder. On-hand stock shall remain unchanged.
7. If available stock is zero, the system shall reject the request with `409` and an `ApiError`; it shall not create an order or reservation or change state.
8. For an accepted request, reserved quantity plus unfilled quantity shall equal requested quantity. Unfilled quantity shall be zero when the full request is reserved and shall not be persisted.
9. Reservation validation and update shall be atomic, including when concurrent requests compete for the same remaining stock. The system shall not allow reserved stock to exceed on-hand stock.
10. A rejected request shall not create a reservation or change inventory or reservation values.
11. Validation errors shall follow project response conventions: malformed or invalid quantities return `400` with an `ApiError`; unknown part/job references return `404` with an `ApiError`; a job that is no longer scheduled or zero available stock returns `409` with an `ApiError`.
12. A successful response shall identify the requested quantity, reserved quantity, unfilled quantity, and outcome (`RESERVED` or `PARTIALLY_RESERVED`). A fully or partially fulfilled valid request returns `201`.

### Use cases

| ID | Scenario | Expected handling |
|---|---|---|
| UC-1 | Requested quantity is less than or equal to available stock. | Reserve the full quantity; report unfilled quantity as `0`. |
| UC-2 | Some stock is available, but less than the requested quantity. | Reserve all available units and report the remaining quantity as unfilled. Do not store an order or backorder for it. |
| UC-3 | No stock is available for a valid request. | Reject with `409`; create no order or reservation and leave state unchanged. |
| UC-4 | Multiple jobs request the same part. | Aggregate reservations against the same available stock; never reserve more than on-hand stock. |

### Acceptance criteria

1. **Stock summary:** Given a part with on-hand quantity `10` and active reservations totaling `3`, when stock is checked, the response reports on-hand `10`, reserved `3`, and available `7`.
2. **UC-1 — Fully stocked request:** Given a scheduled job and a part with available quantity `7`, when `4` units are requested, the system reserves `4`, reports unfilled quantity `0`, leaves on-hand unchanged, and reports available quantity `3`.
3. **UC-2 — Partial stock:** Given a scheduled job, a part with available quantity `3`, and a request for `5`, the system reserves `3`, reports unfilled quantity `2` and outcome `PARTIALLY_RESERVED`, reports available quantity `0`, and stores no order or backorder for the remaining `2`.
4. **UC-3 — No stock:** Given a scheduled job and a part with available quantity `0`, when `5` units are requested, the request returns `409` with an `ApiError`; no order or reservation is created and all inventory/reservation values remain unchanged.
5. **Exact available amount:** Given a part with available quantity `5`, when exactly `5` units are requested, the system reserves `5`, reports unfilled quantity `0`, and reports available quantity `0`.
6. **Reservations aggregate:** Given reservations for different scheduled jobs against the same part, when stock is checked, reserved equals the sum of those reservations.
7. **Invalid quantity:** Given any known part and scheduled job, when quantity is zero, negative, or fractional, the request returns `400` and inventory and reservation data remain unchanged.
8. **Unknown references:** When a request uses an unknown part ID or job ID, it returns `404` and does not change inventory or reservation data.
9. **Ineligible job:** When a request references a completed or cancelled job, it returns `409` and does not change inventory or reservation data.
10. **Concurrent requests:** Given only `5` units available, when concurrent valid requests collectively request more than `5`, accepted reservations total no more than `5`; each successful response reports its own unfilled amount; requests evaluated after stock reaches zero are rejected without state changes.
11. **Repeated request:** Given the same request is submitted twice, each submission is independently evaluated against current availability; an unfilled quantity from the first request is not retained or ordered.
12. **Response shape:** Each accepted request returns `201` with requested quantity, reserved quantity, unfilled quantity, and outcome. Each rejected request returns the specified HTTP status and an `ApiError` body.

### Edge cases

| Case | Expected behavior |
|---|---|
| Part has zero on-hand stock and no reservations | Report available as `0`; reject a reservation request with `409`; create no order or reservation. |
| All on-hand stock is already reserved | Report available as `0`; reject further requests without mutation. |
| Requested amount exactly equals available amount | Reserve the full request, report unfilled quantity `0`, and leave available at `0`. |
| Requested amount exceeds positive available stock | Reserve the available amount and report the remainder as unfilled; do not persist the remainder. |
| Quantity is zero, negative, or not a whole unit | Return `400`; preserve state. |
| Part or job does not exist | Return `404`; preserve state. |
| Job is completed or cancelled before a new reservation | Return `409`; preserve state. |
| Job status changes after a reservation succeeds | Keep the existing reservation; do not auto-release or cancel it. |
| Same request is submitted again | Evaluate against current availability; do not retain any previously unfilled amount. |
| Concurrent requests compete for the final units | Atomically allocate stock so total reservations cannot exceed on-hand. |

### Metrics and verification targets

These are feature correctness metrics for tests and acceptance review; they do not require a new telemetry or analytics system.

| Metric | Definition | Target |
|---|---|---|
| Availability calculation accuracy | Stock checks where `available = onHand - reserved` and all values match current reservations | 100% |
| Stock invariant violations | Observed states where `reserved > onHand` or `available < 0` | 0 |
| Rejected-request mutation rate | Rejected requests that change inventory or reservation state, divided by all rejected requests | 0% |
| Request quantity accounting accuracy | Accepted requests where reserved quantity plus reported unfilled quantity differs from requested quantity | 0% |
| Acceptance criteria pass rate | Passing acceptance criteria divided by all criteria above | 100% before feature completion |

No latency or throughput target is set because the feature uses in-memory mock data and no performance baseline has been defined.

### Error behavior

| Condition | HTTP status | State change |
|---|---:|---|
| Invalid quantity or malformed request | 400 | None |
| Unknown part or job | 404 | None |
| Job not scheduled or no available stock | 409 | None |
| Valid request with full or partial stock | 201 | Reserve available units; on-hand unchanged; report but do not store any unfilled quantity |

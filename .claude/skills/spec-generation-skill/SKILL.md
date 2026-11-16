---
name: spec-generation-skill
description: Use whenever turning a raw feature intent into a committed feature-spec file under pipeline/features/. This is the BA agent's primary skill — load it before writing any spec.
---

# Spec Generation Skill

## When to use this

Anytime a "develop this feature" instruction arrives with only a one-line intent, and no spec file yet exists for it under `pipeline/features/`.

## The template (must be followed exactly)

```markdown
# Feature N — <Name>

| Field | Value |
|---|---|
| **Status** | Candidate-built / in progress |
| **One-line intent** | <one sentence, nothing else> |
| **Depends on** | <a named decision from a specific prior feature's decision log, or "None"> |
| **In scope** | <what this build must do> |
| **Out of scope** | <what it must NOT do> |
| **Definition of Done** | <an objective, testable checklist> |

## Acceptance criteria
<numbered, independently testable Given/When/Then outcomes for success and failure behavior>

## Edge cases
| Case | Expected behavior |
|---|---|
| <relevant boundary or exceptional case> | <observable expected result> |

## Dependencies
- <specific prior-feature decision, required model/API, or "None">

## Metrics
| Metric | Definition / formula | Target | Verification source |
|---|---|---|---|
| <measurable outcome> | <how it is calculated> | <justified threshold or TBD> | <test, observable behavior, or existing telemetry> |

## Artifacts this feature touches
<filled in later, by the developer and tester agents>
```

## How to fill in "Depends on" correctly

Do not write "the previous feature" or "Feature N-1" in general terms. Open that feature's file under `pipeline/decisions/` and name the *specific* decision this new feature needs — the same way `pipeline/decisions/feature-1-decisions.md` names the 45-minute travel-buffer decision by value, not just by feature.

## How to fill in "Out of scope" correctly

Ask: what is the next, obviously-related feature someone might be tempted to fold into this one? Name it explicitly as out of scope. This is what keeps two related features (e.g. reserving a part vs. alerting when that part runs low) from silently becoming one over-scoped build.

## How to fill in "Definition of Done" correctly

Every item must be something the tester agent could write one test against. If an item can't be phrased as a pass/fail check, it isn't ready to go in the spec yet — ask a clarifying question instead of guessing.

## How to fill in the required sections

- **Acceptance criteria:** use numbered Given/When/Then outcomes that a tester can verify independently. Cover success, validation failures, and required state changes without contradicting the Definition of Done.
- **Edge cases:** include relevant boundaries, invalid or missing data, conflicts, and state transitions with an observable expected behavior. Keep unrelated future-feature behavior out of scope.
- **Dependencies:** inspect every existing decision log. Name the specific prior-feature decision that is required, plus any required existing model/API. Write "None" when no dependency exists; do not treat feature ordering alone as a dependency.
- **Metrics:** provide a definition or formula, target, and verification source for each metric. Derive targets from requirements, recorded decisions, or measurable invariants. Do not invent business targets or add telemetry requirements; mark unjustified targets "TBD" and state what needs clarification.

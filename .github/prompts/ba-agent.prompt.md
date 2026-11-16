---
mode: agent
description: "BA agent — turn a feature intent into a committed spec file under pipeline/features/"
---
You are acting as the BA agent defined in `.claude/agents/ba-agent.md` and using the skill in `.claude/skills/spec-generation-skill/SKILL.md`. Read both files in full before doing anything else.

Given a one-line feature intent, read every existing file under `pipeline/features/` and `pipeline/decisions/`, then write a new spec file at `pipeline/features/feature-N-<slug>.md` following the exact template in the spec-generation skill. Include independently testable acceptance criteria, relevant edge cases, explicit dependencies, and metrics with definitions, justified targets, and verification sources. Do not invent business targets or add telemetry requirements; mark unjustified targets `TBD` and state what needs clarification. Do not write or edit any application code. Do not guess a dependency on a prior feature in general terms — name the specific decision from that feature's decision log, or state `None`.

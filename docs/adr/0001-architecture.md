# ADR-0001: LabOps-LLM ⊣ Research Integrity Governor architecture

## Status

Accepted. `cloud-itonami-isic-7210` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-7210` publishes an OSS business blueprint for
research and experimental development on natural sciences and
engineering: systematic creative work to increase scientific/
technical knowledge and its application. Like every prior actor in
this fleet, the blueprint alone is not an implementation: this ADR
records the governed-actor architecture that promotes it to real,
tested code, following the same langgraph-clj StateGraph + independent
Governor + Phase 0→3 rollout pattern established by `cloud-itonami-
isic-6511` (life insurance) and applied across fifty-five prior
siblings, most recently `cloud-itonami-isic-7320` (market research and
public opinion polling).

## Decision

### Decision 1: single-actuation shape

This blueprint's own README, business-model.md and operator-guide.md
consistently name only ONE real-world act: "publishing/submitting a
findings report." Matching `leasing`/`underwriting`/`testlab`/
`clinic`/`veterinary`/`funeral`/`parksafety`/`salon`/`entertainment`/
`facility`/`consulting`/`advertising`/`polling`'s single-actuation
shape, `high-stakes` here is a one-member set,
`#{:actuation/publish-findings-report}`.

### Decision 2: entity and op shape

The primary entity is a `study`. Four ops: `:study/intake` (directory
upsert, no capital risk), `:protocol/verify` (per-jurisdiction
research-integrity evidence checklist, never auto), `:risk/screen`
(data-reproducibility-risk screening, unconditional-evaluation
discipline, never auto), and `:actuation/publish-findings-report`
(POSITIVE, high-stakes -- publishing/submitting a real findings
report).

### Decision 3: `replication-count-insufficient?` -- the 7th MINIMUM-threshold check

Following `veterinary.registry/withdrawal-period-insufficient?` (1st,
temporal), `funeral.registry/waiting-period-elapsed?` (2nd, temporal),
`hospital.registry/observation-period-elapsed?` (3rd, temporal),
`association.registry/continuing-education-hours-insufficient?` (4th,
generalized to non-temporal), `secondary.registry/attendance-hours-
insufficient?` (5th, non-temporal) and `polling.registry/sample-size-
insufficient?` (6th, non-temporal), `research.registry/replication-
count-insufficient?` applies the same minimum-floor comparison to a
study's own actual replication count against its own recorded
minimum-required replication count -- a direct, natural mapping onto
real experimental-science reproducibility practice. Gates only
`:actuation/publish-findings-report`.

### Decision 4: `data-reproducibility-risk-unresolved-violations` -- the 40th unconditional-evaluation screening grounding, a genuinely new concept

Before writing this check, every prior sibling's governor/registry
namespaces were grepped for `reproduc` and `replicat` -- zero hits,
confirming this is a genuinely new concept, avoiding the false-
precedent-claim risk `leasing`'s ADR-0001 documents.
`data-reproducibility-risk-unresolved-violations` reuses the
unconditional-evaluation DISCIPLINE (`casualty.governor/sanctions-
violations`'s original fix) for the 40th distinct application
overall, continuing the count established across this window's
builds (water=25th ... polling=39th, research=40th). Grounded
directly in this blueprint's own Trust Control "fabricated or
unreproducible data forces a hold, not an override." Gates
`:risk/screen` and `:actuation/publish-findings-report`.

### Decision 5: dedicated double-actuation-guard boolean

`:findings-report-published?` is a dedicated boolean on the `study`
record, never a single `:status` value -- the same discipline every
prior sibling governor's guards establish, informed by `cloud-
itonami-isic-6492`'s real status-lifecycle bug (ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`research.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in `test/research/
store_contract_test.clj` -- the same seam every sibling actor uses so
swapping the SSoT backend is a configuration change, not a rewrite.
The protocol's per-entity accessor is named `study` directly -- not a
Clojure special form, so no `-of` suffix workaround was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:study/intake` (no
capital risk). `:protocol/verify` and `:risk/screen` are never auto-
eligible at any phase (matching every sibling's screening-op
posture), and `:actuation/publish-findings-report` is permanently
excluded from every phase's `:auto` set -- a structural fact, not a
rollout milestone, enforced by BOTH `research.phase` and `research.
governor`'s `high-stakes` set independently.

### Decision 8: no bespoke domain capability lib as a code dependency (despite blueprint.edn requiring `:cae`)

This blueprint's own `:itonami.blueprint/required-technologies`
uniquely names `:cae` (computer-aided engineering) beyond the generic
stack. This R0 implementation does NOT add a `:cae` backing library as
an actual `deps.edn` dependency, following the same posture `banking`
(`:banking`/`:swift`) and `aerospace`/`fab` (`:cae`/`:eda`) already
established: implement the specific ground-truth check a governor
needs directly (here, a plain numeric replication-count floor in
`research.registry`) rather than pull in an external capability
library for a governed-actor scaffold this narrow in scope.

### Decision 9: mock + LLM advisor pair

`research.researchadvisor` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-
publishing a findings report).

### Decision 10: no `blueprint.edn` field-sync fixes needed

Matching `advertising`/7310's and `polling`/7320's own experience,
this repo's `blueprint.edn` already had the correct `isic-` prefixed
`:id` and correctly populated `:required-technologies`/`:optional-
technologies` (including `:cae`) matching the `kotoba-lang/industry`
registry's own entry for `"7210"` exactly -- only the `:maturity`
field itself needed adding.

## Alternatives considered

- **A dual-actuation shape** (e.g. adding a separate "submit grant
  report" actuation alongside findings-report publication). Rejected:
  the blueprint's own text consistently names only ONE real-world act;
  inventing a second would not be grounded in the blueprint's own
  text.
- **A single "research-integrity" check merging replication-count and
  data-reproducibility-risk concerns.** Rejected: replication-count is
  a ground-truth numeric recompute needing no proposal inspection;
  data-reproducibility-risk status is an unconditionally-evaluated
  flag that must also HARD-hold the screening op itself on its own
  finding -- merging them would lose the screening op's self-hold
  property.
- **Adding a `:cae` backing library as a real dependency.** Rejected
  for this R0: matching `aerospace`/`fab`'s own precedent, this
  governed-actor scaffold is narrow enough in scope that a plain
  numeric ground-truth check suffices; a production operator wiring
  this to a real simulation/instrument integration would add the
  library at that layer.

## Consequences

- Fifty-sixth actor in this fleet (55 implemented before this build).
- Confirms the MINIMUM-threshold sufficiency check family generalizes
  to a seventh instance, genuinely distinct domain (experimental
  reproducibility).
- Establishes a genuinely NEW unconditional-evaluation-screening
  concept (data-reproducibility-risk), grep-verified absent from every
  prior sibling before the claim was finalized.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/research/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself, matching
  `advertising`'s and `polling`'s own experience.

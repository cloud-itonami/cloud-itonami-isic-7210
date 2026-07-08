# cloud-itonami-isic-7210

Open Business Blueprint for **ISIC Rev.5 7210**: Research and
experimental development on natural sciences and engineering.

This repository publishes an R&D-lab actor -- study intake, research-
integrity evidence assessment, data-reproducibility-risk screening
and findings-report publication -- as an OSS business that any
qualified, licensed research operator can fork, deploy, run, improve
and sell, so a community or independent lab never surrenders customer
data and ledgers to a closed SaaS.

Built on this workspace's
[`langgraph-clj`](https://github.com/com-junkawasaki/langgraph-clj)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691),
[`8569`](https://github.com/cloud-itonami/cloud-itonami-isic-8569),
[`6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419),
[`7310`](https://github.com/cloud-itonami/cloud-itonami-isic-7310),
[`7320`](https://github.com/cloud-itonami/cloud-itonami-isic-7320)) --
here it is **LabOps-LLM ⊣ Research Integrity Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a
> study-intake summary, normalizing records, and checking whether a
> study's own actual replication count actually reaches its own
> recorded minimum requirement -- but it has **no notion of which
> jurisdiction's research-integrity law is official, no license to
> publish a real findings report, and no way to know on its own
> whether a data-reproducibility risk against a study has actually
> stayed unresolved**. Letting it publish a findings report directly
> invites fabricated regulatory citations, a report drawn from an
> under-replicated study, and unreproducible data being quietly
> published as fact -- and liability, and scientific-integrity risk,
> for whoever runs it. This project seals the LabOps-LLM into a single
> node and wraps it with an independent **Research Integrity
> Governor**, a human **approval workflow**, and an immutable **audit
> ledger**.

## Scope: what this actor does and does not do

This actor covers study intake through research-integrity evidence
assessment, data-reproducibility-risk screening and findings-report
publication. It does **not**, by itself, hold any professional
license required to operate as an R&D lab in a given jurisdiction,
and it does not claim to. It also does **not** design or run the
experiment itself, or judge the substantive scientific correctness of
findings -- `research.registry/replication-count-insufficient?` is a
pure floor recompute against the study's own recorded fields, not a
scientific review. Whoever deploys and operates a live instance (a
licensed research lab) supplies any jurisdiction-specific license, the
real experimental operations and the real lab-notebook/instrument
integrations, and bears that jurisdiction's liability -- the software
supplies the governed, spec-cited, audited execution scaffold so that
lab does not have to build the compliance layer from scratch.

### Actuation

**Publishing/submitting a real findings report is never autonomous,
at any phase, by construction.** Two independent layers enforce this
(`research.governor`'s `:actuation/publish-findings-report` high-
stakes gate and `research.phase`'s phase table, which never puts
`:actuation/publish-findings-report` in any phase's `:auto` set) --
see `research.phase`'s docstring and `test/research/phase_test.clj`'s
`publish-findings-report-never-auto-at-any-phase`. The actor may
draft, check and recommend; a human research operator/PI is always
the one who actually publishes a findings report. Matching
`leasing`'s/`underwriting`'s/`testlab`'s/`clinic`'s/`veterinary`'s/
`funeral`'s/`parksafety`'s/`salon`'s/`entertainment`'s/`facility`'s/
`consulting`'s/`advertising`'s/`polling`'s single-actuation shape,
grounded directly in this blueprint's own README text ("No automated
proposal, by itself, can complete the following without governor
approval and audit evidence: publishing/submitting a findings
report") -- a POSITIVE actuation (publishing a real record), matching
this fleet's majority actuation shape (`3600`/`6190` are the fleet's
two NEGATIVE-actuation exceptions).

## The core contract

```
study intake + jurisdiction facts (research.facts, spec-cited)
        |
        v
   ┌──────────────┐   proposal      ┌───────────────────────┐
   │ LabOps-LLM   │ ─────────────▶ │ Research Integrity             │  (independent system)
   │ (sealed)     │  + citations    │ Governor:                    │
   └──────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · replication-     │
          │                         │ count-insufficient (floor) ·  │
    record + ledger        escalate ┼ data-reproducibility-risk-      │
          │              (ALWAYS for│ unresolved (unconditional) ·    │
          │               :actuation│ already-published                │
          │               /publish- └───────────────────────┘
          ▼               findings-
      human approval      report)
```

**The LabOps-LLM never publishes a findings report the Research
Integrity Governor would reject, and never does so without a human
sign-off.** Hard violations (fabricated regulatory requirements;
unsupported evidence; a replication count below its own minimum; an
unresolved data-reproducibility risk; a double publication) force
**hold** and *cannot* be approved past; a clean publication proposal
still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean single-actuation lifecycle + four HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a lab-automation robot
performs physical sample handling and instrument operation, under the
actor, gated by the independent **Research Integrity Governor**. The
governor never dispatches hardware itself; `:high`/`:safety-critical`
actions require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Research Integrity Governor, findings-report draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`7210`), which additionally names a `:cae` (computer-aided engineering)
required technology beyond the generic robotics/identity/forms/dmn/
bpmn/audit-ledger stack, resolving through
[`kotoba-lang/technology`](https://github.com/kotoba-lang/technology)'s
registry. This R0 governed-actor implementation does not add a `:cae`
backing library as a code dependency -- following this fleet's
established convention of implementing the specific ground-truth
checks a governor needs directly (here, a plain numeric replication-
count floor in `research.registry`), the same posture `banking`'s
`:banking`/`:swift` capability entries and `aerospace`'s/`fab`'s
`:cae`/`:eda` entries already take. A production operator wiring this
actor to a real lab-instrument/simulation integration would swap in
the backing `:cae` library at that layer.

## Layout

| File | Role |
|---|---|
| `src/research/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + findings-report history. No dynamically-filed sub-record -- the actuation op acts directly on a pre-seeded study, and the double-actuation guard checks a dedicated `:findings-report-published?` boolean rather than a `:status` value |
| `src/research/registry.cljc` | Findings-report draft records, plus `replication-count-insufficient?` -- the SEVENTH instance of this fleet's MINIMUM-threshold sufficiency check family (`veterinary`/`funeral`/`hospital` established the first three, all temporal; `association`/`secondary`/`polling` generalized it to non-temporal ground truths as the fourth, fifth and sixth) |
| `src/research/facts.cljc` | Per-jurisdiction research-integrity catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/research/researchadvisor.cljc` | **LabOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/protocol-verification/data-reproducibility-risk-screening/findings-report-publication proposals |
| `src/research/governor.cljc` | **Research Integrity Governor** -- 3 HARD checks (spec-basis · evidence-incomplete · replication-count-insufficient, pure ground-truth floor recompute · data-reproducibility-risk-unresolved, unconditional evaluation, the FORTIETH grounding of this discipline, a genuinely new concept grounded in this blueprint's own Trust Control text) + already-published guard + 1 soft (confidence/actuation gate) |
| `src/research/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (findings-report publication always human; study intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/research/operation.cljc` | **OperationActor** -- langgraph-clj StateGraph |
| `src/research/sim.cljc` | demo driver |
| `test/research/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers study intake through research-integrity evidence
assessment, data-reproducibility-risk screening and findings-report
publication -- the core governed lifecycle this blueprint's own
`docs/business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Study intake + per-jurisdiction research-integrity checklisting, HARD-gated on an official spec-basis citation (`:study/intake`/`:protocol/verify`) | Real lab-instrument/simulation integration, real experimental operations itself (see `research.facts`'s docstring) |
| Data-reproducibility-risk screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:risk/screen`) | Any scientific/substantive research judgment itself -- deliberately outside this actor's competence |
| Findings-report publication, HARD-gated on full evidence and the study's own minimum-replication-count floor, plus a double-publication guard (`:actuation/publish-findings-report`) | |
| Immutable audit ledger for every intake/verification/screening/publication decision | |

Extending coverage is additive: add the next gate (e.g. a data-
availability-statement check) as its own governed op with its own
HARD checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world act"
pattern this repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`research.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `research.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `research.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `LabOps-LLM` + `Research Integrity Governor` run as
real, tested code (see `Run` above), promoted from the originally-
published `:blueprint`-tier scaffold, modeled closely on the fifty-
five prior actors' architecture. See `docs/adr/0001-architecture.md`
for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.

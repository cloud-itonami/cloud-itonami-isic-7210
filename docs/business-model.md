# Business Model: Research and experimental development on natural sciences and engineering

## Classification

- Repository: `cloud-itonami-isic-7210`
- ISIC Rev.5: `7210`
- Activity: research and experimental development on natural sciences and engineering -- systematic creative work to increase scientific/technical knowledge and its application
- Social impact: professional standards, data sovereignty, transparent audit

## Customer

- independent R&D labs
- cooperative research consortia
- community/university-adjacent research programs

## Offer

- experiment/protocol intake
- data-collection proposal
- findings-report proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per lab
- support: monthly retainer with SLA
- migration: import from an incumbent lab-notebook system
- per-study fee

## Trust Controls

- no findings report is published/submitted without human sign-off
- fabricated or unreproducible data forces a hold, not an override
- every publication path is auditable
- emergency manual override paths remain outside LLM control
- a fabricated jurisdiction citation, incomplete evidence, or a study
  replicated fewer times than its own recorded minimum requirement -- each
  forces a hold, not an override
- findings-report publication is logged and escalated, and cannot be
  finalized twice for the same study: a double-publication attempt is held
  off this actor's own study facts alone, with no upstream comparison
  needed

## Research Integrity Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:research-
integrity-governor` -- this is not a generic "review step," it is the
one gate the ONE real-world act this business performs (publishing/
submitting a findings report) must pass. The governor sits between
the LabOps-LLM and execution, per the README's Core Contract:

```text
LabOps-LLM -> Research Integrity Governor -> hold, proceed, or human approval
```

**Approves**: routine research actions proposed against a study that
already has a consented protocol on file, a replication count meeting
its own recorded minimum requirement, and no unresolved data-
reproducibility risk. These proceed straight to the engagement
ledger.

**Rejects or escalates**: the governor refuses to let the advisor
publish a findings report on its own authority when any of the
following hold -- a fabricated jurisdiction spec-basis; incomplete
evidence; a replication count below its own minimum requirement; an
unresolved data-reproducibility risk. A clean publication proposal
still always routes to a human -- `:actuation/publish-findings-
report` is never auto-committed, at any rollout phase.

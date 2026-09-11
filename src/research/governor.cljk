(ns research.governor
  "Research Integrity Governor -- the independent compliance layer
  that earns the LabOps-LLM the right to commit. The LLM has no
  notion of research-integrity/good-scientific-practice law, whether
  a study's own actual replication count actually reaches its own
  recorded minimum requirement, whether a data-reproducibility risk
  against a study has actually stayed unresolved, or when an act stops
  being a draft and becomes a real-world findings-report publication,
  so this MUST be a separate system able to *reject* a proposal and
  fall back to HOLD -- the R&D-lab analog of `cloud-itonami-isic-
  6512`'s CasualtyGovernor.

  Four checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence, an
  under-replicated study, or an unresolved data-reproducibility risk).
  The confidence/actuation gate is SOFT: it asks a human to look (low
  confidence / actuation), and the human may approve -- but see
  `research.phase`: for `:stake :actuation/publish-findings-report` (a
  real publication/submission act) NO phase ever allows auto-commit
  either. Two independent layers agree that actuation is always a
  human call.

    1. Spec-basis                  -- did the protocol proposal cite
                                       an OFFICIAL source (`research.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/publish-
                                       findings-report`, has the study
                                       actually been assessed with a
                                       full experiment-protocol-
                                       record/data-collection-record/
                                       methodology-citation-record/
                                       replication-record evidence
                                       checklist on file?
    3. Replication count
       insufficient                   -- for `:actuation/publish-
                                       findings-report`, INDEPENDENTLY
                                       recompute whether the study's
                                       own actual replication count
                                       falls short of its own recorded
                                       minimum-required replication
                                       count (`research.registry/
                                       replication-count-
                                       insufficient?`) -- needs no
                                       proposal inspection at all. The
                                       SEVENTH instance of this
                                       fleet's MINIMUM-threshold
                                       sufficiency check family
                                       (`veterinary.governor/
                                       withdrawal-period-insufficient-
                                       violations`/`funeral.governor/
                                       waiting-period-elapsed-
                                       violations`/`hospital.governor/
                                       observation-period-elapsed-
                                       violations` established the
                                       first three, all TEMPORAL;
                                       `association.governor/
                                       continuing-education-hours-
                                       insufficient-violations` the
                                       fourth, non-temporal;
                                       `secondary.governor/attendance-
                                       hours-insufficient-violations`
                                       the fifth; `polling.governor/
                                       sample-size-insufficient-
                                       violations` the sixth).
    4. Data-reproducibility risk
       unresolved                     -- reported by THIS proposal
                                       itself (a `:risk/screen` that
                                       just found one), or already on
                                       file for the study (`:risk/
                                       screen`/`:actuation/publish-
                                       findings-report`). Evaluated
                                       UNCONDITIONALLY (not scoped to
                                       a specific op), the SAME
                                       discipline `casualty.governor/
                                       sanctions-violations`/...
                                       (thirty-nine prior siblings,
                                       most recently `polling.
                                       governor/unrepresentative-
                                       sample-risk-unresolved-
                                       violations`)...established --
                                       the FORTIETH distinct
                                       application of this exact
                                       discipline overall, and a
                                       genuinely NEW concept (grep-
                                       verified absent from every
                                       prior sibling's check names
                                       before this claim was
                                       finalized -- 'reproduc'/
                                       'replicat' returned zero hits),
                                       grounded directly in this
                                       blueprint's own Trust Control
                                       'fabricated or unreproducible
                                       data forces a hold, not an
                                       override'.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       publish-findings-report` (a REAL
                                       publication/submission act) ->
                                       escalate.

  One more guard, double-publication prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-published-violations` refuses to
  publish a findings report for the SAME study twice, off a dedicated
  `:findings-report-published?` fact (never a `:status` value) -- the
  SAME 'check a dedicated boolean, not status' discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [research.facts :as facts]
            [research.registry :as registry]
            [research.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Publishing/submitting a real findings report is the ONE real-world
  actuation event this actor performs -- a single-member set, matching
  `leasing`'s/`underwriting`'s/`testlab`'s/`clinic`'s/`veterinary`'s/
  `funeral`'s/`parksafety`'s/`salon`'s/`entertainment`'s/`facility`'s/
  `consulting`'s/`advertising`'s/`polling`'s single-actuation shape,
  grounded directly in this blueprint's own README ('No automated
  proposal, by itself, can complete the following without governor
  approval and audit evidence: publishing/submitting a findings
  report')."
  #{:actuation/publish-findings-report})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:protocol/verify` (or `:actuation/publish-findings-report`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's research-integrity requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:protocol/verify :actuation/publish-findings-report} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は研究公正性基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/publish-findings-report`, the jurisdiction's
  required experiment-protocol-record/data-collection-record/
  methodology-citation-record/replication-record evidence must
  actually be satisfied -- do not trust the advisor's self-reported
  confidence alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/publish-findings-report)
    (let [s (store/study st subject)
          protocol (store/protocol-of st subject)]
      (when-not (and protocol
                     (facts/required-evidence-satisfied?
                      (:jurisdiction s) (:checklist protocol)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(実験計画記録/データ収集記録/方法論引用記録/再現実験記録等)が充足していない状態での提案"}]))))

(defn- replication-count-insufficient-violations
  "For `:actuation/publish-findings-report`, INDEPENDENTLY recompute
  whether the study's own actual replication count falls short of its
  own recorded minimum-required replication count via `research.
  registry/replication-count-insufficient?` -- needs no proposal
  inspection at all, since its inputs are permanent ground-truth
  fields already on the study."
  [{:keys [op subject]} st]
  (when (= op :actuation/publish-findings-report)
    (let [s (store/study st subject)]
      (when (registry/replication-count-insufficient? s)
        [{:rule :replication-count-insufficient
          :detail (str subject " の実再現回数(" (:actual-replication-count s)
                      ")が必要最小再現回数(" (:minimum-required-replication-count s) ")を下回る")}]))))

(defn- data-reproducibility-risk-unresolved-violations
  "An unresolved data-reproducibility risk -- reported by THIS
  proposal (e.g. a `:risk/screen` that itself just found one), or
  already on file in the store for the study (`:risk/screen`/
  `:actuation/publish-findings-report`) -- is a HARD, un-overridable
  hold. Evaluated UNCONDITIONALLY (not scoped to a specific op) so the
  screening op itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        study-id (when (contains? #{:risk/screen :actuation/publish-findings-report} op) subject)
        hit-on-file? (and study-id (= :unresolved (:verdict (store/risk-screen-of st study-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :data-reproducibility-risk-unresolved
        :detail "未解決のデータ再現性リスクがある研究の報告書公開提案は進められない"}])))

(defn- already-published-violations
  "For `:actuation/publish-findings-report`, refuses to publish a
  findings report for the SAME study twice, off a dedicated
  `:findings-report-published?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/publish-findings-report)
    (when (store/study-already-published? st subject)
      [{:rule :already-published
        :detail (str subject " は既に報告書公開済み")}])))

(defn check
  "Censors a LabOps-LLM proposal against the governor rules. Returns
  {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (replication-count-insufficient-violations request st)
                           (data-reproducibility-risk-unresolved-violations request proposal st)
                           (already-published-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})

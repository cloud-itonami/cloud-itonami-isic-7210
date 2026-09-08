(ns research.registry
  "Pure-function findings-report-publication record construction -- an
  append-only research-lab book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a findings-report reference
  number -- every research lab/jurisdiction assigns its own reference
  format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `research.facts` uses.

  `replication-count-insufficient?` is the SEVENTH instance of this
  fleet's MINIMUM-threshold sufficiency check family (`veterinary.
  registry/withdrawal-period-insufficient?`, `funeral.registry/
  waiting-period-elapsed?` and `hospital.registry/observation-period-
  elapsed?` established the first three, all TEMPORAL; `association.
  registry/continuing-education-hours-insufficient?` generalized the
  family to a non-temporal numeric ground truth as the fourth,
  `secondary.registry/attendance-hours-insufficient?` the fifth,
  `polling.registry/sample-size-insufficient?` the sixth), applying
  the SAME minimum-floor comparison to a study's own actual
  replication count against its own recorded minimum-required
  replication count -- a direct, natural mapping onto real
  experimental-science reproducibility practice (a findings report
  published from an under-replicated study is exactly the failure
  mode a research operator must not let an advisor wave through).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real lab-notebook/instrument system. It builds the
  RECORD a research lab would keep, not the act of publishing the
  findings report itself (that is `research.operation`'s `:actuation/
  publish-findings-report`, always human-gated -- see README
  `Actuation`)."
  (:require [kotoba.lang.text :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the research lab's own act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn replication-count-insufficient?
  "Does `study`'s own `:actual-replication-count` fall short of its
  own recorded `:minimum-required-replication-count`? A pure ground-
  truth check against the study's own permanent fields -- no upstream
  comparison needed. The SEVENTH instance of this fleet's MINIMUM-
  threshold sufficiency check family (see ns docstring)."
  [{:keys [actual-replication-count minimum-required-replication-count]}]
  (and (number? actual-replication-count) (number? minimum-required-replication-count)
       (< actual-replication-count minimum-required-replication-count)))

(defn register-findings-report
  "Validate + construct the FINDINGS-REPORT registration DRAFT -- the
  research lab's own act of publishing/submitting a real findings
  report. Pure function -- does not touch any real lab-notebook
  system; it builds the RECORD a lab would keep. `research.governor`
  independently re-verifies the study's own replication-count
  sufficiency and data-reproducibility-risk resolution status, and
  blocks a double-publication for the same study, before this is ever
  allowed to commit."
  [study-id jurisdiction sequence]
  (when-not (and study-id (not= study-id ""))
    (throw (ex-info "findings-report: study_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "findings-report: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "findings-report: sequence must be >= 0" {})))
  (let [report-number (str (str/upper jurisdiction) "-RPT-" (zero-pad sequence 6))
        record {"record_id" report-number
                "kind" "findings-report-draft"
                "study_id" study-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "report_number" report-number
     "certificate" (unsigned-certificate "FindingsReport" report-number report-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))

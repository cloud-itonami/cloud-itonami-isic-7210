(ns research.store
  "SSoT for the research actor, behind a `Store` protocol so the
  backend is a swap, not a rewrite -- the same seam every prior
  `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/research/store_contract_test.clj), which is the whole point:
  the actor, the Research Integrity Governor and the audit ledger
  never know which SSoT they run on.

  Like `leasing`/`underwriting`/`testlab`/`clinic`/`veterinary`/
  `funeral`/`parksafety`/`salon`/`entertainment`/`facility`/
  `consulting`/`advertising`/`polling`, this actor has ONE actuation
  event (publishing/submitting a real findings report) acting on a
  `study` entity, with its OWN history collection, sequence counter
  and dedicated double-actuation-guard boolean (`:findings-report-
  published?`, never a `:status` value) -- the same discipline every
  prior sibling governor's guards establish, informed by `cloud-
  itonami-isic-6492`'s status-lifecycle bug (ADR-2607071320).

  The ledger stays append-only on every backend: 'which study was
  screened for an unresolved data-reproducibility risk, which findings
  report was published, on what jurisdictional basis, approved by
  whom' is always a query over an immutable log -- the audit trail the
  scientific community trusting a research lab needs, and the evidence
  a lab needs if a publication decision is later disputed."
  (:require [research.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (study [s id])
  (all-studies [s])
  (risk-screen-of [s study-id] "committed data-reproducibility-risk screening verdict for a study, or nil")
  (protocol-of [s study-id] "committed protocol evidence assessment, or nil")
  (ledger [s])
  (report-history [s] "the append-only findings-report history (research.registry drafts)")
  (next-report-sequence [s jurisdiction] "next report-number sequence for a jurisdiction")
  (study-already-published? [s study-id] "has this study's findings report already been published?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-studies [s studies] "replace/seed the study directory (map id->study)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained study set covering the actuation lifecycle
  (publishing a findings report) so the actor + tests run offline."
  []
  {:studies
   {"study-1" {:id "study-1" :lab-name "Sato Materials Lab"
              :actual-replication-count 5 :minimum-required-replication-count 3
              :data-reproducibility-risk-unresolved? false
              :findings-report-published? false
              :jurisdiction "JPN" :status :intake}
    "study-2" {:id "study-2" :lab-name "Atlantis Photonics Lab"
              :actual-replication-count 5 :minimum-required-replication-count 3
              :data-reproducibility-risk-unresolved? false
              :findings-report-published? false
              :jurisdiction "ATL" :status :intake}
    "study-3" {:id "study-3" :lab-name "鈴木化学研究所"
              :actual-replication-count 1 :minimum-required-replication-count 3
              :data-reproducibility-risk-unresolved? false
              :findings-report-published? false
              :jurisdiction "JPN" :status :intake}
    "study-4" {:id "study-4" :lab-name "田中生物工学研究所"
              :actual-replication-count 5 :minimum-required-replication-count 3
              :data-reproducibility-risk-unresolved? true
              :findings-report-published? false
              :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- publish-findings-report!
  "Backend-agnostic `:study/mark-published` -- looks up the study via
  the protocol and drafts the findings-report record, and returns
  {:result .. :study-patch ..} for the caller to persist."
  [s study-id]
  (let [st (study s study-id)
        seq-n (next-report-sequence s (:jurisdiction st))
        result (registry/register-findings-report study-id (:jurisdiction st) seq-n)]
    {:result result
     :study-patch {:findings-report-published? true
                  :report-number (get result "report_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (study [_ id] (get-in @a [:studies id]))
  (all-studies [_] (sort-by :id (vals (:studies @a))))
  (risk-screen-of [_ id] (get-in @a [:risk-screens id]))
  (protocol-of [_ study-id] (get-in @a [:protocols study-id]))
  (ledger [_] (:ledger @a))
  (report-history [_] (:reports @a))
  (next-report-sequence [_ jurisdiction] (get-in @a [:report-sequences jurisdiction] 0))
  (study-already-published? [_ study-id] (boolean (get-in @a [:studies study-id :findings-report-published?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :study/upsert
      (swap! a update-in [:studies (:id value)] merge value)

      :protocol/set
      (swap! a assoc-in [:protocols (first path)] payload)

      :risk-screen/set
      (swap! a assoc-in [:risk-screens (first path)] payload)

      :study/mark-published
      (let [study-id (first path)
            {:keys [result study-patch]} (publish-findings-report! s study-id)
            jurisdiction (:jurisdiction (study s study-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:report-sequences jurisdiction] (fnil inc 0))
                       (update-in [:studies study-id] merge study-patch)
                       (update :reports registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-studies [s studies] (when (seq studies) (swap! a assoc :studies studies)) s))

(defn seed-db
  "A MemStore seeded with the demo study set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :protocols {} :risk-screens {} :ledger [] :report-sequences {}
                           :reports []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (protocol/risk-screen payloads, ledger facts, report
  records) are stored as EDN strings so `langchain.db` doesn't expand
  them into sub-entities -- the same convention every sibling actor's
  store uses. The identity-schema builder, EDN-blob codec and
  seq-keyed event-log read/append are the shared kotoba-lang/
  langchain-store machinery (ADR-2607141600) -- the seam ~190 actors
  hand-roll; this store keeps only its domain wiring."
  (ls/identity-schema
   [:study/id :protocol/study-id :risk-screen/study-id
    :ledger/seq :report/seq :report-sequence/jurisdiction]))

(defn- study->tx [{:keys [id lab-name actual-replication-count minimum-required-replication-count
                        data-reproducibility-risk-unresolved?
                        findings-report-published?
                        jurisdiction status report-number]}]
  (cond-> {:study/id id}
    lab-name                                       (assoc :study/lab-name lab-name)
    actual-replication-count                       (assoc :study/actual-replication-count actual-replication-count)
    minimum-required-replication-count             (assoc :study/minimum-required-replication-count minimum-required-replication-count)
    (some? data-reproducibility-risk-unresolved?)  (assoc :study/data-reproducibility-risk-unresolved? data-reproducibility-risk-unresolved?)
    (some? findings-report-published?)             (assoc :study/findings-report-published? findings-report-published?)
    jurisdiction                                     (assoc :study/jurisdiction jurisdiction)
    status                                           (assoc :study/status status)
    report-number                                    (assoc :study/report-number report-number)))

(def ^:private study-pull
  [:study/id :study/lab-name :study/actual-replication-count :study/minimum-required-replication-count
   :study/data-reproducibility-risk-unresolved? :study/findings-report-published?
   :study/jurisdiction :study/status :study/report-number])

(defn- pull->study [m]
  (when (:study/id m)
    {:id (:study/id m) :lab-name (:study/lab-name m)
     :actual-replication-count (:study/actual-replication-count m)
     :minimum-required-replication-count (:study/minimum-required-replication-count m)
     :data-reproducibility-risk-unresolved? (boolean (:study/data-reproducibility-risk-unresolved? m))
     :findings-report-published? (boolean (:study/findings-report-published? m))
     :jurisdiction (:study/jurisdiction m) :status (:study/status m)
     :report-number (:study/report-number m)}))

(defrecord DatomicStore [conn]
  Store
  (study [_ id]
    (pull->study (d/pull (d/db conn) study-pull [:study/id id])))
  (all-studies [_]
    (->> (d/q '[:find [?id ...] :where [?e :study/id ?id]] (d/db conn))
         (map #(pull->study (d/pull (d/db conn) study-pull [:study/id %])))
         (sort-by :id)))
  (risk-screen-of [_ id]
    (ls/dec* (d/q '[:find ?p . :in $ ?sid
                :where [?k :risk-screen/study-id ?sid] [?k :risk-screen/payload ?p]]
              (d/db conn) id)))
  (protocol-of [_ study-id]
    (ls/dec* (d/q '[:find ?p . :in $ ?sid
                :where [?a :protocol/study-id ?sid] [?a :protocol/payload ?p]]
              (d/db conn) study-id)))
  (ledger [_] (ls/read-stream conn :ledger/seq :ledger/fact))
  (report-history [_] (ls/read-stream conn :report/seq :report/record))
  (next-report-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :report-sequence/jurisdiction ?j] [?e :report-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (study-already-published? [s study-id]
    (boolean (:findings-report-published? (study s study-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :study/upsert
      (d/transact! conn [(study->tx value)])

      :protocol/set
      (d/transact! conn [{:protocol/study-id (first path) :protocol/payload (ls/enc payload)}])

      :risk-screen/set
      (d/transact! conn [{:risk-screen/study-id (first path) :risk-screen/payload (ls/enc payload)}])

      :study/mark-published
      (let [study-id (first path)
            {:keys [result study-patch]} (publish-findings-report! s study-id)
            jurisdiction (:jurisdiction (study s study-id))
            next-n (inc (next-report-sequence s jurisdiction))]
        (d/transact! conn
                     [(study->tx (assoc study-patch :id study-id))
                      {:report-sequence/jurisdiction jurisdiction :report-sequence/next next-n}
                      {:report/seq (count (report-history s)) :report/record (ls/enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (ls/append-blob! conn :ledger/seq :ledger/fact (count (ledger s)) fact)
    fact)
  (with-studies [s studies]
    (when (seq studies) (d/transact! conn (mapv study->tx (vals studies)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:studies ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [studies]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-studies s studies))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo study set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))

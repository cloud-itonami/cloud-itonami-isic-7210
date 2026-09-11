(ns research.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean study through
  intake -> protocol verification -> data-reproducibility-risk
  screening -> findings-report-publication proposal (always
  escalates) -> human approval -> commit, then shows four HARD holds
  (a jurisdiction with no spec-basis, an under-replicated study below
  its own recorded minimum-required replication count, an unresolved
  data-reproducibility risk screened directly via `:risk/screen`
  [never via an actuation op against an unscreened study -- see this
  actor's own governor ns docstring / the lesson `parksafety`'s
  ADR-2607071922 Decision 5, `eldercare`'s, `museum`'s,
  `conservation`'s, `salon`'s, `entertainment`'s, `casework`'s,
  `hospital`'s, `facility`'s, `school`'s, `association`'s, `leasing`'s,
  `behavioral`'s, `secondary`'s, `card`'s, `water`'s, `telecom`'s,
  `aerospace`'s, `recovery`'s, `consulting`'s, `union`'s,
  `congregation`'s, `fab`'s, `energy`'s, `care`'s, `navigator`'s,
  `learning`'s, `banking`'s, `advertising`'s and `polling`'s ADR-0001s
  already recorded], and a double publication of an already-processed
  study) that never reach a human at all, and prints the audit ledger
  + the draft findings-report records."
  (:require [langgraph.graph :as g]
            [research.store :as store]
            [research.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :research-operator :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== study/intake study-1 (JPN, clean; replicated 5 >= minimum 3, no data-reproducibility risk) ==")
    (println (exec! actor "t1" {:op :study/intake :subject "study-1"
                                :patch {:id "study-1" :lab-name "Sato Materials Lab"}} operator))

    (println "== protocol/verify study-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :protocol/verify :subject "study-1"} operator))
    (println (approve! actor "t2"))

    (println "== risk/screen study-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :risk/screen :subject "study-1"} operator))
    (println (approve! actor "t3"))

    (println "== actuation/publish-findings-report study-1 (always escalates -- actuation/publish-findings-report) ==")
    (let [r (exec! actor "t4" {:op :actuation/publish-findings-report :subject "study-1"} operator)]
      (println r)
      (println "-- human research-operator approves --")
      (println (approve! actor "t4")))

    (println "== protocol/verify study-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t5" {:op :protocol/verify :subject "study-2" :no-spec? true} operator))

    (println "== protocol/verify study-3 (escalates -- human approves; sets up the replication-count-insufficient test) ==")
    (println (exec! actor "t6" {:op :protocol/verify :subject "study-3"} operator))
    (println (approve! actor "t6"))

    (println "== actuation/publish-findings-report study-3 (replicated 1 < minimum 3 -> HARD hold) ==")
    (println (exec! actor "t7" {:op :actuation/publish-findings-report :subject "study-3"} operator))

    (println "== risk/screen study-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t8" {:op :risk/screen :subject "study-4"} operator))

    (println "== actuation/publish-findings-report study-1 AGAIN (double-publication -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/publish-findings-report :subject "study-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft findings-report records ==")
    (doseq [r (store/report-history db)] (println r))))

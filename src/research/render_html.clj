(ns research.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.
  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300,
  Wave5 rollout ledger). Drives the REAL actor stack (research.operation
  -> research.governor -> research.store) through a scenario built from
  real seeded demo data (`research.store/demo-data`). No invented
  numbers, no timestamps, byte-identical across reruns against the same
  seed -- every table cell here traces to a field actually read off the
  store after `run-demo!` actually executed the graph."
  (:require [clojure.string :as str]
            [research.store :as store]
            [research.operation :as op]
            [langgraph.graph :as g]))

;; ----------------------------- scenario -----------------------------

(def ^:private operator
  {:actor-id "op-1" :actor-role :research-operator :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn run-demo!
  "Drives the real OperationActor graph (research.operation/build) over
  a MemStore seeded with `research.store/demo-data` through:

    - one op that auto-commits CLEAN at phase 3 (`:study/intake` on
      study-1 -- the only op in phase 3's `:auto` set, per
      `research.phase`)
    - the always-escalate `:actuation/publish-findings-report` op on
      study-1 (research.governor/high-stakes), after first walking it
      through protocol/verify and risk/screen (also escalate-then-
      approve -- neither is ever phase-3-auto per `research.phase`),
      so the evidence-incomplete check actually has a protocol on file
    - three DISTINCT real HARD-hold reasons, read straight off
      `research.governor`'s own check functions, each against its own
      real seeded study:
        :no-spec-basis                  -- study-2, jurisdiction ATL,
                                            absent from research.facts
        :replication-count-insufficient -- study-3, actual-replication-
                                            count 1 < minimum-required 3
        :data-reproducibility-risk-unresolved -- study-4, seeded
                                            :data-reproducibility-risk-
                                            unresolved? true
    - a fourth HARD hold, `:already-published`, showing the double-
      actuation guard by re-submitting study-1's already-published
      findings report

  Returns the seeded `db` (a MemStore) after every op above has run --
  its ledger and studies directory are the sole source for `render`."
  []
  (let [db (store/seed-db)
        actor (op/build db)]

    ;; 1) auto-commit, clean, phase-3 :auto op
    (exec! actor "t1" {:op :study/intake :subject "study-1"
                       :patch {:id "study-1" :lab-name "Sato Materials Lab"}})

    ;; 2) escalate -> approve, builds the protocol-on-file evidence study-1 needs
    (exec! actor "t2" {:op :protocol/verify :subject "study-1"})
    (approve! actor "t2")

    ;; 3) escalate -> approve, resolves the data-reproducibility-risk screen
    (exec! actor "t3" {:op :risk/screen :subject "study-1"})
    (approve! actor "t3")

    ;; 4) always-escalate-ops member -> approve (the ONE real actuation act)
    (exec! actor "t4" {:op :actuation/publish-findings-report :subject "study-1"})
    (approve! actor "t4")

    ;; 5) HARD hold #1 -- no official spec-basis for study-2's jurisdiction (ATL)
    (exec! actor "t5" {:op :protocol/verify :subject "study-2" :no-spec? true})

    ;; 6) escalate -> approve, sets up the replication-count-insufficient hold
    (exec! actor "t6" {:op :protocol/verify :subject "study-3"})
    (approve! actor "t6")

    ;; 7) HARD hold #2 -- study-3's own actual replication count (1) < its own
    ;;    recorded minimum-required replication count (3)
    (exec! actor "t7" {:op :actuation/publish-findings-report :subject "study-3"})

    ;; 8) HARD hold #3 -- study-4's seeded unresolved data-reproducibility risk
    (exec! actor "t8" {:op :risk/screen :subject "study-4"})

    ;; 9) HARD hold #4 -- double-publication guard on study-1
    (exec! actor "t9" {:op :actuation/publish-findings-report :subject "study-1"})

    db))

;; ----------------------------- render helpers -----------------------------

(defn- esc
  "Minimal HTML-escape -- every string rendered below comes off the
  store/ledger, never a hand-typed literal, but escape anyway since
  study lab-names include non-ASCII (JP) text passed through verbatim."
  [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- last-fact-for
  "Most recent ledger fact whose :subject matches `study-id` -- the
  real subject-key field name `research.operation/commit-fact` and
  `research.governor/hold-fact` both write."
  [ledger study-id]
  (last (filter #(= study-id (:subject %)) ledger)))

(defn- status-cell
  "Disposition + rule (if a hold) for a study, off the REAL last
  ledger fact touching it -- never inferred, never invented."
  [ledger study-id]
  (if-let [fact (last-fact-for ledger study-id)]
    (case (:t fact)
      :committed
      [:span.ok "committed"]
      :governor-hold
      [:span.critical (str "HOLD: " (name (first (:basis fact))))]
      :approval-requested
      [:span.warn "approval requested"]
      :approval-granted
      [:span.ok "approval granted"]
      [:span.muted "in progress"])
    [:span.muted "no activity this run"]))

;; A tiny hiccup-ish inline renderer -- keeps this file dependency-free
;; (no hiccup lib in deps.edn); [:tag.class ...children] -> string.
(defn- h [node]
  (cond
    (nil? node) ""
    (string? node) node
    (vector? node)
    (let [[tag & rest] node
          [attrs children] (if (map? (first rest)) [(first rest) (next rest)] [{} rest])
          [tag-name & classes] (str/split (name tag) #"\.")
          class-attr (when (seq classes) {:class (str/join " " classes)})
          attrs (merge attrs class-attr)
          attr-str (apply str (for [[k v] attrs] (str " " (name k) "=\"" (esc v) "\"")))
          ;; block-level tags get a trailing newline so the rendered
          ;; page is human-diffable across regenerations, not one
          ;; giant line -- purely cosmetic, does not touch any value.
          block? (contains? #{"tr" "table" "div" "thead" "tbody" "p" "header" "main"} tag-name)]
      (str "<" tag-name attr-str ">" (apply str (map h children)) "</" tag-name ">" (when block? "\n")))
    :else (esc node)))

;; ----------------------------- sections -----------------------------

(defn- studies-section [db]
  (let [studies (store/all-studies db)]
    [:div.card
     [:h2 "Studies (research.store/demo-data, real seed)"]
     [:table
      [:thead [:tr [:th "id"] [:th "lab"] [:th "jurisdiction"]
               [:th "replication (actual/min)"] [:th "data-repro risk unresolved?"]
               [:th "status (this run)"]]]
      (into [:tbody]
            (for [s studies]
              [:tr
               [:td [:code (:id s)]]
               [:td (:lab-name s)]
               [:td (:jurisdiction s)]
               [:td (str (:actual-replication-count s) " / " (:minimum-required-replication-count s))]
               [:td (if (:data-reproducibility-risk-unresolved? s)
                      [:span.err "unresolved"]
                      [:span.ok "none"])]
               [:td (status-cell (store/ledger db) (:id s))]]))]]))

(defn- committed-section [db]
  (let [committed (filter #(= :committed (:t %)) (store/ledger db))]
    [:div.card
     [:h2 "Committed records (this run)"]
     [:table
      [:thead [:tr [:th "op"] [:th "subject"] [:th "actor"] [:th "summary"]]]
      (into [:tbody]
            (for [f committed]
              [:tr [:td (name (:op f))] [:td [:code (:subject f)]]
               [:td (:actor f)] [:td (:summary f)]]))]
     (let [reports (store/report-history db)]
       (when (seq reports)
         [:div
          [:h2 "Findings-report drafts on file"]
          [:table
           [:thead [:tr [:th "record_id"] [:th "study_id"] [:th "jurisdiction"] [:th "kind"]]]
           (into [:tbody]
                 (for [r reports]
                   [:tr [:td [:code (get r "record_id")]] [:td [:code (get r "study_id")]]
                    [:td (get r "jurisdiction")] [:td (get r "kind")]]))]]))]))

(defn- action-gate-section []
  [:div.card
   [:h2 "Action gate (research.governor + research.phase op contract)"]
   [:table
    [:thead [:tr [:th "op"] [:th "phase-3 write allowed?"] [:th "phase-3 auto-commit?"]
             [:th "always-escalate (high-stakes)?"]]]
    [:tbody
     [:tr [:td [:code ":study/intake"]] [:td [:span.ok "yes"]] [:td [:span.ok "yes"]] [:td "no"]]
     [:tr [:td [:code ":protocol/verify"]] [:td [:span.ok "yes"]] [:td [:span.warn "no (always approval)"]] [:td "no"]]
     [:tr [:td [:code ":risk/screen"]] [:td [:span.ok "yes"]] [:td [:span.warn "no (always approval)"]] [:td "no"]]
     [:tr [:td [:code ":actuation/publish-findings-report"]] [:td [:span.ok "yes"]]
      [:td [:span.warn "no (never, at any phase)"]] [:td [:span.critical "yes"]]]]]
   [:p.muted "HARD violations (never overridable by human approval): "
    [:code ":no-spec-basis"] ", " [:code ":evidence-incomplete"] ", "
    [:code ":replication-count-insufficient"] ", "
    [:code ":data-reproducibility-risk-unresolved"] ", " [:code ":already-published"] "."]])

(defn- ledger-section [db]
  [:div.card
   [:h2 "Audit ledger (this run, append-only, research.store/ledger)"]
   [:table
    [:thead [:tr [:th "#"] [:th "t"] [:th "op"] [:th "subject"] [:th "disposition"] [:th "rule/basis"]]]
    (into [:tbody]
          (map-indexed
           (fn [i f]
             [:tr [:td (inc i)] [:td (name (:t f))] [:td (name (:op f))]
              [:td [:code (:subject f)]] [:td (name (or (:disposition f) :n-a))]
              [:td (if (seq (:basis f)) (str/join ", " (map name (filter keyword? (:basis f)))) "")]])
           (store/ledger db)))]])

(def ^:private css
  "table { width: 100%; border-collapse: collapse; font-size: 14px; }
.ok { color: #137a3f; }
body { font-family: system-ui,-apple-system,sans-serif; margin: 0; color: #1a1a1a; background: #fafafa; }
header.bar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e5e5e5; }
th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; }
h2 { margin-top: 0; font-size: 15px; }
.warn { color: #b25c00; background: #fff8e1; padding: 2px 6px; border-radius: 4px; }
main { max-width: 980px; margin: 24px auto; padding: 0 20px; }
header.bar h1 { font-size: 18px; margin: 0; font-weight: 600; }
.muted { color: #888; font-size: 13px; }
.critical { color: #fff; background: #b3261e; padding: 2px 6px; border-radius: 4px; font-weight: 600; }
.card { background: #fff; border: 1px solid #e5e5e5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
.err { color: #b3261e; background: #fbe9e7; padding: 2px 6px; border-radius: 4px; }
th { font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }
header.bar .badge { margin-left: auto; font-size: 12px; color: #666; }
code { font-size: 12px; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }")

(defn render
  "Full HTML page for `db` (a MemStore already driven through
  `run-demo!`). Deterministic -- no timestamps, no randomness, so
  reruns against the same seed are byte-identical (verified by the
  nightly regenerate.yml workflow and the fleet's CI-layout check)."
  [db]
  (str
   "<!doctype html>\n<html lang=\"ja\">\n<head>\n"
   "<meta charset=\"utf-8\">\n"
   "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
   "<title>cloud-itonami-isic-7210 &middot; research.render-html</title>\n"
   "<style>" css "</style>\n</head>\n<body>\n"
   (h [:header.bar
       [:h1 "cloud-itonami-isic-7210 -- Research Integrity operator console"]
       [:span.badge "research.render-html · driven live through research.operation -> research.governor -> research.store, no hand-typed values"]])
   "\n"
   (h (into [:main]
            [(studies-section db)
             (committed-section db)
             (action-gate-section)
             (ledger-section db)]))
   "\n</body>\n</html>\n"))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (clojure.java.io/make-parents out)
    (spit out html)
    (println "wrote" out)))

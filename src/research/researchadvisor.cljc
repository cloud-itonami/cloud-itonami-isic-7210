(ns research.researchadvisor
  "LabOps-LLM client -- the *contained intelligence node* for the
  research actor (README: \"LabOps-LLM\").

  It normalizes study-intake, drafts a per-jurisdiction research-
  integrity evidence checklist, screens studies for an unresolved
  data-reproducibility risk, and drafts the findings-report-
  publication action. CRITICAL: it is a smart-but-untrusted advisor.
  It returns a *proposal* (with a rationale + the fields it cited),
  never a committed record or a real findings-report publication.
  Every output is censored downstream by `research.governor` before
  anything touches the SSoT, and `:actuation/publish-findings-report`
  proposals NEVER auto-commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/publish-findings-report | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [research.facts :as facts]
            [research.registry :as registry]
            [research.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the study, jurisdiction or replication count. High
  confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "研究記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :study/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-protocol
  "Per-jurisdiction research-integrity evidence checklist draft.
  `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a jurisdiction with NO official spec-basis
  in `research.facts` -- the Research Integrity Governor must reject
  this (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [s (store/study db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction s))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "research.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :protocol/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :protocol/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-data-reproducibility-risk
  "Data-reproducibility-risk screening draft. `:data-reproducibility-
  risk-unresolved?` on the study record injects the failure mode: the
  Research Integrity Governor must HOLD, un-overridably, on any
  unresolved risk."
  [db {:keys [subject]}]
  (let [s (store/study db subject)]
    (cond
      (nil? s)
      {:summary "対象研究記録が見つかりません" :rationale "no study record"
       :cites [] :effect :risk-screen/set :value {:study-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:data-reproducibility-risk-unresolved? s))
      {:summary    (str (:lab-name s) ": 未解決のデータ再現性リスクを検出")
       :rationale  "スクリーニングが未解決のデータ再現性リスクを検出。人手確認とホールドが必須。"
       :cites      [:data-reproducibility-check]
       :effect     :risk-screen/set
       :value      {:study-id subject :verdict :unresolved}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:lab-name s) ": 未解決のデータ再現性リスクなし")
       :rationale  "データ再現性リスクスクリーニング完了。"
       :cites      [:data-reproducibility-check]
       :effect     :risk-screen/set
       :value      {:study-id subject :verdict :resolved}
       :stake      nil
       :confidence 0.9})))

(defn- propose-findings-report-publication
  "Draft the actual FINDINGS-REPORT action -- publishing/submitting a
  real findings report. ALWAYS `:stake :actuation/publish-findings-
  report` -- this is a REAL-WORLD research act, never a draft the
  actor may auto-run. See README `Actuation`: no phase ever adds this
  op to a phase's `:auto` set (`research.phase`); the governor also
  always escalates on `:actuation/publish-findings-report`. Two
  independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [s (store/study db subject)]
    {:summary    (str subject " 向け報告書公開提案"
                      (when s (str " (lab=" (:lab-name s) ")")))
     :rationale  (if s
                   (str "actual-replication-count=" (:actual-replication-count s)
                        " minimum-required-replication-count=" (:minimum-required-replication-count s))
                   "研究記録が見つかりません")
     :cites      (if s [subject] [])
     :effect     :study/mark-published
     :value      {:study-id subject}
     :stake      :actuation/publish-findings-report
     :confidence (if (and s (not (registry/replication-count-insufficient? s))) 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :study/intake                        (normalize-intake db request)
    :protocol/verify                     (verify-protocol db request)
    :risk/screen                         (screen-data-reproducibility-risk db request)
    :actuation/publish-findings-report    (propose-findings-report-publication db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは研究開発ラボの報告書公開エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。説明や前置きは"
       "一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:study/upsert|:protocol/set|:risk-screen/set|"
       ":study/mark-published) "
       ":stake(:actuation/publish-findings-report か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :protocol/verify                     {:study (store/study st subject)}
    :risk/screen                         {:study (store/study st subject)}
    :actuation/publish-findings-report    {:study (store/study st subject)}
    {:study (store/study st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Research Integrity Governor
  escalates/holds -- an LLM hiccup can never auto-publish a findings
  report."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :researchadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})

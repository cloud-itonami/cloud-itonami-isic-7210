(ns research.facts
  "Per-jurisdiction research-integrity/good-scientific-practice
  regulatory catalog -- the G2-style spec-basis table the Research
  Integrity Governor checks every `:protocol/verify` proposal against
  ('did the advisor cite an OFFICIAL public source for this
  jurisdiction's research-integrity and good-scientific-practice
  framework, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official research-
  integrity office/professional body (see `:provenance`); they are a
  STARTING catalog, not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to
  `catalog`, cite a real source, done -- never invent a jurisdiction's
  requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  experiment-protocol-record/data-collection-record/methodology-
  citation-record/replication-record evidence set every prior
  sibling's evidence checklist submits in some form; `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:actuation/publish-findings-report` proposal
  can commit."
  {"JPN" {:name "Japan"
          :owner-authority "文部科学省 (Ministry of Education, Culture, Sports, Science and Technology, MEXT)"
          :legal-basis "研究活動における不正行為への対応等に関するガイドライン (Guidelines for Responding to Research Misconduct)"
          :national-spec "研究機関における実験データ管理・再現性確保および研究不正防止基準"
          :provenance "https://www.mext.go.jp/a_menu/jinzai/fusei/1349208.htm"
          :required-evidence ["実験計画記録 (experiment-protocol-record)"
                              "データ収集記録 (data-collection-record)"
                              "方法論引用記録 (methodology-citation-record)"
                              "再現実験記録 (replication-record)"]}
   "USA" {:name "United States"
          :owner-authority "Office of Research Integrity (ORI), U.S. Department of Health and Human Services"
          :legal-basis "42 C.F.R. Part 93 (Public Health Service Policies on Research Misconduct)"
          :national-spec "Research-laboratory data-management and reproducibility requirements"
          :provenance "https://ori.hhs.gov/definition-misconduct"
          :required-evidence ["Experiment-protocol record"
                              "Data-collection record"
                              "Methodology-citation record"
                              "Replication record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "UK Research Integrity Office (UKRIO)"
          :legal-basis "Concordat to Support Research Integrity"
          :national-spec "Regulated research-laboratory data-integrity and reproducibility requirements"
          :provenance "https://ukrio.org/publications/concordat-to-support-research-integrity/"
          :required-evidence ["Experiment-protocol record"
                              "Data-collection record"
                              "Methodology-citation record"
                              "Replication record"]}
   "DEU" {:name "Germany"
          :owner-authority "Deutsche Forschungsgemeinschaft (DFG)"
          :legal-basis "Leitlinien zur Sicherung guter wissenschaftlicher Praxis (Code of Conduct)"
          :national-spec "Anforderungen an Forschungslabore zur Datenintegrität und Reproduzierbarkeit"
          :provenance "https://www.dfg.de/foerderung/grundlagen_rahmenbedingungen/gwp/"
          :required-evidence ["Versuchsprotokoll (experiment-protocol-record)"
                              "Datenerhebungsprotokoll (data-collection-record)"
                              "Methodikzitierungsprotokoll (methodology-citation-record)"
                              "Replikationsprotokoll (replication-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to publish a
  findings report on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-7210 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `research.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))

(ns research.facts-test
  (:require [clojure.test :refer [deftest is]]
            [research.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:provenance (facts/spec-basis "JPN")))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "GBR"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["GBR" "JPN"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "JPN")]
    (is (facts/required-evidence-satisfied? "JPN" all))
    (is (not (facts/required-evidence-satisfied? "JPN" (rest all))))
    (is (not (facts/required-evidence-satisfied? "ATL" all)) "no spec-basis -> never satisfied")))

(deftest bel-has-a-spec-basis
  (is (some? (facts/spec-basis "BEL")))
  (is (string? (:provenance (facts/spec-basis "BEL")))))

(deftest bel-spec-basis-has-the-same-shape-as-every-other-entry
  (let [bel (facts/spec-basis "BEL")]
    (is (= #{:name :owner-authority :legal-basis :national-spec
             :provenance :required-evidence}
           (set (keys bel))))
    (is (= 4 (count (:required-evidence bel))))))

(deftest bel-required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "BEL")]
    (is (facts/required-evidence-satisfied? "BEL" all))
    (is (not (facts/required-evidence-satisfied? "BEL" (rest all))))))

(deftest coverage-includes-bel-without-fabricating-scope
  (let [report (facts/coverage ["BEL" "ATL"])]
    (is (= 1 (:covered report)))
    (is (= ["BEL"] (:covered-jurisdictions report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))))

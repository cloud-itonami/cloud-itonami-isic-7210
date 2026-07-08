(ns research.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [research.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sato Materials Lab" (:lab-name (store/study s "study-1"))))
      (is (= "JPN" (:jurisdiction (store/study s "study-1"))))
      (is (= 5 (:actual-replication-count (store/study s "study-1"))))
      (is (= 3 (:minimum-required-replication-count (store/study s "study-1"))))
      (is (false? (:data-reproducibility-risk-unresolved? (store/study s "study-1"))))
      (is (= 1 (:actual-replication-count (store/study s "study-3"))))
      (is (true? (:data-reproducibility-risk-unresolved? (store/study s "study-4"))))
      (is (false? (:findings-report-published? (store/study s "study-1"))))
      (is (= ["study-1" "study-2" "study-3" "study-4"]
             (mapv :id (store/all-studies s))))
      (is (nil? (store/risk-screen-of s "study-1")))
      (is (nil? (store/protocol-of s "study-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/report-history s)))
      (is (zero? (store/next-report-sequence s "JPN")))
      (is (false? (store/study-already-published? s "study-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :study/upsert
                                 :value {:id "study-1" :lab-name "Sato Materials Lab"}})
        (is (= "Sato Materials Lab" (:lab-name (store/study s "study-1"))))
        (is (= 3 (:minimum-required-replication-count (store/study s "study-1"))) "unrelated field preserved"))
      (testing "protocol / risk-screen payloads commit and read back"
        (store/commit-record! s {:effect :protocol/set :path ["study-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/protocol-of s "study-1")))
        (store/commit-record! s {:effect :risk-screen/set :path ["study-1"]
                                 :payload {:study-id "study-1" :verdict :resolved}})
        (is (= {:study-id "study-1" :verdict :resolved} (store/risk-screen-of s "study-1"))))
      (testing "findings report drafts a record and advances the sequence"
        (store/commit-record! s {:effect :study/mark-published :path ["study-1"]})
        (is (= "JPN-RPT-000000" (get (first (store/report-history s)) "record_id")))
        (is (= "findings-report-draft" (get (first (store/report-history s)) "kind")))
        (is (true? (:findings-report-published? (store/study s "study-1"))))
        (is (= 1 (count (store/report-history s))))
        (is (= 1 (store/next-report-sequence s "JPN")))
        (is (true? (store/study-already-published? s "study-1")))
        (is (false? (store/study-already-published? s "study-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/study s "nope")))
    (is (= [] (store/all-studies s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/report-history s)))
    (is (zero? (store/next-report-sequence s "JPN")))
    (store/with-studies s {"x" {:id "x" :lab-name "n"
                              :actual-replication-count 5 :minimum-required-replication-count 3
                              :data-reproducibility-risk-unresolved? false
                              :findings-report-published? false
                              :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:lab-name (store/study s "x"))))))

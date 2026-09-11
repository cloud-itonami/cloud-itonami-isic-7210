(ns isic7210.app-test
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [re-frame.core :as rf]
            [re-frame.db :as rf-db]
            [isic7210.app :as app]))

(use-fixtures :each
  {:before (fn [] (rf/clear-subscription-cache!) (reset! rf-db/app-db {}))})

(deftest initialize-db-sets-defaults
  (testing ":initialize-db populates every fact the Svelte scaffold held"
    (rf/dispatch-sync [:initialize-db])
    (is (= app/default-db @rf-db/app-db))
    (is (= "nerve_aplasia" @(rf/subscribe [:form/substrate-class])))
    (is (= "JP" @(rf/subscribe [:form/locale-country])))
    (is (false? @(rf/subscribe [:form/dfnb9-confirmed?])))
    (is (false? @(rf/subscribe [:query/loading?])))
    (is (= "" @(rf/subscribe [:query/error])))
    (is (= [] @(rf/subscribe [:query/candidates])))))

(deftest match-query-url-test
  (testing "builds the XRPC matchQuery URL from current form state, url-encoded, matching App.svelte's fetchMatch"
    (is (= (str "https://open-otology-uhl-r.etzhayyim.com/xrpc/"
                "jp.etzhayyim.med.uhl.institution.matchQuery"
                "?substrateClass=nerve_aplasia&localeCountry=JP&dfnb9Confirmed=false")
           (app/match-query-url app/default-db))))
  (testing "substrateClass and localeCountry are URL-encoded"
    (is (= (str "https://open-otology-uhl-r.etzhayyim.com/xrpc/"
                "jp.etzhayyim.med.uhl.institution.matchQuery"
                "?substrateClass=sgn_present_hc_loss&localeCountry=U%2FS&dfnb9Confirmed=true")
           (app/match-query-url (assoc app/default-db
                                       :form/substrate-class "sgn_present_hc_loss"
                                       :form/locale-country "U/S"
                                       :form/dfnb9-confirmed? true))))))

(deftest start-fetch-db-test
  (testing "resets loading/error before a match request, like fetchMatch's `loading = true; error = ''`"
    (is (= {:query/loading? true :query/error ""}
           (select-keys (app/start-fetch-db (assoc app/default-db :query/error "stale error"))
                        [:query/loading? :query/error])))))

(deftest set-substrate-class-updates-db
  (testing ":set-substrate-class writes the select's chosen value"
    (rf/dispatch-sync [:initialize-db])
    (rf/dispatch-sync [:set-substrate-class "indeterminate"])
    (is (= "indeterminate" @(rf/subscribe [:form/substrate-class])))))

(deftest set-locale-country-updates-db
  (testing ":set-locale-country writes the text input's value"
    (rf/dispatch-sync [:initialize-db])
    (rf/dispatch-sync [:set-locale-country "US"])
    (is (= "US" @(rf/subscribe [:form/locale-country])))))

(deftest set-dfnb9-confirmed-updates-db
  (testing ":set-dfnb9-confirmed writes the checkbox's toggled value"
    (rf/dispatch-sync [:initialize-db])
    (rf/dispatch-sync [:set-dfnb9-confirmed true])
    (is (true? @(rf/subscribe [:form/dfnb9-confirmed?])))))

(deftest fetch-match-success-sets-candidates
  (testing ":fetch-match-success stores parsed candidates and clears loading, like data.candidates"
    (reset! rf-db/app-db (assoc app/default-db :query/loading? true))
    (rf/dispatch-sync
     [:fetch-match-success
      (clj->js {:candidates [{:institutionId "INST-1"
                               :nameJa "テスト機関"
                               :nameEn "Test Institute"
                               :country "JP"
                               :score 0.92
                               :referralPathIds ["p1" "p2"]}]})])
    (is (false? @(rf/subscribe [:query/loading?])))
    (is (= [{:institutionId "INST-1"
             :nameJa "テスト機関"
             :nameEn "Test Institute"
             :country "JP"
             :score 0.92
             :referralPathIds ["p1" "p2"]}]
           @(rf/subscribe [:query/candidates])))))

(deftest fetch-match-success-defaults-missing-candidates-to-empty
  (testing "matches `data.candidates || []` — an absent field is [], not an error"
    (rf/dispatch-sync [:fetch-match-success (clj->js {})])
    (is (= [] @(rf/subscribe [:query/candidates])))))

(deftest fetch-match-failure-sets-error
  (testing ":fetch-match-failure records the error message and clears loading"
    (reset! rf-db/app-db (assoc app/default-db :query/loading? true))
    (rf/dispatch-sync [:fetch-match-failure "boom"])
    (is (false? @(rf/subscribe [:query/loading?])))
    (is (= "boom" @(rf/subscribe [:query/error])))))

(deftest initialize-db-overwrites-prior-state
  (testing ":initialize-db resets to defaults even if the db already had other data"
    (reset! rf-db/app-db {:form/substrate-class "stale" :query/error "stale" :unrelated 42})
    (rf/dispatch-sync [:initialize-db])
    (is (= app/default-db @rf-db/app-db))))

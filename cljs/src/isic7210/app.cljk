(ns isic7210.app
  "cloud-itonami-isic-7210 appview — reagent + re-frame, view built from
  jp-go-dds (デジタル庁デザインシステム) hiccup.

  Faithful port of the retired Vite/Svelte scaffold's product page
  (`svelte/src/App.svelte`, 116 lines): the UHL-R Clinician Review UI — a
  form (substrate class select, locale-country text input, DFNB9
  checkbox) that calls the `open-otology-uhl-r.etzhayyim.com` XRPC
  `matchQuery` endpoint and renders the returned candidate institutions,
  plus a fixed human-review disclaimer. Every field, label, default
  value, and URL below is copied from `App.svelte`'s `<script>` block;
  nothing here is invented and nothing is dropped.

  `svelte/src/lib/Counter.svelte` was never imported by `App.svelte` and
  is NOT ported. It, `svelte/src/assets/{hero.png,vite.svg,svelte.svg}`,
  `svelte/public/icons.svg` (an unused bluesky/discord/documentation/
  github/social/x icon sprite), and `svelte/public/favicon.svg` (the
  stock Vite \"V\" mark) are all `npm create vite -- --template
  svelte-ts` scaffold boilerplate that `App.svelte` never referenced —
  confirmed by grepping the whole repo for every one of those filenames
  before `svelte/` was deleted. None of it moved here.

  `svelte/src/app.css` (the global stylesheet `main.ts` imported) is the
  same scaffold: its `:root` custom properties, dark-mode block, `.hero`,
  `#next-steps`, `#docs`, `#spacer`, `.ticks`, and `.counter` rules style
  only the default Vite/Svelte landing markup that `App.svelte` deleted.
  The one part of it that *did* reach rendered output — the bare `h1`/
  `h2` element rules (size, weight, color) — is superseded here by
  `jp-go-dds.core/heading`, the design system's own heading component;
  jp-go-dds is light-mode only (no upstream dark palette — see
  `jp-go-dds.page`'s docstring), so the scaffold's dark-mode branch has
  no equivalent and is not carried forward. `App.svelte`'s own <style>
  block (`.container` / `.controls` / `.error` / `.results` / `.paths` /
  `.disclaimer`) is what actually painted the product UI; its *intent* —
  a status banner for the disclaimer, a red error box, one card per
  candidate — is what maps onto `notification-banner` and `card` below,
  not its literal hex values.

  `public/index.html`'s inlined <style> was produced once, at authoring
  time, by `jp-go-dds.page/->page` running on the JVM — the same
  one-time authoring path this design system's own docstrings document
  and that `meet-mcp-component/cljs` already used. This namespace only
  requires `jp-go-dds.core` — the browser bundle does not need
  `jp-go-dds.page` or `html.core` at runtime; those are JVM-only tools
  used to author the static shell once. Regenerate it (e.g. if
  jp-go-dds's core components or ext-rules change) with:

    (require '[jp-go-dds.page :as page] '[clojure.java.io :as io])
    (spit \"public/index.html\"
          (page/->page {:title \"cloud-itonami-isic-7210\"
                         :lang \"en\"
                         :description \"UHL-R Clinician Review UI — decision support interface for congenital right-sided sensorineural hearing loss (reagent + re-frame + jp-go-dds).\"
                         :css (slurp (io/resource \"jp_go_dds/dds.css\"))}
                        [:div {:id \"app\"} \"loading…\"]
                        [:script {:src \"js/app.js\"}]))"
  (:require [clojure.string :as string]
            [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [jp-go-dds.core :as dds]))

;; -- db ------------------------------------------------------------------
;;
;; Same three form fields (substrateClass / localeCountry / dfnb9Confirmed,
;; with `App.svelte`'s exact initial values) plus the same three query-result
;; facts (loading / error / candidates) that `App.svelte`'s <script> held.

(def default-db
  {:form/substrate-class "nerve_aplasia"
   :form/locale-country "JP"
   :form/dfnb9-confirmed? false
   :query/loading? false
   :query/error ""
   :query/candidates []})

(def substrate-class-options
  "Copied verbatim from `App.svelte`'s <select> — order, values, and labels."
  [["sgn_present_hc_loss" "SGN Present / HC Loss"]
   ["sgn_degenerating_nerve_present" "SGN Degenerating"]
   ["sgn_absent_nerve_present" "SGN Absent / Nerve Present"]
   ["nerve_aplasia" "Nerve Aplasia"]
   ["indeterminate" "Indeterminate"]])

(def match-query-base-url
  "https://open-otology-uhl-r.etzhayyim.com/xrpc/jp.etzhayyim.med.uhl.institution.matchQuery")

(defn match-query-url
  "Same query string `App.svelte`'s `fetchMatch` built: substrateClass and
  localeCountry are URL-encoded, dfnb9Confirmed is interpolated raw (it is
  always the literal string \"true\"/\"false\", which needs no encoding)."
  [db]
  (str match-query-base-url
       "?substrateClass=" (js/encodeURIComponent (:form/substrate-class db))
       "&localeCountry=" (js/encodeURIComponent (:form/locale-country db))
       "&dfnb9Confirmed=" (:form/dfnb9-confirmed? db)))

(defn start-fetch-db
  "The :db half of `fetchMatch`'s `loading = true; error = ''`."
  [db]
  (assoc db :query/loading? true :query/error ""))

;; -- events ----------------------------------------------------------------

(rf/reg-event-db
 :initialize-db
 (fn [_ _] default-db))

(rf/reg-event-db
 :set-substrate-class
 (fn [db [_ v]] (assoc db :form/substrate-class v)))

(rf/reg-event-db
 :set-locale-country
 (fn [db [_ v]] (assoc db :form/locale-country v)))

(rf/reg-event-db
 :set-dfnb9-confirmed
 (fn [db [_ v]] (assoc db :form/dfnb9-confirmed? v)))

;; `App.svelte`'s fetchMatch: fetch → (if !res.ok, throw res.text()) → else
;; res.json() → candidates = data.candidates || [].
(rf/reg-fx
 :isic7210/fetch-json
 (fn [{:keys [url on-success on-failure]}]
   (-> (js/fetch url)
       (.then (fn [res]
                (if (.-ok res)
                  (.then (.json res) (fn [data] (rf/dispatch (conj on-success data))))
                  (.then (.text res) (fn [body] (rf/dispatch (conj on-failure body)))))))
       (.catch (fn [err] (rf/dispatch (conj on-failure (.-message err))))))))

(rf/reg-event-fx
 :fetch-match
 (fn [{:keys [db]} _]
   {:db (start-fetch-db db)
    :isic7210/fetch-json {:url (match-query-url db)
                           :on-success [:fetch-match-success]
                           :on-failure [:fetch-match-failure]}}))

(rf/reg-event-db
 :fetch-match-success
 (fn [db [_ ^js data]]
   (let [candidates (.-candidates data)]
     (assoc db
            :query/loading? false
            :query/candidates (if candidates (js->clj candidates :keywordize-keys true) [])))))

(rf/reg-event-db
 :fetch-match-failure
 (fn [db [_ message]]
   (assoc db :query/loading? false :query/error (str message))))

;; -- subs --------------------------------------------------------------------

(rf/reg-sub :form/substrate-class (fn [db _] (:form/substrate-class db)))
(rf/reg-sub :form/locale-country (fn [db _] (:form/locale-country db)))
(rf/reg-sub :form/dfnb9-confirmed? (fn [db _] (:form/dfnb9-confirmed? db)))
(rf/reg-sub :query/loading? (fn [db _] (:query/loading? db)))
(rf/reg-sub :query/error (fn [db _] (:query/error db)))
(rf/reg-sub :query/candidates (fn [db _] (:query/candidates db)))

;; -- view ------------------------------------------------------------------

(defn- controls-view []
  (let [substrate-class  @(rf/subscribe [:form/substrate-class])
        locale-country   @(rf/subscribe [:form/locale-country])
        dfnb9-confirmed? @(rf/subscribe [:form/dfnb9-confirmed?])
        loading?         @(rf/subscribe [:query/loading?])]
    (dds/card
     (dds/stack
      (dds/form-field
       {:label "Substrate Class:" :for "substrate-class"}
       (dds/select {:id "substrate-class"
                    :value substrate-class
                    :attrs {:on-change #(rf/dispatch [:set-substrate-class (.. % -target -value)])}}
                   substrate-class-options))
      (dds/form-field
       {:label "Locale (Country):" :for "locale-country"}
       (dds/input-text {:id "locale-country"
                         :value locale-country
                         :placeholder "JP"
                         :on-change #(rf/dispatch [:set-locale-country (.. % -target -value)])}))
      ;; `dds/checkbox` (unlike `input-text` / `select` / `button`) has no
      ;; :attrs passthrough, so this reproduces its exact markup by hand to
      ;; attach the controlled :on-change this form needs. `App.svelte`
      ;; bound this control with `bind:value` on a checkbox input, which
      ;; does not track Svelte 5's boolean $state the way `bind:checked`
      ;; does; this view implements the control's evident intent — toggle
      ;; `dfnb9Confirmed` — with reagent's standard controlled-checkbox
      ;; idiom (:checked + :on-change), not that literal binding mistake.
      [:label {:class "dads-checkbox" :data-size "md"}
       [:span {:class "dads-checkbox__checkbox"}
        [:input {:class "dads-checkbox__input" :type "checkbox"
                 :checked dfnb9-confirmed?
                 :on-change #(rf/dispatch [:set-dfnb9-confirmed (not dfnb9-confirmed?)])}]]
       [:span {:class "dads-checkbox__label"} "DFNB9 (OTOF) Confirmed"]]
      (dds/button (if loading? "Matching..." "Find Institutions")
                  {:disabled loading?
                   :attrs {:on-click #(rf/dispatch [:fetch-match])}})))))

(defn- error-view []
  (let [error @(rf/subscribe [:query/error])]
    (when (seq error)
      (dds/notification-banner {:type :error :heading "Error"} [:p error]))))

(defn- candidate-view [c]
  (dds/card
   [:p {:class "dads-u-std-18B-160"}
    (str (:nameJa c) " (" (:nameEn c) ") — Score: " (:score c))]
   [:p {:class "dads-u-mono-14N-150"}
    (str (:institutionId c) " | Country: " (:country c))]
   (when (seq (:referralPathIds c))
     [:p {:class "dds-ext-lead"}
      (str "Pathways: " (string/join ", " (:referralPathIds c)))])))

(defn- results-view []
  (let [candidates @(rf/subscribe [:query/candidates])]
    (dds/section
     {:title "Matched Candidates"}
     (if (seq candidates)
       (apply dds/stack (map candidate-view candidates))
       [:p {:class "dds-ext-lead"} "No candidates found or query not executed."]))))

(defn- disclaimer-view []
  ;; Same wording as `App.svelte`'s disclaimer paragraph ("⚠️ Human Review
  ;; Required: This output must be reviewed by a qualified clinician. Ethics
  ;; committee review may be required."), split across `notification-banner`'s
  ;; own :heading (which carries its own warning icon, replacing the ⚠️
  ;; glyph) and body — content unchanged, presentation now the design
  ;; system's warning banner instead of a hand-rolled orange-border div.
  (dds/notification-banner
   {:type :warning :heading "Human Review Required"}
   [:p "This output must be reviewed by a qualified clinician. Ethics committee review may be required."]))

(defn app-view []
  (dds/container
   (dds/section
    {}
    (dds/heading 1 "UHL-R Clinician Review UI")
    [:p {:class "dds-ext-lead"}
     "Decision support interface for congenital right-sided sensorineural hearing loss (UHL-R)."])
   (dds/section {} (controls-view))
   (error-view)
   (results-view)
   (dds/section {} (disclaimer-view))))

;; -- mount -------------------------------------------------------------------

(defn render []
  (rdom/render [app-view] (.getElementById js/document "app")))

(defn ^:export main []
  (rf/dispatch-sync [:initialize-db])
  (render))

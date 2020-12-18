(ns app.client
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.mutations :refer [defmutation]]
   [com.fulcrologic.fulcro.dom :as dom]))

(defonce app (app/fulcro-app))

(defmutation bump-number [ignored]
  (action [{:keys [state]}]
          (swap! state update :ui/number inc)))

(defsc Root [this {:ui/keys [number]}]
  {:query         [:ui/number]
   :initial-state {:ui/number 0}}
  (dom/div
    (dom/h4 "This is an example.")
    (dom/button {:onClick #(comp/transact! this [(bump-number {})])}
                "You've clicked " number " times")))

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in the modules of the main build."
  []
  (app/mount! app Root "app")
  (js/console.log "Loaded"))

(defn ^:export refresh
  "During development, shadow-cljs will call this on every hot reload of source. See shadow-cljs.edn"
  []
  ;; re-mounting will cause forced UI refresh, update internals, etc.
  (app/mount! app Root "app")
  ;; As of Fulcro 3.3.0, this addition will help with stale queries when using dynamic routing:
  (comp/refresh-dynamic-queries! app)
  (js/console.log "Hot reload"))


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

(defsc Task [this {:task/keys [id content] :as props}]
  {:query         [:task/id :task/content]
   :ident         (fn [] [:task/id (:task/id props)])
   :initial-state (fn [{:keys [id content] :as params}] {:task/id id :task/content content})}
  (dom/div
    content))

(def ui-task (comp/factory Task {:keyfn :task/id}))

(defsc Column [this {:column/keys [id title tasks] :as props}]
  {:query         [:column/id :column/title {:column/tasks (comp/get-query Task)}]
   :ident         (fn [] [:column/id (:column/id props)])
   :initial-state (fn [{:keys [id title]}]
                    {:column/id    id
                     :column/title title
                     :column/tasks [(comp/get-initial-state Task {:id 1 :content "Task One"})
                                    (comp/get-initial-state Task {:id 2 :content "Task Two"})
                                    (comp/get-initial-state Task {:id 3 :content "Task Three"})
                                    (comp/get-initial-state Task {:id 4 :content "Task Four"})
                                    ]})}
  (dom/div
    (dom/h4 title)
    (map ui-task tasks)))

(def ui-column (comp/factory Column {:keyfn :column/id}))

(defsc Root [this {:keys [column]}]
  {:query         [{:column (comp/get-query Column)}]
   :initial-state (fn [params] {:column (comp/get-initial-state Column {:id 1 :title "To Do"})})}
  (dom/div
    (ui-column column)))

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


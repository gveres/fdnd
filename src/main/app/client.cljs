(ns app.client
  (:require
   [taoensso.timbre :as log]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.rendering.multiple-roots-renderer :as render]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.mutations :refer [defmutation]]
   [com.fulcrologic.fulcro-css.css-injection :as inj]
   [com.fulcrologic.fulcro-css.css :as css]
   [com.fulcrologic.fulcro-css.localized-dom :as dom]
   [cljs.reader :as rd]
   [com.fulcrologic.fulcro.algorithms.react-interop :as interop :refer [react-factory]]
   ;; [com.fulcrologic.fulcro.algorithms.merge :as merge]
   ["react-beautiful-dnd" :refer [DragDropContext Droppable Draggable]]))

(defonce app (app/fulcro-app {:optimized-render! render/render! }))

(def ui-drag-drop-context (react-factory DragDropContext))
(def ui-droppable (react-factory Droppable))
(def ui-draggable (react-factory Draggable))

(defn drop-index [col idx]
  (filter identity (map-indexed #(if (not= %1 idx) %2) col))) 

(defn insert-at-index [col idx item]
  (concat
    (take idx col)
    (list item)
    (drop idx col)))

;; (insert-at-index [1 2 3] 2 8)

(defn dnd [state src-coll-path src-idx tgt-coll-path tgt-idx]
  (js/console.dir (get-in state src-coll-path))
  (-> state
      (update-in src-coll-path #(vec (drop-index % src-idx)))
      (update-in tgt-coll-path #(vec (insert-at-index % tgt-idx (get-in state (into src-coll-path [src-idx]))))))
  ) 

(defmutation dnd-action [{src :src
                          tgt :tgt :as params}]
  (action [{:keys [state]}]
          (js/console.log "Mutation fired")
          (js/console.dir params)
          (let [->tasks-path  #(into (rd/read-string (.-droppableId %)) [:column/tasks])
                src-coll-path (->tasks-path src)
                tgt-coll-path (->tasks-path tgt)]
            (js/console.log src-coll-path)
            (swap! state dnd src-coll-path (.-index src) tgt-coll-path (.-index tgt)))))

(defsc Task [this {:task/keys [id content] :as props} {:keys [orderColumn orderIndex]}]
  {:query         [:task/id :task/content]
   :ident         :task/id
   :initial-state (fn [{:keys [id content] :as params}] {:task/id id :task/content content})
   :css           [[:.task {:padding          "8px"
                            :border           "1px solid lightgrey"
                            :border-radius    "2px"
                            :margin-bottom    "8px"
                            :background-color "white"
                            }]
                   [:.raised {:box-shadow "2px 2px 5px lightgrey"}]]}
  (ui-draggable {:draggableId (str (comp/get-ident this)) :index orderIndex}
                (fn [provided snapshot]
                  (let [dprops (merge {:ref (.-innerRef provided)}
                                      (js->clj (.-dragHandleProps provided))
                                      (js->clj (.-draggableProps provided)))]
                    (comp/with-parent-context this
                      (let [{:keys [task raised]} (css/get-classnames Task)]
                        (dom/div (merge dprops {:classes [task (when (.-isDragging snapshot) raised)]})
                                 content)))))))

(def ui-task (comp/factory Task {:keyfn :task/id}))

(defsc Column [this {:column/keys [id title tasks] :as props}]
  {:query         [:column/id :column/title {:column/tasks (comp/get-query Task)}]
   :ident         :column/id
   :initial-state (fn [{:keys [id title]}]
                    {:column/id    id
                     :column/title title
                     :column/tasks (if (= id 1)
                                     [(comp/get-initial-state Task {:id 1 :content "Task One"})
                                      (comp/get-initial-state Task {:id 2 :content "Task Two"})
                                      (comp/get-initial-state Task {:id 3 :content "Task Three"})
                                      (comp/get-initial-state Task {:id 4 :content "Task Four"})]
                                     [(comp/get-initial-state Task {:id 5 :content "Task Five"})
                                      (comp/get-initial-state Task {:id 6 :content "Task Six"})
                                      (comp/get-initial-state Task {:id 7 :content "Task Seven"})
                                      (comp/get-initial-state Task {:id 8 :content "Task Eight"})])})
   :css           [[:.title {:padding "8px"}]]}
  (js/console.log "Rendering column")
  (ui-droppable {:droppableId (str (comp/get-ident this))}
                (fn [provided] (comp/with-parent-context this
                                 (dom/div {:ref (.-innerRef provided)}
                                          (dom/h3 :.title title)
                                          (js/console.log "Rendering column droppable with")
                                          (js/console.dir props)
                                          (js/console.dir provided)
                                          (map-indexed (fn [idx t] (ui-task (comp/computed t {:orderColumn id :orderIndex idx}))) tasks)
                                          (.-placeholder provided))))))

(def ui-column (comp/factory Column {:keyfn :column/id}))

(defsc Canvas [this {:canvas/keys [columns] :as props}]
  {:query          [:canvas/id :canvas/name {:canvas/columns
                                             (comp/get-query Column)}]
   :ident          (fn [] [:canvas/id (:canvas/id props)])
   :initial-state  (fn [{:canvas/keys [id name]}] {:canvas/id      1
                                                   :canvas/name    "Main"
                                                   :canvas/columns [(comp/get-initial-state Column {:id 1 :title "To Do"})
                                                                    (comp/get-initial-state Column {:id 2 :title "Done"})]})
   :initLocalState (fn [this _] {:on-drag-end #(comp/transact!!
                                                 this
                                                 [(dnd-action {:src (.-source %)
                                                               :tgt (.-destination %)} )]
                                                 {:only-render [:column/tasks]})} )}
  (let [{:keys [on-drag-end]} (comp/get-state this)]
    (js/console.log "Rendering Canvas with")
    (js/console.dir props)
    (js/console.dir columns)
    (ui-drag-drop-context {:onDragEnd on-drag-end}
                          (map ui-column columns))))

(def ui-canvas (comp/factory Canvas))

(defsc Root [this {:ui/keys [root] :as props}]
  {:query         [{:ui/root (comp/get-query Canvas)}]
   :initial-state (fn [params] {:ui/root (comp/get-initial-state Canvas [{:canvas/id   1
                                                                          :canvas/name "Main"} ])})}
  (dom/div
    (inj/style-element {:component Root})
    (ui-canvas root)))

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in the modules of the main build."
  []
  (log/set-level! :debug)
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


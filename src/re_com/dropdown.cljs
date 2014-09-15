(ns re-com.dropdown
  (:require
    [re-com.util  :as util]
    [reagent.core :as reagent]))

;;  Inspiration: http://alxlit.name/bootstrap-chosen
;;  Alternative: http://silviomoreto.github.io/bootstrap-select


(defn find-option-index
  [options id]
  "In a vector of maps (where each map has an :id), return the index of the first map containing the id parameter
   Returns nil if id not found"
  (let [index-fn    (fn [index item] (when (= (:id item) id) index))
        index-of-id (first (keep-indexed index-fn options))]
    index-of-id))


(defn find-option-id-from-current
  [options id offset]
  "In a vector of maps (where each map has an :id), return the first map containing the id parameter"
  (let [current-index (find-option-index options id)
        new-index     (cond
                        (= offset :start)    0
                        (= offset :end)      (dec (count options))
                        (nil? current-index) 0
                        :else                (mod (+ current-index offset) (count options)))]
    (:id (nth options new-index))))


(defn find-option
  [options id]
  "In a vector of maps (where each map has an :id), return the first map containing the id parameter"
  (let [current-index (find-option-index options id)
        _             (assert ((complement nil?) current-index) (str "Can't find model index '" id "' in options vector"))]
    (if (nil? current-index)
      (first options)                ;; TODO: This is "failing silently"
      (nth options current-index))))


(defn morph
  [opts]
  "Converts the user specified data for the dropdown into a form that this code can better work with"
  (let [new-opts   (atom [])
        last-group (atom nil)]
    (doall
      (for [opt opts]
        (let [new-group (not= (:group opt) @last-group)
              _         (reset! last-group (:group opt))]
          (when new-group
            (swap! new-opts conj {:id (str (:id opt) "##")
                                  :group (:group opt)}))
          (swap! new-opts conj {:id (:id opt)
                                :label (:label opt)}))
        ))
    @new-opts))


(defn backdrop
  [backdrop-click]
  [:div
   {:style {:position         "fixed"
            :left             "0px"
            :top              "0px"
            :width            "100%"
            :height           "100%"
            :background-color "black"
            :opacity          0} ;0.05
    :on-click backdrop-click}])


(defn option-group-item
  [opt]
  "Render a group option item"
  [:li.group-result
   {:style {:-webkit-user-select "none"}
    :value (:value opt)}
   (:group opt)])


#_(defn option-item-base
  []
  "Render an option item and set up appropriate mouse events"
  (let [mouse-over? (reagent/atom false)]
    (fn [opt on-click model]
      (let [selected (= @model (:id opt))
            class    (if selected
                       " highlighted"
                       (when @mouse-over? " mouseover"))]  ;; TODO: mouseover style is in index.css
        [:li
         {:class         (str "active-result group-option" class)
          :style         {:-webkit-user-select "none"}
          :on-mouse-over #(reset! mouse-over? true)
          :on-mouse-out  #(reset! mouse-over? false)
          :on-click      #(on-click (:id opt))}
         (:label opt)]))))


#_(def option-item (with-meta option-item-base
                            {:component-did-mount #(let [dn (reagent/dom-node %)]
                                                    (println "option-item-2 did-mount: " (.-innerText dn)))
                             :component-did-update #(let [dn (reagent/dom-node %)]
                                                     (println "option-item-2 did-update: " (.-innerText dn)))
                             }))


(defn option-item
  [opt on-click model]
  "Render an option item and set up appropriate mouse events"
  (let [mouse-over? (reagent/atom false)]
    (println ">IN option-item" (:label opt))
    (reagent/create-class
      {:component-did-mount
        (fn [me]
          (let [dn        (reagent/dom-node me)
                selected (= @model (:id opt))]
            (when selected (.scrollIntoView dn false))
            (println "option-item - did-mount" (:label opt))))

       :component-did-update
        (fn [me old-argv]
          (let [dn        (reagent/dom-node me)
                selected (= @model (:id opt))]
            (println "option-item - did-update" (:label opt))
            (when selected (.scrollIntoView dn false))
            ))

       :render
        (fn [me]
          (let [selected (= @model (:id opt))
                class    (if selected
                           " highlighted"
                           (when @mouse-over? " mouseover"))]  ;; TODO: mouseover style is in index.css
            (println "option-item - render" (:label opt) (if selected "*SELECTED*" ""))
            [:li
             {:class         (str "active-result group-option" class)
              :style         {:-webkit-user-select "none"}
              :on-mouse-over #(reset! mouse-over? true)
              :on-mouse-out  #(reset! mouse-over? false)
              :on-click      #(on-click (:id opt))}
             (:label opt)]))
       })
    ))


(defn filter-text-box-base
  []
  (fn [filter-text key-handler]
    [:div.chosen-search
     [:input
      {:type          "text"
       :auto-complete "off"
       :value         @filter-text
       :on-change     #(reset! filter-text (-> % .-target .-value))
       :on-key-down   key-handler}]]))


(def filter-text-box
  (with-meta filter-text-box-base
             {:component-did-mount #(let [dn (.-firstChild (reagent/dom-node %))]
                                     #_(println "filter-text-box did-mount:" (.-value dn))
                                     (.focus dn))
              :component-did-update #(let [dn (.-firstChild (reagent/dom-node %))]
                                      #_(println "filter-text-box did-update:" (.-value dn))
                                      (.focus dn))}))


;; TODO: BUG: up/down cycles through full list instead of filtered list ???????
;;       This is because the parameters (filter-text key-handler) change from time to time but render is using the
;;       initial values, when the component was first mounted.
;;       So, the question becomes, "how do we pass fresh versions of the parameters to the render function?
#_(defn filter-text-box
  [filter-text key-handler]
  (reagent/create-class
    {:component-did-mount
      (fn [me]
        (let [dn (.-firstChild (reagent/dom-node me))]
          (println "filter-text-box - did-mount: " (.-value dn))))

     :component-did-update
      (fn [me old-argv]
        (let [dn (.-firstChild (reagent/dom-node me))]
          (println "filter-text-box - did-update: " (.-value dn))
          (.focus dn)))

     :render
      (fn [me]
        (let [argv (reagent/argv me)] ;; TODO: Test code...remove
          [:div.chosen-search
           [:input
            {:type          "text"
             :auto-complete "off"
             :value         @filter-text
             :on-change     #(reset! filter-text (-> % .-target .-value))
             :on-focus      #(println "filter-text-box - FOCUS")
             :on-blur       #(println "filter-text-box BLUR")
             :on-key-down   key-handler
             }]]))
     }))


(defn dropdown-top-base
  []
  (let [ignore-click (atom false)]
    (fn
      [tmp-model options tab-index placeholder dropdown-click key-handler filter-box]
      (let [_ (reagent/set-state (reagent/current-component) {:filter-box filter-box})]
        [:a.chosen-single.chosen-default
         {:style       {:-webkit-user-select "none"}
          :href        "#"                                  ;; Required to make this anchor appear in the tab order
          :tab-index   (when tab-index tab-index)
          :on-click    #(do (println "a.click") (if @ignore-click
                                                  (reset! ignore-click false)
                                                  (dropdown-click)))
          :on-key-down #(do (println "a.key") (key-handler %) (reset! ignore-click true)) ;; Pressing enter on an anchor also triggers click event, which we don't want
          }
         [:span
          (if @tmp-model
            (:label (find-option options @tmp-model))
            placeholder)]
         [:div [:b]]])))) ;; This odd thing produces the visual arrow on the right


(def dropdown-top
  (with-meta dropdown-top-base
             {:component-did-update #(let [dn (reagent/dom-node %)
                                           filter-box (:filter-box (reagent/state %))]
                                      #_(println "dropdown-base-base did-update: " (.-value dn))
                                      (when-not filter-box (.focus dn)))}))


(defn single-dropdown
  [& {:keys [model]}]
  "Render a bootstrap styled choosen"
  (let [tmp-model      (reagent/atom (if (satisfies? cljs.core/IDeref model) @model model)) ;; Create a new atom from the model value passed in for use with keyboard actions
        drop-showing?  (reagent/atom false)
        filter-text    (reagent/atom "")]
    (fn [& {:keys [options model on-select disabled filter-box placeholder width tab-index]}]
      (let [options          (if (satisfies? cljs.core/IDeref options) @options options)
            save-model       (reagent/atom (if (satisfies? cljs.core/IDeref model) @model model))
            disabled         (if (satisfies? cljs.core/IDeref disabled) @disabled disabled)
            changeable       (and on-select (not disabled))
            callback         #(do
                               (reset! tmp-model %)
                               (when changeable (on-select @tmp-model))
                               ;(reset! drop-showing? false)
                               (reset! drop-showing? (not @drop-showing?))
                               (reset! filter-text ""))
            cancel           #(do
                               (reset! drop-showing? false)
                               (reset! filter-text "")
                               (reset! tmp-model @save-model))
            dropdown-click   #(when-not disabled
                               (reset! drop-showing? (not @drop-showing?)))
            re               (try
                               (js/RegExp. @filter-text "i")
                               (catch js/Object e nil))
            filter-fn          (partial (fn [re opt]
                                          (when-not (nil? re)
                                            (or (.test re (:group opt)) (.test re (:label opt)))))
                                        re)
            filtered-options (filter filter-fn options)
            key-handler      #(if (not disabled)
                               (case (.-which %)
                                 13 (if disabled          ;; Enter key
                                      (cancel)
                                      (callback @tmp-model))
                                 27 (cancel)              ;; Esc key
                                 9 (+)                    ;; Tab key ;; NOTE: Use this to add more robust support of tabbing
                                 38 (if @drop-showing?    ;; Up arrow
                                      (reset! tmp-model (find-option-id-from-current filtered-options @tmp-model -1))
                                      (reset! drop-showing? true))
                                 40 (if @drop-showing?    ;; Down arrow
                                      (reset! tmp-model (find-option-id-from-current filtered-options @tmp-model 1))
                                      (reset! drop-showing? true))
                                 36 (when @drop-showing?  ;; Home key
                                      (reset! tmp-model (find-option-id-from-current filtered-options @tmp-model :start)))
                                 35 (when @drop-showing?  ;; End key
                                      (reset! tmp-model (find-option-id-from-current filtered-options @tmp-model :end)))
                                 true))]
        [:div
         {:class (str "chosen-container chosen-container-single" (when @drop-showing? " chosen-container-active chosen-with-drop"))
          :style (if width
                   {:width width}
                   {:flex "auto"})}
         (when @drop-showing? [backdrop cancel])
         [dropdown-top tmp-model options tab-index placeholder dropdown-click key-handler filter-box]
         (when (and @drop-showing? (not disabled))
           [:div.chosen-drop
            (when filter-box [filter-text-box filter-text key-handler])
            [:ul.chosen-results
             (if (-> filtered-options count pos?)
               (for [opt (morph filtered-options)]
                 (if (:group opt)
                   ^{:key (:id opt)} [option-group-item opt]
                   ^{:key (:id opt)} [option-item opt callback tmp-model]))
               [:li.no-results (str "No results match \"" @filter-text "\"")])]])]))))
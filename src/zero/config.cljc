(ns zero.config
  (:refer-clojure :exclude [derive])
  (:require
   [clojure.string :as str]
   [zero.logger :as log]))

#?(:cljs
   (do

     ;; should :zero.core/tag props be ignored?  useful for dev in
     ;; some cases (e.g generated css with shadow-css or similar)
     (goog-define disable-tags? false)

     (defmulti harvest-event
       (fn [^js/Event event]
         (let [class (.-constructor event)]
           (if (= class js/CustomEvent)
             (keyword (.-type event))
             class))))

     (defmethod harvest-event :default [^js/Event event]
       (case (.-type event)
         ("keyup" "keydown" "keypress")
         {:key  (.-key event)
          :code (.-code event)
          :mods (cond-> #{}
                  (.-altKey event) (conj :alt)
                  (.-shiftKey event) (conj :shift)
                  (.-ctrlKey event) (conj :ctrl)
                  (.-metaKey event) (conj :meta))}

         ("input" "change")
         (let [target (.-target event)]
           (when (or (instance? js/HTMLInputElement target) (instance? js/HTMLTextAreaElement target))
             (case (.-type target)
               "checkbox"
               (.-checked target)

               "file"
               (-> target .-files array-seq vec)

               (.-value target))))

         "submit"
         (let [target (.-target event)]
           (when (instance? js/HTMLFormElement target)
             (js/FormData. target)))

         ("drop")
         (->> event .-dataTransfer .-items array-seq
           (mapv #(if (= "file" (.-kind %)) (.getAsFile %) (js/Blob. [(.getAsString %)] #js{:type (.-type %)}))))

         ;; TODO: others

         (or
           (.-detail event))))))
           ;; TODO: others
           

(defonce ^:no-doc !registry (atom {}))

(defn reg-effects "
  Register one or more effects.
  
  ```clojure
  (reg-effect
   ::echo
   (fn [& args]
     (prn args))
  
   ::echo2
   (fn [& args]
    (prn args)))
  
  (act ::echo \"Hello, World!\")
  ```
" {:arglists '[[& keyvals]]}
  [& {:as effect-specs}]
  (swap! !registry update :effect-handlers (fnil into {}) effect-specs))

(defn reg-streams "
  Register one or more data streams.
  
  ```clojure
  (defonce !db (atom {}))
  
  (reg-stream
   :db
   (fn [rx path]
    (rx (get-in @!db path)))
  
   :other
   (fn [rx]
    (rx \"thing\")))
  ```
  
  If a function is returned it will be called to cleanup
  the stream once it's spun down.
  
  Each pair of `[stream-key args]` represents a unique
  stream instance, so the method will be called only once
  for each set of args used with the stream; until the
  stream has been spun down and must be restarted.
" {:arglists '[[& keyvals]]}
  [& {:as stream-specs}]
  (swap! !registry update :stream-handlers (fnil into {}) stream-specs)
  nil)

(defn reg-injections
  [& {:as injection-specs}]
  (swap! !registry update :injection-handlers (fnil into {}) injection-specs)
  nil)

(defn reg-components
  [& {:as component-specs}]
  (swap! !registry update :components (fnil into {}) component-specs)
  nil)

(defn reg-attr-writers
  [& {:as new-attr-writers}]
  (swap! !registry update :attr-writers
    (fn [existing-attr-writers]
      (with-meta (into (or existing-attr-writers {}) new-attr-writers)
        {:cache (atom {})})))
  nil)

(defn reg-attr-readers
  [& {:as new-attr-readers}]
  (swap! !registry update :attr-readers
    (fn [existing-attr-readers]
      (with-meta (into (or existing-attr-readers {}) new-attr-readers)
        {:cache (atom {})})))
  nil)

(defn- get-hierarchically
  [m k]
  (when (map? m)
    (let [!cache (:cache (meta m))
          cached (when !cache (get @!cache k ::not-found))]
      (if (not= ::not-found cached)
        cached
        (let [found (or
                      (get m k)
                      (when-let [ns (when (keyword? k) (namespace k))]
                        (let [ns-parts (vec (str/split ns #"\."))]
                          (reduce
                            (fn [answer i]
                              (if-let [v (get m (keyword (str/join "." (subvec ns-parts 0 i)) "*"))]
                                (reduced v)
                                answer))
                            nil
                            (range (count ns-parts) -1 -1)))))]
          (when !cache
            (swap! !cache assoc k found))
          found)))))

(defn get-attr-reader
  [component-name]
  (or
    (get-hierarchically (:attr-readers @!registry) component-name)
    (fn [x & _] x)))

(defn get-attr-writer
  [component-name]
  (or
    (get-hierarchically (:attr-writers @!registry) component-name)
    (fn [x & _] x)))

(ns bench
  (:require [criterium.core :as c]
            [zero.html :as zh]
            [hiccup2.core :as h]
            [dev.onionpancakes.chassis.core :as ch]))

(def normal-page
  [:html
   [:head
    [:title "Benchmarks"]
    [:link {:rel "stylesheet"
            :href "/css/styles.css"}]
    [:script {:src "/js/app.js"}]]
   [:body
    [:div.sidebar
     [:ul (for [li-no (range 15)]
            [:li [:a {:href (str "/detail/" li-no)}]])]]
    [:div.body
     (for [col ["a" "b" "c"]]
       [:div {:class col}
        (for [item (range 15)]
          [:p (str "Lorem ipsum " col item)])])]]])

(comment
  (do (require '[clj-async-profiler.core :as prof])
      (prof/profile (dotimes [_ 1000]
                      (zh/html normal-page)))
      (prof/serve-ui 8888)))

(defn bench-all!
  []
  (c/quick-bench (zh/html normal-page))
  (c/quick-bench (h/html normal-page))
  (c/quick-bench (ch/html normal-page)))

(comment
  (bench-all!))

; eval (current-form): (bench-all!)
; (out) Evaluation count : 1902 in 6 samples of 317 calls.
; (out)              Execution time mean : 336.430908 µs
; (out)     Execution time std-deviation : 36.254158 µs
; (out)    Execution time lower quantile : 309.046568 µs ( 2.5%)
; (out)    Execution time upper quantile : 379.105479 µs (97.5%)
; (out)                    Overhead used : 5.837269 ns
; (out) Evaluation count : 10782 in 6 samples of 1797 calls.
; (out)              Execution time mean : 60.482391 µs
; (out)     Execution time std-deviation : 6.506236 µs
; (out)    Execution time lower quantile : 55.950375 µs ( 2.5%)
; (out)    Execution time upper quantile : 70.797419 µs (97.5%)
; (out)                    Overhead used : 5.837269 ns
; (out) 
; (out) Found 1 outliers in 6 samples (16.6667 %)
; (out) 	low-severe	 1 (16.6667 %)
; (out)  Variance from outliers : 30.8328 % Variance is moderately inflated by outliers
; (out) Evaluation count : 32436 in 6 samples of 5406 calls.
; (out)              Execution time mean : 20.980002 µs
; (out)     Execution time std-deviation : 2.656850 µs
; (out)    Execution time lower quantile : 18.465951 µs ( 2.5%)
; (out)    Execution time upper quantile : 24.183136 µs (97.5%)
; (out)                    Overhead used : 5.837269 ns

(ns fedit.format
  "Essentially, a pretty-printer which renders Clojure as HTML"
  (:require [clojure.string :as s]
            [hiccup.core :refer [html]]
            [markdown.core :refer [md-to-html-string]]))

(declare format-object)

(defn format-inline
  [o]
  [:span {:class "clojure"} o])

(defn format-string
  [o]
  [:span (md-to-html-string o)])

(defn format-block
  [o]
  [:div {:class "clojure"} o])

(defn format-quoted
  [q]
  (format-inline (str "'" (nth q 1))))


(defn format-sequence
  [s]
  (let [lpar (if (vector? s) "[" "(")
        rpar (if (vector? s) "]" ")")]
    (conj
     (vec
      (cons
       :div
       (cons
        {:class "clojure"}
        (cons
         lpar
         (map format-object s)))))
     rpar)))

(defn format-map
  [m]
  (let [c (count (keys m))]
    (vec
     (cons
      :table
      (for
        [i (range c) k (keys m)]
        [:tr
         [:td (if (zero? i) "{")]
         [:th (format-object k)]
         [:td (format-object (m k))]
         [:td (if (= (inc i) c) "}")]])))))

(defn format-object
  [o]
  (cond
   (and
    (list? o)
    (= (first o) 'quote)
    (not (seq? (nth o 1))))
   (format-quoted o)
   (or
    (nil? o)
    (true? o)
    (number? o)
    (symbol? o))
   (format-inline o)
   (keyword? o)
   (format-inline (str ":" (name o)))
   (string? o)
   (format-string o)
   (or
    (vector? o)
    (list? o))
   (format-sequence o)
   (map? o)
   (format-map o)
   true
   (format-block o)))


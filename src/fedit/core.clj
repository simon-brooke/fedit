(ns fedit.core
  (:require [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]])
  (:import [jline.console ConsoleReader]))


(defn clear-terminal
  "Clear the terminal screen - should be possible to do this by printing a \f, but
   that does not seem to work."
  []
  (dotimes [_ 25] (println)))


(defn read-char
  "Read from standard input a single character which is one of these targets return it."
  [targets]
  (let [cr (ConsoleReader.)
        keyint (.readCharacter cr)
        key (char keyint)]
    (if
      (some #(= % key) targets)
      key
      (recur targets))))


(def symbol-menu
  {\r "Return"
   \s "Substitute"
   \x "eXcise"})


(def sequence-menu
  {\a "cAr"
   \d "cDr"
   \r "Return"
   \s "Substitute"
   \x "eXcise"})


(defn prompt-and-read
  "Show a prompt, and read a form from the input
   TODO: the read should be on the same line as the prompt - again, possibly some hackery needed."
  [prompt]
  ;; print, on its own, does not flush the buffer.
  (print (str prompt " "))
  (flush)
  (read-string
    (read-line)))


(defn print-menu
  "Print this menu."
  [menu]
  (println
    (apply
      str
      (cons
        "Enter one character: "
        (map
          #(str " \t" % ": " (menu %))
          (keys menu))))))


(defn- prepare-screen
  "Prepare the screen for editing this s-expression"
  [sexpr menu]
  (clear-terminal)
  (pprint sexpr)
  (print-menu menu))


(defn- sedit-list
  "Edit something believed to be a list."
  [l]
  (if
    (list? l)
    (do
      (prepare-screen l sequence-menu)
      (let [key (read-char (keys sequence-menu))]
        (case key
          \x nil
          \s (sedit (prompt-and-read "==?"))
          \a (sedit (let [[car & cdr] l] (cons (sedit car) cdr)))
          \d (sedit (let [[car & cdr] l] (cons car (sedit cdr))))
          \r l
          (sedit l))))
    l))


(defn- sedit-vector
  "Edit something believed to be a vector. Different from
  sedit-list, since vectors are recomposed differently."
  [v]
  (if
    (vector? v)
    (do
      (prepare-screen v sequence-menu)
      (let [key (read-char (keys sequence-menu))]
        (case key
          \x nil
          \s (sedit (prompt-and-read "==?"))
          \a (sedit (let [[car & cdr] v] (apply vector (cons (sedit car) cdr))))
          \d (sedit (let [[car & cdr] v] (apply vector (cons car (sedit cdr)))))
          \r v
          (sedit v))))
    v))


(defn- sedit-token
  "Edit something which from our point of view is a single token
  (which for now includes strings)."
  [token]
  (prepare-screen token symbol-menu)
  (let [key (read-char (keys symbol-menu))]
    (case key
      \x nil
      \s (sedit (prompt-and-read "==?"))
      \r token
      (sedit token))))


(defn cons?
  "Return true if this sexpr is either a cons or a list.
   Bizarrely, in Clojure, a cons cell is not a list."
  [sexpr]
  (or
    (list? sexpr)
    (instance? clojure.lang.Cons sexpr)))


(defn sedit
  "Edit an S-Expression, and return a modified version of it"
  [sexpr]
  (cond
    (nil? sexpr) (sedit-token sexpr)
    (cons? sexpr) (sedit-list sexpr)
    (vector? sexpr) (sedit-vector sexpr)
    (or
      (symbol? sexpr)
      (number? sexpr)
      (string? sexpr)) (sedit-token sexpr)
    true (println (str "Unexpected: " (type sexpr)))))


(defn fedit
  "Edit a named function or macro, and recompile the result.
   TODO: recompiles into the current namespace, not the original namespace. Is this the
   right behaviour?"
  [name]
  (let [sexpr (sedit (read-string (source-fn name)))]
  (eval sexpr)))


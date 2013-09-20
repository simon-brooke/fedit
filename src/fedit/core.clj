(ns fedit.core
  (:use clojure.repl))

(defn clear-terminal
  "Clear the terminal screen - should be possible to do this by printing a \f, but
   that does not seem to work."
  []
  (dotimes [_ 25] (println)))

(defn print-indent
  "indent this many spaces and then print this s-expression"
  [x spaces]
  (dotimes [_ spaces] (print " "))
  (println x)
  x)

(defn recursively-frob-strings 
  "Walk this s-expression, replacing strings with quoted strings.

   TODO: does not fix strings in vectors"
  [sexpr]
  (cond 
    (nil? sexpr) nil
    (symbol? sexpr) sexpr
    (empty? sexpr) ()
    (list? sexpr)(cons (recursively-frob-strings (first sexpr))(recursively-frob-strings (rest sexpr)))
    (string? sexpr)(str "\"" sexpr "\"")
    true sexpr))

(defn rereadable-print-str 
  "print-str doesn't produce a re-readable output, because it does not surround 
   embedded strings with quotation marks. This attempts to fix this problem."
  [sexpr]
  (let [fixed (recursively-frob-strings sexpr)]
    (print-str fixed))) 

(defn pretty-print 
  "Print this s-expression neatly indented. 

   TODO: Does not yet handle vectors intelligently"
  ([sexpr] (pretty-print sexpr 0))
  ([sexpr indent]  
  (cond 
    (string? sexpr)
    (let [printform (str "\"" sexpr "\"")](print-indent printform indent))
    (list? sexpr)
    (let [asstring (rereadable-print-str sexpr)]
      ;; print-str isn't right here because it does not substitute in quotation marks around strings
      ;; need to write a new function of my own.
      (cond 
        (< (+ indent (count asstring)) 80) (print-indent asstring indent)
              true (do 
                    (let [firstline (str "(" (rereadable-print-str (first sexpr)))] 
                      (print-indent firstline indent))
                    (doall (map (fn [x] (pretty-print x (+ indent 2))) (rest sexpr)))
                    (print-indent ")" indent))))
    true (print-indent sexpr indent))
  sexpr))

(defn read-char
  "Ultimately this will read a single character, probably requiring some Java hackery; but for now
   just read"
  []
  (read))

(defn prompt-and-read 
  "Show a prompt, and read a form from the input
   TODO: the read should be on the same line as the prompt - again, possibly some hackery needed."
  [prompt]
  ;; print, on its own, does not flush the buffer.
  (println prompt)
  (read))

(defn sedit
  "Edit an S-Expression, and return a modified version of it" 
  [sexpr]
  (clear-terminal)
  (pretty-print sexpr)
  (cond (list? sexpr) (println "Enter one character: a:CAR; d:CDR; s:Substitute; x:Cut; r:Return")
    true (println "Enter one character: s:Substitute; x:Cut; r:Return"))
  (let [key (read-char)]
    (cond
      (= key 'x) nil
      (= key 's) (prompt-and-read "==?")
      (and (= key 'a)(list? sexpr)(> (count sexpr) 0)) 
      (let [car (sedit (first sexpr)) cdr (rest sexpr)](sedit (cons car cdr)))
      (and (= key 'd)(list? sexpr)) 
      (let [car (first sexpr) cdr (sedit (rest sexpr))](sedit (cons car cdr)))
      (= key 'r) sexpr
      true (sedit sexpr))))

(defn fedit
  "Edit a named function or macro, and recompile the result.
   TODO: recompiles into the current namespace, not the original namespace. Is this the
   right behaviour?"
  [name]
  (let [sexpr (sedit (read-string (source-fn name)))]
  (eval sexpr)))


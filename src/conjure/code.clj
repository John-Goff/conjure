(ns conjure.code
  "Tools to render or format Clojure code."
  (:require [clojure.string :as str]
            [zprint.core :as zp]
            [taoensso.timbre :as log]))

(defn zprint
  "Format the code with zprint, swallowing any errors."
  [src]
  (let [code (if (string? src)
               src
               (pr-str src))]
    (try
      (zp/zprint-str code {:parse-string-all? true})
      (catch Exception e
        (log/error "Error while zprinting" e)
        (if (string? code)
          code
          (pr-str code))))))

(defn sample
  "Get a short one line sample snippet of some code."
  [code]
  (let [flat (str/replace code #"\s+" " ")]
    (if (> (count flat) 30)
      (str (subs flat 0 30) "…")
      flat)))

(def ns-re #"\(\s*ns\s+(\D[\w\d\.\*\+!\-'?]*)\s*")
(defn extract-ns [code]
  (second (re-find ns-re code)))

;; TODO Handle CLJS
;; TODO Handle ns switching
(defn eval-str [{:keys [conn ns code]}]
  (case (:lang conn)
    :clj
    (str "
         (try
           (ns " ns ")
           (clojure.core/eval
             (clojure.core/read-string
               {:read-cond :allow}
               \"(do " code ")\"))
           (catch Throwable e
             (binding [*out* *err*]
               (print (-> (Throwable->map e)
                          (clojure.main/ex-triage)
                          (clojure.main/ex-str)))
               (flush)))
           (finally
             (flush)))
         ")

    :cljs
    (throw (Error. "no cljs eval yet"))))

(defn doc-str [{:keys [name] :as ctx}]
  (eval-str
    (assoc ctx :code
           (str "
                (require 'clojure.repl)
                (with-out-str
                (clojure.repl/doc " name "))
                "))))
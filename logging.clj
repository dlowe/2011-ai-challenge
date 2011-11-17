(ns logging)

(def ^{:dynamic true} *enable-logging* false)

(defn log [msg & msgs]
  (if *enable-logging*
    (binding [*out* *err*]
      (println msg msgs))))


(ns test
  (:use ants)
  (:use clojure.set)
  (:use clojure.test))

(defn point-eq? [[x1 y1] [x2 y2]] (and (== x1 x2) (== y1 y2)))

(defn pointseq-eq? [ps1-in ps2-in]
  (loop [ps1 ps1-in ps2 ps2-in]
    (if (or (empty? ps1) (empty? ps2))
      (and (empty? ps1) (empty? ps2))
      (if (not (point-eq? (first ps1) (first ps2)))
        false
        (recur (rest ps1) (rest ps2))))))

(deftest t-trimmed-moves-from-local-point
  (is (pointseq-eq? (trimmed-moves-from-local-point 1 1 [0 0]) '([0 0])))

  (is (pointseq-eq? (trimmed-moves-from-local-point 1 2 [0 0]) '([0 0] [0 1])))
  (is (pointseq-eq? (trimmed-moves-from-local-point 1 2 [0 1]) '([0 1] [0 0])))

  (is (pointseq-eq? (trimmed-moves-from-local-point 2 1 [0 0]) '([0 0] [1 0])))
  (is (pointseq-eq? (trimmed-moves-from-local-point 2 1 [1 0]) '([1 0] [0 0])))

  (is (pointseq-eq? (trimmed-moves-from-local-point 2 2 [0 0]) '([0 0] [1 0] [0 1] [1 1])))
  (is (pointseq-eq? (trimmed-moves-from-local-point 2 2 [0 1]) '([0 1] [1 1] [0 0] [1 0])))
  (is (pointseq-eq? (trimmed-moves-from-local-point 2 2 [1 0]) '([1 0] [0 0] [1 1] [0 1])))
  (is (pointseq-eq? (trimmed-moves-from-local-point 2 2 [1 1]) '([1 1] [0 1] [1 0] [0 0])))

  (is (pointseq-eq? (trimmed-moves-from-local-point 3 3 [0 0]) '([0 0] [1 0] [0 1] [2 0] [0 2] [1 1] [2 1] [2 2] [1 2])))

  (is (point-eq? (first (trimmed-moves-from-local-point 3 20 [1 12])) [1 12]))
)

(defn pointset-eq? [s1 s2] (and (subset? s1 s2) (subset? s2 s1)))

(deftest t-views-from-origin
  (is (pointset-eq? (set (views-from-origin 0)) #{[0 0]}))
  (is (pointset-eq? (set (views-from-origin 1)) #{[-1 0] [0 -1] [0 0] [0 1] [1 0]}))
)

(deftest t-trimmed-views-from-origin
  (is (pointset-eq? (set (trimmed-views-from-origin 0 2 2)) #{[0 0]}))
  (is (pointset-eq? (set (trimmed-views-from-origin 1 1 1)) #{[0 0]}))
)

(deftest t-trimmed-views-from-local-point
  (is (pointset-eq? (trimmed-views-from-local-point 0 2 2 [1 1]) #{[1 1]}))
)

(run-tests 'test)

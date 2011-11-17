(ns MyBot
  (:use clojure.contrib.profile)
  (:use ants)
  (:use paths))

(def directions [:north :east :west :south])

(defn filter-moves [ant occupied vacated possible-directions]
  "Given an ant (ie. location) and a list of occupied locations, and some 
  possible directions the ant might want to go, filter the list of directions
  to only contain legal and non-suicidal moves."
  (prof :filter-moves
  (do ;(binding [*out* *err*] (println ant occupied possible-directions))
    (for [dir possible-directions
      :let [loc (move-ant ant dir)]
      :when (and (or (contains? vacated loc) (passable? loc)) (not (contains? occupied loc)))]
      [dir loc])))
  )

(defn pick-random-no-suicide-direction [ant occupied vacated]
  "What it says on the tin!"
  (first (filter-moves ant occupied vacated (shuffle directions))))

(defn raw-objectives []
  "this returns a lazy sequence of 'objectives' in no particular order
   each objective currently consists of a label and a center.
   (labels currently are :food or :raze)"
  (prof :raw-objectives
  (set (concat
    (for [food_location (food)] [:food food_location])
    (for [hill_location (hills)] [:raze hill_location]))))
  ; TODO: take radius into account
  ; TODO: objective representing "stay within the view radius of places where food has been seen before"
  ; TODO: objective representing "stay within the attack radius of my own hill"
  ; TODO: objective representing "get within view radius of unknown space"
)

(defn all-objective-ants [objectives remaining-ants]
  "return a lazy sequence of [objective, sorted-candidates] tuples"
  (for [[_ goal :as objective] objectives :let [ants (nearest goal remaining-ants)]] [objective ants]))

(defn prioritized-objective-ants [objectives remaining-ants]
  "return the 'easiest' [ant, objective] tuple, where 'easiest' == closest to the closest associated ant"
  (prof :prioritized-objective-ants
  (first (sort-by (fn [[[_ goal] ants]] (distance goal (first ants))) (all-objective-ants objectives remaining-ants))))
  )

(defn move-ant-todo [objective ants occupied vacated]
  "returns a legal [ant, dir, loc] moving an ant toward the objective, or nil if impossible to do so"
  (first (for [ant ants
    :let [path (greedy-best-first ant (second objective) ants/distance)
          [dir loc :as dir-loc] (first (filter-moves ant occupied vacated (direction ant (first path))))]
    :when (not (nil? dir-loc))] [ant dir loc])))

(defn move-ants [initial-ants]
  (prof :move-ants
  (loop [ants initial-ants objectives (raw-objectives) ant-dirs [] occupied #{} vacated #{}]
    (do ;(binding [*out* *err*] (println "move-ants:" objectives ants))
    (if (empty? ants)
      ; no ants to move, we're done!
      ant-dirs
      (if (empty? objectives)
        ; no objectives remain, just shuffle remaining ants
        (let [ant (first ants) others (rest ants) [dir loc] (pick-random-no-suicide-direction ant occupied vacated)]
          (if dir
            (recur others objectives (cons [ant dir] ant-dirs) (conj occupied loc) (conj vacated ant))
            (recur others objectives ant-dirs occupied vacated)))
        ; otherwise, try to accomplish the top objective
        (let [[objective candidates] (prioritized-objective-ants objectives ants)
              others (disj objectives objective)
              [ant dir loc :as ant-dir-loc] (move-ant-todo objective candidates occupied vacated)]
          (if (nil? ant-dir-loc)
            ; skipping this objective; all ants remain available
            (recur ants others ant-dirs occupied vacated)
            ; move an ant
            ; TODO: if we take radius into account, an ant may 'incidentally' achieve multiple
            ; objectives, which we would want to filter out of 'others' at this point.
            (recur (disj ants ant) others (cons [ant dir] ant-dirs) (conj occupied loc) (conj vacated ant)))))))))
  )

(defn simple-bot []
  (do ;(binding [*out* *err*] (println "simple-bot"))
    (doseq [[ant dir] (move-ants (my-ants))]
      (move ant dir))))
      
(with-profile-data (start-game simple-bot))

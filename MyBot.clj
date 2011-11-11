(ns MyBot
  (:use ants))
  ;(:use paths))

(def directions [:north :east :west :south])

(def left-directions [:north :west :south :east])
(def right-directions [:north :east :south :west])

(defn shift-to [target col]
  "Treat collection col as a ring, and rotate it until the first element is target."
  (loop [n (first col) res col]
    (if (= target n)
      res
      (recur (last res) (cons (last res) (butlast res))))))

(defn filter-moves [ant occupied possible-directions]
  "Given an ant (ie. location) and a list of occupied locations, and some 
  possible directions the ant might want to go, filter the list of directions
  to only contain legal and non-suicidal moves."
  (filter #(and (valid-move? ant %) (not (contains? occupied (move-ant ant %)))) possible-directions))

(defn pick-random-no-suicide-direction [ant occupied]
  (first (filter-moves ant occupied (shuffle directions))))

(defn pick-food-direction [ant occupied]
  "Naiive direction selection based on the closest food we have.  Problem is, you
  can easily get stuck in corners; if the direction to the nearest food is not
  a valid move, defaults to random-no-suicide."
  (let [nearest-food (first (nearest ant (food)))]
    (if nearest-food
      (let [food-directions (filter-moves ant occupied (direction ant nearest-food))]
        (if (empty? food-directions)
          (pick-random-no-suicide-direction ant occupied)
          (first food-directions)))
      (pick-random-no-suicide-direction ant occupied))))

(defn pick-food-or-right [ant occupied]
  "Sometime's this crashes.  Why?"
  (let [nearest-food (first (nearest ant (food)))]
    (if nearest-food
      (first (filter-moves ant occupied (shift-to (first (direction ant nearest-food)) right-directions)))
      (first (filter-moves ant occupied right-directions)))))
        
(defn select-move [ant occupied strategy]
  (condp = strategy
    ; pick random direction, taking into account obstacles
    :random             (first (filter #(valid-move? ant %) (shuffle directions)))
    ; pick random direction, taking care not to suicide
    :random-no-suicide  (pick-random-no-suicide-direction ant occupied)
    ; pretty sure we can't make a "righty" bot without remembering what dir the ant was moving in last turn
    :north-or-right     (first (filter-moves ant occupied right-directions))
    :seek-food          (pick-food-direction ant occupied)
    :seek-food-smarter  (pick-food-or-right ant occupied)
    ; really random; should be equiv to :random strategy, since engine ignores illegal moves
    :else               (first (shuffle directions))))

(defn raw-objectives []
  ; this returns a lazy sequence of 'objectives' in no particular order
  ; each objective currently consists of a label and a center.
  ; (currently ':food', the location of a food)
  ; TODO: take radius into account
  (set (for [food_location (food)] [:food food_location]))
  ; TODO: objective representing "stay within the view radius of places where food has been seen before"
  ; TODO: objective representing "get on top of known enemy hills (radius 0)"
  ; TODO: objective representing "stay within the attack radius of my own hill"
  ; TODO: objective representing "get within view radius of unknown space"
)

(defn all-objective-ants [objectives remaining-ants]
  "return a lazy sequence of [objective, sorted-candidates] tuples"
  (for [[_ goal :as objective] objectives :let [ants (nearest goal remaining-ants)]] [objective ants]))

(defn prioritized-objective-ants [objectives remaining-ants]
  "return the 'easiest' [ant, objective] tuple, where 'easiest' == closest to the closest associated ant"
  (first (sort-by (fn [[[_ goal] ants]] (distance goal (first ants))) (all-objective-ants objectives remaining-ants))))

(defn move-ant-todo [objective ants occupied]
  "returns a legal [ant, dir] moving an ant toward the objective, or nil if impossible to do so"
  (first (for [ant ants
    :let [dir (first (filter-moves ant occupied (direction ant (second objective))))]
    :when (not (nil? dir))] [ant dir])))

(defn move-ants [initial-ants]
  (loop [ants initial-ants objectives (raw-objectives) ant-dirs [] destinations #{}]
    (do ;(binding [*out* *err*] (println "move-ants:" objectives ants))
    (if (empty? ants)
      ; no ants to move, we're done!
      ant-dirs
      (if (empty? objectives)
        ; no objectives remain, just shuffle remaining ants
        (let [ant (first ants) others (rest ants) dir (select-move ant destinations :random-no-suicide)]
          (if dir
            (recur others objectives (cons [ant dir] ant-dirs) (conj destinations (move-ant ant dir)))
            (recur others objectives ant-dirs destinations)))
        ; otherwise, try to accomplish the top objective
        (let [[objective candidates] (prioritized-objective-ants objectives ants)
              others (disj objectives objective)
              ant-dir (move-ant-todo objective candidates destinations)]
          (if (nil? ant-dir)
            ; skipping this objective; all ants remain available
            (recur ants others ant-dirs destinations)
            ; move an ant
            (let [ant (first ant-dir) dir (second ant-dir)]
              ; TODO: if we take radius into account, an ant may 'incidentally' achieve multiple
              ; objectives, which we would want to filter out of 'others' at this point.
              (recur (disj ants ant) others (cons [ant dir] ant-dirs) (conj destinations (move-ant ant dir)))))))))))

(defn simple-bot []
  (do ;(binding [*out* *err*] (println "simple-bot"))
    (doseq [[ant dir] (move-ants (my-ants))]
      (move ant dir))))
      
(start-game simple-bot)

(ns MyBot
  (:use ants))

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
  (let [nearest-food (nearest ant (food))]
    (if nearest-food
      (let [food-directions (filter-moves ant occupied (direction ant nearest-food))]
        (if (empty? food-directions)
          (pick-random-no-suicide-direction ant occupied)
          (first food-directions)))
      (pick-random-no-suicide-direction ant occupied))))

(defn pick-food-or-right [ant occupied]
  "Sometime's this crashes.  Why?"
  (let [nearest-food (nearest ant (food))]
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

(defn move-ants [initial-ants]
  ; given the set of initial ants, randomly shuffles them according to the given move strategy
  ; returns a sequence of legal (ant dir) moves to be applied
  (loop [ants initial-ants ant-dirs [] destinations #{}]
    (if (empty? ants)
      ant-dirs
      ;(let [ant (first ants) dir (select-move ant destinations :random-no-suicide)]
      (let [ant (first ants) dir (select-move ant destinations :seek-food)]
        (if dir
          (recur (rest ants) (cons [ant dir] ant-dirs) (conj destinations (move-ant ant dir)))
          (recur (rest ants) ant-dirs destinations))))))

(defn simple-bot []
  (doseq [ant-dirs (move-ants (my-ants))]
    (let [ant (first ant-dirs) dir (second ant-dirs)]
      (move ant dir))))
      
(start-game simple-bot)

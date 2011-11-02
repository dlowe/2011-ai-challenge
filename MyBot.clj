(ns MyBot
  (:use ants))

(def directions [:north :east :west :south])

(defn pick-random-no-suicide-direction [ant occupied]
  (first (filter #(and (valid-move? ant %) (not (contains? occupied (move-ant ant %)))) (shuffle directions))))

(defn pick-food-direction [ant occupied]
  "Naiive direction selection based on the closest food we have.  Problem is, you
  can easily get stuck in corners; if the direction to the nearest food is not
  a valid move, defaults to random-no-suicide."
  (let [nearest-food (nearest ant (food))]
    (if nearest-food
      (let [food-directions (filter #(and (valid-move? ant %) (not (contains? occupied (move-ant ant %)))) (direction ant nearest-food))]
        (if (empty? food-directions)
          (pick-random-no-suicide-direction ant occupied)
          (first food-directions)))
      (pick-random-no-suicide-direction ant occupied))))

(defn select-move [ant occupied strategy]
  (condp = strategy
    ; pick random direction, taking into account obstacles
    :random             (first (filter #(valid-move? ant %) (shuffle directions)))
    ; pick random direction, taking care not to suicide
    :random-no-suicide  (pick-random-no-suicide-direction ant occupied) ;(first (filter #(and (valid-move? ant %) (not (contains? occupied (move-ant ant %)))) (shuffle directions)))
    :seek-food          (pick-food-direction ant occupied)
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

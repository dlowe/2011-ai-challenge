(ns MyBot
  (:use ants))

(def directions [:north :east :west :south])

(defn select-move [ant occupied strategy]
  (condp = strategy
    ; pick random direction, taking into account obstacles
    :random             (first (filter #(valid-move? ant %) (shuffle directions)))

    ; pick random direction, taking care not to suicide
    :random-no-suicide  (first (filter #(and (valid-move? ant %) (not (contains? occupied (move-ant ant %)))) (shuffle directions)))
 
    ; really random; should be equiv to :random strategy, since engine ignores illegal moves
    :else               (first directions)))

(defn move-ants [initial-ants]
  ; given the set of initial ants, randomly shuffles them according to the given move strategy
  ; returns a sequence of legal (ant dir) moves to be applied
  (loop [ants initial-ants ant-dirs [] destinations #{}]
    (if (empty? ants)
      ant-dirs
      (let [ant (first ants) dir (select-move ant destinations :random-no-suicide)]
        (if dir
          (recur (rest ants) (cons [ant dir] ant-dirs) (conj destinations (move-ant ant dir)))
          (recur (rest ants) ant-dirs destinations))))))

(defn simple-bot []
  (doseq [ant-dirs (move-ants (my-ants))]
    (let [ant (first ant-dirs) dir (second ant-dirs)]
      (move ant dir))))
      
(start-game simple-bot)

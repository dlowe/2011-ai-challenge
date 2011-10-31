(ns MyBot
  (:use ants))

(def directions [:north :east :west :south])

(defn move-ants [initial-ants]
  ; given the set of initial ants, randomly shuffles them
  ; without making illegal moves
  ; or suiciding
  ; returns a sequence of legal (ant dir) moves to be applied
  (loop [ants initial-ants ant-dirs [] destinations #{}]
    (if (empty? ants)
      ant-dirs
      (let [ant (first ants) dir (first (filter #(and (valid-move? ant %) (not (contains? destinations (move-ant ant %)))) (shuffle directions)))]
        (if dir
          (recur (rest ants) (cons [ant dir] ant-dirs) (conj destinations (move-ant ant dir)))
          (recur (rest ants) ant-dirs destinations))))))

(defn simple-bot []
  (doseq [ant-dirs (move-ants (my-ants))]
    (let [ant (first ant-dirs) dir (second ant-dirs)]
      (move ant dir))))

(start-game simple-bot)

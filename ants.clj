(ns ants
  (:require [clojure.string :as string]))

;;****************************************************************
;; Constants and lookups
;;****************************************************************

(declare ^{:dynamic true} *game-info*)
(declare ^{:dynamic true} *game-state*)

(def init-state {:turn 0
                 :water #{}
                 :dead #{}
                 :enemies #{}
                 :ants #{}
                 :food #{}
                 :hill #{}})

(def dir-sym {:north "N"
              :south "S"
              :east "E"
              :west "W"})

(def dir-offset {:north [-1 0]
                 :west [0 -1]
                 :south [1 0]
                 :east [0 1]})

(def offset-dir {[-1 0] :north
                 [0 -1] :west
                 [1 0] :south
                 [0 1] :east})

(def messages {:ready #"^ready$"
               :turn #"^turn [0-9]+$"
               :end #"^end$"
               :go #"^go$"
               :tile #"^\w "})

(def map-tiles {"f" :food 
                "w" :water 
                "a" :ant 
                "d" :dead-ant
                "h" :hill})

;;****************************************************************
;; Implementation functions
;;****************************************************************

(defn- parse-tile [msg]
  (let [[tile row col player :as parts] (string/split (string/lower-case msg) #" ")
        player (when player
                     (Integer. player))]
    {:tile (map-tiles tile)
     :row (Integer. row)
     :col (Integer. col)
     :player player}))

(defn- message? [msg-type msg]
  (re-find (messages msg-type) msg))

(defn- build-game-info []
  (loop [cur (read-line)
         info {}]
    (if (message? :ready cur)
      info
      (let [[k v] (string/split cur #" ")
            neue (assoc info (keyword k) (Long/parseLong v))]
        (recur (read-line) neue)))))

(defn- get-turn [msg]
  (Integer. (or (second (string/split msg #" ")) 0)))

(defn- update-tile [state {:keys [tile row col player]}]
  (let [loc [row col]]
        (condp = tile
          :water (update-in state [:water] conj loc)
          :dead-ant (update-in state [:dead] conj (conj loc player))
          :ant (if (zero? player)
                 (update-in state [:ants] conj loc) 
                 (update-in state [:enemies] conj (conj loc player)))
          :food (update-in state [:food] conj loc)
          :hill (update-in state [:hill] conj loc))))

(defn- contains-ant? [ants cur]
  (some #(let [[r c p] %]
             (= [r c] cur))
          ants))

(defn move-ant 
  "Return the location defined by moving the given ant in the given
  direction."
  [ant dir]
  (let [dir-vector (dir-offset dir)
        rows (*game-info* :rows)
        cols (*game-info* :cols)
        [r c] (map + ant dir-vector)]
    [(cond 
       (< r 0) (+ rows r) 
       (>= r rows) (- r rows)
       :else r)
     (cond 
       (< c 0) (+ cols c) 
       (>= c cols) (- c cols)
       :else c)]))


;;****************************************************************
;; Public functions
;;****************************************************************

;; TODO: if you want to collect the information and do something
;; with it at the end, do so here. Look at build-game-info as an
;; example
(defn- collect-stats []
  )

(defn game-info
  "Get some value from the setup information of the game"
  [k]
  (*game-info* k))

(defn move 
  "Issue a move command for the given ant, where the ant is [row col] and dir
  is [:north :south :east :west]"
  [[row col :as ant] dir]
  (println "o" row col (dir-sym dir)))

(defn turn-num
  "Get the turn number"
  []
  (:turn *game-state*))

(defn my-ants 
  "Get a set of all ants belonging to you"
  []
  (:ants *game-state*))

(defn enemy-ants 
  "Get a set of all enemy ants where an enemy ant is [row col player-num]"
  []
  (:enemies *game-state*))

(defn food 
  "Get a set of food locations"
  []
  (:food *game-state*))


(defn unit-distance 
  "Get the vector distance between two points on a torus. Negative deltas are 
  preserved."
  [loc loc2]
  (let [[dx dy] (map - loc2 loc)
        [adx ady] (map #(Math/abs %) [dx dy])
        [adx2 ady2] (map #(- (game-info %) %2) [:rows :cols] [adx ady])
        fx (if (<= adx adx2)
             dx
             (* adx2 (/ (- dx) adx)))
        fy (if (<= ady ady2)
             dy
             (* ady2 (/ (- dy) ady)))]
    [fx fy]))

(defn distance 
  "Get the euclidean distance between two locations on a torus"
  [loc loc2]
  (let [[dx dy] (unit-distance loc loc2)]
    (Math/sqrt (+ (Math/pow dx 2) (Math/pow dy 2)))))

(defn unoccupied? 
  "If the given location does not contain an ant or food, return loc"
  [loc]
  (when (and (not (contains-ant? (food) loc)) 
             (not (contains-ant? (my-ants) loc))
             (not (contains-ant? (enemy-ants) loc)))
    loc))

(defn passable? 
  "Deteremine if the given location can be moved to. If so, loc is returned."
  [loc]
  (when (and (not (contains? (*game-state* :water) loc))
             (unoccupied? loc))
    loc))

(defn valid-move? 
  "Check if moving an ant in the given direction is passable. If so,
  return the location that the ant would then be in."
  [ant dir]
  (passable? (move-ant ant dir)))


(defn direction [loc loc2]
  "Determine the directions needed to move to reach a specific location.
  This does not attempt to avoid water. The result will be a collection
  containing up to two directions."
  (let [[dr dc] (unit-distance loc loc2)
        row (if-not (zero? dr)
            (/ dr (Math/abs dr))
            dr)
        col (if-not (zero? dc)
            (/ dc (Math/abs dc))
            dc)]
    (filter #(not (nil? %))
            [(offset-dir [row 0])
             (offset-dir [0 col])])))

(defn from-origin-n [n]
  "Return the vector of coordinates exactly n moves from the origin"
  (if (zero? n)
    '([0 0])
    (for [[r c dr dc] [[n 0 -1 +1] [0 n -1 -1] [(- n) 0 +1 -1] [0 (- n) +1 +1]] i (range n)]
      [(+ r (* i dr)) (+ c (* i dc))])))

(defn from-origin []
  "Return a lazy, infinite sequence of coordinates ordered by distance from the origin"
  (for [n (range) coord (from-origin-n n)] coord))

(defn extents [rows cols]
  "Returns AABB bounding box coordinates for a map sized (rows x cols)"""
  (let [qr (quot (- rows 1) 2) rr (rem (- rows 1) 2) qc (quot (- cols 1) 2) rc (rem (- cols 1) 2)]
    [(- qr) (- qc) (+ qr rr) (+ qc rc)]))

(def trimmed-from-origin (memoize (fn [rows cols]
  "Return finite sequence of coordinates on a map sized (rows x cols), ordered by distance from
   the origin."
  (let [result (let [[min_r min_c max_r max_c] (extents rows cols) r-ok? (fn [r] (and (<= r max_r) (>= r min_r))) c-ok? (fn [c] (and (<= c max_c) (>= c min_c)))]
    (doall (for [[r c] (from-origin) :while (or (r-ok? r) (c-ok? c)) :when (and (r-ok? r) (c-ok? c))] [r c])))]
    (do ;(binding [*out* *err*] (println "trimmed-from-origin:" rows cols result))
      result)))))

(defn origin-point-to-local-point [rows cols [r c]]
  [(if (neg? r) (+ rows r) r) (if (neg? c) (+ cols c) c)])

(defn trimmed-from-local-point [rows cols [r c]]
  "Returns lazy sequence of coordinates on a map sized (rows x cols), ordered by distance from
   the given point."
  (do
    (assert (>= r 0))
    (assert (>= c 0))
    (assert (< r rows))
    (assert (< c cols))
    (let [result (map #(let [[dr dc] %] (origin-point-to-local-point rows cols [(rem (+ r dr) rows) (rem (+ c dc) cols)])) (trimmed-from-origin rows cols))]
      (do ;(binding [*out* *err*] (println "trimmed-from-local-point:" rows cols r c result))
        result))))

(defn nearest [loc locations]
  "Return the location in collection 'locations' which is closest to loc.
  Use David's oneliner instead of my ugly mess."
  (sort-by (partial distance loc) locations)
  ;(filter #(locations %) (trimmed-from-local-point (game-info :rows) (game-info :cols) loc))
)

; FOR REPL TESTING
(def ^{:dynamic true} *game-info* {:rows 20 :cols 20})
(def loc [7 7])
(def locs #{[1 1] [2 2] [3 3] [4 4] [5 5]})
; END FOR REPL TESTING

(defn play-turn [pre-turn-state bot]
  "Play a single turn with the given bot."
  (loop [state (merge init-state { :turn (pre-turn-state :turn) :water (pre-turn-state :water) })]
    (let [cur (read-line)]
      (if (not (message? :tile cur))
        (binding [*game-state* state] (bot) (println "go") state)
        (recur (update-tile state (parse-tile cur)))))))

(defn start-game 
  "Play the game with the given bot."
  [bot]
  (when (message? :turn (read-line))
    (binding [*game-info* (build-game-info)]
      (println "go") ;; we're "setup" so let's start
      (loop [state init-state]
        (let [cur (read-line)]
          (if (message? :end cur)
            (collect-stats)
            (recur (play-turn state bot))))))))

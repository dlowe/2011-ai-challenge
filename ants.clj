(ns ants
  (:require [clojure.string :as string])
  (:use clojure.set))

(use 'clojure.stacktrace)

;;****************************************************************
;; Constants and lookups
;;****************************************************************

(declare ^{:dynamic true} *game-info*)
(declare ^{:dynamic true} *game-state*)

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

(def ant-tiles #{:ant :dead-ant :hill})

;;****************************************************************
;; Implementation functions
;;****************************************************************

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

(defn hills
  "Get a set of hill locations"
  []
  (:hill *game-state*))


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


(defn passable? 
  "Deteremine if the given location can be moved to. If so, loc is returned."
  [loc]
  (when (and (not (contains? (*game-state* :water) loc))
             (not (contains? (*game-state* :ants) loc))
             (not (contains? (*game-state* :enemies) loc))
             ; do we really have to exclude food locations?  if we get close, we eat it, problem solved
             ; if we make them non-passable, then the path search excludes them as valid cels to visit!
             ;(not (contains? (*game-state* :food) loc))
             )
    loc))

(defn direction [loc loc2]
  "Determine the directions needed to move to reach a specific location.
  This does not attempt to avoid water. The result will be a collection
  containing up to two directions."
  (if (or (nil? loc) (nil? loc2))
    nil
    (let [[dr dc] (unit-distance loc loc2)
          row (if-not (zero? dr)
                (/ dr (Math/abs dr))
                dr)
          col (if-not (zero? dc)
                (/ dc (Math/abs dc))
                dc)]
      (filter #(not (nil? %))
              [(offset-dir [row 0])
               (offset-dir [0 col])]))))

(defn moves-from-origin-n [n]
  "Return the vector of coordinates exactly n moves from the origin"
  (if (zero? n)
    '([0 0])
    (for [[r c dr dc] [[n 0 -1 +1] [0 n -1 -1] [(- n) 0 +1 -1] [0 (- n) +1 +1]] i (range n)]
      [(+ r (* i dr)) (+ c (* i dc))])))

(defn moves-from-origin []
  "Return a lazy, infinite sequence of coordinates ordered by move distance from the origin"
  (for [n (range) coord (moves-from-origin-n n)] coord))

(defn extents [rows cols]
  "Returns AABB bounding box coordinates for a map sized (rows x cols)"""
  (let [qr (quot (- rows 1) 2) rr (rem (- rows 1) 2) qc (quot (- cols 1) 2) rc (rem (- cols 1) 2)]
    [(- qr) (- qc) (+ qr rr) (+ qc rc)]))

(defn trim-from-origin [rows cols move-generator]
  (let [[min_r min_c max_r max_c] (extents rows cols) r-ok? (fn [r] (and (<= r max_r) (>= r min_r))) c-ok? (fn [c] (and (<= c max_c) (>= c min_c)))]
    (doall (for [[r c] (move-generator) :while (or (r-ok? r) (c-ok? c)) :when (and (r-ok? r) (c-ok? c)
)] [r c]))))

(def trimmed-moves-from-origin (memoize (fn [rows cols]
  "Return finite sequence of coordinates on a map sized (rows x cols), ordered by move distance from
   the origin."
  (trim-from-origin rows cols moves-from-origin))))

(def origin-point-to-local-point (memoize (fn [rows cols [r c]]
  [(if (neg? r) (+ rows r) (rem r rows)) (if (neg? c) (+ cols c) (rem c cols))])))

(def trimmed-moves-from-local-point (memoize (fn [rows cols [r c]]
  "Returns lazy sequence of coordinates on a map sized (rows x cols), ordered by move distance from
   the given point."
  (map (fn [[dr dc]] (origin-point-to-local-point rows cols [(+ r dr) (+ c dc)]))
    (trimmed-moves-from-origin rows cols)))))

(defn views-from-origin [radius]
  "Return sequence of coordinates within n distance from the origin on an infinite map"
  (let [orange (range (- radius) (+ radius 1))
        odist (fn [r c] (Math/sqrt (+ (Math/pow r 2) (Math/pow c 2))))]
    (for [r orange c orange :when (<= (odist r c) radius)] [r c])))

(def trimmed-views-from-origin (memoize (fn [radius rows cols]
  "Return the sequence of coordinates on a map sized (rows x cols) which are within n distance from the
   origin"
  (trim-from-origin rows cols (partial views-from-origin radius)))))

(defn trimmed-views-from-local-point [radius rows cols [r c]]
  "Return the set of coordinates on a map sized (rows x cols) which are within n distance from the
   given point."
  (set (map (fn [[dr dc]] (origin-point-to-local-point rows cols [(+ r dr) (+ c dc)]))
    (trimmed-views-from-origin radius rows cols))))

(defn nearest [loc locations]
  "Return the location in set 'locations' which is closest to loc by traversing in the
   order given by 'trimmed-moves-from-local-point'"
  (filter #(locations %) (trimmed-moves-from-local-point (game-info :rows) (game-info :cols) loc)))

; FOR REPL TESTING
;(def ^{:dynamic true} *game-info* {:rows 20 :cols 20})
(def loc [7 7])
(def locs #{[1 1] [2 2] [3 3] [4 4] [5 5]})

(def map1 "
rows 3
cols 5
players 1

m %%%%%
m %a.A%
m %%%%%
")

(defn map-type-locs [row linedata typechar]
  "Given a 'row' number, and some map line data like '%%.aB**%%', return the
  list of [row col] locations corresponding to the typechar (like % or *)."
  (let [filteredlist (filter #(= typechar (first %)) (map list linedata (range)))]
    (map vector (repeat row) (map second filteredlist))))

(defn update-game-state-from-map-row [game-state row data]
  (update-in game-state [:water] into (map-type-locs row data \%)))

(defn game-from-map []
  "Assuming *in* is a map file, initialize a game-info and game-state from it and return them."
  (loop [row 0 line (read-line) game-info {} game-state {}]
    ;(println "found line: " line)
    (cond 
     (nil? line)   [game-info game-state]
     (empty? line) (recur row (read-line) game-info game-state)
     :t            (let [[k v] (string/split line #" ")]
                     (if (= k "m")
                       (recur (+ row 1) 
                              (read-line) 
                              game-info 
                              (update-game-state-from-map-row game-state row v))
                       (recur row 
                              (read-line) 
                              (assoc game-info (keyword k) (Long/parseLong v)) 
                              game-state))))))

(defn load-game [mapdata]
  (let [[gi gs] (with-in-str mapdata (game-from-map))]
    (def ^{:dynamic true} *game-info* gi)
    (def ^{:dynamic true} *game-state* gs)))

; END FOR REPL TESTING

(defn- parse-tile [msg]
  (let [[tile row col player] (string/split msg #" ") player (when player (Integer. player)) tile-t (map-tiles tile)]
    (if (tile-t ant-tiles)
      [tile-t [(Integer. row) (Integer. col) player]]
      [tile-t [(Integer. row) (Integer. col)]])))

(defn turn-state-grep [turn-state tile-t]
  "Return a sequence of tile-t tiles"
  (for [[t data] turn-state :when (identical? tile-t t)] data))

(defn turn-state-grep-player0 [ts tile-t op]
  "Return the set of locations of tile-t tiles where (op player 0) is true"
  (set (for [[row col player] (turn-state-grep ts tile-t) :when (op player 0)] [row col])))

(defn start-state [rows cols]
  {
    :turn 0
    :water #{}
    :dead #{}
    :enemies #{}
    :ants #{}
    :food #{}
    :hill #{}
    :unknown (set (for [row (range rows) col (range cols)] [row col]))
  })

(defn turn-state [pre-turn-state turn-state-strings]
  (let [ts (map parse-tile turn-state-strings)
        my-ants (turn-state-grep-player0 ts :ant ==)
        visible (set (for [ant my-ants v (trimmed-views-from-local-point (int (Math/sqrt (*game-info* :viewradius2))) (*game-info* :rows) (*game-info* :cols) ant)] v))]
    (do ;(binding [*out* *err*] (println pre-turn-state ts))
      {
        :turn 0
        :water (union
          (set (turn-state-grep ts :water))
          (:water pre-turn-state))
        :dead (set (turn-state-grep ts :dead-ant))
        :enemies (turn-state-grep-player0 ts :ant not=)
        :ants (set my-ants)
        :food (set (turn-state-grep ts :food))
        :hill (union
          (difference (:hill pre-turn-state) visible)
          (turn-state-grep-player0 ts :hill not=))
        :unknown (difference (:unknown pre-turn-state) visible)
      })))

(defn play-turn [pre-turn-state bot]
  "Play a single turn with the given bot."
  (let [state (turn-state pre-turn-state (for [cur (repeatedly read-line) :while (message? :tile cur)] cur))]
    (binding [*game-state* state]
      (bot)
      (println "go")
      state)))

(defn start-game 
  "Play the game with the given bot."
  [bot]
  (when (message? :turn (read-line))
    (binding [*game-info* (build-game-info)]
      (println "go") ;; we're "setup" so let's start
      (loop [state (start-state (game-info :rows) (game-info :cols))]
        (let [cur (read-line)]
          (if (message? :end cur)
            (collect-stats)
            (recur (play-turn state bot))))))))

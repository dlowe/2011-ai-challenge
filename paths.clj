(ns paths
  (:use ants))
;  (:use clojure.data.priority-map))

(use 'clojure.data.priority-map)

; Some utilities for path finding

(def map3x3
"rows 3
cols 3
players 0

m ...
m ...
m ...")

(def partitionedmap
"rows 3
cols 5
players 0

m .%.%.
m .%.%.
m .%.%.")

(def horseshoemap
"rows 5
cols 10
players 1

m ..........
m %.%%%%%...
m %..a..%...
m %%%%%%%...
m .....*....")

(defn 
  #^{:test (fn []
             (load-game map3x3)
             ;(println "neighbors test func, *game-info* " ants/*game-info* ", *game-state* " *game-state*)
             (assert (= 4 (count (neighbors [1 1]))))
             (assert (= (into #{} (neighbors [1 1])) #{[0 1] [1 0] [1 2] [2 1]}))
             (assert (= (into #{} (neighbors [0 0])) #{[2 0] [0 1] [1 0] [0 2]})))}
  neighbors [loc]
  "Return a list of neighboring locations which are valid moves.
  FIX: this should probably only take into account water, not ant locations,
  for the purposes of planning a route to a goal."
  ;(println "neighbors " loc)
  (filter #(passable? %) (map #(move-ant loc %) [:north :west :south :east])))

(defn nodes-to-root [tree node] 
  "If tree is a map of child to parent links, and node is a node label, return
  the vector of nodes leading from node to the root in the tree."
  (loop [cur node res []] 
    ; (println "twig for tree " tree ", node " node)
    (if (not (contains? tree cur))
       res 
      (recur (tree cur) (conj res cur)))))


;
; Path algorithms.  
; All return a list of locations, or the empty list if 
; a path can't be found using the algorithm.
;

;
; Straight line seeking path seems to be working.
;
(defn 
  #^{:test (fn []
             (load-game partitionedmap)
             (assert (= 0 (count (straight-line [0 0] [0 0]))))
             (let [p (straight-line [0 0] [2 0])]
               (assert (= (list [2 0]) p)))
             (assert (empty? (straight-line [0 0] [1 2]))))}
  straight-line [loc goal]
  "Simplest thing that could possibly work; try to find a straight line
  path from loc to goal.  Return empty path if there is no such clear path."
  (loop [path [] pos loc]
    (if (= pos goal)
      (rest (conj path pos))
      (let [locs (filter #(passable? %) (map #(move-ant pos %) (direction pos goal)))]
        ;(println "pos " pos ", d " d)
        (if (empty? locs)
          nil
          (recur (conj path pos) (first locs)))))))

(defn- make-to-open-filter [open closed]
  #(and (not (contains? closed %)) (not (contains? open %))))


; 
; Greedy best first actually works now!
;
(defn greedy-best-first [loc goal estimator]
  "Return a list of locations which is a valid path from loc to goal, or nil
  if no such path exists.  estimator should be a function of 2 locations which
  gives an estimated cost for a path from one to the other."
  ;(binding [*out* *err*] (println "calling greedy-best-first with " loc goal))
  (loop [cur loc             ; the location we're handling right now
         open (priority-map) ; a priority map of loc -> priority, holds locations to check
         closed #{loc}       ; set of nodes we've already inspected
         parents {loc nil}   ; a map of (loc -> parent) entries to reconstruct path
         iters 0]
    (if (or (not cur) (> iters 200))
      nil
      (let [lastcur (get closed cur)
            to-open (filter (make-to-open-filter open closed) (neighbors cur))
            open    (into open (map list to-open (map #(estimator % goal) to-open)))
            parents (into parents (map hash-map to-open (repeat cur)))
            closed  (conj closed cur)]
        ;(binding [*out* *err*] (println "cur " cur ", open " open ", closed " closed "at iter " iters))
        (if (= cur goal)
          (let [path (rest (reverse (nodes-to-root parents cur)))]
            ;(binding [*out* *err*] (println "greedy-best-first " loc goal " found " path))
            path)
          (if (empty? open)
            nil
            (recur (first (peek open)) (pop open) closed parents (+ iters 1))))))))
               
;
; Found A* overview at http://www.policyalmanac.org/games/aStarTutorial.htm simple enough for me ;)
; Wikipiedia pseudo code didn't really make it clear to me somehow.
;
; Very detailed summary of path finding for games at 
; http://theory.stanford.edu/~amitp/GameProgramming
;
; There is a clojure example implementation of A* on the web at 
; http://clj-me.cgrand.net/2010/09/04/a-in-clojure/ for reference.
;
; Considered writing the priority queue also, but that seemed masochistic.
;

;
; FIX: still not working
;
(defn A* [loc goal estimate]
  (loop [open (priority-map) ; the priority-map of "open" locations, or ones we wish to process, where priority is the estimated total path length
         closed #{}          ; the set of nodes we have already processed
         parents {}          ; the relation of (child location) -> (parent location) recording the sequence of moves for a path
         cur loc             ; the location we're looking at right now
         lastcur nil         ; the location we came from
         g 0]                ; the # of moves from the starting location up to cur
    (if (= cur goal)
      (reverse (nodes-to-root parents cur))
      (let [newlocs (filter #(not (contains? closed %)) (neighbors cur))
            [nextcur nextg] (peek open)]
        (recur (into open (map vector newlocs (repeat (count newlocs) (+ g 1 (estimate cur goal)))))
               (into closed cur)
               (into parents {cur lastcur})
               nextcur
               cur
               nextg)))))



(ns paths
  (:use ants))
;  (:use clojure.data.priority-map))

(use 'clojure.data.priority-map)

; Some utilities for path finding

(defn 
  #^{:test (fn []
             (assert (= 4 (count (neighbors [1 1]))))
             (assert (= (into #{} (neighbors [1 1])) #{[0 1] [1 0] [1 2] [2 1]})))}
  neighbors [loc]
  "Return a list of neighboring locations which are valid moves.
  FIX: this should probably only take into account water, not ant locations,
  for the purposes of planning a route to a goal."
  (println "neighbors " loc)
  (for [d (filter #(valid-move? loc %) [:north :west :south :east])] (move-ant loc d)))

(defn twig [tree node] 
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

(defn 
  #^{:test (fn []
             (assert (= 0 (count (straight-line [0 0] [0 0]))))
             (let [p (straight-line [3 5] [3 6])]
               (assert (= (list [3 6]) p))))}
  straight-line [loc goal]
  "Simplest thing that could possibly work; try to find a straight line
  path from loc to goal.  Return empty path if there is no such clear path."
  (loop [path [] pos loc]
    (if (= pos goal)
      (rest (conj path pos))
      (let [d (filter #(valid-move? pos %) (direction pos goal))]
        (if d
          (recur (conj path pos) (move-ant pos (first (direction pos goal))))
          nil)))))

(defn greedy-best-first [loc goal estimator]
  "Return a list of locations which is a valid path from loc to goal, or nil
  if no such path exists.  estimator should be a function of 2 locations which
  gives an estimated cost for a path from one to the other."
  (loop [open (priority-map) ; a priority map of loc -> priority, holds locations to check
         closed {}           ; a map of          loc -> parent, entries indicated locations already checked
         cur loc             ; the location we're handling right now
         lastcur nil]        ; the last location we were handling
    (if (not cur)
      nil
      (let [to-open (filter #(not (contains? closed %)) (neighbors cur))
            open    (into open (map list to-open (map #(estimator % goal) to-open)))
            closed  (assoc closed cur lastcur)
            nextcur (first (peek open))]
        (println "in let, to-open " to-open)
        (println "  cur " cur ", nextcur " nextcur)
        (if (= cur goal)
          (rest (reverse (twig closed cur)))
          (recur open closed nextcur cur))))))               
               
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

(defn A* [loc goal estimate]
  (loop [open (priority-map) ; the priority-map of "open" locations, or ones we wish to process, where priority is the estimated total path length
         closed #{}          ; the set of nodes we have already processed
         parents {}          ; the relation of (child location) -> (parent location) recording the sequence of moves for a path
         cur loc             ; the location we're looking at right now
         lastcur nil         ; the location we came from
         g 0]                ; the # of moves from the starting location up to cur
    (if (= cur goal)
      (reverse (twig parents cur))
      (let [newlocs (filter #(not (contains? closed %)) (neighbors cur))
            [nextcur nextg] (peek open)]
        (recur (into open (map vector newlocs (repeat (count newlocs) (+ g 1 (estimate cur goal)))))
               (into closed cur)
               (into parents {cur lastcur})
               nextcur
               cur
               nextg)))))



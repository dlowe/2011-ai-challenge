(ns paths
  (:use ants))
;  (:use clojure.data.priority-map))

(use 'clojure.data.priority-map)

(defn straight-line [loc goal]
  (loop [path [] pos loc]
    (if (= pos goal)
      (rest (conj path pos))
      (recur (conj path pos) (move-ant pos (first (direction pos goal)))))))

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

(defn neighbors [loc]
  (for [d (filter #(valid-move? loc %) [:north :west :south :east])] (move-ant loc d)))

(defn twig [tree node] 
  "If tree is a map of child to parent links, and node is a node label, return
  the vector of nodes leading from node to the root in the tree."
  (loop [cur node res []] 
    (if (not (contains? tree cur)) 
      res 
      (recur (tree cur) (conj res cur)))))

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



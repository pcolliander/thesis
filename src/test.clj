
;; (def a (agent 0))
;; (dotimes [i 10] (.start (Thread. (fn [] (println i)))))
;; (dotimes [i 10] (send a inc) (prn @a))
;; (await a)
;; (prn "done")
;; (prn @a)

;; ------------------------
;; Upvote Function (Could also have used an agent.)
;; ------------------------
;; (def a (agent {:id {:counter 0}}))
;;
;; (defn upvote [id]
;;    (swap! a update-in [(keyword id) :counter] inc)
;; )
;;
;; (dotimes [_ 1000] (.start (Thread. (fn [] (upvote "id")))))
;; (Thread/sleep 1000)
;; (prn @a)


;; (def a (agent []))
;;
;; (dotimes [i 10] (send a conj "new val") (prn @a))
;; (await a)
;; (prn "done")
;; (prn @a)
;; (shutdown-agents)

;; ------------------------
;; Test with refs
;; ------------------------

;; (def remaining (ref {:id {:counter 0, :text "some value"} }))
;;
;; (def finished (ref {}))
;;
;; (prn @remaining)
;; (prn @finished)
;;
;; (dosync
;;   (alter finished assoc :id (get @remaining :id))
;;   (alter remaining dissoc remaining :id)
;; )
;;
;; (prn)
;; (prn @remaining)
;; (prn @finished)
;;
;; (dosync
;;   (alter remaining assoc :id (get @finished :id))
;;   (alter finished dissoc finished :id)
;; )
;;
;; (prn)
;; (prn @remaining)
;; (prn @finished)
;; ------------------------

(defn updateRefs [ns] 
  (prn "running transaction")
  (prn ns)
)


(def remaining (ref 1))
(def finished (ref 0))
(def a (agent {:1 {:counter 0} :2 {:counter 0} :3 {:counter 0} }))

;; add-watch a :key (fn [k r os ns] (prn ns)))
(add-watch a 
           :key (fn 
             [k r os ns] 
             (updateRefs ns)
             ;; (if (> (get-in ns [:1 :counter]) 3) 
               ;; (dosync 
                ;; (alter remaining dec)
                ;; (alter finished inc))
             ;; )
           ;; )
))

(defn upvote [id]
   ;; (send texts update-in [(keyword id) :counter] inc)
   (send a update-in [(keyword (str id)) :counter] inc)
)

;; (dotimes [i 10] (.start (Thread. (fn [] (upvote i)))))
;; (dotimes [i 0] (upvote 1))
;; (await a)
(prn @a)
(prn @remaining)
(prn @finished)



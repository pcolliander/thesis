
;; ----------------------------------------------------------------------------------------------------
;; Functions
;; ----------------------------------------------------------------------------------------------------

(def texts (agent {:id {:counter 0 :text ""}})) ;; <-------data-model

(add-watch texts 
 :key (fn [k r os ns] 
   (if (> (get-in ns [:1 :counter]) 3) 
     (dosync 
      (alter remaining dec)
      (alter finished inc))
   )
 )
)

(def remaining (ref 0))
(def finished (ref 0))

;; (def annotationSymbols ["CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBZ", "VBP", "VBD", "VBN" "VGB", "WDT", "WP", "WP$", "WRB", ".", ",", ":", "(", ")"  ] 

;; (def readInTexts [texts]
;;   ()
;; )

;; ------------------------
;; Read state (to send to web interface)
;; ------------------------
(defn getTexts [] 
  @texts
)

;; ------------------------
;; Validate (that it's got annotations on every word, and perhaps that it's still a dependency tree)
;; ------------------------
(defn validate
  []
  ()
)

;; ------------------------
;; Upvote Function 
;; ------------------------
(defn upvote [id]
   ;; (send texts update-in [(keyword id) :counter] inc)
   (send texts update-in [(keyword (str id)) :counter] inc)
)

;; ------------------------
;; Edit texts
;; ------------------------
(defn editText [id text]
  (send texts assoc-in [(keyword (str id)) :text] text)
)

;; ------------------------
;; Add texts 
;; ------------------------
(defn id-gen []
  (str (Math/round (rand 100000000000000)))
)

(defn addText [text]
  (send texts conj {(keyword (str (id-gen))) {:counter 0 :text text }} )
  ;; (send texts conj {(keyword (str id)) {:counter 0 :text text }} )
)

;; ------------------------
;; Main
;; ------------------------

;; ------------------------
;; Read in texts from (wrongly) pre-annotated source (the first time the app starts I guess)
;; ------------------------

(require '[clojure.string :as str])

(use 'clojure.java.io)
(with-open [rdr (reader "texts.txt")]
    (doseq [line (line-seq rdr)]
      (str/split line #" ")

      (addText line)))

;; (dotimes [i 10] (.start (Thread. (fn [] (addText "yo" i)))))
;; (dotimes [i 10] (addText "yo" i))
(await texts)
(prn seq @texts)

;; (dotimes [i 10] (.start (Thread. (fn [] (upvote i)))))
;; (dotimes [i 10] (upvote i))
(await texts)
;; (prn @texts)

;; (dotimes [i 10] (upvote i))
(await texts)
;; (prn @texts)

;; (getTexts)


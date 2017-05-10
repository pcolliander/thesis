(def annotationSymbols [ "ADJ", "ADP", "ADV", "AUX", "CONJ","DET", "NOUN", "NUM", "PART", "PRON", "PROPN", "PUNCT", "SCONJ", "VERB", "X" ])

(ns myns
    (:import 
      (java.util UUID)
      (marmot.morph.cmd Trainer)
      (marmot.morph.cmd Annotator)
    )
)

;; (def sentencesCounter (agent 0))
(def annotatedCounter (agent 0))
(def finishedCounter (agent 0))

;; Main refs (are updated in transactions)
;; (def sentences (ref {:1 {:text ""} :2 {:text ""} :3 {:text ""}}) )
(def annotatedSentences (ref {}))
(def corroboratedSentences (ref {}))

;; -----------------
;; Utility Functions
;; -----------------
(defn pm [] 
  ;; (prn "sentencesCounter " @sentencesCounter)
  (prn "annotatedCounter " @annotatedCounter)
  (prn "finished: " @finishedCounter)
  ;; (prn "sentences: " @sentences)
  (prn "annotatedSentences " @annotatedSentences)
  (prn "corroboratedSentences: " @corroboratedSentences)
)

;; ------
;; Upvote
;; ------
(defn upvote [id] 
  (dosync
    (if (contains? @annotatedSentences (keyword (str id)))
      (alter annotatedSentences update-in [(keyword (str id)) :counter] inc)
      "false"
    )
  )
  
  (dosync 
    (if (contains? @annotatedSentences (keyword (str id)))
      (do
        (if (> (get-in @annotatedSentences [(keyword (str id)) :counter]) 3)
          (do 
            (alter corroboratedSentences conj {(keyword (str id)) (get @annotatedSentences (keyword (str id)))})
            (alter annotatedSentences dissoc annotatedSentences (keyword (str id)))
            (send annotatedCounter dec)
            (send finishedCounter inc))
        ;; else? "counter not big enough"
        )
      )
      ;; else? "false 2"
    )
  )
)
;; -------------
;; Add sentences
;; -------------
;; (defn addSentence [text]
;;   (dosync 
;;     (alter sentences conj {(keyword (id-gen)) {:text text}})
;;     (send sentencesCounter inc)
;;   )
;; )

;; ----------------------
;; Add annotatedSentences
;; ----------------------
(defn addAnnotated [text]
  (dosync 
    (alter annotatedSentences conj {(keyword (UUID/randomUUID)) {:counter 0, :text text}})
    (send annotatedCounter inc)
  )
)

;; --------------------------------------------------------------------------------------------------
;; Edit sentences (adds a counter to an individual map, and moves from sentences to annotatedCounter)
;; --------------------------------------------------------------------------------------------------
;; (defn edit [id text]
;;   (dosync
;;     (if (contains? @sentences (keyword (str id)))
;;       (do
;;         (alter annotatedSentences conj {(keyword (str id)) {:counter 0, :text text }})
;;         (alter sentences dissoc sentences (keyword (str id)))
;;         (send sentencesCounter dec) ;; will only be sent once transaction is commited
;;         (send annotatedCounter inc) ;; will only be sent once transaction is commited
;;       )
;;     )
;;   )
;; )

;; -----------------
;; Get functions (sentences in different states)
;; -----------------
;; (defn getAnnotated []
;;   (@annotatedSentences)
;; )
;;
;; (defn getCorroborated []
;;   (@corroboratedSentences)
;; )
;;
;; (defn getSentences []
;;   (@sentences)
;; )

(prn "hej")


;; -----------
;; TODO 
;; -----------

;; -----------
;; Train Model
;; -----------
;; (defn trainModel []
;;   ()
;; )

;; -----------
;; Output Textfile
;; -----------
;; (defn outputTxtFile []
;;   ()
;; )

;; -----------
;; Annotate with the trained Model
;; -----------
(defn trainModel []
  (
   (Trainer/main (into-array String ["-train-file","form-index=1,tag-index=4,en-ud-train.conll", "-tag-morph", "false", "-model-file", "fromClojure.marmot"]))
  )
)

;; ---------
;; Run Model
;; ---------
(defn runModel []
  ( 
   (Annotator/main (into-array String ["-train-file","form-index=1,tag-index=4,en-ud-train.conll", "-tag-morph", "false", "-model-file", "fromClojure.marmot"]))
  )
)


;; ----
;; Main
;; ----

;; -----------------------------------------------------------------------------------------
;; Read in texts from annotated source (the first time the app starts I guess)
;; -----------------------------------------------------------------------------------------

;; (require '[clojure.string :as str])
;;
;; (use 'clojure.java.io)
;; (with-open [rdr (reader "lib/UD_English/en-ud-dev.conllu")]
;;   (doseq [line (line-seq rdr)]
;;     (prn line)
;;     ;; (str/split line #" ")
;;     ;; (addText line))
;;     )
;; )

;; (dotimes [i 10] (.start (Thread. (fn [] (upvote i)))))
;; (dotimes [i 0] (upvote 1))
;; (await a)



